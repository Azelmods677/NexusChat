package com.Azelmods.App.ui.screens.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.theme.NexusTokens
import java.util.Locale

/** Qué se va a enviar: decide cómo se sube y cómo se pinta en el chat. */
enum class AttachmentKind { IMAGE, VIDEO, GIF, DOCUMENT }

/**
 * Un archivo elegido que aún no se ha enviado. [name] y [caption] los puede cambiar el
 * usuario antes de enviar: el nombre solo cuenta en los documentos.
 */
data class PendingAttachment(
    val uri: Uri,
    val kind: AttachmentKind,
    val name: String,
    val size: Long,
    val mime: String,
    val caption: String = "",
    /** Duración de los vídeos, para ofrecer "Enviar como GIF" si son cortos. */
    val durationMs: Long = 0,
    /** Vídeo que se envía en bucle y sin sonido, como los GIF de WhatsApp. */
    val asGif: Boolean = false
)

/** Hasta este largo un vídeo se puede enviar como GIF. */
const val MAX_GIF_VIDEO_MS = 15_000L

/** Lee nombre, tamaño y tipo de un archivo elegido con el selector del sistema. */
fun describeAttachment(context: Context, uri: Uri, asDocument: Boolean = false): PendingAttachment {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
    var name: String? = null
    var size = -1L
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val s = c.getColumnIndex(OpenableColumns.SIZE)
                if (n >= 0) name = c.getString(n)
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
            }
        }
    }
    val kind = when {
        asDocument -> AttachmentKind.DOCUMENT
        mime == "image/gif" -> AttachmentKind.GIF
        mime.startsWith("video/") -> AttachmentKind.VIDEO
        mime.startsWith("image/") -> AttachmentKind.IMAGE
        else -> AttachmentKind.DOCUMENT
    }
    val duration = if (kind == AttachmentKind.VIDEO) {
        runCatching {
            android.media.MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        }.getOrDefault(0L)
    } else 0L
    return PendingAttachment(uri, kind, name ?: uri.lastPathSegment ?: tr("Documento"), size, mime, durationMs = duration)
}

/** 1,4 MB, 820 KB… Vacío si no se sabe. */
fun formatFileSize(bytes: Long): String = when {
    bytes < 0 -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024))
}

/** Extensión en mayúsculas para la etiqueta del documento: PDF, DOCX, ZIP… */
fun fileExtensionLabel(name: String): String =
    name.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 5 }?.uppercase(Locale.ROOT) ?: tr("ARCHIVO")

/** Límite de las reglas de Storage. Avisar antes evita una subida larga que acaba rechazada. */
const val MAX_ATTACHMENT_BYTES = 100L * 1024 * 1024

/**
 * Pantalla previa al envío de uno o varios archivos. Cada uno lleva su propia
 * descripción (con @menciones) y los documentos se pueden renombrar.
 */
