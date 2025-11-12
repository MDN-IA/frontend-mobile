package com.example.iot_mobile.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var currentRoute by remember { mutableStateOf(NavigationRoutes.MAIN) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                        currentRoute = route
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
                    title = {
                        Text(
                            "Library Rooms",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF212121),
                        navigationIconContentColor = Color(0xFF616161)
                    )
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
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White
    ) {
        // Header minimalista
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = Color(0xFF42A5F5).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "John Doe",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121)
            )

            Text(
                text = "john.doe@library.com",
                fontSize = 13.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Divider(
            color = Color(0xFFF0F0F0),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Menu items minimalistas
        MinimalMenuItem(
            icon = Icons.Outlined.Home,
            label = "Library Rooms",
            isSelected = currentRoute == NavigationRoutes.MAIN,
            onClick = { onNavigate(NavigationRoutes.MAIN) }
        )

        MinimalMenuItem(
            icon = Icons.Outlined.Person,
            label = "My Profile",
            isSelected = currentRoute == NavigationRoutes.SETTINGS,
            onClick = { onNavigate(NavigationRoutes.SETTINGS) }
        )

        Spacer(modifier = Modifier.weight(1f))

        Divider(
            color = Color(0xFFF0F0F0),
            thickness = 1.dp
        )

        MinimalMenuItem(
            icon = Icons.Outlined.ExitToApp,
            label = "Logout",
            isSelected = false,
            onClick = { onLogout() },
            isDestructive = true
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun MinimalMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val backgroundColor = when {
        isSelected -> Color(0xFF42A5F5).copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    val iconColor = when {
        isDestructive -> Color(0xFFEF5350)
        isSelected -> Color(0xFF42A5F5)
        else -> Color(0xFF757575)
    }

    val textColor = when {
        isDestructive -> Color(0xFFEF5350)
        isSelected -> Color(0xFF212121)
        else -> Color(0xFF616161)
    }

    Surface(
        onClick = onClick,
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}