package com.example.iot_mobile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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

    // Estados para feedback de salas recomendadas por IA
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackRoomId by remember { mutableStateOf<Int?>(null) }
    var feedbackDuration by remember { mutableStateOf(0) }
    var feedbackRoomName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Verificación inicial: si el usuario ya está en una sala al abrir la app, verificar si debe continuar tracking
    LaunchedEffect(Unit) {
        delay(500) // Esperar a que se carguen los datos iniciales

        android.util.Log.d("MainScreen", "🔍 Initial check - User in room: $activeRoomCode")

        if (activeRoomCode != null && rooms.isNotEmpty()) {
            val currentRoom = rooms.find { it.code == activeRoomCode }
            currentRoom?.let { room ->
                val trackedRoomId = sessionManager.getTrackedAIRoomId()
                val candidateRoomId = sessionManager.getAICandidateRoomId()

                android.util.Log.d("MainScreen", "🔍 Initial state - CurrentRoom: ${room.id}, TrackedRoom: $trackedRoomId, Candidate: $candidateRoomId")

                // Si el usuario está en una sala candidata pero no hay tracking activo, iniciarlo
                if (candidateRoomId == room.id && trackedRoomId == -1) {
                    android.util.Log.d("MainScreen", "🎯 User already in AI candidate room but tracking not started. Starting now...")
                    sessionManager.startAIRoomTracking(room.id)
                } else if (trackedRoomId == room.id) {
                    android.util.Log.d("MainScreen", "✅ Tracking already active for current room. Will continue counting time.")
                }
            }
        }
    }

    // Detectar cuando el usuario entra/sale de salas y manejar tracking de IA
    LaunchedEffect(activeRoomCode, rooms) {
        if (rooms.isEmpty()) return@LaunchedEffect

        android.util.Log.d("MainScreen", "🔄 activeRoomCode changed to: $activeRoomCode")

        if (activeRoomCode != null) {
            // Usuario ENTRÓ a una sala
            val currentRoom = rooms.find { it.code == activeRoomCode }
            currentRoom?.let { room ->
                val candidateRoomId = sessionManager.getAICandidateRoomId()
                val trackedRoomId = sessionManager.getTrackedAIRoomId()

                android.util.Log.d("MainScreen", "👤 User entered room: ${room.name} (id: ${room.id}). Candidate: $candidateRoomId, Tracked: $trackedRoomId")

                // Solo iniciar tracking si es sala candidata Y no hay tracking activo
                if (candidateRoomId == room.id && trackedRoomId == -1) {
                    // El usuario entró físicamente a la sala candidata de IA
                    android.util.Log.d("MainScreen", "🎯 User entered AI candidate room! Starting tracking...")
                    sessionManager.startAIRoomTracking(room.id)
                } else if (trackedRoomId == room.id) {
                    android.util.Log.d("MainScreen", "✅ Already tracking this room, continuing...")
                } else {
                    android.util.Log.d("MainScreen", "ℹ️ User entered non-AI room or different room")
                }
            }
        } else {
            // Usuario SALIÓ de una sala (activeRoomCode = null)
            val trackedRoomId = sessionManager.getTrackedAIRoomId()
            android.util.Log.d("MainScreen", "🚪 User exited room. Checking AI tracking... TrackedRoomId: $trackedRoomId")

            if (trackedRoomId != -1) {
                // Había una sala con tracking de IA, finalizar tracking y mostrar feedback
                android.util.Log.d("MainScreen", "🎯 Finishing AI tracking for room $trackedRoomId")

                val trackingInfo = sessionManager.finishAIRoomTracking()
                trackingInfo?.let { (roomId, duration) ->
                    android.util.Log.d("MainScreen", "✅ Tracking finished! RoomId: $roomId, Duration: $duration min")

                    val roomName = rooms.find { it.id == roomId }?.name ?: "Room"
                    android.util.Log.d("MainScreen", "🏠 Room name: $roomName")

                    feedbackRoomId = roomId
                    feedbackDuration = duration
                    feedbackRoomName = roomName
                    showFeedbackDialog = true

                    android.util.Log.d("MainScreen", "🎯 Feedback dialog activated! showFeedbackDialog: $showFeedbackDialog")
                } ?: run {
                    android.util.Log.e("MainScreen", "❌ Failed to finish tracking - trackingInfo is null")
                }
            } else {
                android.util.Log.d("MainScreen", "ℹ️ No AI tracking active for exited room")
            }
        }
    }

    // También verificar si hay feedback pendiente al cargar la pantalla (por si volvió más tarde)
    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty()) {
            android.util.Log.d("MainScreen", "🔍 Checking for pending feedback on room load...")
            val pendingFeedback = sessionManager.getPendingFeedback()

            android.util.Log.d("MainScreen", "📊 Pending feedback: $pendingFeedback")

            pendingFeedback?.let { (roomId, duration) ->
                android.util.Log.d("MainScreen", "✅ Found pending feedback! RoomId: $roomId, Duration: $duration min")

                val roomName = rooms.find { it.id == roomId }?.name ?: "Room"

                android.util.Log.d("MainScreen", "🏠 Room name: $roomName")

                feedbackRoomId = roomId
                feedbackDuration = duration
                feedbackRoomName = roomName
                showFeedbackDialog = true

                android.util.Log.d("MainScreen", "🎯 Feedback dialog should now show! showFeedbackDialog: $showFeedbackDialog")
            } ?: run {
                android.util.Log.d("MainScreen", "ℹ️ No pending feedback found")
            }
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFFAFAFA),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SinglePreferenceChip(
                        selectedPreference = selectedPreference
                    )

                    // NEW: show current room if any
                    if (activeRoomCode != null) {
                        CurrentRoomChip(
                            roomName = activeRoomName ?: "Room $activeRoomCode"
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
                            text = errorMessage ?: "Unknown error",
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
                                            errorMessage = "Error obtaining rooms"
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
                            Text("Retry")
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
                            text = "No rooms available",
                            color = Color(0xFF9E9E9E),
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    // Grid de habitaciones con padding inferior para la barra de IA
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 5.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(rooms) { room ->
                            RoomCard(
                                room = room,
                                matchesPreference = room.temperatureType.name == selectedPreference,
                                isCurrentRoom = room.code == activeRoomCode,
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

        // Barra flotante de IA pegada al fondo
        Surface(
            shape = MaterialTheme.shapes.small,
            color = Color.White,
            shadowElevation = 4.dp,
            border = BorderStroke(0.5.dp, Color(0xFFE8E8E8)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.75f)
                .padding(bottom = 16.dp)
                .clickable { navController.navigate(NavigationRoutes.RECOMMENDATIONS) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Recommendation",
                    modifier = Modifier.size(17.dp),
                    tint = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "AI Recommendation",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A),
                    letterSpacing = 0.1.sp
                )
            }
        }
    }

    // ========== DIÁLOGO DE FEEDBACK PARA SALAS RECOMENDADAS POR IA ==========
    android.util.Log.d("MainScreen", "🎨 Rendering MainScreen - showFeedbackDialog: $showFeedbackDialog, feedbackRoomId: $feedbackRoomId")

    if (showFeedbackDialog && feedbackRoomId != null) {
        android.util.Log.d("MainScreen", "✅ SHOWING FEEDBACK DIALOG in MainScreen - Room: $feedbackRoomName, Duration: $feedbackDuration min")

        FeedbackDialogForRooms(
            roomName = feedbackRoomName,
            durationMinutes = feedbackDuration,
            onDismiss = {
                android.util.Log.d("MainScreen", "❌ Feedback dialog dismissed")
                showFeedbackDialog = false
                sessionManager.clearPendingFeedback()
            },
            onSubmit = { rating, satisfaction ->
                coroutineScope.launch {
                    try {
                        android.util.Log.d("MainScreen", "Submitting feedback for room $feedbackRoomId: rating=$rating, duration=$feedbackDuration, satisfaction=$satisfaction")

                        val response = ApiClient.sendRecommendationFeedback(
                            userId = sessionManager.getUserId(),
                            roomId = feedbackRoomId!!,
                            rating = rating,
                            actualUsage = feedbackDuration,
                            satisfaction = satisfaction
                        )

                        if (response != null) {
                            android.util.Log.d("MainScreen", "Feedback submitted successfully: $response")
                        } else {
                            android.util.Log.e("MainScreen", "Failed to submit feedback")
                        }

                        sessionManager.clearPendingFeedback()
                        showFeedbackDialog = false
                    } catch (e: Exception) {
                        android.util.Log.e("MainScreen", "Error submitting feedback: ${e.message}", e)
                        showFeedbackDialog = false
                    }
                }
            }
        )
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
        border = BorderStroke(
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
fun CurrentRoomChip(
    roomName: String
) {
    val color = Color(0xFF4CAF50)

    Surface(
        modifier = Modifier.height(28.dp),
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.08f),
        shadowElevation = 0.dp,
        border = BorderStroke(
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
                text = "Currently in",
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
                text = roomName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121),
                letterSpacing = 0.15.sp
            )
        }
    }
}


