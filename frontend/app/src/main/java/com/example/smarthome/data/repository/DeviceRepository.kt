package com.example.smarthome.data.repository

import android.util.Log
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.SubSwitch
import com.example.smarthome.data.model.UsageLog
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class DeviceRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val devicesCollection = firestore.collection("devices")
    private val roomsCollection = firestore.collection("rooms")

    private var roomsListener: ListenerRegistration? = null
    private var devicesListener: ListenerRegistration? = null
    private var currentRoomMap = emptyMap<String, String>()

    fun observeDevices(
        onResult: (List<Device>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        stopObserving()

        // 1. Listen for room changes
        roomsListener = roomsCollection.addSnapshotListener { roomsSnapshot, roomsError ->
            if (roomsError != null) {
                Log.e("SmartHome_Repo", "Rooms fetch warning: ${roomsError.message}")
            }
            currentRoomMap = roomsSnapshot?.documents?.associate { doc ->
                doc.id to (doc.getString("name") ?: doc.id)
            } ?: emptyMap()
        }

        // 2. Continuous real-time document listener
        devicesListener = devicesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SmartHome_Repo", "Firestore error fetching devices: ${error.message}", error)
                onError(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                Log.d("SmartHome_Repo", "Raw document count in Firestore: ${snapshot.documents.size}")

                val devices = snapshot.documents.mapNotNull { doc ->
                    try {
                        val device = doc.toObject(Device::class.java)
                        device?.apply {
                            id = doc.id
                            // Handle floorId vs floorID casing inconsistency in Firestore
                            if (this.floorId.isBlank()) {
                                this.floorId = (doc.get("floorId") ?: doc.get("floorID"))?.toString() ?: ""
                            }
                            
                            // Ensure row and column are parsed correctly
                            val rawRow = doc.get("row")
                            val rawCol = doc.get("column")
                            this.row = when(rawRow) {
                                is Number -> rawRow.toInt()
                                is String -> rawRow.toIntOrNull() ?: 0
                                else -> 0
                            }
                            this.column = when(rawCol) {
                                is Number -> rawCol.toInt()
                                is String -> rawCol.toIntOrNull() ?: 0
                                else -> 0
                            }

                            roomName = currentRoomMap[roomId] ?: roomId
                        }
                    } catch (e: Exception) {
                        Log.e("SmartHome_Repo", "FAILED TO PARSE DOC '${doc.id}': ${e.localizedMessage}", e)
                        null
                    }
                }

                Log.d("SmartHome_Repo", "Successfully parsed ${devices.size} devices")
                onResult(devices)
            } else {
                onResult(emptyList())
            }
        }

        return devicesListener
    }

    fun observeDevicesByFloor(
        floorId: String,
        level: Int? = null,
        onResult: (List<Device>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        if (floorId.isBlank() || floorId.equals("All Floors", ignoreCase = true) || floorId.equals("ALL", ignoreCase = true)) {
            return observeDevices(onResult, onError)
        }

        stopObserving()

        roomsListener = roomsCollection.addSnapshotListener { roomsSnapshot, _ ->
            currentRoomMap = roomsSnapshot?.documents?.associate { doc ->
                doc.id to (doc.getString("name") ?: doc.id)
            } ?: emptyMap()
        }

        devicesListener = devicesCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SmartHome_Repo", "Error fetching devices for floor $floorId: ${error.message}")
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val targetDocId = floorId.trim()
                    
                    val devices = snapshot.documents.mapNotNull { doc ->
                        try {
                            // 1. Handle floorId / floorID casing and type mismatch
                            val rawFloorVal = doc.get("floorId") ?: doc.get("floorID")
                            val docFloorId = when (rawFloorVal) {
                                is String -> rawFloorVal
                                is Number -> rawFloorVal.toInt().toString()
                                else -> rawFloorVal?.toString() ?: ""
                            }.trim()

                            // 2. Strict, Precise Filtering
                            val isMatch = if (level != null) {
                                // If level is provided, match strictly against known level formats
                                // e.g. Level 0 -> "floor001" or "1"
                                // e.g. Level 1 -> "floor002" or "2"
                                val displayLevel = level + 1
                                val floorStr = "floor${displayLevel.toString().padStart(3, '0')}"
                                val shortId = displayLevel.toString()
                                
                                docFloorId == targetDocId || docFloorId == floorStr || docFloorId == shortId
                            } else {
                                // Fallback to strict Document ID match
                                docFloorId == targetDocId
                            }

                            if (isMatch) {
                                val device = doc.toObject(Device::class.java)
                                device?.apply {
                                    id = doc.id
                                    this.floorId = docFloorId
                                    roomName = currentRoomMap[roomId] ?: roomId
                                    
                                    // 3. Ensure row and column are parsed correctly
                                    val rawRow = doc.get("row")
                                    val rawCol = doc.get("column")
                                    
                                    this.row = when(rawRow) {
                                        is Number -> rawRow.toInt()
                                        is String -> rawRow.toIntOrNull() ?: 0
                                        else -> 0
                                    }
                                    
                                    this.column = when(rawCol) {
                                        is Number -> rawCol.toInt()
                                        is String -> rawCol.toIntOrNull() ?: 0
                                        else -> 0
                                    }
                                }
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.e("SmartHome_Repo", "FAILED TO PARSE DOC '${doc.id}': ${e.localizedMessage}")
                            null
                        }
                    }
                    onResult(devices)
                } else {
                    onResult(emptyList())
                }
            }

        return devicesListener
    }

    fun stopObserving() {
        roomsListener?.remove()
        devicesListener?.remove()
        roomsListener = null
        devicesListener = null
    }

    fun updateDeviceStatus(
        deviceId: String,
        status: DeviceStatus,
        onComplete: (Boolean) -> Unit
    ) {
        if (deviceId.isBlank()) {
            onComplete(false)
            return
        }

        val updates = mutableMapOf<String, Any>(
            "status" to status.name,
            "lastStatusChange" to FieldValue.serverTimestamp()
        )

        if (status == DeviceStatus.ON) {
            updates["turnedOnAt"] = FieldValue.serverTimestamp()
            updates["safetyCutoff"] = false
        } else if (status == DeviceStatus.OFF) {
            updates["turnedOffAt"] = FieldValue.serverTimestamp()
        }

        devicesCollection.document(deviceId)
            .update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdate", "Repository: Failed to update status for $deviceId", e)
                onComplete(false)
            }
    }

    fun updateSubSwitchStatus(
        deviceId: String,
        subSwitches: List<SubSwitch>,
        onComplete: (Boolean) -> Unit
    ) {
        if (deviceId.isBlank()) {
            onComplete(false)
            return
        }

        val formattedSwitches = subSwitches.map {
            mapOf(
                "id" to it.id,
                "name" to it.name,
                "status" to it.status.name
            )
        }

        devicesCollection.document(deviceId)
            .update("subSwitches", formattedSwitches)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdate", "Repository: Failed to update subswitches for $deviceId", e)
                onComplete(false)
            }
    }

    fun updateDeviceSchedule(
        deviceId: String,
        autoScheduleEnabled: Boolean,
        startTime: String?,
        endTime: String?,
        onComplete: (Boolean) -> Unit
    ) {
        if (deviceId.isBlank()) {
            onComplete(false)
            return
        }

        val scheduleObj = mapOf(
            "enabled" to autoScheduleEnabled,
            "onTime" to startTime,
            "offTime" to endTime
        )

        devicesCollection.document(deviceId)
            .update("schedule", scheduleObj)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdate", "Repository: Failed to update schedule for $deviceId", e)
                onComplete(false)
            }
    }

    fun updateSafetyDuration(
        deviceId: String,
        maxMinutes: Int,
        onComplete: (Boolean) -> Unit
    ) {
        if (deviceId.isBlank()) {
            onComplete(false)
            return
        }

        val seconds = maxMinutes * 60L
        val updates = mapOf(
            "maxDuration" to seconds,
            "maxOnDuration" to seconds,
            "maxOnDurationMinutes" to maxMinutes
        )

        devicesCollection.document(deviceId)
            .update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("FirebaseUpdate", "Repository: Failed to update status for $deviceId", e)
                onComplete(false)
            }
    }

    fun fetchAllUsageLogs(
        onResult: (List<UsageLog>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("usageLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val logs = snapshot.toObjects(UsageLog::class.java)
                onResult(logs)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun fetchDeviceUsage(
        deviceId: String,
        onResult: (List<UsageLog>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("usageLogs")
            .whereEqualTo("deviceId", deviceId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val logs = snapshot.toObjects(UsageLog::class.java)
                onResult(logs)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun logSafetyEvent(deviceId: String, log: UsageLog) {
        val docRef = firestore.collection("usageLogs").document()
        val finalLog = log.copy(
            id = docRef.id,
            deviceId = deviceId,
            timestamp = null
        )
        docRef.set(finalLog)
    }
}