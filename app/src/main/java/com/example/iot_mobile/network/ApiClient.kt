package com.example.iot_mobile.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    const val BASE_URL = "http://10.0.2.2:4000/api" // Usa la IP local del backend
    //const val BASE_URL = "http://64.226.100.1:4000/api" // Usa la IP publica (nube) del backend


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
    suspend fun registerRoomAccess(userId: Int, roomId: Int): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/rooms/access")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("userId", userId)
                put("roomId", roomId)
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            Log.d("ApiClient", "RegisterRoomAccess - Código: $responseCode")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("ApiClient", "Error: $error")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiClient", "Error en registerRoomAccess: ${e.message}")
            null
        }
    }
}