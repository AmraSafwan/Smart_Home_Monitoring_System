package com.example.smarthome.ui.floorplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.viewmodel.DeviceViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.smarthome.ui.device.DeviceControlDialog

@Composable
fun FloorPlanScreen(
    floorId: String,
    floorName: String,
    deviceViewModel: DeviceViewModel = viewModel(),
    onDeviceClick: (Device) -> Unit
) {

    val devices by deviceViewModel.devices

    val isLoading by deviceViewModel.isLoading

    val errorMessage by deviceViewModel.errorMessage

    var selectedDevice by remember {
        mutableStateOf<Device?>(null)
    }

    // Load devices whenever the selected floor changes
    LaunchedEffect(floorId) {

        deviceViewModel.observeDevicesByFloor(
            floorId = floorId
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = floorName,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Interactive Floor Plan",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )


        // Loading state
        if (isLoading) {

            Box(
                modifier = Modifier
                    .fillMaxSize(),

                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator()
            }

        }

        // Error state
        else if (errorMessage != null) {

            Box(
                modifier = Modifier
                    .fillMaxSize(),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = errorMessage
                        ?: "Unable to load devices"
                )
            }

        }

        // Empty floor
        else if (devices.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize(),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "No devices configured on this floor."
                )
            }

        }

        // Devices available
        else {

            FloorGrid(
                devices = devices,
                onDeviceClick = { device ->

                    selectedDevice = device

                    onDeviceClick(device)
                },
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
    selectedDevice?.let { device ->

        DeviceControlDialog(

            device = device,

            onDismiss = {
                selectedDevice = null
            },

            onToggle = {
                deviceViewModel.toggleDevice(device)
            }
        )
    }
}


@Composable
private fun FloorGrid(
    devices: List<Device>,
    onDeviceClick: (Device) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        for (row in 0 until 6) {

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {

                for (column in 0 until 8) {

                    val device =
                        devices.firstOrNull {

                            it.position.row == row &&
                                    it.position.column == column
                        }


                    FloorGridCell(

                        device = device,

                        onClick = {

                            if (device != null) {

                                onDeviceClick(device)
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun FloorGridCell(
    device: Device?,
    onClick: () -> Unit
) {

    Box(

        modifier = Modifier
            .size(42.dp)

            .border(
                width = 1.dp,
                color =
                    MaterialTheme.colorScheme.outline
            )

            .background(
                if (device != null) {

                    MaterialTheme
                        .colorScheme
                        .surfaceVariant

                } else {

                    MaterialTheme
                        .colorScheme
                        .surface
                }
            )

            .clickable {
                onClick()
            },

        contentAlignment = Alignment.Center
    ) {

        if (device != null) {

            val icon = when (
                device.type.uppercase()
            ) {

                "LIGHT" ->
                    Icons.Default.Lightbulb

                "OUTLET" ->
                    Icons.Default.Power

                else ->
                    Icons.Default.Power
            }


            Icon(

                imageVector = icon,

                contentDescription =
                    device.name,

                tint =
                    if (
                        device.status ==
                        DeviceStatus.ON
                    ) {

                        MaterialTheme
                            .colorScheme
                            .primary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    }
            )
        }
    }
}