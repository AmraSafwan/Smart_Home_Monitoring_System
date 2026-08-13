package com.example.smarthome.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus

@Composable
fun DeviceControlDialog(
    device: Device,
    onDismiss: () -> Unit,
    onToggle: () -> Unit
) {

    val isOn = device.status == DeviceStatus.ON

    val isUnavailable =
        device.status == DeviceStatus.ERROR ||
                device.status == DeviceStatus.DISCONNECTED

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text(
                text = device.name
            )
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Type: ${device.type}"
                )

                Text(
                    text = "Room: ${device.roomId}"
                )

                Text(
                    text = "Status: ${device.status}"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Power",
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = isOn,
                        enabled = !isUnavailable,
                        onCheckedChange = {
                            onToggle()
                        }
                    )
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}