package com.example.iot_mobile.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.iot_mobile.ui.slideshow.SlideshowScreen

object NavigationRoutes {
    const val SLIDESHOW = "slideshow"
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val SETTINGS = "settings"
    const val FRIENDS = "friends"
    const val PLANS = "plans"
}

@Composable
fun AppNavigator(
    navController: NavHostController,
    context: Context,
    startDestination: String = NavigationRoutes.SLIDESHOW
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationRoutes.SLIDESHOW) {
            SlideshowScreen(navController, context)
        }

        composable(NavigationRoutes.REGISTER) {
            // RegisterScreen(navController, context)
        }

        composable(NavigationRoutes.LOGIN) {
            // LoginScreen(navController, context)
        }

        composable(NavigationRoutes.SETTINGS) {
            // SettingsScreen(navController, context)
        }

        composable(NavigationRoutes.FRIENDS) {
            // FriendsScreen(navController, context)
        }

        composable(NavigationRoutes.PLANS) {
            // PlansScreen(navController, context)
        }
    }
}

@Composable
fun SlideshowScreen(x0: NavHostController, x1: Context) {
    TODO("Not yet implemented")
}
