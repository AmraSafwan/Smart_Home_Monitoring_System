package com.example.smarthome.ui.floor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.data.model.Floor
import com.example.smarthome.viewmodel.FloorViewModel

@Composable
fun FloorManagementScreen(
    onFloorClick: (Floor) -> Unit,
    floorViewModel: FloorViewModel = viewModel()
) {

    val floors by floorViewModel.floors
    val isLoading by floorViewModel.isLoading
    val errorMessage by floorViewModel.errorMessage

    Column(
        modifier = Modifier
            .fillMaxSize()
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

            CircularProgressIndicator(
                modifier = Modifier.padding(top = 24.dp)
            )

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
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloorCard(
    floor: Floor,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = floor.name,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Level ${floor.level}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}