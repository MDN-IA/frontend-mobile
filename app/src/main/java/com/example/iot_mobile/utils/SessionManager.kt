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


    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
