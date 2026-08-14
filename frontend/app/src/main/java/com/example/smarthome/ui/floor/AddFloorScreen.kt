package com.example.smarthome.ui.floor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.viewmodel.FloorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFloorScreen(
    onBack: () -> Unit,
    floorViewModel: FloorViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    val isLoading by floorViewModel.isLoading
    val errorMessage by floorViewModel.errorMessage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Floor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Floor Name (e.g., Ground Floor)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = level,
                onValueChange = { level = it },
                label = { Text("Floor Level (e.g., 0, 1, 2)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val levelInt = level.toIntOrNull() ?: 0
                    floorViewModel.addFloor(name, levelInt)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && level.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Add Floor")
                }
            }
        }
    }
}
