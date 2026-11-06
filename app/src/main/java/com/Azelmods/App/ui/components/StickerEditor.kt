package com.Azelmods.App.ui.components

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

private const val STICKER_SIZE = 512
private const val MAX_SOURCE_SIZE = 1280

private val TEXT_COLORS = listOf(0xFFFFFFFF, 0xFF000000, 0xFFFFD60A, 0xFFFF375F, 0xFF30D158, 0xFF0A84FF)

/**
 * Crear un sticker: encuadrar con dos dedos, quitar el fondo, añadir texto y borde.
 * Se guarda en WebP sin pérdida de 512×512, con el fondo transparente.
 */
@Composable
fun StickerEditorDialog(
    source: Uri,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var cutout by remember { mutableStateOf<Bitmap?>(null) }
    var removingBackground by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var text by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(TEXT_COLORS.first()) }
    var textOnTop by remember { mutableStateOf(false) }
    var outline by remember { mutableStateOf(true) }

    LaunchedEffect(source) {
        original = withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val largest = maxOf(info.size.width, info.size.height)
                    if (largest > MAX_SOURCE_SIZE) {
                        val ratio = MAX_SOURCE_SIZE.toFloat() / largest
                        decoder.setTargetSize((info.size.width * ratio).toInt(), (info.size.height * ratio).toInt())
                    }
                }
            }.getOrNull()
        }
        if (original == null) error = tr("No se pudo abrir esa imagen")
    }

    val base = cutout ?: original
    val preview = remember(base, scale, offset, text, textColor, textOnTop, outline) {
        base?.let { renderSticker(it, scale, offset, text, textColor.toInt(), textOnTop, outline && cutout != null) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = NexusTokens.Color.BgBase, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(tr("Crear sticker"), style = MaterialTheme.typography.titleLarge, color = NexusTokens.Color.TextPrimary)

                // Cuadrícula de tablero: deja claro qué parte del sticker queda transparente.
                Box(
                    Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NexusTokens.Color.BgElevated)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.4f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (preview != null) {
                        Image(preview.asImageBitmap(), tr("Vista previa"), Modifier.fillMaxSize())
                    } else if (error == null) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                    if (removingBackground) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
                Text(tr("Mueve y amplía con dos dedos"), style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted)
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.Error) }

                OutlinedButton(
                    enabled = original != null && !removingBackground,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (cutout != null) {
                            cutout = null
                        } else {
                            val image = original ?: return@OutlinedButton
                            removingBackground = true
                            error = null
                            scope.launch {
                                val result = runCatching { removeBackground(image) }
                                result.onSuccess { cutout = it }
                                result.onFailure { error = tr("No se pudo quitar el fondo. Se descarga la primera vez: revisa tu conexión.") }
                                removingBackground = false
                            }
                        }
                    }
                ) { Text(if (cutout != null) tr("Recuperar el fondo") else tr("Quitar el fondo")) }

                if (cutout != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Borde blanco"), color = NexusTokens.Color.TextPrimary, modifier = Modifier.weight(1f))
                        Switch(checked = outline, onCheckedChange = { outline = it })
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(40) },
                    label = { Text(tr("Texto")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (text.isNotBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TEXT_COLORS.forEach { color ->
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color(color))
                                    .then(
                                        if (color == textColor) {
                                            Modifier.padding(2.dp).background(androidx.compose.ui.graphics.Color(color), CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .pointerInput(color) {
                                        awaitPointerEventScope { awaitPointerEvent(); textColor = color }
                                    }
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { textOnTop = !textOnTop }) { Text(if (textOnTop) tr("Arriba") else tr("Abajo")) }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(tr("Cancelar")) }
                    Button(
                        enabled = preview != null && !saving,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val image = preview ?: return@Button
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    File(context.cacheDir, "sticker_${System.currentTimeMillis()}.webp").also { target ->
                                        target.outputStream().use { image.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it) }
                                    }
                                }
                                onSave(file)
                            }
                        }
                    ) {
                        if (saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Text(tr("Guardar sticker"))
                    }
                }
            }
        }
    }
}

/** Recorta la persona u objeto principal con ML Kit. Funciona en el teléfono: la foto no sale de él. */
private suspend fun removeBackground(image: Bitmap): Bitmap {
    val options = SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
    val client = SubjectSegmentation.getClient(options)
    try {
        val result = client.process(InputImage.fromBitmap(image, 0)).await()
        return result.foregroundBitmap ?: error("Sin sujeto")
    } finally {
        client.close()
    }
}

internal fun renderSticker(
    source: Bitmap,
    scale: Float,
    offset: Offset,
    text: String,
    textColor: Int,
    textOnTop: Boolean,
    outline: Boolean
): Bitmap {
    val result = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val fit = STICKER_SIZE.toFloat() / maxOf(source.width, source.height)
    canvas.save()
    canvas.translate(STICKER_SIZE / 2f + offset.x, STICKER_SIZE / 2f + offset.y)
    canvas.scale(fit * scale, fit * scale)
    canvas.translate(-source.width / 2f, -source.height / 2f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    if (outline) {
        // Se ensancha la silueta del recorte para formar el borde blanco típico de un sticker.
        val shift = IntArray(2)
        val silhouette = source.extractAlpha(
            Paint().apply { maskFilter = BlurMaskFilter(14f / (fit * scale).coerceAtLeast(0.1f), BlurMaskFilter.Blur.OUTER) },
            shift
        )
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        repeat(3) { canvas.drawBitmap(silhouette, shift[0].toFloat(), shift[1].toFloat(), edge) }
        silhouette.recycle()
    }
    canvas.drawBitmap(source, 0f, 0f, paint)
    canvas.restore()

    if (text.isNotBlank()) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = if (text.length > 18) 40f else 58f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            color = textColor
        }
        val stroke = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = 9f
            color = if (textColor == android.graphics.Color.BLACK) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        }
        val y = if (textOnTop) 72f else STICKER_SIZE - 32f
        canvas.drawText(text, STICKER_SIZE / 2f, y, stroke)
        canvas.drawText(text, STICKER_SIZE / 2f, y, fill)
    }
    return result
}
