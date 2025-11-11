package com.example.iot_mobile.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.iot_mobile.ui.navigation.AppNavigator
import com.example.iot_mobile.ui.navigation.NavigationRoutes
import com.example.iot_mobile.ui.theme.IOTMobileTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IOTMobileTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route)
                    }
                },
                onLogout = {
                    scope.launch {
                        drawerState.close()
                        // Aquí implementarás la lógica de logout
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rooms Map") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AppNavigator(navController = navController)
            }
        }
    }
}

@Composable
fun DrawerContent(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet {
        // Header del drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "IOT Mobile",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "usuario@ejemplo.com",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        HorizontalDivider()

        // Opciones del menú
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Rooms Map") },
            selected = false,
            onClick = { onNavigate(NavigationRoutes.MAP) }
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("My Profile") },
            selected = false,
            onClick = { onNavigate(NavigationRoutes.SETTINGS) }
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            label = { Text("Logout") },
            selected = false,
            onClick = { onLogout() }
        )
    }
}