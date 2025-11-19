package com.example.iot_mobile.ui.profile

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.iot_mobile.R
import com.example.iot_mobile.network.ApiClient
import com.example.iot_mobile.utils.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

data class UserProfile(
    val name: String,
    val email: String,
    val temperaturePreference: String,
    val notificationsEnabled: Boolean,
    val darkModeEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val initialName = sessionManager.getUserName() ?: "User"
    val initialEmail = sessionManager.getUserEmail() ?: ""
    val initialTempPref = sessionManager.getTempPreference() ?: "COLD"
    val userId = sessionManager.getUserId()

    var userProfile by remember {
        mutableStateOf(
            UserProfile(
                name = initialName,
                email = initialEmail,
                temperaturePreference = initialTempPref,
                notificationsEnabled = true,
                darkModeEnabled = false
            )
        )
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf("") }
    var editingEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var deleteConfirmPassword by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Función para cambiar la preferencia de temperatura de forma cíclica
    fun cycleTemperaturePreference() {
        val nextPref = when (userProfile.temperaturePreference) {
            "COLD" -> "WARM"
            "WARM" -> "HOT"
            "HOT" -> "COLD"
            else -> "COLD"
        }

        isUpdating = true
        coroutineScope.launch {
            try {
                val updateData = JSONObject().apply {
                    put("preferenciaTemperatura", nextPref)
                }

                val response = ApiClient.updateUser(userId, updateData)

                if (response != null) {
                    // Actualizar en la sesión local
                    sessionManager.updateTempPreference(nextPref)
                    // Actualizar el estado de la UI
                    userProfile = userProfile.copy(temperaturePreference = nextPref)
                    Log.d("ProfileScreen", "Preferencia actualizada a: $nextPref")
                } else {
                    Log.e("ProfileScreen", "Error al actualizar preferencia en el servidor")
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error al cambiar preferencia: ${e.message}")
            } finally {
                isUpdating = false
            }
        }
    }

    // Función para actualizar el perfil con verificación de contraseña
    fun updateProfile() {
        if (currentPassword.isEmpty()) {
            errorMessage = "Please enter your current password"
            showError = true
            return
        }

        if (editingName.isEmpty() || editingEmail.isEmpty()) {
            errorMessage = "Name and email cannot be empty"
            showError = true
            return
        }

        isUpdating = true
        showError = false

        coroutineScope.launch {
            try {
                // Primero verificar la contraseña haciendo login
                val loginResponse = ApiClient.login(userProfile.email, currentPassword, "/users/login")

                if (loginResponse == null) {
                    errorMessage = "Incorrect password"
                    showError = true
                    isUpdating = false
                    return@launch
                }

                // Si la contraseña es correcta, actualizar el perfil
                val updateData = JSONObject().apply {
                    put("nombre", editingName)
                    put("correo", editingEmail)
                }

                val response = ApiClient.updateUser(userId, updateData)

                if (response != null) {
                    val jsonResponse = JSONObject(response)
                    val newName = jsonResponse.getString("nombre")
                    val newEmail = jsonResponse.getString("correo")

                    // Actualizar la sesión local
                    val tempPref = sessionManager.getTempPreference()
                    val isAdmin = sessionManager.isAdmin()
                    sessionManager.saveLoginSession(userId, newName, newEmail, tempPref, isAdmin)

                    // Actualizar el estado de la UI
                    userProfile = userProfile.copy(name = newName, email = newEmail)

                    Log.d("ProfileScreen", "Perfil actualizado exitosamente")
                    showEditDialog = false
                    currentPassword = ""
                } else {
                    errorMessage = "Error updating profile. Please try again."
                    showError = true
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error al actualizar perfil: ${e.message}")
                errorMessage = "An error occurred. Please try again."
                showError = true
            } finally {
                isUpdating = false
            }
        }
    }

    // Función para cambiar la contraseña con verificación
    fun changePassword() {
        if (currentPassword.isEmpty()) {
            errorMessage = "Please enter your current password"
            showError = true
            return
        }

        if (newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            errorMessage = "Please fill all password fields"
            showError = true
            return
        }

        if (newPassword.length < 6) {
            errorMessage = "New password must be at least 6 characters"
            showError = true
            return
        }

        if (newPassword != confirmNewPassword) {
            errorMessage = "New passwords do not match"
            showError = true
            return
        }

        if (newPassword == currentPassword) {
            errorMessage = "New password must be different from current password"
            showError = true
            return
        }

        isUpdating = true
        showError = false

        coroutineScope.launch {
            try {
                // Primero verificar la contraseña actual haciendo login
                val loginResponse = ApiClient.login(userProfile.email, currentPassword, "/users/login")

                if (loginResponse == null) {
                    errorMessage = "Incorrect current password"
                    showError = true
                    isUpdating = false
                    return@launch
                }

                // Si la contraseña es correcta, actualizar con la nueva contraseña
                val updateData = JSONObject().apply {
                    put("contrasena", newPassword)
                }

                val response = ApiClient.updateUser(userId, updateData)

                if (response != null) {
                    Log.d("ProfileScreen", "Contraseña actualizada exitosamente")
                    showChangePasswordDialog = false
                    currentPassword = ""
                    newPassword = ""
                    confirmNewPassword = ""
                } else {
                    errorMessage = "Error updating password. Please try again."
                    showError = true
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error al cambiar contraseña: ${e.message}")
                errorMessage = "An error occurred. Please try again."
                showError = true
            } finally {
                isUpdating = false
            }
        }
    }

    // Función para eliminar la cuenta con verificación de contraseña
    fun deleteAccount() {
        if (deleteConfirmPassword.isEmpty()) {
            errorMessage = "Please enter your password to confirm"
            showError = true
            return
        }

        isUpdating = true
        showError = false

        coroutineScope.launch {
            try {
                // Primero verificar la contraseña haciendo login
                val loginResponse = ApiClient.login(userProfile.email, deleteConfirmPassword, "/users/login")

                if (loginResponse == null) {
                    errorMessage = "Incorrect password"
                    showError = true
                    isUpdating = false
                    return@launch
                }

                // Si la contraseña es correcta, eliminar la cuenta
                val response = ApiClient.deleteUser(userId)

                if (response != null) {
                    Log.d("ProfileScreen", "Cuenta eliminada exitosamente")
                    // Limpiar la sesión y hacer logout
                    sessionManager.clearSession()
                    showDeleteAccountDialog = false
                    onLogout()
                } else {
                    errorMessage = "Error deleting account. Please try again."
                    showError = true
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error al eliminar cuenta: ${e.message}")
                errorMessage = "An error occurred. Please try again."
                showError = true
            } finally {
                isUpdating = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo/Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
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
                                    width = 2.dp,
                                    color = Color(0xFF366FAD),
                                    shape = CircleShape
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = userProfile.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Text(
                    text = userProfile.email,
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Sección de Preferencias
            SectionHeader("Preferences")

            ProfileCard(
                icon = Icons.Outlined.Settings,
                title = "Temperature Preference",
                subtitle = if (isUpdating) "Updating..." else userProfile.temperaturePreference,
                onClick = {
                    if (!isUpdating) {
                        cycleTemperaturePreference()
                    }
                }
            )

            Spacer(modifier = Modifier.height(7.dp))

            // Sección de Cuenta
            SectionHeader("Account")

            ProfileCard(
                icon = Icons.Outlined.Edit,
                title = "Edit Profile",
                subtitle = "Update your information",
                onClick = {
                    editingName = userProfile.name
                    editingEmail = userProfile.email
                    currentPassword = ""
                    showError = false
                    showEditDialog = true
                }
            )

            ProfileCard(
                icon = Icons.Outlined.Lock,
                title = "Change Password",
                subtitle = "Update your password",
                onClick = {
                    currentPassword = ""
                    newPassword = ""
                    confirmNewPassword = ""
                    showError = false
                    showChangePasswordDialog = true
                }
            )

            Spacer (modifier = Modifier.height(12.dp))
            Card(
                onClick = {
                    deleteConfirmPassword = ""
                    showError = false
                    showDeleteAccountDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF5F5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color(0xFFE53935).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Delete Account",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            text = "Permanently delete your account",
                            fontSize = 13.sp,
                            color = Color(0xFFE57373),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFE53935).copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Logout
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF5350)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFF212121),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Logout",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog para editar perfil
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isUpdating) {
                    showEditDialog = false
                    currentPassword = ""
                    showError = false
                }
            },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large,
            title = {
                Column {
                    Text(
                        text = "Edit Profile",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Update your personal information",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Normal
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mensaje de error
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showError,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFFFF3F3),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFE53935),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Campos de entrada
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = {
                            Text(
                                "Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF42A5F5),
                            focusedLabelColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                            disabledBorderColor = Color(0xFFF5F5F5),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    OutlinedTextField(
                        value = editingEmail,
                        onValueChange = { editingEmail = it },
                        label = {
                            Text(
                                "Email",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF42A5F5),
                            focusedLabelColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                            disabledBorderColor = Color(0xFFF5F5F5),
                            disabledLabelColor = Color(0xFFBDBDBD),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    // Separador sutil con texto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                        Text(
                            text = "VERIFICATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFBDBDBD),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    }

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = {
                            Text(
                                "Current Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        placeholder = {
                            Text(
                                "Enter password to confirm",
                                fontSize = 13.sp,
                                color = Color(0xFFBDBDBD)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF42A5F5),
                            focusedLabelColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                            disabledBorderColor = Color(0xFFF5F5F5),
                            disabledLabelColor = Color(0xFFBDBDBD),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { updateProfile() },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(44.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Updating...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            "Save",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp,
                            color = Color(0xFF212121)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isUpdating) {
                            showEditDialog = false
                            currentPassword = ""
                            showError = false
                        }
                    },
                    enabled = !isUpdating,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isUpdating) Color(0xFFBDBDBD) else Color(0xFF616161)
                    )
                }
            }
        )
    }

    // Dialog para cambiar contraseña
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isUpdating) {
                    showChangePasswordDialog = false
                    currentPassword = ""
                    newPassword = ""
                    confirmNewPassword = ""
                    showError = false
                }
            },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large,
            title = {
                Column {
                    Text(
                        text = "Change Password",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your current and new password",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Normal
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mensaje de error
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showError,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFFFF3F3),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFE53935),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Separador con texto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                        Text(
                            text = "CURRENT PASSWORD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFBDBDBD),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    }

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = {
                            Text(
                                "Current Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        placeholder = {
                            Text(
                                "Enter your current password",
                                fontSize = 13.sp,
                                color = Color(0xFFBDBDBD)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF42A5F5),
                            focusedLabelColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                            disabledBorderColor = Color(0xFFF5F5F5),
                            disabledLabelColor = Color(0xFFBDBDBD),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    // Separador con texto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                        Text(
                            text = "NEW PASSWORD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFBDBDBD),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    }

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = {
                            Text(
                                "New Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        placeholder = {
                            Text(
                                "At least 6 characters",
                                fontSize = 13.sp,
                                color = Color(0xFFBDBDBD)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF42A5F5),
                            focusedLabelColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                            disabledBorderColor = Color(0xFFF5F5F5),
                            disabledLabelColor = Color(0xFFBDBDBD),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = {
                            Text(
                                "Confirm New Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        placeholder = {
                            Text(
                                "Re-enter new password",
                                fontSize = 13.sp,
                                color = Color(0xFFBDBDBD)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF42A5F5),
                            focusedLabelColor = Color(0xFF42A5F5),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                            disabledBorderColor = Color(0xFFF5F5F5),
                            disabledLabelColor = Color(0xFFBDBDBD),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { changePassword() },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(44.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Updating...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            "Change",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp,
                            color = Color(0xFF212121)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isUpdating) {
                            showChangePasswordDialog = false
                            currentPassword = ""
                            newPassword = ""
                            confirmNewPassword = ""
                            showError = false
                        }
                    },
                    enabled = !isUpdating,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isUpdating) Color(0xFFBDBDBD) else Color(0xFF616161)
                    )
                }
            }
        )
    }

    // Dialog para eliminar cuenta
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isUpdating) {
                    showDeleteAccountDialog = false
                    deleteConfirmPassword = ""
                    showError = false
                }
            },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large,
            title = {
                Column {
                    Text(
                        text = "Delete Account",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = Color(0xFFE53935),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This action cannot be undone",
                        fontSize = 13.sp,
                        color = Color(0xFFE57373),
                        fontWeight = FontWeight.Normal
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Advertencia
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3F3)
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Warning",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935)
                            )
                            Text(
                                text = "Deleting your account will:",
                                fontSize = 13.sp,
                                color = Color(0xFF616161),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "• Permanently delete all your data\n• Remove access to all your rooms\n• Cannot be recovered",
                                fontSize = 13.sp,
                                color = Color(0xFF757575),
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Mensaje de error
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showError,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFFFF3F3),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFE53935),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Separador con texto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                        Text(
                            text = "CONFIRM PASSWORD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFBDBDBD),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    }

                    OutlinedTextField(
                        value = deleteConfirmPassword,
                        onValueChange = { deleteConfirmPassword = it },
                        label = {
                            Text(
                                "Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        placeholder = {
                            Text(
                                "Enter your password to confirm",
                                fontSize = 13.sp,
                                color = Color(0xFFBDBDBD)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            focusedLabelColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                            disabledBorderColor = Color(0xFFF5F5F5),
                            disabledLabelColor = Color(0xFFBDBDBD),
                            focusedTextColor = Color(0xFF212121),
                            unfocusedTextColor = Color(0xFF212121)
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { deleteAccount() },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(44.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Deleting...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            "Delete",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp,
                            color = Color(0xFF212121)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isUpdating) {
                            showDeleteAccountDialog = false
                            deleteConfirmPassword = ""
                            showError = false
                        }
                    },
                    enabled = !isUpdating,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isUpdating) Color(0xFFBDBDBD) else Color(0xFF616161)
                    )
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF9E9E9E),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun ProfileCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFF42A5F5).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF42A5F5),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}