package com.example.iot_mobile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.iot_mobile.network.ApiClient
import com.example.iot_mobile.ui.navigation.NavigationRoutes
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray

data class Room(
    val id: Int? = null,
    val code: String,
    val name: String,
    val temp: Float,
    val light: Float,
    val hum: Float,
    val temperatureType: TemperatureType,
    val available: Boolean
)

enum class TemperatureType {
    COLD, WARM, HOT;

    companion object {
        fun fromTemperature(temp: Float): TemperatureType {
            return when {
                temp < 20 -> COLD
                temp in 20f..23f -> WARM
                else -> HOT
            }
        }
    }
}

// Menos de 900 -> DISPONIBLE
// Mas de 900 -> NO DISPONIBLE

enum class AvailabilityStatus {
    AVAILABLE, NOT_AVAILABLE;

    companion object {
        fun fromLight(light: Float): AvailabilityStatus {
            return if (light < 900) AVAILABLE else NOT_AVAILABLE
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    onNavigate: (String) -> Unit = { route -> navController.navigate(route) }
) {
    var selectedPreference by remember { mutableStateOf("WARM") }
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Cargar y refrescar habitaciones desde el backend periódicamente
    LaunchedEffect(Unit) {
        while (true) { // Bucle infinito para refrescar continuamente
            coroutineScope.launch {
                if (rooms.isEmpty()) { // Muestra el indicador de carga solo la primera vez
                    isLoading = true
                }
                errorMessage = null

                try {
                    val response = ApiClient.get("rooms")

                    response?.let {
                        val jsonArray = JSONArray(it)
                        val fetchedRooms = mutableListOf<Room>()

                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val temperature = jsonObject.optDouble("temp", 0.0).toFloat()
                            val light = jsonObject.optDouble("light", 0.0).toFloat()
                            val humidity = jsonObject.optDouble("hum", 0.0).toFloat() //

                            fetchedRooms.add(
                                Room(
                                    id = jsonObject.optInt("id"),
                                    code = jsonObject.getString("code"),
                                    name = jsonObject.getString("name"),
                                    temp = temperature,
                                    light = light,
                                    hum = humidity,
                                    temperatureType = TemperatureType.fromTemperature(temperature),
                                    available = AvailabilityStatus.fromLight(light) == AvailabilityStatus.AVAILABLE
                                )
                            )
                        }

                        rooms = fetchedRooms
                        isLoading = false
                    } ?: run {
                        errorMessage = "Error al obtener las habitaciones"
                        isLoading = false
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    isLoading = false
                }
            }

            delay(30000L) // Espera 10 segundos antes de la siguiente actualización
        }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Temperature Preference",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9E9E9E),
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
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFAFAFA))
        ) {
            when {
                isLoading -> {
                    // Indicador de carga
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF42A5F5)
                        )
                    }
                }

                errorMessage != null -> {
                    // Mensaje de error con botón de reintentar
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Error desconocido",
                            color = Color(0xFFFF7043),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null

                                    try {
                                        val response = ApiClient.get("rooms")
                                        response?.let {
                                            val jsonArray = JSONArray(it)
                                            val fetchedRooms = mutableListOf<Room>()

                                            for (i in 0 until jsonArray.length()) {
                                                val jsonObject = jsonArray.getJSONObject(i)
                                                val temperature = jsonObject.optDouble("temp", 0.0).toFloat()
                                                val light = jsonObject.optDouble("light", 0.0).toFloat()
                                                val humidity = jsonObject.optDouble("hum", 0.0).toFloat()

                                                fetchedRooms.add(
                                                    Room(
                                                        id = jsonObject.optInt("id"),
                                                        code = jsonObject.getString("code"),
                                                        name = jsonObject.getString("name"),
                                                        temp = temperature,
                                                        light = light,
                                                        hum = humidity,
                                                        temperatureType = TemperatureType.fromTemperature(temperature),
                                                        available = AvailabilityStatus.fromLight(light) == AvailabilityStatus.AVAILABLE
                                                    )
                                                )
                                            }

                                            rooms = fetchedRooms
                                            isLoading = false
                                        } ?: run {
                                            errorMessage = "Error al obtener las habitaciones"
                                            isLoading = false
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF42A5F5)
                            )
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                rooms.isEmpty() -> {
                    // No hay habitaciones
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay habitaciones disponibles",
                            color = Color(0xFF9E9E9E),
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    // Grid de habitaciones
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(rooms) { room ->
                            RoomCard(
                                room = room,
                                matchesPreference = room.temperatureType.name == selectedPreference,
                                onClick = {
                                    room.id?.let { id ->
                                        onNavigate(NavigationRoutes.roomDetails(id))
                                    }
                                }
                            )
                        }
                    }
                }
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
fun RoomCard(room: Room, matchesPreference: Boolean = false, onClick: () -> Unit = {}) {
    val textColor = if (room.available) Color.Black else Color(0xFF9E9E9E)
    val temperatureColor = when (room.temperatureType) {
        TemperatureType.COLD -> Color(0xFF42A5F5)
        TemperatureType.WARM -> Color(0xFFFFB74D)
        TemperatureType.HOT -> Color(0xFFFF7043)
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (matchesPreference && room.available) 2.dp
            else if (room.available) 1.dp
            else 0.dp
        ),
        shape = MaterialTheme.shapes.large,
        border = if (matchesPreference && room.available) {
            androidx.compose.foundation.BorderStroke(2.dp, temperatureColor)
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEEEEEE))
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!room.available) {
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
                    fontWeight = if (matchesPreference && room.available) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor,
                    letterSpacing = 0.8.sp
                )

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${room.temp.toInt()}",
                        fontSize = if (matchesPreference && room.available) 38.sp else 35.sp,
                        fontWeight = FontWeight.Light,
                        color = if (room.available) temperatureColor else textColor,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = "°C",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light,
                        color = if (room.available) temperatureColor.copy(alpha = 0.6f) else textColor.copy(alpha = 0.6f)
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
                                color = if (room.available) temperatureColor.copy(alpha = 0.4f) else Color(0xFFBDBDBD),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}