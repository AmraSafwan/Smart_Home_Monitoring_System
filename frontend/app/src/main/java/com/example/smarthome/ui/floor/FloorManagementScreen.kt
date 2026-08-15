package com.example.smarthome.ui.floor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.data.model.Floor
import com.example.smarthome.viewmodel.FloorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorManagementScreen(
    onFloorClick: (Floor) -> Unit,
    onAddFloorClick: () -> Unit,
    floorViewModel: FloorViewModel = viewModel()
) {

    val floors by floorViewModel.floors
    val isLoading by floorViewModel.isLoading
    val errorMessage by floorViewModel.errorMessage

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFloorClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Floor")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Text(
                text = "Floor Management",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Select a floor to view its devices.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (isLoading) {

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            } else if (errorMessage != null) {

                Text(
                    text = errorMessage ?: "Unable to load floors",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp)
                )

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalArrangement =
                    Arrangement.spacedBy(12.dp)
                ) {

                    items(floors) { floor ->

                        FloorCard(
                            floor = floor,
                            onClick = {
                                onFloorClick(floor)
                            },
                            onDelete = {
                                floorViewModel.deleteFloor(floor.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloorCard(
    floor: Floor,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = floor.name,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Level ${floor.level}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Floor",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}