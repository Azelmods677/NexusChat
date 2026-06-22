package com.Azelmods.App.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
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
    
    init {
        loadFiles()
    }
    
    // Load files from Firebase in real-time
    private fun loadFiles() {
        viewModelScope.launch {
            val ref = db.getReference("codeFiles/$uid")
                .orderByChild("timestamp")
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    _files.value = snap.children
                        .mapNotNull { it.getValue(CodeFile::class.java) }
                        .sortedByDescending { it.timestamp }
                }
                
                override fun onCancelled(e: DatabaseError) {}
            })
        }
    }
    
    // Create new file
    fun newFile(name: String, language: String) {
        viewModelScope.launch {
            val ref = db.getReference("codeFiles/$uid").push()
            val file = CodeFile(
                id = ref.key ?: "",
                name = name,
                language = language,
                content = getTemplate(language),
                userId = uid,
                timestamp = System.currentTimeMillis()
            )
            ref.setValue(file).await()
            _currentFile.value = file
        }
    }
    
    // Save file
    fun saveFile(content: String) {
        val file = _currentFile.value ?: return
        viewModelScope.launch {
            val updated = file.copy(
                content = content,
                timestamp = System.currentTimeMillis(),
                size = content.length.toLong()
            )
            db.getReference("codeFiles/$uid/${file.id}")
                .setValue(updated).await()
            _currentFile.value = updated
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

    /**
     * Called by the Composable after WebView JS execution completes.
     */
    fun onJsResult(result: String, error: String?) {
        _isRunning.value = false
        _output.value = if (error != null) {
            "❌ Error:\n$error\n\n$result"
        } else {
            "🟨 JavaScript Output:\n$result"
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
