package com.example.smarthome.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType

@Composable
fun DeviceCard(
    device: Device,
    onToggle: () -> Unit,
    onClick: () -> Unit = {}
) {
    val isOn = device.status == DeviceStatus.ON
    val isUnavailable = device.status == DeviceStatus.ERROR || device.status == DeviceStatus.DISCONNECTED

    // Display the fetched room name, or fallback to raw roomId if empty
    val displayRoomName = device.roomName.ifBlank { device.roomId.ifBlank { "Unknown Room" } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isUnavailable) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getDeviceIcon(device.type),
                            contentDescription = null,
                            tint = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    // Device Name (Headline)
                    Text(
                        text = device.name.ifBlank { "Smart Device" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Display Real Room Name (Subtitle)
                    Text(
                        text = displayRoomName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = device.status)
                }
            }

            when (device.type) {
                DeviceType.MULTI_SWITCH -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val activeSwitches = device.subSwitches.count { it.status == DeviceStatus.ON }
                        Text(
                            text = "$activeSwitches/${device.subSwitches.size} ON",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = "Manage Switches")
                    }
                }
                DeviceType.CAMERA -> {
                    Icon(Icons.Default.ChevronRight, contentDescription = "View Live Stream")
                }
                else -> {
                    Switch(
                        checked = isOn,
                        enabled = !isUnavailable,
                        onCheckedChange = { onToggle() }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DeviceStatus) {
    val color = when (status) {
        DeviceStatus.ON -> Color(0xFF4CAF50)
        DeviceStatus.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
        DeviceStatus.ERROR -> MaterialTheme.colorScheme.error
        DeviceStatus.DISCONNECTED -> Color.Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .aspectRatio(1f)
                .align(Alignment.CenterVertically)
        ) {
            Surface(color = color, shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.fillMaxSize()) {}
        }
        Spacer(Modifier.width(4.dp))
        Text(text = status.name, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun getDeviceIcon(type: DeviceType): ImageVector = when (type) {
    DeviceType.LIGHT -> Icons.Default.Lightbulb
    DeviceType.OUTLET -> Icons.Default.Power
    DeviceType.MULTI_SWITCH -> Icons.Default.SettingsInputComponent
    DeviceType.SAFETY_DEVICE, DeviceType.IRON -> Icons.Default.Iron
    DeviceType.CAMERA -> Icons.Default.Videocam
}