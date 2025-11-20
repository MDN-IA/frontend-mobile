package com.example.iot_mobile.ui.qr

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.example.iot_mobile.utils.SessionManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.iot_mobile.network.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val isAdmin = sessionManager.isAdmin()
    val userId = sessionManager.getUserId()
    val userName = sessionManager.getUserName() ?: "User"

    var selectedTab by remember { mutableStateOf(if (isAdmin) 0 else 1) }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QR Code",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF616161)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF212121)
                ),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFAFAFA))
        ) {
            // Tabs - solo mostrar si es admin
            if (isAdmin) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF42A5F5),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF42A5F5)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scan QR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        selectedContentColor = Color(0xFF42A5F5),
                        unselectedContentColor = Color(0xFF757575)
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "My QR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        selectedContentColor = Color(0xFF42A5F5),
                        unselectedContentColor = Color(0xFF757575)
                    )
                }
            }

            // Contenido según la pestaña seleccionada
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isAdmin && selectedTab == 0) {
                    // Pantalla de escaneo para admin
                    QRScannerView()
                } else {
                    // Pantalla de mostrar QR (para admin y usuarios normales)
                    QRDisplayView(userId = userId, userName = userName)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScannerView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var scannedUserId by remember { mutableStateOf<String?>(null) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var selectedRoomId by remember { mutableStateOf<Int?>(null) }
    var rooms by remember { mutableStateOf<List<RoomForScanning>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var accessResult by remember { mutableStateOf<AccessResult?>(null) }

    // Cargar salas disponibles
    LaunchedEffect(Unit) {
        scope.launch {
            val roomsJson = ApiClient.get("rooms")
            roomsJson?.let {
                try {
                    val jsonArray = org.json.JSONArray(it)
                    val roomsList = mutableListOf<RoomForScanning>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        roomsList.add(
                            RoomForScanning(
                                id = obj.getInt("id"),
                                name = obj.getString("name"),
                                code = obj.getString("code")
                            )
                        )
                    }
                    rooms = roomsList
                } catch (e: Exception) {
                    Log.e("QRScannerView", "Error parsing rooms: ${e.message}")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (cameraPermissionState.status.isGranted) {
            Text(
                text = "Scan QR Code",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ← SELECTOR DE HABITACIÓN
            if (rooms.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Text(
                                text = selectedRoomId?.let { roomId ->
                                    rooms.find { it.id == roomId }?.name ?: "Select Room"
                                } ?: "Select Room",
                                fontSize = 14.sp,
                                color = Color(0xFF212121),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF757575)
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            rooms.forEach { room ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${room.name} (${room.code})",
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        selectedRoomId = room.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Vista previa de la cámara
            Surface(
                modifier = Modifier.size(300.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 4.dp,
                            color = Color(0xFF42A5F5),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    CameraPreview(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                        onQrCodeScanned = { code ->
                            scannedCode = code
                            scannedUserId = extractUserIdFromQR(code)
                        },
                        lifecycleOwner = lifecycleOwner,
                        useFrontCamera = useFrontCamera
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { useFrontCamera = !useFrontCamera }
            ) {
                Text(
                    text = if (useFrontCamera) "Switch to Back Camera" else "Switch to Front Camera",
                    color = Color(0xFF42A5F5),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar resultado
            if (accessResult != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (accessResult!!.success) 
                        Color(0xFF4CAF50).copy(alpha = 0.1f) 
                    else 
                        Color(0xFFEF5350).copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (accessResult!!.success) 
                                "✓ ${accessResult!!.action}" 
                            else 
                                "✗ ${accessResult!!.message}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (accessResult!!.success) Color(0xFF4CAF50) else Color(0xFFEF5350)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = accessResult!!.userName,
                            fontSize = 14.sp,
                            color = Color(0xFF212121),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Room: ${accessResult!!.roomName}",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Occupancy: ${accessResult!!.currentOccupancy}/${accessResult!!.capacity}",
                            fontSize = 12.sp,
                            color = Color(0xFF757575),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scannedCode = null
                        scannedUserId = null
                        accessResult = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    Text(
                        text = "Scan Another",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (scannedCode != null && selectedRoomId != null) {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            val result = ApiClient.registerRoomAccess(
                                userId = scannedUserId?.toIntOrNull() ?: 0,
                                roomId = selectedRoomId!!
                            )
                            result?.let {
                                try {
                                    val json = org.json.JSONObject(it)
                                    accessResult = AccessResult(
                                        success = json.getBoolean("success"),
                                        action = json.getString("action"),
                                        userName = json.getString("userName"),
                                        roomName = json.getString("roomName"),
                                        currentOccupancy = json.getInt("currentOccupancy"),
                                        capacity = json.getInt("capacity"),
                                        message = json.optString("message", "")
                                    )
                                } catch (e: Exception) {
                                    Log.e("QRScannerView", "Error parsing response: ${e.message}")
                                }
                            }
                            isProcessing = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Confirm Access",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    text = if (selectedRoomId == null) 
                        "Select a room first" 
                    else 
                        "Place QR code within the frame",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = "Camera Permission",
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF42A5F5).copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Camera Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We need camera access to scan QR codes",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

data class RoomForScanning(
    val id: Int,
    val name: String,
    val code: String
)

data class AccessResult(
    val success: Boolean,
    val action: String,
    val userName: String,
    val roomName: String,
    val currentOccupancy: Int,
    val capacity: Int,
    val message: String
)

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
    lifecycleOwner: LifecycleOwner,
    useFrontCamera: Boolean = false
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(useFrontCamera) {
        onDispose { }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                clipToOutline = true
            }

            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val barcodeScanner = BarcodeScanning.getClient()

                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy, onQrCodeScanned)
                    }

                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    Log.d("CameraPreview", "Camera bound successfully. Using ${if (useFrontCamera) "FRONT" else "BACK"} camera")
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Error binding camera", e)
                }
            }, executor)

            previewView
        },
        update = { previewView ->
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val barcodeScanner = BarcodeScanning.getClient()

                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy, onQrCodeScanned)
                    }

                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    Log.d("CameraPreview", "Camera updated. Using ${if (useFrontCamera) "FRONT" else "BACK"} camera")
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Error updating camera", e)
                }
            }, executor)
        },
        modifier = modifier
    )
}

@ExperimentalGetImage
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onQrCodeScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_TEXT, Barcode.TYPE_URL -> {
                            barcode.rawValue?.let { value ->
                                onQrCodeScanned(value)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("QRScanner", "Error scanning barcode", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun extractUserIdFromQR(qrCode: String): String? {
    return when {
        qrCode.startsWith("USER_ID:", ignoreCase = true) -> {
            qrCode.substringAfter("USER_ID:", "").trim()
        }
        qrCode.toIntOrNull() != null -> qrCode
        else -> qrCode.take(50)
    }
}

@Composable
fun QRDisplayView(userId: Int, userName: String) {
    val context = LocalContext.current
    var qrImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        isLoading = true
        try {
            val imageBytes = ApiClient.getQRImage(userId)
            if (imageBytes != null) {
                qrImageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                errorMessage = null
            } else {
                errorMessage = "Error al obtener la imagen QR"
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            Log.e("QRDisplayView", "Error cargando QR", e)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "My QR Code",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = userName,
            fontSize = 16.sp,
            color = Color(0xFF757575)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier
                .size(280.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "Error",
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    qrImageBitmap != null -> {
                        Image(
                            bitmap = qrImageBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF42A5F5).copy(alpha = 0.1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "User ID: $userId",
                    fontSize = 14.sp,
                    color = Color(0xFF212121),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Show this code to access rooms",
                    fontSize = 12.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}