package com.Azelmods.App.ui.screens.editor

import com.Azelmods.App.i18n.tr

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Editor de código: pestañas, guardado automático, importar y exportar, y ejecución.
 *
 * Los archivos se guardan en la cuenta (Firebase) para tenerlos en cualquier dispositivo, y
 * se pueden abrir o guardar en el almacenamiento del teléfono.
 *
 * Qué se ejecuta de verdad: JavaScript (motor del WebView), HTML/CSS (vista previa), shell
 * (proceso real del sistema) y JSON (validar y formatear). Python, Kotlin, C o TypeScript
 * necesitan un compilador o intérprete que Android no incluye; se dice claramente en vez de
 * fingir que corren.
 */
@HiltViewModel
class CodeEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = FirebaseDatabase.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _files = MutableStateFlow<List<CodeFile>>(emptyList())
    val files: StateFlow<List<CodeFile>> = _files.asStateFlow()

    /** Archivos abiertos en pestañas. */
    private val _tabs = MutableStateFlow<List<CodeFile>>(emptyList())
    val tabs: StateFlow<List<CodeFile>> = _tabs.asStateFlow()

    private val _currentFile = MutableStateFlow<CodeFile?>(null)
    val currentFile: StateFlow<CodeFile?> = _currentFile.asStateFlow()

    /** Ids con cambios sin guardar. */
    private val _unsaved = MutableStateFlow<Set<String>>(emptySet())
    val unsaved: StateFlow<Set<String>> = _unsaved.asStateFlow()

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _jsToExecute = MutableStateFlow<String?>(null)
    val jsToExecute: StateFlow<String?> = _jsToExecute.asStateFlow()

    private val _htmlToPreview = MutableStateFlow<String?>(null)
    val htmlToPreview: StateFlow<String?> = _htmlToPreview.asStateFlow()

    private var filesQuery: Query? = null
    private var filesListener: ValueEventListener? = null
    private var autosaveJob: Job? = null
    private var runJob: Job? = null
    private var process: Process? = null

    init {
        loadFiles()
    }

    private fun loadFiles() {
        if (uid.isBlank()) return
        val ref = db.getReference("codeFiles/$uid").orderByChild("timestamp")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                _files.value = snap.children
                    .mapNotNull { it.getValue(CodeFile::class.java) }
                    .sortedByDescending { it.timestamp }
            }

            override fun onCancelled(e: DatabaseError) {
                _output.value = "No se pudieron cargar tus archivos: ${e.message}"
            }
        }
        ref.addValueEventListener(listener)
        filesQuery = ref
        filesListener = listener
    }

    override fun onCleared() {
        super.onCleared()
        filesListener?.let { filesQuery?.removeEventListener(it) }
        process?.destroy()
    }

    private fun push(file: CodeFile) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            runCatching { db.getReference("codeFiles/$uid/${file.id}").setValue(file).await() }
                .onSuccess { _unsaved.value = _unsaved.value - file.id }
                .onFailure { _output.value = "No se pudo guardar ${file.name}: ${it.message}" }
        }
    }

    fun newFile(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        if (uid.isBlank()) {
            _output.value = "Inicia sesión para crear archivos."
            return
        }
        val language = CodeLanguages.fromFileName(clean)
        val id = db.getReference("codeFiles/$uid").push().key ?: "file_${System.currentTimeMillis()}"
        val content = template(language)
        val file = CodeFile(
            id = id,
            name = clean,
            language = language,
            content = content,
            userId = uid,
            timestamp = System.currentTimeMillis(),
            size = content.length.toLong()
        )
        push(file)
        openFile(file)
    }

    /** Abre un archivo del teléfono en una pestaña nueva. */
    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                    } ?: "archivo.txt"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("No se pudo leer")
                    if (bytes.size > MAX_FILE_BYTES) error("El archivo pasa de 2 MB")
                    name to String(bytes)
                }
            }
            result.onSuccess { (name, text) ->
                val id = db.getReference("codeFiles/$uid").push().key ?: "file_${System.currentTimeMillis()}"
                val file = CodeFile(
                    id = id,
                    name = name,
                    language = CodeLanguages.fromFileName(name),
                    content = text,
                    userId = uid,
                    timestamp = System.currentTimeMillis(),
                    size = text.length.toLong()
                )
                push(file)
                openFile(file)
            }.onFailure { _output.value = "No se pudo abrir: ${it.message}" }
        }
    }

    /** Guarda el archivo abierto en el teléfono. */
    fun exportTo(uri: Uri, content: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray()) }
                }.isSuccess
            }
            _output.value = if (ok) tr("Guardado en el teléfono") else tr("No se pudo guardar en el teléfono")
        }
    }

    fun openFile(file: CodeFile) {
        if (_tabs.value.none { it.id == file.id }) _tabs.value = _tabs.value + file
        _currentFile.value = _tabs.value.first { it.id == file.id }
    }

    /** Cierra una pestaña guardando antes lo pendiente. */
    fun closeTab(fileId: String, latestContent: String?) {
        if (latestContent != null) saveFile(fileId, latestContent)
        val remaining = _tabs.value.filterNot { it.id == fileId }
        _tabs.value = remaining
        if (_currentFile.value?.id == fileId) _currentFile.value = remaining.lastOrNull()
    }

    fun closeAllTabs() {
        _tabs.value = emptyList()
        _currentFile.value = null
    }

    /** El usuario ha escrito: se guarda solo poco después de dejar de teclear. */
    fun onContentChanged(fileId: String, content: String) {
        val current = _tabs.value.firstOrNull { it.id == fileId } ?: return
        if (current.content == content) return
        _unsaved.value = _unsaved.value + fileId
        _tabs.value = _tabs.value.map { if (it.id == fileId) it.copy(content = content) else it }
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            saveFile(fileId, content)
        }
    }

    fun saveFile(fileId: String, content: String) {
        val file = _tabs.value.firstOrNull { it.id == fileId } ?: return
        val updated = file.copy(content = content, timestamp = System.currentTimeMillis(), size = content.length.toLong())
        _tabs.value = _tabs.value.map { if (it.id == fileId) updated else it }
        if (_currentFile.value?.id == fileId) _currentFile.value = updated
        push(updated)
    }

    fun renameFile(fileId: String, newName: String) {
        val clean = newName.trim()
        val file = _tabs.value.firstOrNull { it.id == fileId } ?: return
        if (clean.isEmpty()) return
        val updated = file.copy(name = clean, language = CodeLanguages.fromFileName(clean))
        _tabs.value = _tabs.value.map { if (it.id == fileId) updated else it }
        if (_currentFile.value?.id == fileId) _currentFile.value = updated
        push(updated)
    }

    fun deleteFile(file: CodeFile) {
        closeTab(file.id, null)
        if (uid.isBlank()) return
        viewModelScope.launch {
            runCatching { db.getReference("codeFiles/$uid/${file.id}").removeValue().await() }
                .onFailure { _output.value = "No se pudo eliminar: ${it.message}" }
        }
    }

    fun executeCode(code: String, language: String) {
        when (language) {
            "js" -> {
                _isRunning.value = true
                _output.value = ""
                _jsToExecute.value = code
            }
            "html" -> _htmlToPreview.value = code
            "css" -> _htmlToPreview.value = wrapCssInHtml(code)
            "json" -> _output.value = formatJson(code)
            "bash" -> runShell(code)
            "python" -> _output.value = needsTool("Python", "pkg install python")
            "kotlin", "java" -> _output.value = needsTool("Este lenguaje", "pkg install kotlin")
            "c" -> _output.value = needsTool("Este lenguaje", "pkg install clang")
            "ts", "tsx", "jsx" -> _output.value =
                "Este lenguaje debe compilarse a JavaScript antes de ejecutarse, y el compilador no " +
                    "corre en el teléfono. Pásalo a un archivo .js y pulsa ejecutar."
            else -> _output.value = "Este tipo de archivo no se ejecuta."
        }
    }

    private fun needsTool(what: String, command: String) =
        "$what necesita un intérprete o compilador que Android no incluye.\n\n" +
            "Puedes usar Termux: $command"

    /** Ejecuta el script en una shell real y muestra la salida a medida que llega. */
    private fun runShell(script: String) {
        runJob?.cancel()
        _isRunning.value = true
        _output.value = ""
        runJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val home = File(context.filesDir, "home").apply { mkdirs() }
                val proc = ProcessBuilder("/system/bin/sh", "-c", script)
                    .directory(home)
                    .redirectErrorStream(true)
                    .apply { environment()["HOME"] = home.absolutePath }
                    .start()
                process = proc
                val buffer = CharArray(4096)
                proc.inputStream.bufferedReader().use { reader ->
                    while (isActive) {
                        val read = reader.read(buffer)
                        if (read < 0) break
                        _output.value = (_output.value + String(buffer, 0, read)).takeLast(MAX_OUTPUT_CHARS)
                    }
                }
                val code = proc.waitFor()
                _output.value += "\n[Terminó con código $code]"
            } catch (e: Exception) {
                _output.value += "\nError: ${e.message}"
            } finally {
                _isRunning.value = false
                process = null
            }
        }
    }

    fun stopExecution() {
        process?.destroy()
        runJob?.cancel()
        _isRunning.value = false
    }

    fun clearHtmlPreview() {
        _htmlToPreview.value = null
    }

    private fun wrapCssInHtml(css: String): String = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>
        $css
          </style>
        </head>
        <body>
          <h1>Título de ejemplo</h1>
          <p>Párrafo de ejemplo para previsualizar tu CSS.</p>
          <button>Botón</button>
          <div class="box">Elemento .box</div>
        </body>
        </html>
    """.trimIndent()

    fun onJsResult(result: String, error: String?) {
        _isRunning.value = false
        _output.value = when {
            error != null -> "Error:\n$error" + if (result.isNotBlank()) "\n\n$result" else ""
            result.isBlank() -> tr("Ejecutado (sin salida en la consola)")
            else -> result
        }
        _jsToExecute.value = null
    }

    private fun formatJson(code: String): String {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return "El archivo está vacío."
        return try {
            val pretty = when (trimmed.first()) {
                '[' -> org.json.JSONArray(trimmed).toString(2)
                '{' -> org.json.JSONObject(trimmed).toString(2)
                else -> return "JSON no válido: debe empezar por { o ["
            }
            "JSON válido\n\n$pretty"
        } catch (e: org.json.JSONException) {
            "JSON no válido\n\n${e.message}"
        }
    }

    private fun template(language: String) = when (language) {
        "python" -> "#!/usr/bin/env python3\n\nprint('Hola')\n"
        "kotlin" -> tr("fun main() {\n    println(\"Hola\")\n}\n")
        "bash" -> "#!/bin/sh\n\necho \"Hola desde $(uname -m)\"\n"
        "js" -> tr("console.log('Hola');\n")
        "c" -> "#include <stdio.h>\n\nint main(void) {\n    printf(\"Hola\\n\");\n    return 0;\n}\n"
        "html" -> tr("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n  <title>Página</title>\n</head>\n<body>\n  <h1>Hola</h1>\n</body>\n</html>\n")
        "css" -> tr("body {\n  font-family: sans-serif;\n  padding: 2rem;\n}\n")
        "json" -> tr("{\n  \"nombre\": \"proyecto\",\n  \"version\": \"1.0.0\"\n}\n")
        else -> ""
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 1_500L
        const val MAX_FILE_BYTES = 2 * 1024 * 1024
        const val MAX_OUTPUT_CHARS = 100_000
    }
}
