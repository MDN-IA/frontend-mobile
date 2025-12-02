package com.example.iot_mobile.ui.recommendRoom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.iot_mobile.network.RoomRecommendation
import com.example.iot_mobile.network.UserPreferences
import com.example.iot_mobile.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun RecommendationScreen(navController: NavHostController) {
    val context = LocalContext.current
    var recommendations by remember { mutableStateOf<List<RoomRecommendation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var userPreferences by remember { mutableStateOf(UserPreferences()) }
    val scope = rememberCoroutineScope()

    // Cargar recomendaciones
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = ApiClient.getRoomRecommendations(
                userId = SessionManager(context).getUserId(),
                preferences = userPreferences
            )
            recommendations = result ?: emptyList()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .verticalScroll(rememberScrollState())
    ) {
        // Encabezado con padding específico
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Text(
                text = "AI Recommendations",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                letterSpacing = (-0.8).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Personalized room suggestions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF757575),
                letterSpacing = 0.1.sp,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Panel de preferencias con padding horizontal
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            PreferencesPanel(
                preferences = userPreferences,
                onPreferencesChange = { newPrefs ->
                    userPreferences = newPrefs
                    // Recargar recomendaciones
                    scope.launch {
                        isLoading = true
                        val result = ApiClient.getRoomRecommendations(
                            userId = SessionManager(context).getUserId(),
                            preferences = newPrefs
                        )
                        recommendations = result ?: emptyList()
                        isLoading = false
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Encabezado de resultados
        if (!isLoading && recommendations.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Your Match",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lista de recomendaciones
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF1A1A1A),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            if (recommendations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recommendations found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF424242),
                            letterSpacing = 0.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Adjust your preferences to see personalized suggestions",
                            fontSize = 13.sp,
                            color = Color(0xFFAAAAAA),
                            letterSpacing = 0.sp
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    recommendations.forEach { rec ->
                        RecommendationCard(
                            recommendation = rec,
                            onSelectRoom = { roomId ->
                                navController.navigate("room_details/$roomId")
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: RoomRecommendation,
    onSelectRoom: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        color = Color.White,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF424242))
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            // Header Section con Info
            Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = recommendation.roomName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A0A0A),
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Match Score Badge - Diseño profesional y minimalista
                    val matchScore = (recommendation.compatibilityScore * 100).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F8F4)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$matchScore% Match",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }
                    }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF0F0F0))
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Razones de recomendación
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                recommendation.reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Punto verde minimalista
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .background(
                                    color = Color(0xFF4CAF50),
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = reason,
                            fontSize = 14.sp,
                            color = Color(0xFF3A3A3A),
                            lineHeight = 21.sp,
                            letterSpacing = 0.15.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Botón de acción premium
            Button(
                onClick = { onSelectRoom(recommendation.roomId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF000000),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "View Details",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun PreferencesPanel(
    preferences: UserPreferences,
    onPreferencesChange: (UserPreferences) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Your Preferences",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A),
                letterSpacing = (-0.2).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Capacity preference
            Text(
                text = "ROOM SIZE",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9E9E9E),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("small", "medium", "large").forEach { size ->
                    FilterChip(
                        selected = preferences.preferredCapacity == size,
                        onClick = {
                            onPreferencesChange(preferences.copy(preferredCapacity = size))
                        },
                        label = {
                            Text(
                                text = size.replaceFirstChar { it.uppercase() },
                                fontSize = 13.sp,
                                fontWeight = if (preferences.preferredCapacity == size) FontWeight.SemiBold else FontWeight.Normal,
                                letterSpacing = 0.2.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFF8F8F8),
                            selectedContainerColor = Color(0xFF1A1A1A),
                            labelColor = Color(0xFF757575),
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = preferences.preferredCapacity == size,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Time slot preference
            Text(
                text = "PREFERRED TIME",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9E9E9E),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("morning", "afternoon", "evening").forEach { time ->
                    FilterChip(
                        selected = preferences.preferredTimeSlot == time,
                        onClick = {
                            onPreferencesChange(preferences.copy(preferredTimeSlot = time))
                        },
                        label = {
                            Text(
                                text = time.replaceFirstChar { it.uppercase() },
                                fontSize = 13.sp,
                                fontWeight = if (preferences.preferredTimeSlot == time) FontWeight.SemiBold else FontWeight.Normal,
                                letterSpacing = 0.2.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFF8F8F8),
                            selectedContainerColor = Color(0xFF1A1A1A),
                            labelColor = Color(0xFF757575),
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = preferences.preferredTimeSlot == time,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

