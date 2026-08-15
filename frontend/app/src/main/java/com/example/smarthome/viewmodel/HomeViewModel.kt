package com.example.smarthome.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.repository.DeviceRepository

class HomeViewModel : ViewModel() {
    private val repository = DeviceRepository()
    
    var devices = mutableStateOf<List<Device>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(false)
        private set

    fun loadHomeData() {
        isLoading.value = true
        repository.observeDevices(
            onResult = {
                devices.value = it
                isLoading.value = false
            },
            onError = {
                isLoading.value = false
            }
        )
    }
}
