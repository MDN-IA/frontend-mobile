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

data class Room(
    val name: String,
    val temperature: Int,
    val isAvailable: Boolean,
    val temperatureType: TemperatureType
)

enum class TemperatureType {
    COLD, WARM, HOT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    var selectedPreference by remember { mutableStateOf("COLD") }

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
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 2.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                            horizontalArrangement = Arrangement.Start
                        ) {
                            SinglePreferenceChip(
                                selectedPreference = selectedPreference
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 2.dp)
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
fun SinglePreferenceChip(
    selectedPreference: String
) {
    val (label, range, color) = when (selectedPreference) {
        "COLD" -> Triple("COLD", "< 20°C", Color(0xFF42A5F5))
        "WARM" -> Triple("WARM", "20-23°C", Color(0xFFFFB74D))
        else -> Triple("HOT", "> 23°C", Color(0xFFFF7043))
    }

    Surface(
        modifier = Modifier
            .height(56.dp)
            .widthIn(min = 160.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.9f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
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
                Text(
                    text = room.name,
                    fontSize = 12.sp,
                    fontWeight = if (matchesPreference && room.isAvailable) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor,
                    letterSpacing = 0.8.sp
                )

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