package com.Azelmods.App.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.asDrawable
import com.Azelmods.App.ui.theme.rememberThemeColor
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import com.Azelmods.App.ui.theme.ErrorRed
import com.Azelmods.App.ui.theme.DarkSurface

/**
 * 🖼️ FULL SCREEN IMAGE VIEWER
 * Visor de imágenes a pantalla completa con zoom, pan, descarga y swipe-to-dismiss
 * Similar a WhatsApp/Telegram - Usa Dialog fullscreen para cubrir barras del sistema
 *
 * Los `@Suppress("USELESS_IS_CHECK")` y `@Suppress("KotlinConstantConditions")`
 * que llevaba este archivo silenciaban los avisos del compilador sobre las ramas
 * `is AsyncImagePainter.State.Loading/Error`, que efectivamente eran inalcanzables
 * porque se comparaba contra un `StateFlow` en vez de contra el estado. Corregido
 * el tipo, los avisos desaparecen y las supresiones sobran.
 *
 * @param onDelete si se indica, el visor muestra un botón de eliminar operativo.
 *   Recibe `true` cuando el usuario elige borrar para todos.
 * @param canDeleteForEveryone `true` sólo si la foto la envió el propio usuario.
 */
@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    senderName: String = "",
    timestamp: String = "",
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onDelete: ((Boolean) -> Unit)? = null,
    canDeleteForEveryone: Boolean = false
) {
    // Dialog fullscreen que cubre toda la pantalla incluyendo barras del sistema
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        FullScreenImageContent(
            imageUrl = imageUrl,
            senderName = senderName,
            timestamp = timestamp,
            onDismiss = onDismiss,
            onDownload = onDownload,
            onDelete = onDelete,
            canDeleteForEveryone = canDeleteForEveryone
        )
    }
}

