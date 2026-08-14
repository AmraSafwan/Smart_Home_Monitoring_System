package com.example.smarthome.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class DeviceStatus {
    ON, OFF, ERROR, DISCONNECTED
}

enum class DeviceType {
    LIGHT, OUTLET, MULTI_SWITCH, SAFETY_DEVICE, IRON, CAMERA
}

@IgnoreExtraProperties
data class DevicePosition(
    val x: Float = 0f,
    val y: Float = 0f
)

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