package com.example.smarthome.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class UsageLog(
    val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val deviceType: String = "",
    val roomId: String? = null,
    val floorId: String? = null,
    val action: String = "", // e.g., "ON", "OFF", "SAFETY_CUTOFF"

    @ServerTimestamp
    val timestamp: Date? = null
)