@Composable
private fun FullScreenImageContent(
    imageUrl: String,
    senderName: String,
    timestamp: String,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)?,
    onDelete: ((Boolean) -> Unit)? = null,
    canDeleteForEveryone: Boolean = false
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeColor = rememberThemeColor()

    // Swipe-to-dismiss state
    var swipeOffsetY by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    val dismissThreshold = 300f // pixels to trigger dismiss

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible && !isDismissing) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible && !isDismissing) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Image painter with loading state
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()
    )

    // En Coil 3, `painter.state` es un StateFlow<State>, NO un State.
    //
    // El código anterior lo trataba como si fuera el estado en sí. Dos
    // consecuencias, y las dos se notaban:
    //   · el `when (imageState)` no casaba nunca con Loading ni con Error, así
    //     que no había ni indicador de carga ni aviso de imagen rota;
    //   · `extractBitmap()` intentaba castear el StateFlow a State.Success y
    //     devolvía null SIEMPRE, de modo que Descargar, Compartir y Reenviar
    //     respondían "Espera a que cargue la imagen" aunque la foto llevara
    //     rato visible en pantalla.
    val imageState by painter.state.collectAsState()

    // Extracts the currently-loaded bitmap (if the image finished loading).
    fun extractBitmap(): Bitmap? {
        val success = painter.state.value as? AsyncImagePainter.State.Success ?: return null
        val image = success.result.image
        val drawable = image.asDrawable(context.resources)
        (drawable as? BitmapDrawable)?.bitmap?.let { return it }

        // Imágenes que no vienen respaldadas por un Bitmap (vectores, algunos
        // formatos animados) se rasterizan aquí en vez de darse por perdidas.
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: image.width
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: image.height
        if (width <= 0 || height <= 0) return null
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("FullScreenImageViewer", "No se pudo rasterizar la imagen", e)
            null
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
    }

    // Calculate background alpha based on swipe
    val bgAlpha = if (scale <= 1f) {
        (1f - (abs(swipeOffsetY) / (dismissThreshold * 2f))).coerceIn(0.3f, 1f)
    } else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha * animatedAlpha))
            .graphicsLayer {
                this.alpha = animatedAlpha
                this.scaleX = animatedScale
                this.scaleY = animatedScale
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = { tapOffset ->
                        // Double tap to zoom
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            offset = Offset(
                                (centerX - tapOffset.x) * (scale - 1f),
                                (centerY - tapOffset.y) * (scale - 1f)
                            )
                        }
                    }
                )
            }
            .pointerInput(scale) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)

                    if (scale > 1f) {
                        val maxX = (size.width * (scale - 1f)) / 2f
                        val maxY = (size.height * (scale - 1f)) / 2f

                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    } else {
                        offset = Offset.Zero
                        // Swipe-to-dismiss when not zoomed
                        swipeOffsetY += pan.y
                    }
                }
            }
            .pointerInput(scale) {
                // Detect drag end for swipe-to-dismiss
                detectDragGestures(
                    onDragEnd = {
                        if (scale <= 1f && abs(swipeOffsetY) > dismissThreshold) {
                            isDismissing = true
                            onDismiss()
                        } else {
                            swipeOffsetY = 0f
                        }
                    },
                    onDragCancel = {
                        swipeOffsetY = 0f
                    }
                ) { _, dragAmount ->
                    if (scale <= 1f) {
                        swipeOffsetY += dragAmount.y
                    }
                }
            }
    ) {
        // Image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y + (if (scale <= 1f) swipeOffsetY else 0f)
                },
            contentAlignment = Alignment.Center
        ) {
            when (imageState) {
                is AsyncImagePainter.State.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = themeColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                is AsyncImagePainter.State.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error al cargar imagen",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                else -> {
                    Image(
                        painter = painter,
                        contentDescription = "Full screen image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Top controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)),
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Sender info
                    if (senderName.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = senderName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (timestamp.isNotEmpty()) {
                                Text(
                                    text = timestamp,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // More options
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            containerColor = DarkSurface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save to device", color = Color.White) },
                                onClick = {
                                    showOptionsMenu = false
                                    onDownload?.invoke()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Download, null, tint = Color.White)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share", color = Color.White) },
                                onClick = {
                                    showOptionsMenu = false
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, imageUrl)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share image"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, null, tint = Color.White)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Close", color = Color.White) },
                                onClick = {
                                    showOptionsMenu = false
                                    onDismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share button
                    ImageViewerAction(
                        icon = Icons.Default.Share,
                        label = "Compartir",
                        onClick = {
                            val bmp = extractBitmap()
                            if (bmp == null) {
                                android.widget.Toast.makeText(context, "Espera a que cargue la imagen", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val ok = com.Azelmods.App.ui.utils.ImageActions.shareImage(context, bmp)
                                if (!ok) android.widget.Toast.makeText(context, "No se pudo compartir", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Download button
                    ImageViewerAction(
                        icon = Icons.Default.Download,
                        label = "Descargar",
                        onClick = {
                            scope.launch {
                                val bmp = extractBitmap()
                                if (bmp == null) {
                                    android.widget.Toast.makeText(context, "Espera a que cargue la imagen", android.widget.Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val ok = com.Azelmods.App.ui.utils.ImageActions.saveToGallery(context, bmp)
                                android.widget.Toast.makeText(
                                    context,
                                    if (ok) "Imagen guardada en la Galería" else "Error al guardar la imagen",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                if (ok) onDownload?.invoke()
                            }
                        }
                    )

                    // Forward button (uses the system share sheet to forward anywhere)
                    ImageViewerAction(
                        icon = Icons.AutoMirrored.Filled.Forward,
                        label = "Reenviar",
                        onClick = {
                            val bmp = extractBitmap()
                            if (bmp == null) {
                                android.widget.Toast.makeText(context, "Espera a que cargue la imagen", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                com.Azelmods.App.ui.utils.ImageActions.shareImage(context, bmp)
                            }
                        }
                    )

                    // Delete button
                    if (onDelete != null) {
                        ImageViewerAction(
                            icon = Icons.Default.Delete,
                            label = "Eliminar",
                            color = ErrorRed,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
            }
        }

        // Diálogo de borrado. "Para todos" sólo se ofrece si la foto es propia:
        // ofrecerlo sobre la de otra persona sería prometer algo que las reglas
        // del servidor rechazan.
        if (showDeleteDialog && onDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                title = { Text("¿Eliminar la foto?") },
                text = {
                    Text(
                        if (canDeleteForEveryone) {
                            "Puedes quitarla solo de tu vista o borrarla también para la otra persona."
                        } else {
                            "La foto desaparecerá de tu conversación. La otra persona la seguirá viendo."
                        }
                    )
                },
                confirmButton = {
                    if (canDeleteForEveryone) {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onDelete(true)
                            }
                        ) {
                            Text("Eliminar para todos", color = ErrorRed)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onDelete(false)
                            }
                        ) {
                            Text("Eliminar", color = ErrorRed)
                        }
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancelar")
                        }
                        if (canDeleteForEveryone) {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    onDelete(false)
                                }
                            ) {
                                Text("Solo para mí")
                            }
                        }
                    }
                },
                containerColor = DarkSurface
            )
        }

        // Swipe indicator
        if (scale <= 1f && abs(swipeOffsetY) > 50f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (swipeOffsetY > 0) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Desliza para cerrar",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Zoom indicator
        if (scale > 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ImageViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.safeClickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = color,
            fontSize = 12.sp
        )
    }
}
