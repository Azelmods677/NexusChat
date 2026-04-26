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
    
    // Execute code (shell command)
    fun executeCode(code: String, language: String) {
        viewModelScope.launch {
            _isRunning.value = true
            _output.value = ""
            try {
                val result = when (language) {
                    "python" -> executeShell("python3", code)
                    "bash" -> executeShell("bash", code)
                    "js" -> executeShell("node", code)
                    else -> "Lenguaje no soportado para ejecución"
                }
                _output.value = result
            } catch (e: Exception) {
                _output.value = "Error: ${e.message}"
            }
            _isRunning.value = false
        }
    }
    
    private suspend fun executeShell(interpreter: String, code: String): String {
        return try {
            val process = ProcessBuilder(interpreter, "-c", code)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.ifBlank { "[Sin salida]" }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    
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
    
    fun clearOutput() {
        _output.value = ""
    }
}