@Composable
fun AttachmentSendScreen(
    initial: List<PendingAttachment>,
    mentionCandidates: List<String>,
    onDismiss: () -> Unit,
    onSend: (List<PendingAttachment>) -> Unit
) {
    val items = remember { mutableStateListOf<PendingAttachment>().apply { addAll(initial) } }
    var selected by remember { mutableIntStateOf(0) }
    var renaming by remember { mutableStateOf(false) }
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    selected = selected.coerceIn(0, items.lastIndex)
    val current = items[selected]
    val tooBig = items.filter { it.size > MAX_ATTACHMENT_BYTES }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(color = NexusTokens.Color.BgBase, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, tr("Cancelar"), tint = NexusTokens.Color.TextPrimary) }
                    Text(
                        if (items.size == 1) tr("Enviar 1 archivo") else tr("Enviar %s archivos", items.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = NexusTokens.Color.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Vista grande del archivo elegido.
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NexusTokens.Color.BgSurface),
                    contentAlignment = Alignment.Center
                ) {
                    when (current.kind) {
                        AttachmentKind.IMAGE, AttachmentKind.GIF -> AsyncImage(
                            model = current.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        AttachmentKind.VIDEO -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Videocam, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(current.name, color = NexusTokens.Color.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(formatFileSize(current.size), color = NexusTokens.Color.TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        AttachmentKind.DOCUMENT -> Column(
                            Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DocumentBadge(current.name, big = true)
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    current.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NexusTokens.Color.TextPrimary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(onClick = { renaming = true }) {
                                    Icon(Icons.Outlined.Edit, tr("Cambiar nombre"), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                listOf(fileExtensionLabel(current.name), formatFileSize(current.size)).filter { it.isNotBlank() }.joinToString(" · "),
                                color = NexusTokens.Color.TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Vídeo corto: se puede mandar como GIF (en bucle y sin sonido).
                if (current.kind == AttachmentKind.VIDEO && current.durationMs in 1..MAX_GIF_VIDEO_MS) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { items[selected] = current.copy(asGif = !current.asGif) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Enviar como GIF"), color = NexusTokens.Color.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                            Text(tr("Se repite sin sonido, como un GIF"), color = NexusTokens.Color.TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = current.asGif, onCheckedChange = { items[selected] = current.copy(asGif = it) })
                    }
                }

                if (tooBig.isNotEmpty()) {
                    Text(
                        tr("Algunos archivos pasan de 100 MB y no se pueden enviar: %s", tooBig.joinToString { it.name }),
                        color = NexusTokens.Color.Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Tira de miniaturas: tocar elige, la X quita.
                if (items.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.uri.toString() }) { index, item ->
                            Box(
                                Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NexusTokens.Color.BgElevated)
                                    .border(
                                        2.dp,
                                        if (index == selected) MaterialTheme.colorScheme.primary else NexusTokens.Color.BgElevated,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selected = index },
                                contentAlignment = Alignment.Center
                            ) {
                                when (item.kind) {
                                    AttachmentKind.IMAGE, AttachmentKind.GIF ->
                                        AsyncImage(item.uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    AttachmentKind.VIDEO -> Icon(Icons.Outlined.Videocam, null, tint = MaterialTheme.colorScheme.primary)
                                    AttachmentKind.DOCUMENT -> DocumentBadge(item.name, big = false)
                                }
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(NexusTokens.Color.BgBase.copy(alpha = 0.8f))
                                        .clickable { items.removeAt(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, tr("Quitar"), tint = NexusTokens.Color.TextPrimary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Descripción del archivo elegido, con sugerencias al escribir @.
                val mentionQuery = currentMentionQuery(current.caption)
                val matches = remember(mentionQuery, mentionCandidates) {
                    if (mentionQuery == null) emptyList()
                    else mentionCandidates.filter { it.contains(mentionQuery, ignoreCase = true) }.take(5)
                }
                if (matches.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexusTokens.Color.BgSurface)
                    ) {
                        matches.forEach { name ->
                            Text(
                                "@$name",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { items[selected] = current.copy(caption = applyMention(current.caption, name)) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = current.caption,
                        onValueChange = { items[selected] = current.copy(caption = it.take(2_000)) },
                        placeholder = { Text(tr("Añade una descripción, @ para mencionar")) },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    val sendable = items.filter { it.size <= MAX_ATTACHMENT_BYTES }
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (sendable.isEmpty()) NexusTokens.Color.BgElevated else MaterialTheme.colorScheme.primary)
                            .clickable(enabled = sendable.isNotEmpty()) { onSend(sendable.toList()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, tr("Enviar"), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }

    if (renaming) {
        RenameFileDialog(
            name = current.name,
            onDismiss = { renaming = false },
            onRename = { newName ->
                items[selected] = current.copy(name = newName)
                renaming = false
            }
        )
    }
}

/**
 * Cambiar el nombre de un documento antes de enviarlo. La extensión se conserva aparte:
 * si se borrara sin querer, el que lo recibe no sabría con qué abrirlo.
 */
@Composable
fun RenameFileDialog(name: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    val extension = name.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 5 && name.contains('.') }
    val base = if (extension != null) name.removeSuffix(".$extension") else name
    var text by remember { mutableStateOf(base) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Cambiar nombre")) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.replace(Regex("""[\\/:*?"<>|\n]"""), "").take(120) },
                singleLine = true,
                suffix = extension?.let { { Text(".$it", color = NexusTokens.Color.TextMuted) } },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onRename(text.trim() + (extension?.let { ".$it" } ?: "")) }
            ) { Text(tr("Guardar")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

/** Etiqueta de color con la extensión: así un PDF y un ZIP se distinguen de un vistazo. */
@Composable
fun DocumentBadge(name: String, big: Boolean) {
    val label = fileExtensionLabel(name)
    val color = documentColor(label)
    Box(
        Modifier
            .size(if (big) 72.dp else 40.dp)
            .clip(RoundedCornerShape(if (big) 16.dp else 10.dp))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Outlined.InsertDriveFile,
                null,
                tint = color,
                modifier = Modifier.size(if (big) 30.dp else 18.dp)
            )
            Text(
                label,
                color = color,
                fontWeight = FontWeight.Bold,
                style = if (big) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

private fun documentColor(label: String): androidx.compose.ui.graphics.Color = when (label) {
    "PDF" -> androidx.compose.ui.graphics.Color(0xFFE5484D)
    "DOC", "DOCX", "ODT", "RTF", "TXT", "MD" -> androidx.compose.ui.graphics.Color(0xFF3E8EF7)
    "XLS", "XLSX", "ODS", "CSV" -> androidx.compose.ui.graphics.Color(0xFF30A46C)
    "PPT", "PPTX", "ODP", "KEY" -> androidx.compose.ui.graphics.Color(0xFFF76B15)
    "ZIP", "RAR", "7Z", "TAR", "GZ" -> androidx.compose.ui.graphics.Color(0xFFFFB224)
    "APK", "XAPK" -> androidx.compose.ui.graphics.Color(0xFF46A758)
    "MP3", "WAV", "OGG", "M4A", "FLAC" -> androidx.compose.ui.graphics.Color(0xFF8E4EC6)
    else -> androidx.compose.ui.graphics.Color(0xFF8B8D98)
}
