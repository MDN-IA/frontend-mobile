package com.example.iot_mobile.ui.roomdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class RoomDetailData(
    val name: String,
    val temperature: Float,
    val isAvailable: Boolean,
    val humidity: Int,
    val state: String, // "COLD", "WARM", "HOT"
    val recentTemperatures: List<Float> = listOf(19f, 18.5f, 20f, 19f, 18f, 19.5f, 18.5f) // Últimas 7 lecturas
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailsScreen(
    navController: NavController
) {
    // Sample data - esto vendría de tu API/backend
    val roomData = remember {
        RoomDetailData(
            name = "ROOM A",
            temperature = 18.5f,
            isAvailable = true,
            humidity = 65,
            state = "COLD",
            recentTemperatures = listOf(19f, 18.5f, 20f, 19f, 18f, 19.5f, 18.5f)
        )
    }

    val temperatureColor = when (roomData.state) {
        "COLD" -> Color(0xFF42A5F5)
        "WARM" -> Color(0xFFFFB74D)
        "HOT" -> Color(0xFFFF7043)
        else -> Color.Gray
    }

    Scaffold {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFAFAFA))
        ) {
            // Header Section con diseño minimalista
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = roomData.name,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${roomData.temperature}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        color = temperatureColor,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = "°C",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = temperatureColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = temperatureColor.copy(alpha = 0.1f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = roomData.state,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = temperatureColor,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Info Cards Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    label = "Availability",
                    value = if (roomData.isAvailable) "Available" else "Not Available",
                    color = if (roomData.isAvailable) Color(0xFF66BB6A) else Color(0xFFEF5350)
                )

                InfoCard(
                    label = "Humidity",
                    value = "${roomData.humidity}%",
                    color = Color(0xFF42A5F5)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chart Section con diseño mejorado
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Recent Temperature",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    letterSpacing = 0.15.sp
                )

                Text(
                    text = "Last 7 readings",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                SimpleBarChart(
                    data = roomData.recentTemperatures,
                    color = temperatureColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InfoCard(
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 0.4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = color, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

@Composable
fun SimpleBarChart(
    data: List<Float>,
    color: Color
) {
    val maxValue = data.maxOrNull() ?: 25f
    val minValue = data.minOrNull() ?: 15f
    val range = maxValue - minValue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                color = Color(0xFFF5F5F5),
                shape = MaterialTheme.shapes.medium
            )
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { value ->
            // Normalizar la altura proporcionalmente al rango
            val normalizedHeight = if (range > 0) {
                (value - minValue) / range
            } else {
                1f
            }

            // Altura base de 30% + 70% proporcional para que todas las barras sean visibles
            val heightFraction = 0.3f + (normalizedHeight * 0.7f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "${value}°",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF616161),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(160.dp * heightFraction)
                        .background(
                            color = color.copy(alpha = 0.85f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}