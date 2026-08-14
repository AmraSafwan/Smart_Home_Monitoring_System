package com.example.smarthome.ui.floorplan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType

@Composable
fun FloorGrid(
    devices: List<Device>,
    onDeviceClick: (Device) -> Unit,
    modifier: Modifier = Modifier,
    layoutUrl: String? = null
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
    ) {
        val gridRows = 8
        val gridCols = 10
        val cellWidth = maxWidth / gridCols
        val cellHeight = maxHeight / gridRows

        // Display the Floor Plan image if available
        if (!layoutUrl.isNullOrBlank()) {
            AsyncImage(
                model = layoutUrl,
                contentDescription = "Floor Layout",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                alpha = 0.6f
            )
        }

        // Draw Grid Lines for the "Abstract Mapping" feel
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color.Gray.copy(alpha = 0.2f)
            for (i in 1 until gridCols) {
                drawLine(lineColor, Offset(i * cellWidth.toPx(), 0f), Offset(i * cellWidth.toPx(), size.height))
            }
            for (i in 1 until gridRows) {
                drawLine(lineColor, Offset(0f, i * cellHeight.toPx()), Offset(size.width, i * cellHeight.toPx()))
            }
        }

        // Place Devices
        devices.forEach { device ->
            val x = cellWidth * device.column.toFloat()
            val y = cellHeight * device.row.toFloat()

            DeviceMarker(
                device = device,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(cellWidth, cellHeight)
                    .clickable { onDeviceClick(device) }
            )
        }
    }
}

@Composable
fun DeviceMarker(device: Device, modifier: Modifier) {
    val statusColor = when (device.status) {
        DeviceStatus.ON -> MaterialTheme.colorScheme.primary
        DeviceStatus.OFF -> MaterialTheme.colorScheme.outline
        DeviceStatus.ERROR -> MaterialTheme.colorScheme.error
        DeviceStatus.DISCONNECTED -> Color.Gray
    }

    val icon = getDeviceIcon(device.type)

    Box(
        modifier = modifier.padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (device.status == DeviceStatus.ON) statusColor.copy(alpha = 0.2f) else Color.Transparent)
                    .border(2.dp, statusColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = device.name,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (device.name.isNotEmpty()) {
                Text(
                    text = device.name,
                    fontSize = 8.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun getDeviceIcon(type: DeviceType): ImageVector = when (type) {
    DeviceType.LIGHT -> Icons.Default.Lightbulb
    DeviceType.OUTLET -> Icons.Default.Power
    DeviceType.MULTI_SWITCH -> Icons.Default.SettingsInputComponent
    DeviceType.SAFETY_DEVICE -> Icons.Default.Security
    DeviceType.IRON -> Icons.Default.Iron
    DeviceType.CAMERA -> Icons.Default.Videocam
}
