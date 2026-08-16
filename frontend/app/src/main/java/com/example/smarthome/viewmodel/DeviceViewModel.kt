package com.example.smarthome.viewmodel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

import android.util.Log

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.UsageLog
import com.example.smarthome.data.model.getWattage
import com.example.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DeviceViewModel : ViewModel() {

    private val repository = DeviceRepository()
    private val safetyCutoffJobs = mutableMapOf<String, Job>()

    var devices = mutableStateOf<List<Device>>(emptyList())
        private set

    var usageLogs = mutableStateOf<List<UsageLog>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    init {
        observeAllDevices()
        startPeriodicScheduleCheck()
    }

    private fun startPeriodicScheduleCheck() {
        viewModelScope.launch {
            while (true) {
                checkSchedules(devices.value)
                delay(60000) // Check every minute
            }
        }
    }


    fun observeAllDevices() {
        Log.d("FirebaseDebug", "Started observing devices")
        isLoading.value = true

        repository.observeDevices(
            onResult = { deviceList ->
                Log.d("FirebaseDebug", "Data received. List size: ${deviceList.size}")
                devices.value = deviceList
                isLoading.value = false
                errorMessage.value = null
                checkSafetyCutoffs(deviceList)
                checkSchedules(deviceList)
            },
            onError = { exception ->
                Log.e("FirebaseDebug", "Firebase error: ${exception.message}")
                isLoading.value = false
                errorMessage.value = exception.message ?: "Unable to load devices"
            }
        )
    }

    private fun checkSafetyCutoffs(deviceList: List<Device>) {
        val activeOnDeviceIds = deviceList
            .filter { it.safetyCritical && it.status == DeviceStatus.ON }
            .map { it.id }
            .toSet()

        // 1. Cancel timers for devices that are OFF or missing to avoid memory leaks
        val existingJobIds = safetyCutoffJobs.keys.toList()
        for (id in existingJobIds) {
            if (!activeOnDeviceIds.contains(id)) {
                safetyCutoffJobs[id]?.cancel()
                safetyCutoffJobs.remove(id)
            }
        }

        // 2. Only launch a timer if one isn't currently running
        deviceList
            .filter { it.safetyCritical && it.status == DeviceStatus.ON }
            .forEach { device ->
                if (!safetyCutoffJobs.containsKey(device.id)) {
                    startSafetyTimer(device)
                }
            }
    }

    private fun startSafetyTimer(device: Device) {
        safetyCutoffJobs[device.id]?.cancel()

        val maxOnDuration = device.maxOnDuration ?: 300L
        val turnedOnAt = device.turnedOnAt ?: Date()

        val job = viewModelScope.launch {
            val elapsedSeconds = (System.currentTimeMillis() - turnedOnAt.time) / 1000
            val remainingSeconds = maxOnDuration - elapsedSeconds

            if (remainingSeconds > 0) {
                delay(remainingSeconds * 1000)
            }

            performSafetyCutoff(device)
        }
        safetyCutoffJobs[device.id] = job
    }

    private fun performSafetyCutoff(device: Device) {
        repository.updateDeviceStatus(device.id, DeviceStatus.OFF) { success ->
            if (success) {
                val log = UsageLog(
                    action = "SAFETY_CUTOFF",
                    deviceName = device.name,
                    deviceType = device.type.name,
                    floorId = device.floorId,
                    roomId = device.roomId
                )
                repository.logSafetyEvent(device.id, log)
            }
        }
        safetyCutoffJobs.remove(device.id)
    }

    private fun checkSchedules(deviceList: List<Device>) {
        val now = Calendar.getInstance()
        val currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

        deviceList.filter { it.autoScheduleEnabled }.forEach { device ->
            val startTime = device.startTime // e.g., "18:00"
            val endTime = device.endTime     // e.g., "06:00"

            if (startTime != null && endTime != null) {
                val shouldBeOn = isTimeInInterval(currentTimeString, startTime, endTime)
                val isCurrentlyOn = device.status == DeviceStatus.ON

                if (shouldBeOn && !isCurrentlyOn) {
                    repository.updateDeviceStatus(device.id, DeviceStatus.ON) { }
                } else if (!shouldBeOn && isCurrentlyOn) {
                    repository.updateDeviceStatus(device.id, DeviceStatus.OFF) { }
                }
            }
        }
    }

    private fun isTimeInInterval(current: String, start: String, end: String): Boolean {
        return if (start <= end) {
            current >= start && current < end
        } else {
            // Overnights, e.g., 22:00 to 06:00
            current >= start || current < end
        }
    }

    fun observeDevicesByFloor(floorId: String, level: Int? = null) {
        isLoading.value = true
        errorMessage.value = null

        repository.observeDevicesByFloor(
            floorId = floorId,
            level = level,
            onResult = { deviceList ->
                devices.value = deviceList
                isLoading.value = false
                errorMessage.value = null
            },
            onError = { exception ->
                isLoading.value = false
                errorMessage.value = exception.message ?: "Unable to load devices"
            }
        )
    }

    fun toggleDevice(device: Device) {
        val isTurningOn = device.status != DeviceStatus.ON
        val newStatus = if (isTurningOn) DeviceStatus.ON else DeviceStatus.OFF

        // Optimistic update only for immediate UI feedback.
        val updatedList = devices.value.map {
            if (it.id == device.id) it.copy(statusString = newStatus.name) else it
        }
        devices.value = updatedList

        repository.updateDeviceStatus(device.id, newStatus) { success ->
            if (!success) {
                Log.e("FirebaseUpdate", "Failed to update device status for ${device.id}")
                repository.observeDevices(
                    onResult = { devices.value = it },
                    onError = { }
                )
                errorMessage.value = "Failed to update device status"
            }
        }
    }

    fun toggleSubSwitch(
        device: Device,
        subSwitchId: Int
    ) {
        val updatedSubSwitches = device.subSwitches.map {
            if (it.id == subSwitchId) {
                it.copy(statusString = if (it.status == DeviceStatus.ON) DeviceStatus.OFF.name else DeviceStatus.ON.name)
            } else {
                it
            }
        }

        // Optimistic update
        val updatedDevices = devices.value.map {
            if (it.id == device.id) {
                it.copy(subSwitches = updatedSubSwitches)
            } else {
                it
            }
        }
        devices.value = updatedDevices

        repository.updateSubSwitchStatus(
            deviceId = device.id,
            subSwitches = updatedSubSwitches,
            onComplete = { success ->
                if (!success) {
                    errorMessage.value = "Failed to update switch"
                    // Revert on failure
                    repository.observeDevices(
                        onResult = { devices.value = it },
                        onError = { }
                    )
                }
            }
        )
    }

    fun updateSafetyDuration(
        device: Device,
        maxMinutes: Int
    ) {
        repository.updateSafetyDuration(
            deviceId = device.id,
            maxMinutes = maxMinutes,
            onComplete = { success ->
                if (!success) {
                    errorMessage.value = "Failed to update safety cutoff duration"
                }
            }
        )
    }

    fun updateSchedule(
        device: Device,
        enabled: Boolean,
        startTime: String?,
        endTime: String?
    ) {
        repository.updateDeviceSchedule(
            deviceId = device.id,
            autoScheduleEnabled = enabled,
            startTime = startTime,
            endTime = endTime,
            onComplete = { success ->
                if (!success) {
                    errorMessage.value = "Failed to update schedule"
                }
            }
        )
    }

    fun loadDeviceUsageLogs(deviceId: String) {
        repository.fetchDeviceUsage(
            deviceId = deviceId,
            onResult = { logs ->
                usageLogs.value = logs
            },
            onError = { exception ->
                errorMessage.value = exception.message ?: "Unable to fetch device usage logs"
            }
        )
    }

    fun loadAllUsageLogs() {
        repository.fetchAllUsageLogs(
            onResult = { logs ->
                usageLogs.value = logs
            },
            onError = { exception ->
                errorMessage.value = exception.message ?: "Unable to fetch usage logs"
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopObserving()
        safetyCutoffJobs.values.forEach { it.cancel() }
        safetyCutoffJobs.clear()
    }

    fun captureNewSnapshot(deviceId: String) {
        val newSeed = System.currentTimeMillis()
        val newSnapshotUrl = "https://picsum.photos/seed/$newSeed/800/450"

        Firebase.firestore.collection("devices")
            .document(deviceId)
            .update(
                mapOf(
                    "streamUrl" to newSnapshotUrl,
                    "lastSnapshotUrl" to newSnapshotUrl,
                    "lastStatusChange" to com.google.firebase.Timestamp.now()
                )
            )
    }
    fun calculateTotalKwh(devices: List<Device>, usageLogs: List<UsageLog>): Double {
        var totalKwh = 0.0

        // 1. Calculate energy for currently active (ON) devices
        val now = System.currentTimeMillis()
        devices.filter { it.status == DeviceStatus.ON }.forEach { device ->
            val startTime = device.turnedOnAt?.time ?: now
            val activeHours = (now - startTime) / (1000.0 * 60.0 * 60.0)
            val watts = device.getWattage()
            totalKwh += (watts * activeHours) / 1000.0
        }

        // 2. Add energy from historical usage logs (if logs store duration in seconds)
        usageLogs.forEach { log ->
            val durationHours = (log.durationSeconds ?: 0L) / 3600.0
            val watts = log.wattage ?: 100.0
            totalKwh += (watts * durationHours) / 1000.0
        }

        return totalKwh
    }

}