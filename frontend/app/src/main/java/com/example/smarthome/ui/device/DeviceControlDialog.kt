package com.example.smarthome.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType

@Composable
fun DeviceControlDialog(
    device: Device,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onSubSwitchToggle: (Int) -> Unit = {},
    onSafetyUpdate: (Int) -> Unit = {},
    onScheduleUpdate: (Boolean, String?, String?) -> Unit = { _, _, _ -> }
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getDeviceIcon(device.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = device.name, style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusIndicator(status = device.status)

                when (device.type) {
                    DeviceType.OUTLET -> {
                        PowerControl(isOn = device.status == DeviceStatus.ON, onToggle = onToggle)
                    }
                    DeviceType.LIGHT -> {
                        PowerControl(isOn = device.status == DeviceStatus.ON, onToggle = onToggle)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SchedulingControl(
                            enabled = device.autoScheduleEnabled,
                            startTime = device.startTime ?: "18:00",
                            endTime = device.endTime ?: "06:00",
                            onUpdate = onScheduleUpdate
                        )
                    }
                    DeviceType.MULTI_SWITCH -> {
                        MultiSwitchControl(
                            subSwitches = device.subSwitches,
                            onToggle = onSubSwitchToggle
                        )
                    }
                    DeviceType.SAFETY_DEVICE, DeviceType.IRON -> {
                        PowerControl(isOn = device.status == DeviceStatus.ON, onToggle = onToggle)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Calculate initial minutes from maxDuration (seconds) or maxOnDurationMinutes
                        val currentMinutes = device.maxOnDurationMinutes
                            ?: ((device.maxDuration ?: 300L) / 60).toInt()

                        SafetyConfigControl(
                            maxDuration = currentMinutes,
                            onSave = onSafetyUpdate
                        )
                    }
                    DeviceType.CAMERA -> {
                        CameraMockStream(streamUri = device.streamUri ?: device.lastSnapshotUrl)
                    }
                }
            }
        }
    )
}

@Composable
fun StatusIndicator(status: DeviceStatus) {
    val color = when (status) {
        DeviceStatus.ON -> Color(0xFF4CAF50)
        DeviceStatus.OFF -> Color(0xFF757575)
        DeviceStatus.ERROR -> Color(0xFFF44336)
        DeviceStatus.DISCONNECTED -> Color(0xFFFF9800)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.name,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PowerControl(isOn: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Main Power", style = MaterialTheme.typography.titleMedium)
        Switch(checked = isOn, onCheckedChange = { onToggle() })
    }
}

@Composable
fun MultiSwitchControl(
    subSwitches: List<com.example.smarthome.data.model.SubSwitch>,
    onToggle: (Int) -> Unit
) {
    Text("Individual Switches", style = MaterialTheme.typography.titleMedium)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        subSwitches.forEach { sw ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sw.name)
                Switch(
                    checked = sw.status == DeviceStatus.ON,
                    onCheckedChange = { onToggle(sw.id) }
                )
            }
        }
    }
}

@Composable
fun SafetyConfigControl(maxDuration: Int, onSave: (Int) -> Unit) {
    var textValue by remember { mutableStateOf(maxDuration.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Safety Cutoff Config", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "Iron will automatically switch OFF after continuous usage.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text("Max Duration (Mins)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { onSave(textValue.toIntOrNull() ?: 5) }) {
                Text("Save")
            }
        }
    }
}

@Composable
fun SchedulingControl(
    enabled: Boolean,
    startTime: String,
    endTime: String,
    onUpdate: (Boolean, String, String) -> Unit
) {
    var isScheduleEnabled by remember { mutableStateOf(enabled) }
    var start by remember { mutableStateOf(startTime) }
    var end by remember { mutableStateOf(endTime) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Switch(
                checked = isScheduleEnabled,
                onCheckedChange = {
                    isScheduleEnabled = it
                    onUpdate(isScheduleEnabled, start, end)
                }
            )
        }
        if (isScheduleEnabled) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = {
                        start = it
                        onUpdate(isScheduleEnabled, start, end)
                    },
                    label = { Text("Turn ON (HH:mm)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = {
                        end = it
                        onUpdate(isScheduleEnabled, start, end)
                    },
                    label = { Text("Turn OFF (HH:mm)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CameraMockStream(streamUri: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            Text("Live Stream Feed", color = Color.White)
            Text("URI: ${streamUri ?: "rtsp://mock.camera.local/stream1"}", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

private fun getDeviceIcon(type: DeviceType) = when (type) {
    DeviceType.OUTLET -> Icons.Default.Power
    DeviceType.LIGHT -> Icons.Default.Lightbulb
    DeviceType.MULTI_SWITCH -> Icons.Default.SettingsInputComponent
    DeviceType.SAFETY_DEVICE, DeviceType.IRON -> Icons.Default.Iron
    DeviceType.CAMERA -> Icons.Default.Videocam
}