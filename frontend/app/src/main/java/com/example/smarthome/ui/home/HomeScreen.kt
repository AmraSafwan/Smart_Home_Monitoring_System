package com.example.smarthome.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.ui.device.DeviceCard
import com.example.smarthome.viewmodel.DeviceViewModel
import com.example.smarthome.viewmodel.FloorViewModel

@Composable
fun HomeScreen(
    deviceViewModel: DeviceViewModel = viewModel(),
    floorViewModel: FloorViewModel = viewModel()
) {

    val devices by deviceViewModel.devices
    val isLoading by deviceViewModel.isLoading
    val errorMessage by deviceViewModel.errorMessage
    val floors by floorViewModel.floors

    LaunchedEffect(Unit) {
        deviceViewModel.observeAllDevices()
    }

    val activeDevices =
        devices.count { it.status == DeviceStatus.ON }

    val errorDevices =
        devices.count { it.status == DeviceStatus.ERROR }

    val disconnectedDevices =
        devices.count { it.status == DeviceStatus.DISCONNECTED }

    var selectedFloorName by remember {
        mutableStateOf("All Floors")
    }
    
    var selectedFloorId by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = "Smart Home",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Welcome back!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            DashboardStatCard(
                title = "Total Devices",
                value = devices.size.toString(),
                icon = Icons.Default.Devices,
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                title = "Active Now",
                value = activeDevices.toString(),
                icon = Icons.Default.Power,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.padding(6.dp))

                    Column {

                        Text(
                            text = "System Alert",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "$errorDevices device errors, " +
                                        "$disconnectedDevices offline"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Floor selection
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Filter by Floor",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        var floorMenuExpanded by remember {
            mutableStateOf(false)
        }

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedButton(
                onClick = {
                    floorMenuExpanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedFloorName)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            DropdownMenu(
                expanded = floorMenuExpanded,
                onDismissRequest = {
                    floorMenuExpanded = false
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {

                DropdownMenuItem(
                    text = {
                        Text("All Floors")
                    },
                    onClick = {
                        selectedFloorName = "All Floors"
                        selectedFloorId = null
                        floorMenuExpanded = false
                    }
                )

                floors.forEach { floor ->
                    DropdownMenuItem(
                        text = {
                            Text(floor.name)
                        },
                        onClick = {
                            selectedFloorName = floor.name
                            selectedFloorId = floor.id
                            floorMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Devices heading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "My Devices",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = {
                    selectedFloorName = "All Floors"
                    selectedFloorId = null
                }
            ) {
                Text("Reset Filter")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessage ?: "Unable to load devices",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {

            val filteredDevices = if (selectedFloorId == null) {
                devices
            } else {
                devices.filter { it.floorId == selectedFloorId }
            }

            if (filteredDevices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No devices found in this area.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredDevices) { device ->
                        DeviceCard(
                            device = device,
                            onToggle = {
                                deviceViewModel.toggleDevice(device)
                            },
                            onClick = {
                                // Potentially open dialog
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}