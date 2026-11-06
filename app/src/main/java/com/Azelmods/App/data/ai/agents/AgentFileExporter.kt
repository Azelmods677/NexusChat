package com.Azelmods.App.data.ai.agents

import android.content.ContentValues
import android.content.Context
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que un agente quiere guardar en el teléfono, a la espera de que el usuario lo decida. */
data class AgentFileRequest(
    val fileName: String,
    val preview: String,
    internal val decision: CompletableDeferred<Boolean>
)

/**
 * Guarda en el almacenamiento del teléfono lo que producen los agentes.
 *
 * Un agente lee páginas de internet que podrían intentar convencerlo de hacer cosas, así que no
 * escribe nada fuera de la app sin que el usuario lo apruebe viendo el nombre y el contenido.
 * Solo crea archivos nuevos: nunca abre ni sobrescribe los que ya había.
 */
@Singleton
class AgentFileExporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("agent_files", Context.MODE_PRIVATE)

    private val _pending = MutableStateFlow<AgentFileRequest?>(null)
    val pending: StateFlow<AgentFileRequest?> = _pending.asStateFlow()

    private val _folder = MutableStateFlow(prefs.getString(KEY_FOLDER, null)?.let(Uri::parse))
    val folder: StateFlow<Uri?> = _folder.asStateFlow()

    fun folderLabel(): String? = _folder.value?.let { DocumentFile.fromTreeUri(context, it)?.name }

    /** Recuerda la carpeta elegida, con permiso persistente para no volver a pedirla. */
    fun setFolder(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.edit().putString(KEY_FOLDER, uri.toString()).apply()
        _folder.value = uri
    }

    fun answer(allowed: Boolean) {
        _pending.value?.decision?.complete(allowed)
        _pending.value = null
    }

    /** Pide permiso y, si lo dan, guarda. Devuelve el mensaje para el agente. */
    suspend fun export(fileName: String, content: String): ToolOutcome {
        val request = AgentFileRequest(fileName, content.take(PREVIEW_CHARS), CompletableDeferred())
        _pending.value = request
        if (!request.decision.await()) {
            return ToolOutcome("El usuario no permitió guardar «$fileName». No lo intentes de nuevo; dale el resultado en el chat.", false)
        }
        return runCatching {
            val bytes = if (fileName.endsWith(".pdf", ignoreCase = true)) renderPdf(fileName, content) else content.toByteArray()
            val where = write(fileName, mimeTypeOf(fileName), bytes)
            ToolOutcome("Guardado «$fileName» en $where (${bytes.size / 1024 + 1} KB).", true)
        }.getOrElse { ToolOutcome("No se pudo guardar «$fileName»: ${it.message}", false) }
    }

    private fun write(fileName: String, mime: String, bytes: ByteArray): String {
        val tree = _folder.value?.let { DocumentFile.fromTreeUri(context, it) }
        if (tree != null && tree.canWrite()) {
            val file = tree.createFile(mime, fileName) ?: error("La carpeta no aceptó el archivo")
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) } ?: error("No se pudo abrir el archivo")
            return "la carpeta ${tree.name ?: "elegida"}"
        }
        // Sin carpeta elegida: Descargas/NexusChat, que no requiere permisos especiales.
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/NexusChat")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo en Descargas")
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("No se pudo escribir")
        return "Descargas/NexusChat"
    }

    /** Maqueta el texto en A4, con títulos para las líneas que empiezan por #. */
    private fun renderPdf(title: String, content: String): ByteArray {
        val document = PdfDocument()
        val body = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val heading = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val width = PAGE_WIDTH - MARGIN * 2
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var y = MARGIN.toFloat()

        fun place(text: String, paint: TextPaint, gapAfter: Float) {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.1f)
                .build()
            for (line in 0 until layout.lineCount) {
                val lineHeight = (layout.getLineBottom(line) - layout.getLineTop(line)).toFloat()
                if (y + lineHeight > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    y = MARGIN.toFloat()
                }
                val piece = text.substring(layout.getLineStart(line), layout.getLineEnd(line)).trimEnd('\n')
                page.canvas.drawText(piece, MARGIN.toFloat(), y - layout.getLineAscent(line), paint)
                y += lineHeight
            }
            y += gapAfter
        }

        place(title.substringBeforeLast('.'), heading, 12f)
        content.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> y += 6f
                trimmed.startsWith("#") -> place(trimmed.trimStart('#').trim(), heading, 6f)
                else -> place(line, body, 3f)
            }
        }
        document.finishPage(page)
        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    private fun mimeTypeOf(name: String): String = when (name.substringAfterLast('.').lowercase()) {
        "pdf" -> "application/pdf"
        "html", "htm" -> "text/html"
        "md" -> "text/markdown"
        "csv" -> "text/csv"
        "json" -> "application/json"
        else -> "text/plain"
    }

    private companion object {
        const val KEY_FOLDER = "folder"
        const val PREVIEW_CHARS = 600
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48
    }
}
