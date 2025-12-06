package com.example.iot_mobile.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    //const val BASE_URL = "http://10.0.2.2:4000/api" // Usa la IP local del backend
    const val BASE_URL = "http://64.226.100.1:4000/api" // Usa la IP publica (nube) del backend


    /**
     * Método para realizar una petición GET en segundo plano.
     * @param endpoint Ruta del recurso
     * @return Respuesta en formato JSON o `null` si hay error.
     */
    suspend fun get(endpoint: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val response = connection.inputStream.bufferedReader().readText()

            println("Código de respuesta: $responseCode")
            println("Respuesta del servidor: $response")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                response
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error al conectar con el backend: ${e.message}")
            null
        }
    }

    /**
     * Método para obtener los detalles de una habitación por ID.
     * @param roomId ID de la habitación
     * @return Respuesta en formato JSON o `null` si hay error.
     */
    suspend fun getRoomById(roomId: Int): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/rooms/$roomId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                println("Código de respuesta: $responseCode")
                println("Respuesta del servidor (Room $roomId): $response")
                response
            } else {
                Log.e("ApiClient", "Error al obtener habitación: código $responseCode")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ApiClient", "Error al conectar con el backend: ${e.message}")
            null
        }
    }

    /**
     * Método para realizar una petición POST en segundo plano.
     * @param endpoint Ruta del recurso
     * @param jsonBody Cuerpo de la solicitud en formato JSON.
     * @return Respuesta en formato JSON o `null` si hay error.
     */
    suspend fun post(endpoint: String, jsonBody: JSONObject): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/$endpoint")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Escribir el cuerpo de la petición
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                Log.d("ApiClient", "Código de respuesta: $responseCode")

                return@withContext if (responseCode in 200..299) { // Acepta códigos 2XX
                    connection.inputStream.bufferedReader()
                        .use { it.readText() } // Lee la respuesta correctamente
                } else {
                    Log.e("ApiClient", "Error en la respuesta del servidor: código $responseCode")
                    connection.errorStream?.bufferedReader()
                        ?.use { it.readText() } // Leer el mensaje de error si lo hay
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ApiClient", "Error de conexión con el backend: ${e.message}")
                null
            }
        }

    /**
     * Método para crear un nuevo usuario (registro).
     * @param nombre Nombre completo del usuario
     * @param correo Correo electrónico del usuario
     * @param contrasena Contraseña del usuario
     * @param preferenciaTemperatura Preferencia de temperatura (COLD, WARM, HOT)
     * @return Respuesta en formato JSON con los datos del usuario creado o `null` si hay error.
     */
    suspend fun createUser(
        nombre: String,
        correo: String,
        contrasena: String,
        preferenciaTemperatura: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("nombre", nombre)
                put("correo", correo)
                put("contrasena", contrasena)
                put("preferenciaTemperatura", preferenciaTemperatura)
                put("esAdmin", false)
            }

            val url = URL("$BASE_URL/users")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Escribir el cuerpo de la petición
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            Log.d("ApiClient", "CreateUser - Código de respuesta: $responseCode")

            return@withContext if (responseCode == HttpURLConnection.HTTP_CREATED) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiClient", "Usuario creado exitosamente: $response")
                response
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiClient", "Error al crear usuario: código $responseCode - $errorResponse")
                errorResponse // Retornar el mensaje de error para poder mostrarlo
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ApiClient", "Error al conectar con el backend en createUser: ${e.message}")
            null
        }
    }

    /**
     * Método para realizar login de usuario.
     * @param correo Correo electrónico del usuario
     * @param contrasena Contraseña del usuario
     * @return Respuesta en formato JSON con los datos del usuario o `null` si hay error.
     */
    suspend fun login(correo: String, contrasena: String, endpoint: String): String? = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("correo", correo)
                put("contrasena", contrasena)
            }

            val url = URL("$BASE_URL$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Escribir el cuerpo de la petición
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            Log.d("ApiClient", "Login - Código de respuesta: $responseCode")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiClient", "Login exitoso: $response")
                response
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiClient", "Error en login: código $responseCode - $errorResponse")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ApiClient", "Error al conectar con el backend en login: ${e.message}")
            null
        }
    }

    /**
     * Método para actualizar un usuario.
     * @param userId ID del usuario
     * @param updateData Datos a actualizar (puede incluir nombre, correo, preferenciaTemperatura, etc.)
     * @return Respuesta en formato JSON con los datos actualizados o `null` si hay error.
     */
    suspend fun updateUser(userId: Int, updateData: JSONObject): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/users/$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Escribir el cuerpo de la petición
            connection.outputStream.use { os ->
                os.write(updateData.toString().toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            Log.d("ApiClient", "UpdateUser - Código de respuesta: $responseCode")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiClient", "Usuario actualizado exitosamente: $response")
                response
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiClient", "Error al actualizar usuario: código $responseCode - $errorResponse")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ApiClient", "Error al conectar con el backend en updateUser: ${e.message}")
            null
        }
    }

    /**
     * Método para eliminar un usuario.
     * @param userId ID del usuario a eliminar
     * @return Respuesta en formato JSON con el mensaje de confirmación o `null` si hay error.
     */
    suspend fun deleteUser(userId: Int): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/users/$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            Log.d("ApiClient", "DeleteUser - Código de respuesta: $responseCode")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("ApiClient", "Usuario eliminado exitosamente: $response")
                response
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiClient", "Error al eliminar usuario: código $responseCode - $errorResponse")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ApiClient", "Error al conectar con el backend en deleteUser: ${e.message}")
            null
        }
    }

    /**
     * Método para obtener la imagen QR de un usuario.
     * @param userId ID del usuario
     * @return Array de bytes con la imagen PNG o `null` si hay error.
     */
    suspend fun getQRImage(userId: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/users/qr-image/$userId")  // ← AQUÍ
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "image/png")

            val responseCode = connection.responseCode
            Log.d("ApiClient", "GetQRImage - Código de respuesta: $responseCode")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                val imageBytes = connection.inputStream.readBytes()
                Log.d("ApiClient", "QR obtenido exitosamente: ${imageBytes.size} bytes")
                imageBytes
            } else {
                Log.e("ApiClient", "Error al obtener QR: código $responseCode")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ApiClient", "Error al conectar con el backend en getQRImage: ${e.message}")
            null
        }
    }

    /**
     * Solicitar reset de contraseña
     */
    suspend fun forgotPassword(correo: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/users/forgot-password")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("correo", correo)
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            Log.d("ApiClient", "ForgotPassword - Código: $responseCode")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiClient", "Error: $error")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiClient", "Error en forgotPassword: ${e.message}")
            null
        }
    }

    /**
     * Verificar token de reset
     */
    suspend fun verifyResetToken(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/users/verify-reset-token/$token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ApiClient", "Error en verifyResetToken: ${e.message}")
            null
        }
    }

    /**
     * Reset de contraseña
     */
    suspend fun resetPassword(token: String, nuevaContrasena: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/users/reset-password")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("token", token)
                put("nuevaContrasena", nuevaContrasena)
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ApiClient", "Error en resetPassword: ${e.message}")
            null
        }
    }


    /**
     * Registrar entrada/salida en una habitación
     */
    suspend fun registerRoomAccess(userId: Int, roomCode: String): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/access")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val requestBody = JSONObject().apply {
                put("userId", userId)
                put("roomCode", roomCode)
            }

            Log.d("ApiClient", "URL: $url")
            Log.d("ApiClient", "Body: $requestBody")

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
                os.flush()
            }

            val code = connection.responseCode
            Log.d("ApiClient", "Response Code: $code")

            // SIEMPRE devolver el cuerpo (éxito o error)
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""

            Log.d("ApiClient", "Response Text: $response")

            // NUNCA devuelvas null si hay texto
            return@withContext if (response.isNotEmpty()) response else null

        } catch (e: Exception) {
            Log.e("ApiClient", "Exception: ${e.message}", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Get current user info (including activeRoomCode) by userId
     */
    suspend fun getCurrentUser(userId: Int): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/users/me?userId=$userId")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (response.isNotEmpty()) response else null
        } catch (e: Exception) {
            Log.e("ApiClient", "getCurrentUser error: ${e.message}", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Obtener recomendación de sala usando el sistema ML del backend
     */
    suspend fun getRoomRecommendation(
        userId: Int?,
        preferences: UserPreferences
    ): RoomRecommendation? = withContext(Dispatchers.IO) {
        try {
            Log.d("ApiClient", "==================== GET ML RECOMMENDATION ====================")
            Log.d("ApiClient", "UserId: ${userId ?: "anónimo"}")
            Log.d("ApiClient", "Preferences: capacity=${preferences.preferredCapacity}, time=${preferences.preferredTimeSlot}")

            val requestBody = JSONObject().apply {
                userId?.let { put("userId", it) }
                preferences.preferredCapacity?.let { put("preferredCapacity", it) }
                preferences.preferredTimeSlot?.let { put("preferredTimeSlot", it) }
            }

            val response = post("recommendations", requestBody)

            if (response == null) {
                Log.e("ApiClient", "Response is null")
                return@withContext null
            }

            val jsonResponse = JSONObject(response)

            if (!jsonResponse.optBoolean("success", false)) {
                Log.e("ApiClient", "API returned success: false")
                val message = jsonResponse.optString("message", "Error desconocido")
                Log.e("ApiClient", "Message: $message")
                return@withContext null
            }

            val recommendationJson = jsonResponse.getJSONObject("recommendation")
            val featuresJson = recommendationJson.getJSONObject("features")

            val reasonsArray = recommendationJson.getJSONArray("reasons")
            val reasons = mutableListOf<String>()
            for (i in 0 until reasonsArray.length()) {
                reasons.add(reasonsArray.getString(i))
            }

            val recommendation = RoomRecommendation(
                roomId = recommendationJson.getInt("roomId"),
                roomName = recommendationJson.getString("roomName"),
                roomCode = recommendationJson.optString("roomCode", ""),
                compatibilityScore = recommendationJson.getDouble("score").toFloat(),
                reasons = reasons,
                features = RoomFeatures(
                    temperature = featuresJson.optDouble("temperature", 0.0),
                    light = featuresJson.optDouble("light", 0.0),
                    humidity = featuresJson.optDouble("humidity", 0.0),
                    capacity = featuresJson.getInt("capacity"),
                    currentOccupancy = featuresJson.getInt("currentOccupancy"),
                    occupancyRate = featuresJson.getDouble("occupancyRate").toFloat()
                )
            )

            Log.d("ApiClient", "Recomendación obtenida: ${recommendation.roomName}")
            Log.d("ApiClient", "   Score: ${(recommendation.compatibilityScore * 100).toInt()}%")
            Log.d("ApiClient", "   Razones: ${recommendation.reasons.size}")
            recommendation.reasons.forEachIndexed { index, reason ->
                Log.d("ApiClient", "   ${index + 1}. $reason")
            }
            Log.d("ApiClient", "==================== END REQUEST ====================")

            return@withContext recommendation

        } catch (e: JSONException) {
            Log.e("ApiClient", "Error parsing JSON: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            Log.e("ApiClient", "Exception in getRoomRecommendation: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtener múltiples recomendaciones (wrapper de compatibilidad)
     */
    suspend fun getRoomRecommendations(
        userId: Int?,
        preferences: UserPreferences,
        topN: Int = 5
    ): List<RoomRecommendation>? = withContext(Dispatchers.IO) {
        try {
            Log.d("ApiClient", "==================== GET TOP $topN RECOMMENDATIONS ====================")

            val recommendation = getRoomRecommendation(userId, preferences)

            if (recommendation != null) {
                Log.d("ApiClient", "✓ 1 recomendación generada (top sala)")
                return@withContext listOf(recommendation)
            } else {
                Log.e("ApiClient", "No se pudo obtener recomendación")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e("ApiClient", "Exception in getRoomRecommendations: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Enviar feedback sobre una recomendación para entrenar el modelo ML
     * @param userId ID del usuario
     * @param roomId ID de la sala recomendada
     * @param rating Calificación de 1 a 5
     * @param actualUsage Tiempo real de uso en minutos (opcional)
     * @param satisfaction Nivel de satisfacción: "satisfied", "neutral", "unsatisfied" (opcional)
     * @return Respuesta del servidor con métricas de entrenamiento
     */
    suspend fun sendRecommendationFeedback(
        userId: Int,
        roomId: Int,
        rating: Int,
        actualUsage: Int? = null,
        satisfaction: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("ApiClient", "==================== SEND RECOMMENDATION FEEDBACK ====================")
            Log.d("ApiClient", "UserId: $userId")
            Log.d("ApiClient", "RoomId: $roomId")
            Log.d("ApiClient", "Rating: $rating/5")
            Log.d("ApiClient", "Actual Usage: ${actualUsage ?: "N/A"} minutes")
            Log.d("ApiClient", "Satisfaction: ${satisfaction ?: "N/A"}")

            val requestBody = JSONObject().apply {
                put("userId", userId)
                put("roomId", roomId)
                put("rating", rating)
                actualUsage?.let { put("actualUsage", it) }
                satisfaction?.let { put("satisfaction", it) }
            }

            val response = post("recommendations/train", requestBody)

            if (response != null) {
                val jsonResponse = JSONObject(response)
                if (jsonResponse.optBoolean("success", false)) {
                    Log.d("ApiClient", "Feedback enviado correctamente")
                    val metrics = jsonResponse.optJSONObject("metrics")
                    if (metrics != null) {
                        Log.d("ApiClient", "Mejora del modelo: ${metrics.optDouble("improvement", 0.0) * 100}%")
                    }
                } else {
                    Log.e("ApiClient", "Error en feedback: ${jsonResponse.optString("message")}")
                }
            }

            Log.d("ApiClient", "==================== END FEEDBACK ====================")
            return@withContext response

        } catch (e: JSONException) {
            Log.e("ApiClient", "Error parsing JSON in sendRecommendationFeedback: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            Log.e("ApiClient", "Exception in sendRecommendationFeedback: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtener historial de accesos del usuario
     * @param userId ID del usuario
     * @param limit Número máximo de registros a obtener (por defecto 50)
     * @return Lista de entradas de historial o null si hay error
     */
    suspend fun getUserHistory(
        userId: Int,
        limit: Int = 50
    ): List<HistoryEntry>? = withContext(Dispatchers.IO) {
        try {
            Log.d("ApiClient", "==================== GET USER HISTORY ====================")
            Log.d("ApiClient", "UserId: $userId")
            Log.d("ApiClient", "Limit: $limit")

            val response = get("users/$userId/history?limit=$limit")

            if (response == null) {
                Log.e("ApiClient", "Response is null")
                return@withContext null
            }

            val jsonResponse = JSONObject(response)

            if (!jsonResponse.optBoolean("success", false)) {
                Log.e("ApiClient", "API returned success: false")
                return@withContext null
            }

            val historyArray = jsonResponse.getJSONArray("history")
            val historyList = mutableListOf<HistoryEntry>()

            for (i in 0 until historyArray.length()) {
                val entry = historyArray.getJSONObject(i)
                historyList.add(
                    HistoryEntry(
                        id = entry.getInt("id"),
                        action = entry.getString("action"),
                        timestamp = entry.getString("timestamp"),
                        duration = entry.optInt("duration", 0),
                        roomId = entry.getInt("roomId"),
                        roomName = entry.getString("roomName"),
                        roomCode = entry.getString("roomCode"),
                        temperature = entry.optDouble("temperature", 0.0),
                        light = entry.optDouble("light", 0.0),
                        humidity = entry.optDouble("humidity", 0.0)
                    )
                )
            }

            Log.d("ApiClient", "Historial obtenido: ${historyList.size} entradas")
            Log.d("ApiClient", "==================== END HISTORY ====================")

            return@withContext historyList

        } catch (e: JSONException) {
            Log.e("ApiClient", "Error parsing JSON in getUserHistory: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            Log.e("ApiClient", "Exception in getUserHistory: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtener estadísticas de uso del usuario
     * @param userId ID del usuario
     * @return Estadísticas del usuario o null si hay error
     */
    suspend fun getUserStats(userId: Int): UserStats? = withContext(Dispatchers.IO) {
        try {
            Log.d("ApiClient", "==================== GET USER STATS ====================")
            Log.d("ApiClient", "UserId: $userId")

            val response = get("users/$userId/stats")

            if (response == null) {
                Log.e("ApiClient", "Response is null")
                return@withContext null
            }

            val jsonResponse = JSONObject(response)

            if (!jsonResponse.optBoolean("success", false)) {
                Log.e("ApiClient", "API returned success: false")
                return@withContext null
            }

            val statsJson = jsonResponse.getJSONObject("stats")

            // Parsear salas más visitadas
            val mostVisitedArray = statsJson.getJSONArray("mostVisitedRooms")
            val mostVisitedRooms = mutableListOf<MostVisitedRoom>()

            for (i in 0 until mostVisitedArray.length()) {
                val room = mostVisitedArray.getJSONObject(i)
                mostVisitedRooms.add(
                    MostVisitedRoom(
                        id = room.getInt("id"),
                        name = room.getString("name"),
                        code = room.getString("code"),
                        visitCount = room.getInt("visitCount")
                    )
                )
            }

            val stats = UserStats(
                totalVisits = statsJson.getInt("totalVisits"),
                avgDuration = statsJson.getDouble("avgDuration"),
                mostVisitedRooms = mostVisitedRooms,
                preferredHour = statsJson.getInt("preferredHour")
            )

            Log.d("ApiClient", "Estadísticas obtenidas:")
            Log.d("ApiClient", "   Total visitas: ${stats.totalVisits}")
            Log.d("ApiClient", "   Duración promedio: ${stats.avgDuration} min")
            Log.d("ApiClient", "   Hora preferida: ${stats.preferredHour}:00")
            Log.d("ApiClient", "   Salas más visitadas: ${stats.mostVisitedRooms.size}")
            Log.d("ApiClient", "==================== END STATS ====================")

            return@withContext stats

        } catch (e: JSONException) {
            Log.e("ApiClient", "Error parsing JSON in getUserStats: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            Log.e("ApiClient", "Exception in getUserStats: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

// ============================================================================
// MODELOS DE DATOS
// ============================================================================

/**
 * Entrada de historial de acceso
 */
data class HistoryEntry(
    val id: Int,
    val action: String,
    val timestamp: String,
    val duration: Int,
    val roomId: Int,
    val roomName: String,
    val roomCode: String,
    val temperature: Double,
    val light: Double,
    val humidity: Double
)

/**
 * Estadísticas de uso del usuario
 */
data class UserStats(
    val totalVisits: Int,
    val avgDuration: Double,
    val mostVisitedRooms: List<MostVisitedRoom>,
    val preferredHour: Int
)

/**
 * Sala más visitada
 */
data class MostVisitedRoom(
    val id: Int,
    val name: String,
    val code: String,
    val visitCount: Int
)

/**
 * Recomendación de sala generada por el sistema ML
 */
data class RoomRecommendation(
    val roomId: Int,
    val roomName: String,
    val roomCode: String = "",
    val compatibilityScore: Float,
    val reasons: List<String>,
    val features: RoomFeatures? = null
)

/**
 * Características detalladas de una sala
 */
data class RoomFeatures(
    val temperature: Double?,
    val light: Double?,
    val humidity: Double?,
    val capacity: Int,
    val currentOccupancy: Int,
    val occupancyRate: Float
)

/**
 * Preferencias del usuario para obtener recomendaciones
 */
data class UserPreferences(
    val preferredCapacity: String = "medium",
    val preferredAmenities: List<String> = emptyList(),
    val noiseTolerance: Float = 0.5f,
    val preferredTimeSlot: String = "morning"
)