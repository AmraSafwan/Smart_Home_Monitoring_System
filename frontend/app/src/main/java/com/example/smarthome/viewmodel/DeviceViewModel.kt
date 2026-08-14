package com.example.smarthome.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.UsageLog
import com.example.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

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
    }

    fun observeAllDevices() {
        isLoading.value = true

        repository.observeDevices(
            onResult = { deviceList ->
                devices.value = deviceList
                isLoading.value = false
                errorMessage.value = null
                checkSafetyCutoffs(deviceList)
            },
            onError = { exception ->
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

    fun observeDevicesByFloor(floorId: String) {
        isLoading.value = true
        errorMessage.value = null

        repository.observeDevicesByFloor(
            floorId = floorId,
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
        val newStatus = if (device.status == DeviceStatus.ON) {
            DeviceStatus.OFF
        } else {
            DeviceStatus.ON
        }

        repository.updateDeviceStatus(
            deviceId = device.id,
            status = newStatus,
            onComplete = { success ->
                if (!success) {
                    errorMessage.value = "Failed to update device status"
                }
            }
        )
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

        repository.updateSubSwitchStatus(
            deviceId = device.id,
            subSwitches = updatedSubSwitches,
            onComplete = { success ->
                if (!success) {
                    errorMessage.value = "Failed to update switch"
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
}