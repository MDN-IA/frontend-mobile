package com.example.iot_mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.iot_mobile.ui.auth.LoginScreen
import com.example.iot_mobile.ui.auth.RegisterScreen
import com.example.iot_mobile.ui.main.MainScreen
import com.example.iot_mobile.ui.profile.ProfileScreen
import com.example.iot_mobile.ui.roomdetails.RoomDetailsScreen

object NavigationRoutes {
    const val MAIN = "main"
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val PROFILE = "profile"
    const val ROOM_DETAILS = "room_details"
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
            RegisterScreen(navController)
        }

        composable(NavigationRoutes.LOGIN) {
            LoginScreen(navController)
        }

        composable(NavigationRoutes.PROFILE) {
            ProfileScreen(navController)
        }

        composable(NavigationRoutes.ROOM_DETAILS){
            RoomDetailsScreen(navController)
        }
    }
}

