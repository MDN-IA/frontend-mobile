package com.example.iot_mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.iot_mobile.ui.main.MainScreen

object NavigationRoutes {
    const val MAIN = "main"
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val SETTINGS = "settings"
    const val MAP = "map"
}

@Composable
fun AppNavigator(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.MAIN
    ) {

        composable(NavigationRoutes.MAIN) {
            MainScreen(navController)
        }

        composable(NavigationRoutes.REGISTER) {
        }

        composable(NavigationRoutes.LOGIN) {
            // LoginScreen(navController)
        }

        composable(NavigationRoutes.SETTINGS) {
            // SettingsScreen(navController)
        }

        composable(NavigationRoutes.MAP) {
            // MapScreen(navController)
        }
    }
}

