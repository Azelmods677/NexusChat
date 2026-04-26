package com.Azelmods.App.ui.screens.conversation

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONObject
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    navController: NavController,
    onQRScanned: (String, String, String) -> Unit // uid, username, name
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var scannedData by remember { mutableStateOf<ScannedUserData?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear código QR") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            cameraPermissionState.status.isGranted -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Camera preview
                    CameraPreview(
                        onQRCodeDetected = { qrContent ->
                            try {
                                val userData = parseQRCode(qrContent)
                                if (userData != null) {
                                    scannedData = userData
                                    showConfirmDialog = true
                                } else {
                                    errorMessage = "Código QR inválido. Por favor escanea un código QR de NexusChat."
                                }
                            } catch (e: Exception) {
                                Log.e("QRScanner", "Error parsing QR: ${e.message}", e)
                                errorMessage = "Error al procesar el código QR"
                            }
                        }
                    )
                    
                    // Scanning overlay
                    ScanningOverlay()
                    
                    // Instructions
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp)
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Apunta la cámara al código QR",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El código se escaneará automáticamente",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            cameraPermissionState.status.shouldShowRationale -> {
                // Permission rationale
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permiso de cámara necesario",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Para escanear códigos QR, necesitamos acceso a tu cámara.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Conceder permiso")
                    }
                }
            }
            else -> {
                // Permission denied
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permiso de cámara denegado",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Por favor, habilita el permiso de cámara en la configuración de la app.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog && scannedData != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Usuario encontrado") },
            text = {
                Column {
                    Text("¿Deseas agregar a este usuario?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nombre: ${scannedData!!.name}", fontWeight = FontWeight.Bold)
                    Text("Usuario: ${scannedData!!.username}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onQRScanned(scannedData!!.uid, scannedData!!.username, scannedData!!.name)
                        navController.popBackStack()
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Error message
    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CameraPreview(
    onQRCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    var hasScanned by remember { mutableStateOf(false) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(executor) { imageProxy ->
                        if (!hasScanned) {
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                
                                val scanner = BarcodeScanning.getClient()
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            if (barcode.valueType == Barcode.TYPE_TEXT) {
                                                barcode.rawValue?.let { qrContent ->
                                                    hasScanned = true
                                                    onQRCodeDetected(qrContent)
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close()
                        }
                    }
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera binding failed", e)
            }
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScanningOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Scanning frame size
        val frameSize = minOf(canvasWidth, canvasHeight) * 0.7f
        val left = (canvasWidth - frameSize) / 2
        val top = (canvasHeight - frameSize) / 2
        
        // Draw semi-transparent overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = Size(canvasWidth, top)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top + frameSize),
            size = Size(canvasWidth, canvasHeight - top - frameSize)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top),
            size = Size(left, frameSize)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(left + frameSize, top),
            size = Size(canvasWidth - left - frameSize, frameSize)
        )
        
        // Draw scanning frame
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(frameSize, frameSize),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
        
        // Draw corner markers
        val cornerLength = 40f
        val cornerWidth = 6f
        
        // Top-left corner
        drawLine(
            color = Color.Green,
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = Color.Green,
            start = Offset(left, top),
            end = Offset(left, top + cornerLength),
            strokeWidth = cornerWidth
        )
        
        // Top-right corner
        drawLine(
            color = Color.Green,
            start = Offset(left + frameSize, top),
            end = Offset(left + frameSize - cornerLength, top),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = Color.Green,
            start = Offset(left + frameSize, top),
            end = Offset(left + frameSize, top + cornerLength),
            strokeWidth = cornerWidth
        )
        
        // Bottom-left corner
        drawLine(
            color = Color.Green,
            start = Offset(left, top + frameSize),
            end = Offset(left + cornerLength, top + frameSize),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = Color.Green,
            start = Offset(left, top + frameSize),
            end = Offset(left, top + frameSize - cornerLength),
            strokeWidth = cornerWidth
        )
        
        // Bottom-right corner
        drawLine(
            color = Color.Green,
            start = Offset(left + frameSize, top + frameSize),
            end = Offset(left + frameSize - cornerLength, top + frameSize),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = Color.Green,
            start = Offset(left + frameSize, top + frameSize),
            end = Offset(left + frameSize, top + frameSize - cornerLength),
            strokeWidth = cornerWidth
        )
    }
}

data class ScannedUserData(
    val uid: String,
    val username: String,
    val name: String
)

fun parseQRCode(qrContent: String): ScannedUserData? {
    return try {
        val json = JSONObject(qrContent)
        val type = json.optString("type", "")
        
        if (type == "nexuschat_user") {
            ScannedUserData(
                uid = json.getString("uid"),
                username = json.getString("username"),
                name = json.getString("name")
            )
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("QRScanner", "Error parsing QR code: ${e.message}", e)
        null
    }
}
