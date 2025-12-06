package com.example.iot_mobile.ui.roomdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.iot_mobile.network.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject

data class RoomDetailData(
    val id: Int,
    val name: String,
    val temperature: Float,
    val isAvailable: Boolean,
    val humidity: Float,
    val light: Float,
    val state: String,
    val capacity: Int,
    val currentOccupancy: Int,
    val tempHistory: List<Float> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailsScreen(
    navController: NavController,
    roomId: Int
) {
    var roomData by remember { mutableStateOf<RoomDetailData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(roomId) {
        while (true) {
            coroutineScope.launch {
                if (roomData == null) {
                    isLoading = true
                }
                errorMessage = null

                try {
                    val response = ApiClient.getRoomById(roomId)

                    response?.let {
                        val jsonObject = JSONObject(it)
                        val temperature = jsonObject.optDouble("temp", 0.0).toFloat()
                        val light = jsonObject.optDouble("light", 0.0).toFloat()
                        val humidity = jsonObject.optDouble("hum", 0.0).toFloat()
                        val capacity = jsonObject.optInt("capacity", 30)
                        val currentOccupancy = jsonObject.optInt("currentOccupancy", 0)

                        val tempHistoryArray = mutableListOf<Float>()
                        val tempHistoryJson = jsonObject.optJSONArray("tempHistory")
                        if (tempHistoryJson != null) {
                            for (i in 0 until tempHistoryJson.length()) {
                                tempHistoryArray.add(tempHistoryJson.getDouble(i).toFloat())
                            }
                        }

                        val state = when {
                            temperature < 20 -> "COLD"
                            temperature in 20f..23f -> "WARM"
                            else -> "HOT"
                        }

                        val available = light < 900

                        roomData = RoomDetailData(
                            id = roomId,
                            name = jsonObject.getString("name"),
                            temperature = temperature,
                            isAvailable = available,
                            humidity = humidity,
                            light = light,
                            state = state,
                            capacity = capacity,
                            currentOccupancy = currentOccupancy,
                            tempHistory = tempHistoryArray
                        )

                        isLoading = false
                    } ?: run {
                        errorMessage = "Error obtaining room data"
                        isLoading = false
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    isLoading = false
                }
            }
            delay(2000L)
        }
    }

    val temperatureColor = when (roomData?.state) {
        "COLD" -> Color(0xFF42A5F5)
        "WARM" -> Color(0xFFFFB74D)
        "HOT" -> Color(0xFFFF7043)
        else -> Color.Gray
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFFAFAFA)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF42A5F5))
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFFAFAFA)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Back")
                    }
                }
            }
        } else {
            roomData?.let { room ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFAFAFA))
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF616161),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { navController.popBackStack() }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = room.name,
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
                                text = "${room.temperature}",
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
                                    text = room.state,
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
                        // ← TARJETA DE CAPACIDAD
                        CapacityCard(
                            currentOccupancy = room.currentOccupancy,
                            capacity = room.capacity
                        )

                        InfoCard(
                            label = "Availability",
                            value = if (room.isAvailable) "Available" else "Not Available",
                            color = if (room.isAvailable) Color(0xFF66BB6A) else Color(0xFFEF5350)
                        )

                        InfoCard(
                            label = "Humidity",
                            value = "${room.humidity.toInt()}%",
                            color = Color(0xFF42A5F5)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Chart Section
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
                            data = room.tempHistory,
                            color = temperatureColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CapacityCard(
    currentOccupancy: Int,
    capacity: Int
) {
    val availableSpaces = capacity - currentOccupancy
    val occupancyPercentage = (currentOccupancy.toFloat() / capacity) * 100

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Capacity",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 0.4.sp
                )

                Text(
                    text = "$currentOccupancy / $capacity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progreso
            LinearProgressIndicator(
                progress = if (capacity > 0) occupancyPercentage / 100f else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    occupancyPercentage < 50 -> Color(0xFF66BB6A)
                    occupancyPercentage < 80 -> Color(0xFFFFB74D)
                    else -> Color(0xFFEF5350)
                },
                trackColor = Color(0xFFE0E0E0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$availableSpaces available spaces",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF42A5F5)
            )
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
    if (data.isEmpty()) {
        Text("No data available", color = Color.Gray)
        return
    }

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
            val normalizedHeight = if (range > 0) {
                (value - minValue) / range
            } else {
                1f
            }

            val heightFraction = 0.3f + (normalizedHeight * 0.7f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = String.format("%.1f", value),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF616161)
                    )
                    Text(
                        text = "°C",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF757575),
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
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