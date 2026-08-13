package com.example.smarthome.ui.home

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.ui.device.DeviceCard
import com.example.smarthome.viewmodel.DeviceViewModel
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@Composable
fun HomeScreen(
    deviceViewModel: DeviceViewModel = viewModel()
) {

    val devices by deviceViewModel.devices
    val isLoading by deviceViewModel.isLoading
    val errorMessage by deviceViewModel.errorMessage

    LaunchedEffect(Unit) {
        deviceViewModel.observeAllDevices()
    }

    val activeDevices =
        devices.count { it.status == DeviceStatus.ON }

    val errorDevices =
        devices.count { it.status == DeviceStatus.ERROR }

    val disconnectedDevices =
        devices.count { it.status == DeviceStatus.DISCONNECTED }

    var selectedFloor by remember {
        mutableStateOf("All Floors")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Header
        Text(
            text = "Smart Home",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Welcome back!",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            DashboardStatCard(
                title = "Devices",
                value = devices.size.toString(),
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "Active",
                value = activeDevices.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Warning information
        if (errorDevices > 0 || disconnectedDevices > 0) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning"
                    )

                    Spacer(modifier = Modifier.padding(6.dp))

                    Column {

                        Text(
                            text = "Device Attention Required",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text =
                                "$errorDevices error, " +
                                        "$disconnectedDevices disconnected"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Floor selection
        Text(
            text = "Current Floor",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        var floorMenuExpanded by remember {
            mutableStateOf(false)
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedButton(
                onClick = {
                    floorMenuExpanded = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(selectedFloor)
            }

            DropdownMenu(
                expanded = floorMenuExpanded,
                onDismissRequest = {
                    floorMenuExpanded = false
                }
            ) {

                DropdownMenuItem(
                    text = {
                        Text("All Floors")
                    },
                    onClick = {

                        selectedFloor = "All Floors"

                        floorMenuExpanded = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("Ground Floor")
                    },
                    onClick = {

                        selectedFloor = "Ground Floor"

                        floorMenuExpanded = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("First Floor")
                    },
                    onClick = {

                        selectedFloor = "First Floor"

                        floorMenuExpanded = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("Second Floor")
                    },
                    onClick = {

                        selectedFloor = "Second Floor"

                        floorMenuExpanded = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("Third Floor")
                    },
                    onClick = {

                        selectedFloor = "Third Floor"

                        floorMenuExpanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Devices heading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Devices",
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(
                onClick = {
                    selectedFloor = "All Floors"
                }
            ) {
                Text("View All")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {

            Text(
                text = "Loading devices..."
            )

        } else if (errorMessage != null) {

            Text(
                text = errorMessage ?: "Unable to load devices",
                color = MaterialTheme.colorScheme.error
            )

        } else if (devices.isEmpty()) {

            Text(
                text = "No devices available."
            )

        } else {

            val filteredDevices =
                when (selectedFloor) {

                    "All Floors" ->
                        devices

                    "Ground Floor" ->
                        devices.filter {
                            it.floorId == "ground"
                        }

                    "First Floor" ->
                        devices.filter {
                            it.floorId == "first"
                        }

                    "Second Floor" ->
                        devices.filter {
                            it.floorId == "second"
                        }

                    "Third Floor" ->
                        devices.filter {
                            it.floorId == "third"
                        }

                    else ->
                        devices
                }

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                items(devices) { device ->

                    DeviceCard(
                        device = device,
                        onToggle = {
                            deviceViewModel.toggleDevice(device)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}