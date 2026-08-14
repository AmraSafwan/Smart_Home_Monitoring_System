package com.example.smarthome.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Schedule(
    val enabled: Boolean = false,
    val onTime: String? = null,
    val offTime: String? = null
)

@IgnoreExtraProperties
data class SubSwitch(
    val id: Int = 0,
    val name: String = "",

    @get:PropertyName("status") @set:PropertyName("status")
    var statusString: String = "OFF"
) {
    @get:Exclude
    val status: DeviceStatus
        get() = try { DeviceStatus.valueOf(statusString) } catch (e: Exception) { DeviceStatus.OFF }
}

@IgnoreExtraProperties
data class Device(
    var id: String = "",
    var name: String = "",

    @get:PropertyName("type") @set:PropertyName("type")
    var typeString: String = "LIGHT",

    @get:PropertyName("status") @set:PropertyName("status")
    var statusString: String = "OFF",

    @get:PropertyName("floorId") @set:PropertyName("floorId")
    var floorId: String = "",

    @get:PropertyName("roomId") @set:PropertyName("roomId")
    var roomId: String = "",

    var roomName: String = "",
    var position: DevicePosition? = null,

    var row: Int = 0,
    var column: Int = 0,

    var safetyCritical: Boolean = false,
    var safetyCutoff: Boolean = false,

    @get:PropertyName("maxDuration") @set:PropertyName("maxDuration")
    var maxDuration: Long? = 300L,

    @get:PropertyName("maxOnDuration") @set:PropertyName("maxOnDuration")
    var maxOnDuration: Long? = null,

    val maxOnDurationMinutes: Int? = null,

    var schedule: Schedule? = null,

    val subSwitches: List<SubSwitch> = emptyList(),

    val streamUri: String? = null,
    val streamUrl: String? = null,
    val lastSnapshotUrl: String? = null,

    val energyConsumptionWh: Double = 0.0,

    @ServerTimestamp val lastActiveTimestamp: Date? = null,
    @ServerTimestamp val turnedOnAt: Date? = null,
    @ServerTimestamp val turnedOffAt: Date? = null,
    @ServerTimestamp val lastStatusChange: Date? = null,
    @ServerTimestamp val safetyCutoffAt: Date? = null
) {
    @get:Exclude
    val status: DeviceStatus
        get() = try { DeviceStatus.valueOf(statusString) } catch (e: Exception) { DeviceStatus.OFF }

    @get:Exclude
    val type: DeviceType
        get() = try { DeviceType.valueOf(typeString) } catch (e: Exception) { DeviceType.LIGHT }

    @get:Exclude
    val autoScheduleEnabled: Boolean
        get() = schedule?.enabled ?: false

    @get:Exclude
    val startTime: String?
        get() = schedule?.onTime

    @get:Exclude
    val endTime: String?
        get() = schedule?.offTime
}
// Add this extension property at the bottom of Device.kt
val Device.powerRatingWatts: Double
    get() = when (this.type) {
        DeviceType.LIGHT -> 15.0
        DeviceType.OUTLET -> 1000.0
        DeviceType.CAMERA -> 8.0
        DeviceType.SAFETY_DEVICE -> 1500.0 // e.g., Clothing Iron
        else -> 50.0
    }

/**
 * Calculates current accrued Wh:
 * Combines stored Firestore historical energy with active runtime since turnedOnAt.
 */
val Device.totalCalculatedWh: Double
    get() {
        var baseWh = energyConsumptionWh
        if (status == DeviceStatus.ON && turnedOnAt != null) {
            val activeMs = System.currentTimeMillis() - turnedOnAt!!.time
            if (activeMs > 0) {
                val activeHours = activeMs / (1000.0 * 60.0 * 60.0)
                baseWh += powerRatingWatts * activeHours
            }
        }
        return baseWh
    }