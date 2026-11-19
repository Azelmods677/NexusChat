package com.Azelmods.App.ui.components.chat

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.screens.chat.DocumentBadge
import com.Azelmods.App.ui.screens.chat.fileExtensionLabel
import com.Azelmods.App.ui.screens.chat.formatFileSize
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/** Nombre que se enseña: el campo propio o, en mensajes antiguos, el texto. */
val Message.documentName: String
    get() = fileName?.takeIf { it.isNotBlank() } ?: content.ifBlank { tr("Documento") }

/**
 * Documentos de los chats. Se descargan con el SDK de Firebase dentro de la app y se
 * abren con su tipo real.
 *
 * Antes se pasaba el enlace al navegador: muchos móviles no sabían qué hacer con él
 * (página en blanco o un archivo llamado "o?alt=media"), y además el navegador salía
 * fuera de Tor y enseñaba la IP del usuario.
 */
object DocumentFiles {
    private fun folder(context: Context, message: Message) =
        File(context.cacheDir, "documents/${message.messageId.ifBlank { message.timestamp.toString() }}")

    fun cached(context: Context, message: Message): File? =
        folder(context, message).listFiles()?.firstOrNull { it.isFile && it.length() > 0 }

    suspend fun download(context: Context, message: Message, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        cached(context, message)?.let { return@withContext it }
        val url = message.mediaUrl ?: error("Sin archivo")
        val dir = folder(context, message).apply { mkdirs() }
        val safe = message.documentName.replace(Regex("""[\\/:*?"<>|]"""), "_").take(120)
        val target = File(dir, safe)
        val partial = File(dir, "$safe.part")
        try {
            FirebaseStorage.getInstance().getReferenceFromUrl(url).getFile(partial)
                .addOnProgressListener { snap ->
                    if (snap.totalByteCount > 0) onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount)
                }
                .await()
            partial.renameTo(target)
            target
        } catch (e: Exception) {
            partial.delete()
            throw e
        }
    }

    fun mimeOf(message: Message, file: File): String =
        message.mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: "*/*"

    fun open(context: Context, message: Message, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeOf(message, file))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(Intent.createChooser(intent, tr("Abrir con")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { android.widget.Toast.makeText(context, tr("No hay ninguna app para abrirlo"), android.widget.Toast.LENGTH_SHORT).show() }
    }

    fun share(context: Context, message: Message, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(mimeOf(message, file))
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Copia a Descargas/NexusChat para encontrarlo después con cualquier gestor de archivos. */
    fun saveToDownloads(context: Context, message: Message, file: File): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeOf(message, file).takeIf { it != "*/*" } ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/NexusChat")
        }
        val uri: Uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
        true
    }.getOrDefault(false)
}

/** Tarjeta del documento dentro de la burbuja: etiqueta con la extensión, nombre, tamaño y estado. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentAttachment(message: Message, onHold: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember(message.messageId) { mutableStateOf<Float?>(null) }
    var downloaded by remember(message.messageId) { mutableStateOf(DocumentFiles.cached(context, message) != null) }
    var menu by remember { mutableStateOf(false) }
    val name = message.documentName

    fun fetch(then: (File) -> Unit) {
        if (progress != null) return
        progress = 0f
        scope.launch {
            runCatching { DocumentFiles.download(context, message) { progress = it } }
                .onSuccess { file ->
                    downloaded = true
                    progress = null
                    then(file)
                }
                .onFailure {
                    progress = null
                    android.widget.Toast.makeText(context, tr("No se pudo descargar el documento"), android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }

    Row(
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .widthIn(min = 220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NexusTokens.Color.BgHigh.copy(alpha = 0.6f))
            .combinedClickable(
                onClick = { fetch { DocumentFiles.open(context, message, it) } },
                onLongClick = { menu = true }
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DocumentBadge(name, big = false)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOf(fileExtensionLabel(name), formatFileSize(message.fileSize.takeIf { it > 0 } ?: -1))
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted)
        }
        Spacer(Modifier.width(8.dp))
        val current = progress
        when {
            current != null -> CircularProgressIndicator(
                progress = { current },
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
            !downloaded -> Icon(Icons.Outlined.Download, tr("Descargar"), tint = NexusTokens.Color.TextSecondary, modifier = Modifier.size(22.dp))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = NexusTokens.Color.BgSurface) {
            DropdownMenuItem(text = { Text(tr("Abrir")) }, onClick = {
                menu = false
                fetch { DocumentFiles.open(context, message, it) }
            })
            DropdownMenuItem(text = { Text(tr("Guardar en Descargas")) }, onClick = {
                menu = false
                fetch { file ->
                    val ok = DocumentFiles.saveToDownloads(context, message, file)
                    android.widget.Toast.makeText(
                        context,
                        if (ok) tr("Guardado en Descargas/NexusChat") else tr("No se pudo guardar"),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            })
            DropdownMenuItem(text = { Text(tr("Compartir")) }, onClick = {
                menu = false
                fetch { DocumentFiles.share(context, message, it) }
            })
            DropdownMenuItem(text = { Text(tr("Más opciones")) }, onClick = {
                menu = false
                onHold()
            })
        }
    }
}
