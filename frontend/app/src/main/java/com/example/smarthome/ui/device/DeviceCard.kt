package com.example.smarthome.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus

@Composable
fun DeviceCard(
    device: Device,
    onToggle: () -> Unit
) {

    val isOn = device.status == DeviceStatus.ON

    val isUnavailable =
        device.status == DeviceStatus.ERROR ||
                device.status == DeviceStatus.DISCONNECTED


    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                if (isUnavailable) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            // Device information
            Row(
                modifier = Modifier.weight(1f),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                DeviceTypeIcon(
                    type = device.type
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column {

                    Text(
                        text = device.name.ifBlank {
                            "Unnamed Device"
                        },

                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = device.type.uppercase(),

                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Spacer(
                        modifier = Modifier.padding(2.dp)
                    )

                    StatusText(
                        status = device.status
                    )
                }
            }


            // ON/OFF control
            Switch(

                checked = isOn,

                enabled = !isUnavailable,

                onCheckedChange = {
                    onToggle()
                }
            )
        }
    }
}


@Composable
private fun StatusText(
    status: DeviceStatus
) {

    val statusText =
        when (status) {

            DeviceStatus.ON ->
                "● ON"

            DeviceStatus.OFF ->
                "● OFF"

            DeviceStatus.ERROR ->
                "● ERROR"

            DeviceStatus.DISCONNECTED ->
                "● DISCONNECTED"
        }


    val statusColor =
        when (status) {

            DeviceStatus.ON ->
                MaterialTheme.colorScheme.primary

            DeviceStatus.OFF ->
                MaterialTheme.colorScheme.onSurfaceVariant

            DeviceStatus.ERROR ->
                MaterialTheme.colorScheme.error

            DeviceStatus.DISCONNECTED ->
                MaterialTheme.colorScheme.error
        }


    Text(
        text = statusText,

        color = statusColor,

        style =
            MaterialTheme.typography.bodyMedium
    )
}


@Composable
private fun DeviceTypeIcon(
    type: String
) {

    val icon =
        when (type.uppercase()) {

            "LIGHT" ->
                Icons.Default.Lightbulb

            "OUTLET" ->
                Icons.Default.Power

            "SWITCH" ->
                Icons.Default.Bolt

            else ->
                Icons.Default.Warning
        }


    Icon(
        imageVector = icon,

        contentDescription = type
    )
}