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

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

admin.initializeApp();

const db = admin.firestore();

setGlobalOptions({
  maxInstances: 10,
  region: "asia-south1",
});

/*
============================================================
HELPER FUNCTIONS
============================================================
*/

/**
 * Check whether a device status is valid.
 *
 * @param {string} status Device status to validate.
 * @return {boolean} True if the status is valid.
 */

/**
 * Create a usage log for a device.
 *
 * @param {string} deviceId Firestore device document ID.
 * @param {Object} device Device data.
 * @param {string} action Action performed on the device.
 * @return {Promise<void>} Resolves when the usage log is created.
 */
async function createUsageLog(deviceId, device, action) {
  await db.collection("usageLogs").add({
    deviceId: deviceId,
    deviceName: device.name || "Unknown Device",
    deviceType: device.type || "UNKNOWN",
    roomId: device.roomId || null,
    floorId: device.floorId || null,
    action: action,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  });
}


/**
 * Create an alert.
 */
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
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  });
}


/*
============================================================
1. DEVICE UPDATE LISTENER
============================================================

Triggered whenever a device document changes.

Responsibilities:

- Detect ON/OFF changes
- Record timestamps
- Create usage logs
- Support external simulator updates
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
      DEVICE STATUS CHANGE
      --------------------------------------------------------
      */

      if (before.status !== after.status) {
        logger.info(
            `Device ${deviceId}: ${before.status} -> ${after.status}`,
        );

        /*
        Device turned ON
        */

        if (after.status === "ON") {
          await db.collection("devices")
              .doc(deviceId)
              .update({
                turnedOnAt:
                  admin.firestore.FieldValue.serverTimestamp(),

                lastStatusChange:
                  admin.firestore.FieldValue.serverTimestamp(),

                safetyCutoff: false,
              });

          await createUsageLog(
              deviceId,
              after,
              "ON",
          );
        }

        /*
        Device turned OFF
        */

        if (after.status === "OFF") {
          await db.collection("devices")
              .doc(deviceId)
              .update({
                turnedOffAt:
                  admin.firestore.FieldValue.serverTimestamp(),

                lastStatusChange:
                  admin.firestore.FieldValue.serverTimestamp(),
              });

          await createUsageLog(
              deviceId,
              after,
              "OFF",
          );
        }

        /*
        ERROR state
        */

        if (after.status === "ERROR") {
          await createAlert({
            deviceId: deviceId,
            deviceName: after.name,
            type: "DEVICE_ERROR",
            severity: "MEDIUM",
            message:
              `${after.name || "Device"} reported an ERROR state.`,
          });
        }

        /*
        DISCONNECTED state
        */

        if (after.status === "DISCONNECTED") {
          await createAlert({
            deviceId: deviceId,
            deviceName: after.name,
            type: "DEVICE_DISCONNECTED",
            severity: "HIGH",
            message:
              `${after.name || "Device"} is disconnected.`,
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
        JSON.stringify(before.switches) !==
        JSON.stringify(after.switches)
      ) {
        const beforeSwitches = before.switches || {};
        const afterSwitches = after.switches || {};

        for (const switchId of Object.keys(afterSwitches)) {
          const oldStatus =
            beforeSwitches[switchId]?.status;

          const newStatus =
            afterSwitches[switchId]?.status;

          if (oldStatus !== newStatus) {
            await createUsageLog(
                deviceId,
                {
                  ...after,
                  name:
                    `${after.name || "Multi Switch"} - ` +
                    `${afterSwitches[switchId].name ||
                    switchId}`,
                },
                newStatus,
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

Runs every minute.

Checks devices such as:

- Iron
- Heater
- High-power appliance

If maximum ON duration is exceeded:

ON -> OFF

and an alert is generated.
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
        logger.info(
            "No safety-critical devices currently ON",
        );

        return null;
      }

      const now = Date.now();

      const batch = db.batch();

      for (const document of snapshot.docs) {
        const device = document.data();

        if (!device.turnedOnAt) {
          continue;
        }

        const turnedOnAt =
          device.turnedOnAt.toDate().getTime();

        const durationSeconds =
          (now - turnedOnAt) / 1000;

        const maxDuration =
          Number(device.maxOnDuration || 0);

        if (
          maxDuration > 0 &&
          durationSeconds >= maxDuration
        ) {
          logger.warn(
              `Safety cutoff activated for ${document.id}`,
          );

          /*
          Turn device OFF
          */

          batch.update(document.ref, {
            status: "OFF",

            safetyCutoff: true,

            safetyCutoffAt:
              admin.firestore.FieldValue.serverTimestamp(),

            lastStatusChange:
              admin.firestore.FieldValue.serverTimestamp(),

            turnedOffAt:
              admin.firestore.FieldValue.serverTimestamp(),
          });

          /*
          Create alert
          */

          const alertRef =
            db.collection("alerts").doc();

          batch.set(alertRef, {
            deviceId: document.id,

            deviceName:
              device.name || "Unknown Device",

            type: "SAFETY_CUTOFF",

            severity: "HIGH",

            message:
              `${device.name || "Device"} was automatically ` +
              `turned OFF because the maximum ON ` +
              `duration was exceeded.`,

            timestamp:
              admin.firestore.FieldValue.serverTimestamp(),

            acknowledged: false,
          });

          /*
          Create usage log
          */

          const usageRef =
            db.collection("usageLogs").doc();

          batch.set(usageRef, {
            deviceId: document.id,

            deviceName:
              device.name || "Unknown Device",

            deviceType:
              device.type || "UNKNOWN",

            roomId:
              device.roomId || null,

            floorId:
              device.floorId || null,

            action: "SAFETY_CUTOFF",

            timestamp:
              admin.firestore.FieldValue.serverTimestamp(),
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

Example:

schedule:
{
    enabled: true,
    onTime: "18:00",
    offTime: "23:00"
}
*/

exports.lightScheduleWorker = onSchedule(
    {
      schedule: "every 1 minutes",
      timeZone: "Asia/Colombo",
    },
    async () => {
      logger.info(
          "Running light schedule worker",
      );

      const snapshot = await db.collection("devices")
          .where("type", "==", "LIGHT")
          .where("schedule.enabled", "==", true)
          .get();

      if (snapshot.empty) {
        return null;
      }

      /*
      Current Sri Lankan time
      */

      const now = new Date();

      const currentTime =
        now.toLocaleTimeString(
            "en-GB",
            {
              timeZone: "Asia/Colombo",
              hour: "2-digit",
              minute: "2-digit",
              hour12: false,
            },
        );

      logger.info(
          `Current time: ${currentTime}`,
      );

      const batch = db.batch();

      for (const document of snapshot.docs) {
        const device = document.data();

        const schedule = device.schedule;

        if (!schedule) {
          continue;
        }

        /*
        Turn ON
        */

        if (
          schedule.onTime === currentTime &&
          device.status !== "ON"
        ) {
          batch.update(document.ref, {
            status: "ON",

            scheduledAction: "ON",

            lastStatusChange:
              admin.firestore.FieldValue.serverTimestamp(),
          });

          logger.info(
              `Scheduled ON: ${document.id}`,
          );
        }

        /*
        Turn OFF
        */

        if (
          schedule.offTime === currentTime &&
          device.status !== "OFF"
        ) {
          batch.update(document.ref, {
            status: "OFF",

            scheduledAction: "OFF",

            lastStatusChange:
              admin.firestore.FieldValue.serverTimestamp(),
          });

          logger.info(
              `Scheduled OFF: ${document.id}`,
          );
        }
      }

      await batch.commit();

      return null;
    },
);


/*
============================================================
4. MOBILE APP - TOGGLE DEVICE
============================================================

Android calls:

toggleDevice({
    deviceId: "device001",
    status: "ON"
})
*/

exports.toggleDevice = onCall(
    async (request) => {
      const data = request.data || {};

      const deviceId = data.deviceId;
      const newStatus = data.status;

      if (!deviceId) {
        throw new HttpsError(
            "invalid-argument",
            "deviceId is required",
        );
      }

      if (
        newStatus !== "ON" &&
        newStatus !== "OFF"
      ) {
        throw new HttpsError(
            "invalid-argument",
            "status must be ON or OFF",
        );
      }

      const deviceRef =
        db.collection("devices").doc(deviceId);

      const deviceSnapshot =
        await deviceRef.get();

      if (!deviceSnapshot.exists) {
        throw new HttpsError(
            "not-found",
            "Device does not exist",
        );
      }

      const device =
        deviceSnapshot.data();

      /*
      Prevent manually turning off
      disconnected device etc.
      */

      if (
        device.status === "DISCONNECTED" &&
        newStatus === "ON"
      ) {
        throw new HttpsError(
            "failed-precondition",
            "Device is disconnected",
        );
      }

      await deviceRef.update({
        status: newStatus,

        source: "mobile",

        lastStatusChange:
          admin.firestore.FieldValue.serverTimestamp(),
      });

      return {
        success: true,

        deviceId: deviceId,

        status: newStatus,
      };
    },
);


/*
============================================================
5. MULTI-SWITCH CONTROL
============================================================

Controls one switch inside a multi-switch unit.
*/

exports.toggleSwitch = onCall(
    async (request) => {
      const data = request.data || {};

      const deviceId = data.deviceId;
      const switchId = data.switchId;
      const newStatus = data.status;

      if (!deviceId || !switchId) {
        throw new HttpsError(
            "invalid-argument",
            "deviceId and switchId are required",
        );
      }

      if (
        newStatus !== "ON" &&
        newStatus !== "OFF"
      ) {
        throw new HttpsError(
            "invalid-argument",
            "status must be ON or OFF",
        );
      }

      const deviceRef =
        db.collection("devices").doc(deviceId);

      const deviceSnapshot =
        await deviceRef.get();

      if (!deviceSnapshot.exists) {
        throw new HttpsError(
            "not-found",
            "Device does not exist",
        );
      }

      const device =
        deviceSnapshot.data();

      if (device.type !== "MULTI_SWITCH") {
        throw new HttpsError(
            "failed-precondition",
            "Device is not a multi-switch unit",
        );
      }

      const switches =
        device.switches || {};

      if (!switches[switchId]) {
        throw new HttpsError(
            "not-found",
            "Switch does not exist",
        );
      }

      switches[switchId].status =
        newStatus;

      await deviceRef.update({
        switches: switches,

        lastStatusChange:
          admin.firestore.FieldValue.serverTimestamp(),

        source: "mobile",
      });

      return {
        success: true,

        deviceId: deviceId,

        switchId: switchId,

        status: newStatus,
      };
    },
);


/*
============================================================
6. ACKNOWLEDGE ALERT
============================================================
*/

exports.acknowledgeAlert = onCall(
    async (request) => {
      const data = request.data || {};

      const alertId = data.alertId;

      if (!alertId) {
        throw new HttpsError(
            "invalid-argument",
            "alertId is required",
        );
      }

      const alertRef =
        db.collection("alerts").doc(alertId);

      const alertSnapshot =
        await alertRef.get();

      if (!alertSnapshot.exists) {
        throw new HttpsError(
            "not-found",
            "Alert does not exist",
        );
      }

      await alertRef.update({
        acknowledged: true,

        acknowledgedAt:
          admin.firestore.FieldValue.serverTimestamp(),
      });

      return {
        success: true,

        alertId: alertId,
      };
    },
);


/*
============================================================
7. GET DEVICE USAGE
============================================================
*/

exports.getDeviceUsage = onCall(
    async (request) => {
      const data = request.data || {};

      const deviceId = data.deviceId;

      if (!deviceId) {
        throw new HttpsError(
            "invalid-argument",
            "deviceId is required",
        );
      }

      const snapshot =
        await db.collection("usageLogs")
            .where("deviceId", "==", deviceId)
            .orderBy("timestamp", "desc")
            .limit(100)
            .get();

      const logs = [];

      snapshot.forEach((doc) => {
        logs.push({
          id: doc.id,
          ...doc.data(),
        });
      });

      return {
        success: true,
        deviceId: deviceId,
        logs: logs,
      };
    },
);


/*
============================================================
8. GET ACTIVE ALERTS
============================================================
*/

exports.getActiveAlerts = onCall(
    async () => {
      const snapshot =
        await db.collection("alerts")
            .where("acknowledged", "==", false)
            .orderBy("timestamp", "desc")
            .limit(50)
            .get();

      const alerts = [];

      snapshot.forEach((doc) => {
        alerts.push({
          id: doc.id,
          ...doc.data(),
        });
      });

      return {
        success: true,
        alerts: alerts,
      };
    },
);
