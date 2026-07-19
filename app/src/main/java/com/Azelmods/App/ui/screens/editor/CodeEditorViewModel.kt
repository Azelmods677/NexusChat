package com.Azelmods.App.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CodeEditorViewModel @Inject constructor() : ViewModel() {
    
    private val db = FirebaseDatabase.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    private val _files = MutableStateFlow<List<CodeFile>>(emptyList())
    val files: StateFlow<List<CodeFile>> = _files.asStateFlow()
    
    private val _currentFile = MutableStateFlow<CodeFile?>(null)
    val currentFile: StateFlow<CodeFile?> = _currentFile.asStateFlow()
    
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // 🔥 JS execution via WebView (Composable handles the actual WebView)
    private val _jsToExecute = MutableStateFlow<String?>(null)
    val jsToExecute: StateFlow<String?> = _jsToExecute.asStateFlow()

    // 🌐 HTML/CSS live preview: el Composable renderiza este HTML en un WebView visible.
    private val _htmlToPreview = MutableStateFlow<String?>(null)
    val htmlToPreview: StateFlow<String?> = _htmlToPreview.asStateFlow()
    
    // Referencia + listener del historial de archivos, para poder removerlo en
    // onCleared() y no fugar el listener (antes se registraba y nunca se quitaba).
    private var filesQuery: Query? = null
    private var filesListener: ValueEventListener? = null

    init {
        loadFiles()
    }

    // Load files from Firebase in real-time
    private fun loadFiles() {
        viewModelScope.launch {
            val ref = db.getReference("codeFiles/$uid")
                .orderByChild("timestamp")
            val listener = object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    _files.value = snap.children
                        .mapNotNull { it.getValue(CodeFile::class.java) }
                        .sortedByDescending { it.timestamp }
                }

                override fun onCancelled(e: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            filesQuery = ref
            filesListener = listener
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Evita la fuga del listener de Firebase cuando se cierra la pantalla.
        filesListener?.let { filesQuery?.removeEventListener(it) }
        filesListener = null
        filesQuery = null
    }
    
    // Create new file
    fun newFile(name: String, language: String) {
        viewModelScope.launch {
            try {
                // Validar que el usuario esté autenticado
                if (uid.isBlank()) {
                    android.util.Log.e("CodeEditorVM", "❌ User not authenticated")
                    _output.value = "❌ Error: Usuario no autenticado. Inicia sesión para crear archivos."
                    return@launch
                }
                
                // Validar nombre del archivo
                if (name.isBlank()) {
                    android.util.Log.e("CodeEditorVM", "❌ File name is blank")
                    _output.value = "❌ Error: El nombre del archivo no puede estar vacío"
                    return@launch
                }
                
                android.util.Log.d("CodeEditorVM", "📝 Creating file: $name (language: $language)")
                
                val ref = db.getReference("codeFiles/$uid").push()
                val fileId = ref.key ?: run {
                    android.util.Log.e("CodeEditorVM", "❌ Firebase push() returned null key")
                    // Generar ID manualmente como fallback
                    val generatedId = "file_${System.currentTimeMillis()}_${(0..999).random()}"
                    android.util.Log.d("CodeEditorVM", "📝 Using generated ID: $generatedId")
                    generatedId
                }
                
                val template = getTemplate(language)
                val file = CodeFile(
                    id = fileId,
                    name = name,
                    language = language,
                    content = template,
                    userId = uid,
                    timestamp = System.currentTimeMillis(),
                    size = template.length.toLong()
                )
                
                android.util.Log.d("CodeEditorVM", "💾 Saving file to Firebase: codeFiles/$uid/$fileId")
                
                // Si Firebase no dio una key, usar el ID generado manualmente
                val saveRef = if (ref.key != null) {
                    ref
                } else {
                    db.getReference("codeFiles/$uid/$fileId")
                }
                
                saveRef.setValue(file).await()
                
                // 🔥 FIX: Abrir el archivo inmediatamente después de crearlo
                _currentFile.value = file
                _output.value = "✅ Archivo creado: $name\n💡 Listo para editar"
                android.util.Log.d("CodeEditorVM", "✅ File created successfully: $name (id: $fileId)")
            } catch (e: Exception) {
                android.util.Log.e("CodeEditorVM", "❌ Error creating file: ${e.message}", e)
                _output.value = "❌ Error al crear archivo: ${e.message ?: "Error desconocido"}\n\nVerifica tu conexión a internet y que tengas permisos en Firebase."
            }
        }
    }
    
    // Save file
    fun saveFile(content: String) {
        val file = _currentFile.value 
        if (file == null) {
            android.util.Log.w("CodeEditorVM", "⚠️ No file selected to save")
            _output.value = "⚠️ Ningún archivo seleccionado para guardar"
            return
        }
        
        viewModelScope.launch {
            try {
                android.util.Log.d("CodeEditorVM", "💾 Saving file: ${file.name}")
                
                val updated = file.copy(
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    size = content.length.toLong()
                )
                
                db.getReference("codeFiles/$uid/${file.id}")
                    .setValue(updated).await()
                    
                _currentFile.value = updated
                _output.value = "✅ Guardado: ${file.name}"
                android.util.Log.d("CodeEditorVM", "✅ File saved successfully: ${file.name}")
            } catch (e: Exception) {
                android.util.Log.e("CodeEditorVM", "❌ Error saving file: ${e.message}", e)
                _output.value = "❌ Error al guardar: ${e.message ?: "Error desconocido"}\n\nVerifica tu conexión a internet."
            }
        }
    }
    
    /**
     * Execute code. For JavaScript, delegates to the Composable via [jsToExecute].
     * For other languages, shows informative fallback messages.
     */
    fun executeCode(code: String, language: String) {
        viewModelScope.launch {
            _isRunning.value = true
            _output.value = "Analizando..."
            kotlinx.coroutines.delay(300)
            
            when (language) {
                "js" -> {
                    // 🔥 Signal Composable to execute JS via WebView
                    _output.value = "🟨 Ejecutando JavaScript..."
                    _jsToExecute.value = code
                }
                "html" -> {
                    // Renderiza el HTML (con su CSS/JS embebido) en un WebView visible.
                    _output.value = "🌐 Vista previa HTML — pulsa la X para volver al editor"
                    _htmlToPreview.value = code
                }
                "css" -> {
                    // El CSS solo no es una página: se previsualiza sobre contenido de ejemplo.
                    _output.value = "🎨 Vista previa del CSS con contenido de ejemplo"
                    _htmlToPreview.value = wrapCssInHtml(code)
                }
                "python" -> {
                    _output.value = "🐍 Python no está disponible en Android nativo.\n\n" +
                        "Para ejecutar Python:\n" +
                        "1. Instala Termux desde F-Droid\n" +
                        "2. Ejecuta: pkg install python\n" +
                        "3. Corre tu script desde el terminal"
                }
                "bash" -> {
                    _output.value = "💻 Bash en Android es limitado.\n\n" +
                        "Algunos comandos básicos funcionan (ls, pwd, cat)\n" +
                        "pero otros requieren Termux.\n\n" +
                        "Usa el Terminal integrado (Settings > Terminal) para un shell completo."
                }
                "kotlin" -> {
                    _output.value = "💜 Kotlin en Android requiere compilación.\n\n" +
                        "No se puede ejecutar código fuente directamente en el dispositivo.\n\n" +
                        "Prueba Kotlin Playground:\nhttps://play.kotlinlang.org/"
                }
                "c" -> {
                    _output.value = "🔵 C requiere compilador (gcc/clang).\n\n" +
                        "No disponible en Android sin Termux.\n\n" +
                        "Instala via Termux: pkg install clang"
                }
                else -> {
                    _output.value = "⚠️ Lenguaje '$language' no soportado para ejecución en este dispositivo."
                }
            }
            if (language != "js") {
                _isRunning.value = false
            }
        }
    }

    fun clearHtmlPreview() {
        _htmlToPreview.value = null
    }

    /** Envuelve una hoja de estilos en una página mínima para poder previsualizarla. */
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

    /**
     * Called by the Composable after WebView JS execution completes.
     */
    fun onJsResult(result: String, error: String?) {
        _isRunning.value = false
        _output.value = when {
            error != null -> "❌ Error:\n$error" + if (result.isNotBlank()) "\n\n$result" else ""
            result.isBlank() -> "🟨 JavaScript ejecutado (sin salida de consola)"
            else -> "🟨 JavaScript Output:\n$result"
        }
        _jsToExecute.value = null
    }
    
    private fun executePython(code: String): String = ""  // removed — no crash
    private fun executeJavaScript(code: String): String = ""  // removed — no crash
    private fun executeKotlin(code: String): String = ""  // removed — no crash
    private suspend fun executeShell(interpreter: String, code: String): String = ""  // removed — no crash
    
    // Templates by language
    private fun getTemplate(lang: String) = when (lang) {
        "python" -> "#!/usr/bin/env python3\n# Nexus Chat Dev Framework\n\nprint('Hello from Nexus Chat!')\n"
        "kotlin" -> "fun main() {\n    println(\"Hello from Nexus Chat!\")\n}\n"
        "bash" -> "#!/bin/bash\n# Nexus Chat Framework\n\necho 'Hello from Nexus Chat!'\n"
        "js" -> "// Nexus Chat Framework\nconsole.log('Hello from Nexus Chat!');\n"
        "c" -> "#include <stdio.h>\nint main() {\n    printf(\"Hello from Nexus Chat!\\n\");\n    return 0;\n}\n"
        "html" -> "<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n  <title>Nexus Chat</title>\n  <style>\n    body { font-family: sans-serif; text-align: center; padding: 2rem; }\n    h1 { color: #7C6FE0; }\n  </style>\n</head>\n<body>\n  <h1>Hello from Nexus Chat!</h1>\n  <p>Edita este HTML y pulsa ▶ para ver la vista previa.</p>\n  <script>console.log('Listo');</script>\n</body>\n</html>\n"
        "css" -> "/* Nexus Chat Framework */\nbody {\n  font-family: sans-serif;\n  background: #0D0D1E;\n  color: #FFFFFF;\n  padding: 2rem;\n}\nh1 { color: #7C6FE0; }\n.box {\n  margin-top: 1rem;\n  padding: 1rem;\n  border: 2px solid #00D4FF;\n  border-radius: 12px;\n}\n"
        else -> "// Nuevo archivo\n"
    }
    
    fun openFile(file: CodeFile) {
        _currentFile.value = file
    }
    
    fun closeFile() {
        _currentFile.value = null
    }
    
    fun clearOutput() {
        _output.value = ""
    }
}
