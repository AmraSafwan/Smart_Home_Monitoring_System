package com.example.smarthome.data.model

data class Device(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val status: DeviceStatus = DeviceStatus.OFF,
    val floorId: String = "",
    val roomId: String = "",
    val position: DevicePosition = DevicePosition()
)