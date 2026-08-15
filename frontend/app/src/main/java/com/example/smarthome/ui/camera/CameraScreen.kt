package com.example.smarthome.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType
import com.example.smarthome.viewmodel.DeviceViewModel

@Composable
fun CameraScreen(
    deviceViewModel: DeviceViewModel = viewModel()
) {
    val devices by deviceViewModel.devices

    val cameras = remember(devices) {
        devices.filter { it.type == DeviceType.CAMERA }
    }

    var selectedCameraForFullscreen by remember { mutableStateOf<Device?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Security Cameras",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${cameras.count { it.status == DeviceStatus.ON }} of ${cameras.size} cameras active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { deviceViewModel.observeAllDevices() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Cameras")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (cameras.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No security cameras found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = cameras,
                    key = { it.id.ifEmpty { it.hashCode().toString() } }
                ) { camera ->
                    CameraFeedCard(
                        camera = camera,
                        onTogglePower = { deviceViewModel.toggleDevice(camera) },
                        onCaptureSnapshot = { deviceViewModel.captureNewSnapshot(camera.id) },
                        onViewFullscreen = { selectedCameraForFullscreen = camera }
                    )
                }
            }
        }
    }

    // Fullscreen Dialog Stream Preview
    selectedCameraForFullscreen?.let { camera ->
        FullscreenCameraDialog(
            camera = camera,
            onDismiss = { selectedCameraForFullscreen = null }
        )
    }
}

@Composable
fun CameraFeedCard(
    camera: Device,
    onTogglePower: () -> Unit,
    onCaptureSnapshot: () -> Unit,
    onViewFullscreen: () -> Unit
) {
    var isLive by remember { mutableStateOf(true) }

    // Read directly from Firebase streamUrl or fallback to lastSnapshotUrl
    val imageUrl = camera.streamUrl ?: camera.lastSnapshotUrl ?: "https://picsum.photos/seed/front_door/800/450"
    val isPoweredOn = camera.status == DeviceStatus.ON

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black)
                    .clickable(enabled = isPoweredOn) { onViewFullscreen() },
                contentAlignment = Alignment.Center
            ) {
                if (isPoweredOn && isLive) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Camera Stream",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (!isPoweredOn) Icons.Default.VideocamOff else Icons.Default.PauseCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (!isPoweredOn) "CAMERA OFFLINE" else "STREAM PAUSED",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Overlay Controls & Badges
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Top Bar Overlay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = camera.name.ifEmpty { "Camera Feed" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        // Live Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPoweredOn && isLive) Color.Red else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPoweredOn && isLive) "LIVE" else "OFFLINE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (isPoweredOn) {
                        // Bottom Right Action Bar (Snapshot + Fullscreen)
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onCaptureSnapshot,
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Capture Snapshot",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = onViewFullscreen,
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isLive = !isLive },
                        enabled = isPoweredOn
                    ) {
                        Icon(
                            imageVector = if (isLive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Stream"
                        )
                    }
                }

                // Power Toggle Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPoweredOn) "ON" else "OFF",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPoweredOn) Color(0xFF4CAF50) else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isPoweredOn,
                        onCheckedChange = { onTogglePower() }
                    )
                }
            }
        }
    }
}

@Composable
fun FullscreenCameraDialog(
    camera: Device,
    onDismiss: () -> Unit
) {
    val imageUrl = camera.streamUrl ?: camera.lastSnapshotUrl ?: "https://picsum.photos/seed/front_door/800/450"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Fullscreen Camera Stream",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = camera.name.ifEmpty { "Security Camera" },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Room ID: ${camera.roomId ?: "N/A"}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}