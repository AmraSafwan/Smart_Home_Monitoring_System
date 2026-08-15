package com.example.smarthome.ui.floorplan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.Floor
import com.example.smarthome.ui.device.DeviceControlDialog
import com.example.smarthome.viewmodel.DeviceViewModel
import com.example.smarthome.viewmodel.FloorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanScreen(
    floorId: String,
    floorName: String,
    onBack: () -> Unit,
    deviceViewModel: DeviceViewModel = viewModel(),
    floorViewModel: FloorViewModel = viewModel(),
    onDeviceClick: (Device) -> Unit
) {
    val devices by deviceViewModel.devices
    val isLoading by deviceViewModel.isLoading
    val errorMessage by deviceViewModel.errorMessage
    var floor by remember { mutableStateOf<Floor?>(null) }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }

    // Always fetch the freshest device state from the observed list
    val currentSelectedDevice = devices.find { it.id == selectedDeviceId }

    LaunchedEffect(floorId) {
        deviceViewModel.observeDevicesByFloor(floorId = floorId)
        floorViewModel.getFloor(floorId) { fetchedFloor ->
            floor = fetchedFloor
            // Re-observe with level for better matching if level is available
            fetchedFloor?.let {
                deviceViewModel.observeDevicesByFloor(floorId = floorId, level = it.level)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(floorName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Interactive Floor Plan",
                style = MaterialTheme.typography.bodyMedium
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage ?: "Unable to load devices")
                }
            } else if (devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No devices configured on this floor.")
                }
            } else {
                FloorGrid(
                    devices = devices,
                    layoutUrl = floor?.layout,
                    onDeviceClick = { device ->
                        selectedDeviceId = device.id
                        onDeviceClick(device)
                    },
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }

    currentSelectedDevice?.let { device ->
        DeviceControlDialog(
            device = device,
            onDismiss = { selectedDeviceId = null },
            onToggle = {
                deviceViewModel.toggleDevice(device)
            },
            onSubSwitchToggle = { subSwitchId ->
                deviceViewModel.toggleSubSwitch(device, subSwitchId)
            },
            onScheduleUpdate = { enabled, start, end ->
                deviceViewModel.updateSchedule(device, enabled, start, end)
            }
        )
    }
}
