package com.example.iot_mobile.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.iot_mobile.R
import com.example.iot_mobile.ui.navigation.NavigationRoutes
import com.example.iot_mobile.network.ApiClient
import com.example.iot_mobile.utils.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Log

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo/Icon
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(
                        color = Color(0xFF42A5F5).copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Borde circular
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .then(
                            Modifier.border(
                                width = 3.dp,
                                color = Color(0xFF366FAD),
                                shape = CircleShape
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Título
            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Text(
                text = "Sign in to continue",
                fontSize = 14.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                placeholder = { Text("your.email@example.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = Color(0xFF757575)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF42A5F5),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = Color(0xFF42A5F5),
                    cursorColor = Color(0xFF42A5F5)
                ),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFF757575)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF757575)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        // Trigger login
                        if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
                            isLoading = true
                            errorMessage = null

                            coroutineScope.launch {
                                try {
                                    val response = ApiClient.login(email, password, "/users/login")

                                    if (response != null) {
                                        val jsonResponse = JSONObject(response)

                                        if (jsonResponse.has("user")) {
                                            val user = jsonResponse.getJSONObject("user")
                                            val userId = user.getInt("id")
                                            val userName = user.getString("nombre")
                                            val userEmail = user.getString("correo")
                                            val userTemp = if (user.has("preferenciaTemperatura") && !user.isNull("preferenciaTemperatura")) {
                                                user.getString("preferenciaTemperatura")
                                            } else {
                                                null
                                            }
                                            val isAdmin = user.getBoolean("esAdmin")

                                            Log.d("LoginScreen", "Login exitoso: $userName (Admin: $isAdmin, Temp: $userTemp)")

                                            // Guardar la sesión
                                            sessionManager.saveLoginSession(
                                                userId = userId,
                                                userName = userName,
                                                userEmail = userEmail,
                                                tempPreference = userTemp,
                                                isAdmin = isAdmin
                                            )

                                            navController.navigate(NavigationRoutes.MAIN) {
                                                popUpTo(NavigationRoutes.LOGIN) { inclusive = true }
                                            }
                                        } else if (jsonResponse.has("error")) {
                                            errorMessage = jsonResponse.getString("error")
                                        } else {
                                            errorMessage = "Unexpected response from server"
                                        }
                                    } else {
                                        errorMessage = "Unable to connect to server"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "An error occurred: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF42A5F5),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = Color(0xFF42A5F5),
                    cursorColor = Color(0xFF42A5F5)
                ),
                shape = MaterialTheme.shapes.medium
            )

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFEF5350),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Forgot Password
            TextButton(
                onClick = { /* Implementar recuperación de contraseña */ },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Forgot Password?",
                    color = Color(0xFF42A5F5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                    } else {
                        isLoading = true
                        errorMessage = null

                        coroutineScope.launch {
                            try {
                                val response = ApiClient.login(email, password, "/users/login")

                                if (response != null) {
                                    val jsonResponse = JSONObject(response)

                                    // Verificar si el login fue exitoso
                                    if (jsonResponse.has("user")) {
                                        val user = jsonResponse.getJSONObject("user")
                                        val userId = user.getInt("id")
                                        val userName = user.getString("nombre")
                                        val userEmail = user.getString("correo")
                                        val userTemp = if (user.has("preferenciaTemperatura") && !user.isNull("preferenciaTemperatura")) {
                                            user.getString("preferenciaTemperatura")
                                        } else {
                                            null
                                        }
                                        val isAdmin = user.getBoolean("esAdmin")

                                        Log.d("LoginScreen", "Login exitoso: $userName (Admin: $isAdmin, Temp: $userTemp)")

                                        // Guardar la sesión
                                        sessionManager.saveLoginSession(
                                            userId = userId,
                                            userName = userName,
                                            userEmail = userEmail,
                                            tempPreference = userTemp,
                                            isAdmin = isAdmin
                                        )

                                        // Navegar a la pantalla principal
                                        navController.navigate(NavigationRoutes.MAIN) {
                                            // Limpiar el back stack para que no pueda volver al login
                                            popUpTo(NavigationRoutes.LOGIN) { inclusive = true }
                                        }
                                    } else if (jsonResponse.has("error")) {
                                        errorMessage = jsonResponse.getString("error")
                                        Log.e("LoginScreen", "Error en login: $errorMessage")
                                    } else {
                                        errorMessage = "Unexpected response from server"
                                        Log.e("LoginScreen", "Respuesta inesperada: $response")
                                    }
                                } else {
                                    errorMessage = "Please, fill correctly your credentials"
                                    Log.e("LoginScreen", "No se pudo conectar con el servidor")
                                }

                            } catch (e: Exception) {
                                errorMessage = "An error occurred: ${e.message}"
                                Log.e("LoginScreen", "Excepción en login", e)
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF42A5F5),
                    disabledContainerColor = Color(0xFF42A5F5).copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider con texto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0)
                )
                Text(
                    text = "OR",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Button
            OutlinedButton(
                onClick = { navController.navigate(NavigationRoutes.REGISTER) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF42A5F5)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Color(0xFF42A5F5)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer text
            Text(
                text = "By signing in, you agree to our Terms & Privacy Policy",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}