package com.example.iot_mobile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

// Data class para representar una sala
data class Room(
    val name: String,
    val temperature: Int,
    val isAvailable: Boolean,
    val temperatureType: TemperatureType
)

enum class TemperatureType {
    COLD,    // <20°C - Blue
    WARM,    // 20-23°C - Yellow
    HOT      // >23°C - Orange
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    var selectedPreference by remember { mutableStateOf("COLD") }

    // Sample data - esto vendría de tu API/backend
    val rooms = remember {
        listOf(
            Room("ROOM A", 18, true, TemperatureType.COLD),
            Room("ROOM B", 21, true, TemperatureType.WARM),
            Room("ROOM C", 25, false, TemperatureType.HOT),
            Room("ROOM D", 22, true, TemperatureType.WARM),
            Room("ROOM E", 19, true, TemperatureType.COLD),
            Room("ROOM F", 23, false, TemperatureType.WARM),
            Room("ROOM G", 20, true, TemperatureType.WARM),
            Room("ROOM H", 24, true, TemperatureType.HOT),
            Room("ROOM I", 17, true, TemperatureType.COLD),
            Room("ROOM J", 22, false, TemperatureType.WARM),
            Room("ROOM K", 21, true, TemperatureType.WARM),
            Room("ROOM L", 19, true, TemperatureType.COLD)
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Temperature Preference",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFBDBDBD),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PreferenceChip("COLD", "< 20°C", selectedPreference, Color(0xFF42A5F5)) {
                            selectedPreference = "COLD"
                        }
                        PreferenceChip("WARM", "20-23°C", selectedPreference, Color(0xFFFFB74D)) {
                            selectedPreference = "WARM"
                        }
                        PreferenceChip("HOT", "> 23°C", selectedPreference, Color(0xFFFF7043)) {
                            selectedPreference = "HOT"
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFAFAFA))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(rooms) { room ->
                RoomCard(
                    room = room,
                    matchesPreference = room.temperatureType.name == selectedPreference
                )
            }
        }
    }
}

@Composable
fun PreferenceChip(
    text: String,
    range: String,
    selectedPreference: String,
    color: Color,
    onClick: () -> Unit
) {
    val isSelected = selectedPreference == text

    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(64.dp)
            .widthIn(min = 100.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) color.copy(alpha = 0.12f) else Color(0xFFF5F5F5),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.Black else Color(0xFF616161)
                )
                Text(
                    text = range,
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
fun RoomCard(room: Room, matchesPreference: Boolean = false) {
    val textColor = if (room.isAvailable) Color.Black else Color(0xFF9E9E9E)
    val temperatureColor = when (room.temperatureType) {
        TemperatureType.COLD -> Color(0xFF42A5F5)
        TemperatureType.WARM -> Color(0xFFFFB74D)
        TemperatureType.HOT -> Color(0xFFFF7043)
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (matchesPreference && room.isAvailable) 2.dp
                else if (room.isAvailable) 1.dp
                else 0.dp
        ),
        shape = MaterialTheme.shapes.large,
        border = if (matchesPreference && room.isAvailable) {
            androidx.compose.foundation.BorderStroke(2.dp, temperatureColor)
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEEEEEE))
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Overlay si no está disponible
            if (!room.isAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.65f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Nombre de sala
                Text(
                    text = room.name,
                    fontSize = 12.sp,
                    fontWeight = if (matchesPreference && room.isAvailable) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor,
                    letterSpacing = 0.8.sp
                )

                // Temperatura
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${room.temperature}",
                        fontSize = if (matchesPreference && room.isAvailable) 38.sp else 35.sp,
                        fontWeight = FontWeight.Light,
                        color = if (room.isAvailable) temperatureColor else textColor,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = "°C",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light,
                        color = if (room.isAvailable) temperatureColor.copy(alpha = 0.6f) else textColor.copy(alpha = 0.6f)
                    )
                }

                // Status compacto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.Right,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (room.isAvailable) temperatureColor.copy(alpha = 0.4f) else Color(0xFFBDBDBD),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}