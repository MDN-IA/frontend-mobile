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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iot_mobile.network.ApiClient
import com.example.iot_mobile.ui.navigation.NavigationRoutes
import com.example.iot_mobile.utils.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

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
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var selectedPreference = sessionManager.getTempPreference() ?: "COLD"
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var activeRoomCode by remember { mutableStateOf(sessionManager.getActiveRoomCode()) }
    var activeRoomName by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Cargar y refrescar habitaciones desde el backend periódicamente

    LaunchedEffect(Unit) {
        while (true) {
            coroutineScope.launch {
                try {
                    // 1) Refrescar activeRoomCode desde backend
                    val userId = sessionManager.getUserId()
                    if (userId != -1) {
                        val userResponse = ApiClient.getCurrentUser(userId)
                        userResponse?.let {
                            val json = JSONObject(it)
                            if (json.has("user")) {
                                val userJson = json.getJSONObject("user")
                                val code = if (userJson.has("activeRoomCode") && !userJson.isNull("activeRoomCode")) {
                                    userJson.getString("activeRoomCode")
                                } else {
                                    null
                                }
                                // guardar también en prefs por si se usa en otros sitios
                                sessionManager.saveActiveRoomCode(code)
                                activeRoomCode = code
                            }
                        }
                    }

                    // 2) Refrescar rooms
                    if (rooms.isEmpty()) {
                        isLoading = true
                    }
                    errorMessage = null

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

                        // 3) Resolver nombre de la sala actual a partir del código
                        activeRoomCode?.let { code ->
                            val found = fetchedRooms.find { it.code == code }
                            activeRoomName = found?.name
                        }

                        isLoading = false
                    } ?: run {
                        errorMessage = "Error getting rooms"
                        isLoading = false
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    isLoading = false
                }
            }

            // refresco cada 2 segundos (ajusta si quieres)
            delay(2000L)
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    SinglePreferenceChip(
                        selectedPreference = selectedPreference
                    )
                }

                // NEW: show current room if any
                if (activeRoomCode != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "You are currently in:",
                            fontSize = 11.sp,
                            color = Color(0xFF757575)
                        )
                        Text(
                            text = activeRoomName ?: "Room code: $activeRoomCode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF212121)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color(0xFFE0E0E0)
                )
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
        "COLD" -> Triple("COLD", "< 20°", Color(0xFF42A5F5))
        "WARM" -> Triple("WARM", "20-23°", Color(0xFFFFB74D))
        else -> Triple("HOT", "> 23°", Color(0xFFFF7043))
    }

    Surface(
        modifier = Modifier.height(28.dp),
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.08f),
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = color.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Temperature Preference",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                letterSpacing = 0.2.sp
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = color, shape = CircleShape)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121),
                letterSpacing = 0.15.sp
            )
            Text(
                text = range,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF9E9E9E),
                letterSpacing = 0.1.sp
            )
        }
    }
}


@Composable
fun RoomCard(room: Room, matchesPreference: Boolean = false, onClick: () -> Unit = {}) {
    val textColor = if (room.available) Color(0xFF212121) else Color(0xFF9E9E9E)
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
            defaultElevation = if (matchesPreference && room.available) 3.dp
            else if (room.available) 1.dp
            else 0.dp
        ),
        shape = MaterialTheme.shapes.medium,
        border = if (matchesPreference && room.available) {
            androidx.compose.foundation.BorderStroke(1.5.dp, temperatureColor)
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE0E0E0))
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!room.available) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = room.name,
                    fontSize = 11.sp,
                    fontWeight = if (matchesPreference && room.available) FontWeight.Bold else FontWeight.SemiBold,
                    color = textColor,
                    letterSpacing = 0.3.sp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${room.temp.toInt()}",
                            fontSize = if (matchesPreference && room.available) 36.sp else 34.sp,
                            fontWeight = FontWeight.Light,
                            color = if (room.available) temperatureColor else textColor,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = "°",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Light,
                            color = if (room.available) temperatureColor.copy(alpha = 0.7f) else textColor.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                color = if (room.available) temperatureColor else Color(0xFFBDBDBD),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}