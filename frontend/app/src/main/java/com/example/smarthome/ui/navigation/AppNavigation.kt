package com.example.smarthome.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smarthome.ui.camera.CameraScreen
import com.example.smarthome.ui.floor.FloorManagementScreen
import com.example.smarthome.ui.floorplan.FloorPlanScreen
import com.example.smarthome.ui.home.HomeScreen
import com.example.smarthome.ui.main.MainScreen
import com.example.smarthome.ui.reports.ReportsScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    MainScreen(
        navController = navController
    ) {

        NavHost(
            navController = navController,
            startDestination = Routes.HOME
        ) {

            // HOME
            composable(Routes.HOME) {

                HomeScreen()
            }

            // FLOORS
            composable(Routes.FLOORS) {

                FloorManagementScreen(

                    onFloorClick = { floor ->

                        navController.navigate(
                            "floor_plan/${floor.id}/${floor.name}"
                        )
                    }
                )
            }

            // FLOOR PLAN
            composable(
                route = Routes.FLOOR_PLAN,

                arguments = listOf(

                    navArgument("floorId") {
                        type = NavType.StringType
                    },

                    navArgument("floorName") {
                        type = NavType.StringType
                    }
                )

            ) { backStackEntry ->

                val floorId =
                    backStackEntry.arguments
                        ?.getString("floorId")
                        ?: ""

                val floorName =
                    backStackEntry.arguments
                        ?.getString("floorName")
                        ?: "Floor"

                FloorPlanScreen(

                    floorId = floorId,

                    floorName = floorName,

                    onDeviceClick = { device ->

                        // Device control will be added next.
                    }
                )
            }

            // CAMERA
            composable(Routes.CAMERA) {

                CameraScreen()
            }

            // REPORTS
            composable(Routes.REPORTS) {

                ReportsScreen()
            }
        }
    }
}