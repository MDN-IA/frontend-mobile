package com.example.iot_mobile.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    const val BASE_URL = "http://10.0.2.2:4000/api" // Usa la IP local del backend
    //const val BASE_URL = "http://@IP/api" // Usa la IP publica (nube) del backend


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
}