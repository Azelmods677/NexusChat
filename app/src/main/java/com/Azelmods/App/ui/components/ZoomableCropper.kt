package com.Azelmods.App.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Zoomable image cropper component (like WhatsApp)
 * Supports pinch-to-zoom and pan gestures
 */
@Composable
fun ZoomableCropper(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Zoomable image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)

                            // Calculate max offset based on scale
                            val maxX = (size.width * (scale - 1)) / 2
                            val maxY = (size.height * (scale - 1)) / 2

                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(imageUri)
                            .build()
                    ),
                    contentDescription = "Crop image",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            }

            // Top bar with instructions
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pinch to zoom, drag to reposition",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Zoom indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Cancel button
                FloatingActionButton(
                    onClick = onDismiss,
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Reset button
                FloatingActionButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Text(
                        text = "1:1",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Confirm button
                FloatingActionButton(
                    onClick = {
                        // BUG ORIGINAL: acá se devolvía imageUri sin transformar. El zoom/pan
                        // vivía solo en graphicsLayer (efecto de RENDER, nunca toca los píxeles),
                        // así que "confirmar" ignoraba silenciosamente lo que el usuario encuadró.
                        // Ahora se proyecta el viewport sobre el bitmap y se recorta de verdad.
                        if (!isProcessing) {
                            isProcessing = true
                            scope.launch {
                                val cropped = cropVisibleRegion(
                                    context = context,
                                    sourceUri = imageUri,
                                    scale = scale,
                                    offset = offset,
                                    viewport = viewportSize,
                                    fitInside = true
                                )
                                isProcessing = false
                                onConfirm(cropped ?: imageUri)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Confirm",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Circular cropper for profile photos
 */
@Composable
fun CircularCropper(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // El viewport de recorte es la caja circular de 300.dp, no la pantalla completa.
    val cropBoxPx = with(LocalDensity.current) { 300.dp.roundToPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Zoomable image with circular clip
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)

                            val maxX = (size.width * (scale - 1)) / 2
                            val maxY = (size.height * (scale - 1)) / 2

                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Circular preview
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(imageUri)
                                .build()
                        ),
                        contentDescription = "Crop image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Instructions
            Text(
                text = "Adjust your profile photo",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            )

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FloatingActionButton(
                    onClick = onDismiss,
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Close, "Cancel")
                }

                FloatingActionButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ) {
                    Text("Reset", fontSize = 12.sp)
                }

                FloatingActionButton(
                    onClick = {
                        // Mismo bug que ZoomableCropper: se confirmaba el URI original sin
                        // aplicar el encuadre. Se recorta el cuadrado visible dentro del
                        // círculo; la UI que muestre el avatar lo clipea a círculo al renderizar.
                        if (!isProcessing) {
                            isProcessing = true
                            scope.launch {
                                val cropped = cropVisibleRegion(
                                    context = context,
                                    sourceUri = imageUri,
                                    scale = scale,
                                    offset = offset,
                                    viewport = IntSize(cropBoxPx, cropBoxPx),
                                    fitInside = false
                                )
                                isProcessing = false
                                onConfirm(cropped ?: imageUri)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, "Confirm")
                    }
                }
            }
        }
    }
}

/**
 * Recorta la región del bitmap visible dentro del viewport y la persiste en cache.
 *
 * Cómo funciona: la UI dibuja el bitmap con un ContentScale base (Fit o Crop),
 * y encima graphicsLayer aplica zoom (origen = centro del viewport) y pan.
 * Para recortar se invierte esa cadena de transformaciones: se proyectan las
 * esquinas del viewport a coordenadas del layer y de ahí a píxeles del bitmap.
 *
 *   p_layer  = (p_pantalla - offset - centro) / scale + centro
 *   p_bitmap = (p_layer - origenContenido) / escalaBase
 *
 * @param fitInside true = ContentScale.Fit (cropper rectangular);
 *                  false = ContentScale.Crop (cropper circular).
 * @return Uri del archivo recortado en cacheDir, o null si algo falla
 *         (el caller decide el fallback).
 */
private suspend fun cropVisibleRegion(
    context: Context,
    sourceUri: Uri,
    scale: Float,
    offset: Offset,
    viewport: IntSize,
    fitInside: Boolean
): Uri? = withContext(Dispatchers.Default) {
    try {
        if (viewport.width == 0 || viewport.height == 0) return@withContext null

        // ALLOCATOR_SOFTWARE: los hardware bitmaps no permiten leer píxeles
        // (Bitmap.createBitmap lanzaría IllegalStateException).
        val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

        val vw = viewport.width.toFloat()
        val vh = viewport.height.toFloat()
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()

        // Escala base con la que ContentScale posiciona el bitmap en el viewport.
        val baseScale = if (fitInside) min(vw / bw, vh / bh) else max(vw / bw, vh / bh)
        // Origen (top-left) del contenido dentro del viewport, centrado.
        val contentLeft = (vw - bw * baseScale) / 2f
        val contentTop = (vh - bh * baseScale) / 2f

        val centerX = vw / 2f
        val centerY = vh / 2f

        // Proyección inversa de las esquinas del viewport al espacio del layer.
        fun toLayerX(screenX: Float) = (screenX - offset.x - centerX) / scale + centerX
        fun toLayerY(screenY: Float) = (screenY - offset.y - centerY) / scale + centerY

        // Del layer a píxeles del bitmap, acotado a los bordes reales.
        val left = (((toLayerX(0f) - contentLeft) / baseScale)).coerceIn(0f, bw)
        val top = (((toLayerY(0f) - contentTop) / baseScale)).coerceIn(0f, bh)
        val right = (((toLayerX(vw) - contentLeft) / baseScale)).coerceIn(0f, bw)
        val bottom = (((toLayerY(vh) - contentTop) / baseScale)).coerceIn(0f, bh)

        val cropX = left.roundToInt()
        val cropY = top.roundToInt()
        val cropW = (right - left).roundToInt().coerceAtLeast(1).coerceAtMost(bitmap.width - cropX)
        val cropH = (bottom - top).roundToInt().coerceAtLeast(1).coerceAtMost(bitmap.height - cropY)

        val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)

        val outFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        outFile.outputStream().use { stream ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        if (cropped !== bitmap) cropped.recycle()
        bitmap.recycle()

        Uri.fromFile(outFile)
    } catch (e: Exception) {
        android.util.Log.e("ZoomableCropper", "Error recortando imagen: ${e.message}", e)
        null
    }
}
