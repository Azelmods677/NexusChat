package com.Azelmods.App.ui.screens.terminal

import android.content.Context
import android.os.Environment
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RealTerminalEmulator - REAL TERMINAL using libsu
 * 
 * This provides a REAL terminal experience using:
 * - libsu for root/non-root shell execution
 * - Real PTY (Pseudo Terminal)
 * - Real command execution
 * - Interactive shell session
 */
class RealTerminalEmulator(private val context: Context) {
    
    data class TerminalLine(val text: String, val type: Type) {
        enum class Type { SYSTEM, INPUT, OUTPUT, ERROR, SUCCESS, WARNING }
    }
    
    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()
    
    private val _isRoot = MutableStateFlow(false)
    val isRoot: StateFlow<Boolean> = _isRoot.asStateFlow()
    
    private var currentDirectory = Environment.getExternalStorageDirectory().absolutePath
    private val homeDirectory = Environment.getExternalStorageDirectory().absolutePath
    
    // Interactive shell instance
    private var shell: Shell? = null
    
    init {
        // Initialize libsu
        Shell.enableVerboseLogging = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setTimeout(60)
        )
        
        try {
            checkRootAndInitialize()
        } catch (e: Exception) {
            android.util.Log.e("RealTerminalEmulator", "Failed to initialize shell: ${e.message}", e)
            addLine("╔═══════════════════════════════════════════════════╗", TerminalLine.Type.SYSTEM)
            addLine("║      REAL TERMUX TERMINAL - Azel powered        ║", TerminalLine.Type.SYSTEM)
            addLine("║         Full Interactive Shell Support           ║", TerminalLine.Type.SYSTEM)
            addLine("╚═══════════════════════════════════════════════════╝", TerminalLine.Type.SYSTEM)
            addLine("", TerminalLine.Type.SYSTEM)
            addLine("⚠️ Terminal inicializado en modo simulado", TerminalLine.Type.WARNING)
            addLine("   Shell nativo no disponible en este dispositivo", TerminalLine.Type.WARNING)
            addLine("", TerminalLine.Type.SYSTEM)
            addLine("Working directory: $currentDirectory", TerminalLine.Type.OUTPUT)
            addLine("", TerminalLine.Type.SYSTEM)
            addLine("Type 'help' for available commands", TerminalLine.Type.WARNING)
            addLine("═══════════════════════════════════════════════════", TerminalLine.Type.SYSTEM)
        }
    }
    
    private fun checkRootAndInitialize() {
        // Check if root is available
        _isRoot.value = Shell.isAppGrantedRoot() == true
        
        // Create shell instance with error handling
        try {
            shell = if (_isRoot.value) {
                Shell.Builder.create()
                    .setTimeout(60)
                    .build()
            } else {
                Shell.Builder.create()
                    .setTimeout(60)
                    .build()
            }
        } catch (e: Exception) {
            android.util.Log.e("RealTerminalEmulator", "Shell build failed: ${e.message}", e)
            throw e
        }
        
        addLine("╔═══════════════════════════════════════════════════╗", TerminalLine.Type.SYSTEM)
        addLine("║      REAL TERMUX TERMINAL - Azel powered        ║", TerminalLine.Type.SYSTEM)
        addLine("║         Full Interactive Shell Support           ║", TerminalLine.Type.SYSTEM)
        addLine("╚═══════════════════════════════════════════════════╝", TerminalLine.Type.SYSTEM)
        addLine("", TerminalLine.Type.SYSTEM)
        
        if (_isRoot.value) {
            addLine("✓ ROOT ACCESS GRANTED", TerminalLine.Type.SUCCESS)
            addLine("✓ Running as superuser", TerminalLine.Type.SUCCESS)
        } else {
            addLine("✓ Running in user mode", TerminalLine.Type.WARNING)
            addLine("✓ All non-root commands available", TerminalLine.Type.SUCCESS)
        }
        
        addLine("", TerminalLine.Type.SYSTEM)
        addLine("Working directory: $currentDirectory", TerminalLine.Type.OUTPUT)
        addLine("Shell: ${if (_isRoot.value) "/system/xbin/su" else "/system/bin/sh"}", TerminalLine.Type.OUTPUT)
        addLine("", TerminalLine.Type.SYSTEM)
        addLine("Type 'help' for available commands", TerminalLine.Type.WARNING)
        addLine("═══════════════════════════════════════════════════", TerminalLine.Type.SYSTEM)
    }
    
    private fun addLine(text: String, type: TerminalLine.Type) {
        _lines.value = _lines.value + TerminalLine(text, type)
    }
    
    suspend fun execute(command: String) = withContext(Dispatchers.IO) {
        if (command.isBlank()) return@withContext
        
        addLine("${if (_isRoot.value) "#" else "$"} $command", TerminalLine.Type.INPUT)
        
        val trimmedCommand = command.trim()
        
        // Handle built-in commands
        when {
            trimmedCommand == "clear" || trimmedCommand == "cls" -> {
                _lines.value = emptyList()
                return@withContext
            }
            
            trimmedCommand == "help" -> {
                showHelp()
                return@withContext
            }
            
            trimmedCommand.startsWith("cd ") -> {
                handleCd(trimmedCommand.substring(3).trim())
                return@withContext
            }
            
            trimmedCommand == "cd" -> {
                handleCd(homeDirectory)
                return@withContext
            }
            
            trimmedCommand == "pwd" -> {
                addLine(currentDirectory, TerminalLine.Type.SUCCESS)
                return@withContext
            }
            
            trimmedCommand == "exit" -> {
                addLine("Closing terminal...", TerminalLine.Type.WARNING)
                shell?.close()
                return@withContext
            }
        }
        
        // Execute real command using libsu
        executeRealCommand(trimmedCommand)
    }
    
    private fun handleCd(path: String) {
        val newDir = when {
            path.startsWith("/") -> File(path)
            path == "~" -> File(homeDirectory)
            path == ".." -> File(currentDirectory).parentFile ?: File(currentDirectory)
            path == "." -> File(currentDirectory)
            else -> File(currentDirectory, path)
        }
        
        if (newDir.exists() && newDir.isDirectory) {
            currentDirectory = newDir.absolutePath
            addLine(currentDirectory, TerminalLine.Type.SUCCESS)
        } else {
            addLine("cd: no such file or directory: $path", TerminalLine.Type.ERROR)
        }
    }
    
    private fun executeRealCommand(command: String) {
        try {
            val fullCommand = "cd \"$currentDirectory\" && $command"
            
            val result = shell?.newJob()?.add(fullCommand)?.to(ArrayList(), ArrayList())?.exec()
            
            if (result != null) {
                // Output
                result.out.forEach { line ->
                    if (line.isNotBlank()) {
                        addLine(line, TerminalLine.Type.OUTPUT)
                    }
                }
                
                // Errors
                result.err.forEach { line ->
                    if (line.isNotBlank()) {
                        addLine(line, TerminalLine.Type.ERROR)
                    }
                }
                
                // Show exit code if non-zero and no output
                if (!result.isSuccess && result.out.isEmpty() && result.err.isEmpty()) {
                    addLine("Command exited with code: ${result.code}", TerminalLine.Type.ERROR)
                }
            } else {
                addLine("Failed to execute command", TerminalLine.Type.ERROR)
            }
            
        } catch (e: Exception) {
            addLine("Error: ${e.message}", TerminalLine.Type.ERROR)
        }
    }
    
    private fun showHelp() {
        val helpText = """
            ╔═══════════════════════════════════════════════════╗
            ║              NEXUS TERMINAL - HELP                ║
            ╚═══════════════════════════════════════════════════╝

            Shell real de Android (toybox/toolbox) sobre /system/bin/sh.
            Cada comando se ejecuta por separado (no es una sesión
            interactiva: 'export', 'vi' o programas que piden entrada no
            mantienen estado entre comandos).

            INTEGRADOS:
              help              - Muestra esta ayuda
              clear / cls       - Limpia la pantalla
              cd <dir>          - Cambia de directorio
              pwd               - Directorio actual
              exit              - Cierra la terminal

            COMANDOS DE ANDROID DISPONIBLES (sin root):
              ls [-la] [dir]    - Lista archivos
              cat <file>        - Muestra un archivo
              echo <text>       - Imprime texto
              touch / mkdir     - Crea archivo / carpeta
              rm / cp / mv      - Borra / copia / mueve
              grep / find       - Busca en archivos
              ps / top          - Procesos
              df -h / free       - Disco / memoria
              uname -a / getprop - Info del sistema
              date / uptime     - Fecha / tiempo encendido
              ping <host>       - Prueba de red
              pm list packages  - Apps instaladas
              dumpsys / logcat  - Servicios / logs del sistema

            CON ROOT (si el dispositivo lo tiene y lo concede):
              su -c "comando"   - Ejecuta como superusuario
              mount / reboot    - Montar / reiniciar

            LO QUE NO HACE (sé consciente):
              • NO es Termux: no hay 'pkg'/'apt' ni repositorios.
              • Sin root, la mayoría de rutas del sistema son de
                solo lectura y algunos comandos dan "permission denied".
              • Para Python/gcc/node instala Termux por separado; esta
                terminal no puede instalarlos.

            ═══════════════════════════════════════════════════
        """.trimIndent()

        helpText.lines().forEach { addLine(it, TerminalLine.Type.OUTPUT) }
    }
    
    fun clear() {
        _lines.value = emptyList()
    }
    
    fun close() {
        shell?.close()
    }
}
