const {setGlobalOptions} = require("firebase-functions");
const {
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const {
  onSchedule,
} = require("firebase-functions/v2/scheduler");
const {
  onCall,
  HttpsError,
} = require("firebase-functions/v2/https");

const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const logger = require("firebase-functions/logger");

initializeApp();

const db = getFirestore();

setGlobalOptions({
  maxInstances: 10,
  region: "asia-south1",
});

/*
============================================================
HELPER FUNCTIONS
============================================================
*/

async function createUsageLog(deviceId, device, action) {
  await db.collection("usageLogs").add({
    deviceId: deviceId,
    deviceName: device.name || "Unknown Device",
    deviceType: device.type || "UNKNOWN",
    roomId: device.roomId || null,
    floorId: device.floorId || null,
    action: action,
    timestamp: FieldValue.serverTimestamp(),
  });
}

async function createAlert({
  deviceId,
  deviceName,
  type,
  severity,
  message,
}) {
  await db.collection("alerts").add({
    deviceId: deviceId,
    deviceName: deviceName || "Unknown Device",
    type: type,
    severity: severity,
    message: message,
    acknowledged: false,
    timestamp: FieldValue.serverTimestamp(),
  });
}

/*
============================================================
1. DEVICE UPDATE LISTENER
============================================================
*/

exports.onDeviceUpdated = onDocumentUpdated(
    "devices/{deviceId}",
    async (event) => {
      const before = event.data.before.data();
      const after = event.data.after.data();
      const deviceId = event.params.deviceId;

      if (!before || !after) {
        return null;
      }

      logger.info(`Device updated: ${deviceId}`);

      /*
      --------------------------------------------------------
      DEVICE STATUS CHANGE (Recursive Loop Protection)
      --------------------------------------------------------
      */
      if (before.status !== after.status) {
        logger.info(
            `Device ${deviceId}: ${before.status} -> ${after.status}`,
        );

        const updates = {};

        if (after.status === "ON" && !after.turnedOnAt) {
          updates.turnedOnAt = FieldValue.serverTimestamp();
          updates.lastStatusChange = FieldValue.serverTimestamp();
          updates.safetyCutoff = false;
          await createUsageLog(deviceId, after, "ON");
        }

        if (after.status === "OFF" && !after.turnedOffAt) {
          updates.turnedOffAt = FieldValue.serverTimestamp();
          updates.lastStatusChange = FieldValue.serverTimestamp();
          await createUsageLog(deviceId, after, "OFF");
        }

        if (Object.keys(updates).length > 0) {
          await db.collection("devices").doc(deviceId).update(updates);
        }

        if (after.status === "ERROR") {
          await createAlert({
            deviceId: deviceId,
            deviceName: after.name,
            type: "DEVICE_ERROR",
            severity: "MEDIUM",
            message: `${after.name || "Device"} reported an ERROR state.`,
          });
        }

        if (after.status === "DISCONNECTED") {
          await createAlert({
            deviceId: deviceId,
            deviceName: after.name,
            type: "DEVICE_DISCONNECTED",
            severity: "HIGH",
            message: `${after.name || "Device"} is disconnected.`,
          });
        }
      }

      /*
      --------------------------------------------------------
      MULTI-SWITCH CHANGES
      --------------------------------------------------------
      */
      if (
        after.type === "MULTI_SWITCH" &&
        JSON.stringify(before.subSwitches) !== JSON.stringify(after.subSwitches)
      ) {
        const beforeSwitches = before.subSwitches || [];
        const afterSwitches = after.subSwitches || [];

        for (const newSw of afterSwitches) {
          const oldSw = beforeSwitches.find((s) => s.id === newSw.id);
          if (!oldSw || oldSw.status !== newSw.status) {
            await createUsageLog(
                deviceId,
                {
                  ...after,
                  name: `${after.name || "Multi Switch"} - ${newSw.name || newSw.id}`,
                },
                newSw.status,
            );
          }
        }
      }

      return null;
    },
);

/*
============================================================
2. SAFETY CUTOFF WORKER
============================================================
*/

exports.safetyCutoffWorker = onSchedule(
    {
      schedule: "every 1 minutes",
      timeZone: "Asia/Colombo",
    },
    async () => {
      logger.info("Running safety cutoff worker");

      const snapshot = await db.collection("devices")
          .where("safetyCritical", "==", true)
          .where("status", "==", "ON")
          .get();

      if (snapshot.empty) {
        return null;
      }

      const now = Date.now();
      const batch = db.batch();

      for (const document of snapshot.docs) {
        const device = document.data();

        if (!device.turnedOnAt) {
          continue;
        }

        const turnedOnAt = device.turnedOnAt.toDate().getTime();
        const durationSeconds = (now - turnedOnAt) / 1000;
        const maxDuration = Number(
            device.maxOnDuration || device.maxDuration || 0,
        );

        if (maxDuration > 0 && durationSeconds >= maxDuration) {
          logger.warn(`Safety cutoff activated for ${document.id}`);

          batch.update(document.ref, {
            status: "OFF",
            safetyCutoff: true,
            safetyCutoffAt: FieldValue.serverTimestamp(),
            lastStatusChange: FieldValue.serverTimestamp(),
            turnedOffAt: FieldValue.serverTimestamp(),
          });

          const alertRef = db.collection("alerts").doc();
          batch.set(alertRef, {
            deviceId: document.id,
            deviceName: device.name || "Unknown Device",
            type: "SAFETY_CUTOFF",
            severity: "HIGH",
            message:
              `${device.name || "Device"} was automatically ` +
              `turned OFF because maximum ON duration was exceeded.`,
            timestamp: FieldValue.serverTimestamp(),
            acknowledged: false,
          });

          const usageRef = db.collection("usageLogs").doc();
          batch.set(usageRef, {
            deviceId: document.id,
            deviceName: device.name || "Unknown Device",
            deviceType: device.type || "UNKNOWN",
            roomId: device.roomId || null,
            floorId: device.floorId || null,
            action: "SAFETY_CUTOFF",
            timestamp: FieldValue.serverTimestamp(),
          });
        }
      }

      await batch.commit();
      return null;
    },
);

/*
============================================================
3. AUTOMATIC LIGHT SCHEDULER
============================================================
*/

exports.lightScheduleWorker = onSchedule(
    {
      schedule: "every 1 minutes",
      timeZone: "Asia/Colombo",
    },
    async () => {
      logger.info("Running light schedule worker");

      const snapshot = await db.collection("devices")
          .where("schedule.enabled", "==", true)
          .get();

      if (snapshot.empty) {
        return null;
      }

      const now = new Date();
      const currentTime = now.toLocaleTimeString("en-GB", {
        timeZone: "Asia/Colombo",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      });

      const batch = db.batch();

      for (const document of snapshot.docs) {
        const device = document.data();
        const schedule = device.schedule;

        if (!schedule) continue;

        if (schedule.onTime === currentTime && device.status !== "ON") {
          batch.update(document.ref, {
            status: "ON",
            scheduledAction: "ON",
            lastStatusChange: FieldValue.serverTimestamp(),
          });
        }

        if (schedule.offTime === currentTime && device.status !== "OFF") {
          batch.update(document.ref, {
            status: "OFF",
            scheduledAction: "OFF",
            lastStatusChange: FieldValue.serverTimestamp(),
          });
        }
      }

      await batch.commit();
      return null;
    },
);

/*
============================================================
4. MOBILE APP CALLABLES
============================================================
*/

exports.toggleDevice = onCall(async (request) => {
  const data = request.data || {};
  const {deviceId, status: newStatus} = data;

  if (!deviceId || (newStatus !== "ON" && newStatus !== "OFF")) {
    throw new HttpsError("invalid-argument", "Valid deviceId and status required");
  }

  const deviceRef = db.collection("devices").doc(deviceId);
  const deviceSnapshot = await deviceRef.get();

  if (!deviceSnapshot.exists) {
    throw new HttpsError("not-found", "Device does not exist");
  }

  if (deviceSnapshot.data().status === "DISCONNECTED" && newStatus === "ON") {
    throw new HttpsError("failed-precondition", "Device is disconnected");
  }

  await deviceRef.update({
    status: newStatus,
    source: "mobile",
    lastStatusChange: FieldValue.serverTimestamp(),
  });

  return {success: true, deviceId, status: newStatus};
});

exports.toggleSwitch = onCall(async (request) => {
  const data = request.data || {};
  const {deviceId, switchId, status: newStatus} = data;

  if (!deviceId || switchId === undefined || (newStatus !== "ON" && newStatus !== "OFF")) {
    throw new HttpsError("invalid-argument", "Valid arguments required");
  }

  const deviceRef = db.collection("devices").doc(deviceId);
  const deviceSnapshot = await deviceRef.get();

  if (!deviceSnapshot.exists) {
    throw new HttpsError("not-found", "Device does not exist");
  }

  const device = deviceSnapshot.data();
  const subSwitches = device.subSwitches || [];
  const targetIndex = subSwitches.findIndex((s) => s.id === switchId);

  if (targetIndex === -1) {
    throw new HttpsError("not-found", "Switch does not exist");
  }

  subSwitches[targetIndex].status = newStatus;

  await deviceRef.update({
    subSwitches: subSwitches,
    lastStatusChange: FieldValue.serverTimestamp(),
    source: "mobile",
  });

  return {success: true, deviceId, switchId, status: newStatus};
});

exports.acknowledgeAlert = onCall(async (request) => {
  const alertId = request.data?.alertId;
  if (!alertId) throw new HttpsError("invalid-argument", "alertId required");

  await db.collection("alerts").doc(alertId).update({
    acknowledged: true,
    acknowledgedAt: FieldValue.serverTimestamp(),
  });

  return {success: true, alertId};
});

exports.getDeviceUsage = onCall(async (request) => {
  const deviceId = request.data?.deviceId;
  if (!deviceId) throw new HttpsError("invalid-argument", "deviceId required");

  const snapshot = await db.collection("usageLogs")
      .where("deviceId", "==", deviceId)
      .orderBy("timestamp", "desc")
      .limit(100)
      .get();

  const logs = snapshot.docs.map((doc) => ({id: doc.id, ...doc.data()}));
  return {success: true, deviceId, logs};
});

exports.getActiveAlerts = onCall(async () => {
  const snapshot = await db.collection("alerts")
      .where("acknowledged", "==", false)
      .orderBy("timestamp", "desc")
      .limit(50)
      .get();

  const alerts = snapshot.docs.map((doc) => ({id: doc.id, ...doc.data()}));
  return {success: true, alerts};
});