package com.example.smarthome.data.model

enum class DeviceType {
    OUTLET,
    LIGHT,
    MULTI_SWITCH,
    SAFETY_DEVICE,
    IRON, // Added to directly parse "type": "IRON" from Firestore
    CAMERA
}