@Composable
fun RoomCard(
    room: Room,
    matchesPreference: Boolean = false,
    isCurrentRoom: Boolean = false,
    onClick: () -> Unit = {}
) {
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
            containerColor = if (isCurrentRoom) Color(0xFFF1F8F4) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentRoom) 4.dp
            else if (matchesPreference && room.available) 3.dp
            else if (room.available) 1.dp
            else 0.dp
        ),
        shape = MaterialTheme.shapes.medium,
        border = if (isCurrentRoom) {
            BorderStroke(2.dp, Color(0xFF4CAF50))
        } else if (matchesPreference && room.available) {
            BorderStroke(1.5.dp, temperatureColor)
        } else {
            BorderStroke(0.5.dp, Color(0xFFE0E0E0))
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
                            text = "°C",
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

/**
 * Diálogo de feedback para salas recomendadas por IA
 */
@Composable
fun FeedbackDialogForRooms(
    roomName: String,
    durationMinutes: Int,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, satisfaction: String) -> Unit
) {
    var selectedRating by remember { mutableStateOf(3) }
    var selectedSatisfaction by remember { mutableStateOf("neutral") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How was your experience?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = roomName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF42A5F5),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mostrar duración
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Text(
                        text = "Duration: $durationMinutes ${if (durationMinutes == 1) "minute" else "minutes"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF616161),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Rating con estrellas
                Text(
                    text = "Rate this room",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { selectedRating = i },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text(
                                text = if (i <= selectedRating) "★" else "☆",
                                fontSize = 32.sp,
                                color = if (i <= selectedRating) Color(0xFFFFB300) else Color(0xFFE0E0E0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Satisfaction level
                Text(
                    text = "Overall satisfaction",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "poor" to "😞",
                        "neutral" to "😐",
                        "good" to "😊"
                    ).forEach { (level, emoji) ->
                        Surface(
                            onClick = { selectedSatisfaction = level },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = if (selectedSatisfaction == level) Color(0xFF42A5F5).copy(alpha = 0.15f) else Color(0xFFF5F5F5),
                            border = BorderStroke(
                                width = if (selectedSatisfaction == level) 2.dp else 1.dp,
                                color = if (selectedSatisfaction == level) Color(0xFF42A5F5) else Color(0xFFE0E0E0)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = level.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedSatisfaction == level) Color(0xFF42A5F5) else Color(0xFF757575)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedRating, selectedSatisfaction) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF42A5F5)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Submit Feedback",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
            }
        }
    )
}
