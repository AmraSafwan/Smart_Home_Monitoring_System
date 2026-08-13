package com.example.smarthome.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.repository.DeviceRepository

class DeviceViewModel : ViewModel() {

    private val repository =
        DeviceRepository()

    var devices =
        mutableStateOf<List<Device>>(emptyList())
        private set

    var isLoading =
        mutableStateOf(false)
        private set

    var errorMessage =
        mutableStateOf<String?>(null)
        private set


    // Load all devices
    fun observeAllDevices() {

        isLoading.value = true

        repository.observeDevices(

            onResult = { deviceList ->

                devices.value = deviceList

                isLoading.value = false

                errorMessage.value = null
            },

            onError = { exception ->

                isLoading.value = false

                errorMessage.value =
                    exception.message
                        ?: "Unable to load devices"
            }
        )
    }


    // Load devices for selected floor
    fun observeDevicesByFloor(
        floorId: String
    ) {

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

                errorMessage.value =
                    exception.message
                        ?: "Unable to load devices"
            }
        )
    }


    // Toggle device ON/OFF
    fun toggleDevice(
        device: Device
    ) {

        val newStatus =
            if (device.status == DeviceStatus.ON) {

                DeviceStatus.OFF

            } else {

                DeviceStatus.ON
            }

        repository.updateDeviceStatus(

            deviceId = device.id,

            status = newStatus.name,

            onComplete = { success ->

                if (!success) {

                    errorMessage.value =
                        "Failed to update device status"
                }
            }
        )
    }
}