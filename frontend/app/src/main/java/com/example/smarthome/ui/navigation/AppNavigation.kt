package com.example.smarthome.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smarthome.ui.auth.AuthStateStore
import com.example.smarthome.ui.auth.LoginScreen
import com.example.smarthome.ui.auth.SignUpScreen
import com.example.smarthome.ui.camera.CameraScreen
import com.example.smarthome.ui.floor.AddFloorScreen
import com.example.smarthome.ui.floor.FloorManagementScreen
import com.example.smarthome.ui.floorplan.FloorPlanScreen
import com.example.smarthome.ui.home.HomeScreen
import com.example.smarthome.ui.main.MainScreen
import com.example.smarthome.ui.reports.ReportsScreen

@Composable
fun AppNavigation() {

    val context = LocalContext.current
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthRouteResolver.determineStartDestination(AuthStateStore.hasAccount(context))
    ) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    AuthStateStore.saveAccount(context, "user@example.com")
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGNUP)
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignUpScreen(
                onSignUpSuccess = {
                    AuthStateStore.saveAccount(context, "newuser@example.com")
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGNUP) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HOME) {
            val homeNavController = rememberNavController()

            MainScreen(
                navController = homeNavController
            ) {

                NavHost(
                    navController = homeNavController,
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
                                homeNavController.navigate(
                                    "floor_plan/${floor.id}/${floor.name}"
                                )
                            },
                            onAddFloorClick = {
                                homeNavController.navigate(Routes.ADD_FLOOR)
                            }
                        )
                    }

                    // ADD FLOOR
                    composable(Routes.ADD_FLOOR) {
                        AddFloorScreen(
                            onBack = {
                                homeNavController.popBackStack()
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
                            onBack = {
                                homeNavController.popBackStack()
                            },
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
    }
}