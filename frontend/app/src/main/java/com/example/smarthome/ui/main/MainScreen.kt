package com.example.smarthome.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.smarthome.ui.navigation.Routes
import androidx.compose.foundation.layout.padding

data class BottomNavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MainScreen(
    navController: NavController,
    content: @Composable () -> Unit
) {

    val items = listOf(

        BottomNavigationItem(
            route = Routes.HOME,
            label = "Home",
            icon = Icons.Default.Home
        ),

        BottomNavigationItem(
            route = Routes.FLOORS,
            label = "Floors",
            icon = Icons.Default.Map
        ),

        BottomNavigationItem(
            route = Routes.CAMERA,
            label = "Camera",
            icon = Icons.Default.CameraAlt
        ),

        BottomNavigationItem(
            route = Routes.REPORTS,
            label = "Reports",
            icon = Icons.Default.BarChart
        )
    )

    val navBackStackEntry by
    navController.currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry?.destination?.route

    Scaffold(

        bottomBar = {

            NavigationBar {

                items.forEach { item ->

                    NavigationBarItem(

                        selected =
                            currentRoute == item.route,

                        onClick = {

                            navController.navigate(
                                item.route
                            ) {

                                popUpTo(Routes.HOME) {
                                    saveState = true
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                        },

                        icon = {

                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },

                        label = {
                            Text(item.label)
                        }
                    )
                }
            }
        }

    ) { paddingValues ->

        androidx.compose.foundation.layout.Box(
            modifier =
                androidx.compose.ui.Modifier
                    .padding(paddingValues)
        ) {

            content()
        }
    }
}