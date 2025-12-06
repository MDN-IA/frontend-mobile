package com.example.iot_mobile.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "iot_mobile_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_TEMP_PREFERENCE = "user_temp_preference"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_ACTIVE_ROOM_CODE = "active_room_code"

        // Keys para tracking de salas recomendadas por IA
        private const val KEY_AI_CANDIDATE_ROOM_ID = "ai_candidate_room_id"
        private const val KEY_AI_RECOMMENDED_ROOM_ID = "ai_recommended_room_id"
        private const val KEY_AI_ROOM_ENTRY_TIME = "ai_room_entry_time"
        private const val KEY_PENDING_FEEDBACK_ROOM_ID = "pending_feedback_room_id"
        private const val KEY_PENDING_FEEDBACK_DURATION = "pending_feedback_duration"
    }

    fun saveLoginSession(
        userId: Int,
        userName: String,
        userEmail: String,
        tempPreference: String?,
        isAdmin: Boolean
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            if (tempPreference != null) {
                putString(KEY_USER_TEMP_PREFERENCE, tempPreference)
            } else {
                remove(KEY_USER_TEMP_PREFERENCE)
            }
            putBoolean(KEY_IS_ADMIN, isAdmin)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun getTempPreference(): String? = prefs.getString(KEY_USER_TEMP_PREFERENCE, null)

    fun isAdmin(): Boolean = prefs.getBoolean(KEY_IS_ADMIN, false)

    fun updateTempPreference(tempPreference: String) {
        prefs.edit().putString(KEY_USER_TEMP_PREFERENCE, tempPreference).apply()
    }

    fun saveActiveRoomCode(code: String?) {
        prefs.edit().apply {
            if (!code.isNullOrEmpty()) {
                putString(KEY_ACTIVE_ROOM_CODE, code)
            } else {
                remove(KEY_ACTIVE_ROOM_CODE)
            }
            apply()
        }
    }

    fun getActiveRoomCode(): String? = prefs.getString(KEY_ACTIVE_ROOM_CODE, null)

    // ===== TRACKING DE SALAS RECOMENDADAS POR IA =====

    /**
     * Marcar una sala como candidata (usuario vio los detalles desde recomendaciones)
     * El tracking se iniciará cuando entre físicamente a la sala
     */
    fun setAICandidateRoom(roomId: Int) {
        android.util.Log.d("SessionManager", "🎯 Setting AI candidate room - RoomId: $roomId (tracking will start on physical entry)")

        prefs.edit().apply {
            putInt(KEY_AI_CANDIDATE_ROOM_ID, roomId)
            apply()
        }

        android.util.Log.d("SessionManager", "✅ AI candidate room set to $roomId")
    }

    /**
     * Obtener ID de sala candidata de IA
     */
    fun getAICandidateRoomId(): Int {
        val id = prefs.getInt(KEY_AI_CANDIDATE_ROOM_ID, -1)
        android.util.Log.d("SessionManager", "🔍 getAICandidateRoomId - Result: $id")
        return id
    }

    /**
     * Limpiar sala candidata
     */
    fun clearAICandidateRoom() {
        prefs.edit().apply {
            remove(KEY_AI_CANDIDATE_ROOM_ID)
            apply()
        }
    }

    /**
     * Iniciar tracking cuando el usuario ENTRA a una sala recomendada por la IA
     * @param roomId ID de la sala recomendada
     */
    fun startAIRoomTracking(roomId: Int) {
        val timestamp = System.currentTimeMillis()
        android.util.Log.d("SessionManager", "🟢 Starting AI tracking - RoomId: $roomId, Timestamp: $timestamp")

        prefs.edit().apply {
            putInt(KEY_AI_RECOMMENDED_ROOM_ID, roomId)
            putLong(KEY_AI_ROOM_ENTRY_TIME, timestamp)
            // Limpiar candidato ya que ahora está en tracking activo
            remove(KEY_AI_CANDIDATE_ROOM_ID)
            apply()
        }

        android.util.Log.d("SessionManager", "✅ AI tracking started successfully for room $roomId")
    }

    /**
     * Verificar si el usuario está en una sala recomendada por IA
     * @param roomId ID de la sala actual
     * @return true si la sala actual fue recomendada por IA
     */
    fun isInAIRecommendedRoom(roomId: Int): Boolean {
        val trackedRoomId = prefs.getInt(KEY_AI_RECOMMENDED_ROOM_ID, -1)
        val result = trackedRoomId == roomId && trackedRoomId != -1
        android.util.Log.d("SessionManager", "🔍 isInAIRecommendedRoom - RoomId: $roomId, TrackedRoomId: $trackedRoomId, Result: $result")
        return result
    }

    /**
     * Obtener el ID de la sala recomendada por IA actual
     */
    fun getTrackedAIRoomId(): Int {
        val id = prefs.getInt(KEY_AI_RECOMMENDED_ROOM_ID, -1)
        android.util.Log.d("SessionManager", "🔍 getTrackedAIRoomId - Result: $id")
        return id
    }

    /**
     * Finalizar tracking y preparar feedback
     *
     * IMPORTANTE: El tiempo se calcula desde el timestamp de entrada guardado en SharedPreferences.
     * Esto significa que el tracking PERSISTE entre sesiones de la app:
     * - Si el usuario cierra la app mientras está en la sala, el tiempo sigue corriendo
     * - Cuando vuelve y sale de la sala, se calcula la duración total real
     * - El cálculo es: tiempo_actual - tiempo_entrada (ambos en milisegundos)
     *
     * @return Pair<roomId, durationMinutes> o null si no hay tracking activo
     */
    fun finishAIRoomTracking(): Pair<Int, Int>? {
        val roomId = prefs.getInt(KEY_AI_RECOMMENDED_ROOM_ID, -1)
        val entryTime = prefs.getLong(KEY_AI_ROOM_ENTRY_TIME, 0L)

        android.util.Log.d("SessionManager", "🔴 Finishing AI tracking - RoomId: $roomId, EntryTime: $entryTime")

        if (roomId == -1 || entryTime == 0L) {
            android.util.Log.e("SessionManager", "❌ Cannot finish tracking - Invalid data (roomId: $roomId, entryTime: $entryTime)")
            return null
        }

        // Calcular duración REAL incluyendo tiempo con app cerrada
        val currentTime = System.currentTimeMillis()
        val durationMillis = currentTime - entryTime
        val durationMinutes = (durationMillis / 60000).toInt() // Convertir a minutos

        android.util.Log.d("SessionManager", "📊 Duration calculated - Millis: $durationMillis, Minutes: $durationMinutes")
        android.util.Log.d("SessionManager", "⏱️ Time tracked includes ALL time (even if app was closed)")

        // Guardar información para feedback pendiente
        prefs.edit().apply {
            putInt(KEY_PENDING_FEEDBACK_ROOM_ID, roomId)
            putInt(KEY_PENDING_FEEDBACK_DURATION, durationMinutes)
            remove(KEY_AI_RECOMMENDED_ROOM_ID)
            remove(KEY_AI_ROOM_ENTRY_TIME)
            apply()
        }

        android.util.Log.d("SessionManager", "✅ AI tracking finished - Pending feedback saved (RoomId: $roomId, Duration: $durationMinutes min)")

        return Pair(roomId, durationMinutes)
    }

    /**
     * Obtener información de feedback pendiente
     * @return Pair<roomId, durationMinutes> o null si no hay feedback pendiente
     */
    fun getPendingFeedback(): Pair<Int, Int>? {
        val roomId = prefs.getInt(KEY_PENDING_FEEDBACK_ROOM_ID, -1)
        val duration = prefs.getInt(KEY_PENDING_FEEDBACK_DURATION, 0)

        android.util.Log.d("SessionManager", "🔍 getPendingFeedback - RoomId: $roomId, Duration: $duration")

        if (roomId == -1) {
            android.util.Log.d("SessionManager", "ℹ️ No pending feedback (roomId is -1)")
            return null
        }

        android.util.Log.d("SessionManager", "✅ Returning pending feedback: ($roomId, $duration)")
        return Pair(roomId, duration)
    }

    /**
     * Limpiar feedback pendiente después de enviarlo
     */
    fun clearPendingFeedback() {
        prefs.edit().apply {
            remove(KEY_PENDING_FEEDBACK_ROOM_ID)
            remove(KEY_PENDING_FEEDBACK_DURATION)
            apply()
        }
    }

    /**
     * Cancelar tracking de sala recomendada por IA (si el usuario sale sin completar)
     */
    fun cancelAIRoomTracking() {
        prefs.edit().apply {
            remove(KEY_AI_RECOMMENDED_ROOM_ID)
            remove(KEY_AI_ROOM_ENTRY_TIME)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
