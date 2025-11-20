package com.example.iot_mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.iot_mobile.ui.auth.EnterResetCodeScreen
import com.example.iot_mobile.ui.auth.ForgotPasswordScreen
import com.example.iot_mobile.ui.auth.LoginScreen
import com.example.iot_mobile.ui.auth.RegisterScreen
import com.example.iot_mobile.ui.auth.ResetPasswordScreen
import com.example.iot_mobile.ui.main.MainScreen
import com.example.iot_mobile.ui.profile.ProfileScreen
import com.example.iot_mobile.ui.qr.QRScreen
import com.example.iot_mobile.ui.roomdetails.RoomDetailsScreen

object NavigationRoutes {
    const val MAIN = "main"
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val PROFILE = "profile"
    const val QR = "qr"
    const val ROOM_DETAILS = "room_details/{roomId}"
    const val FORGOT_PASSWORD = "forgot-password"
    const val ENTER_RESET_CODE = "enter-reset-code"
    const val RESET_PASSWORD = "reset-password/{resetCode}"

    fun roomDetails(roomId: Int): String {
        return "room_details/$roomId"
    }
}

@Composable
fun AppNavigator(
    navController: NavHostController,
    startDestination: String = NavigationRoutes.LOGIN,
    onLogout: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
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

        composable(NavigationRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController)
        }

        composable(NavigationRoutes.ENTER_RESET_CODE) {
            EnterResetCodeScreen(navController = navController)
        }

        composable(
            route = NavigationRoutes.RESET_PASSWORD,
            arguments = listOf(
                navArgument("resetCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val resetCode = backStackEntry.arguments?.getString("resetCode")
            ResetPasswordScreen(resetCode = resetCode, navController = navController)
        }

        composable(NavigationRoutes.PROFILE) {
            ProfileScreen(navController = navController, onLogout = onLogout)
        }

        composable(NavigationRoutes.QR) {
            QRScreen(navController = navController)
        }

        composable(
            route = NavigationRoutes.ROOM_DETAILS,
            arguments = listOf(
                navArgument("roomId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getInt("roomId") ?: 0
            RoomDetailsScreen(navController = navController, roomId = roomId)
        }
    }
}