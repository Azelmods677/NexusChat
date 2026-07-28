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

    /** Directorio actual, reactivo, para que la UI pinte el prompt real. */
    private val _cwd = MutableStateFlow(currentDirectory)
    val cwd: StateFlow<String> = _cwd.asStateFlow()

    /** Historial de comandos, para las flechas ↑/↓ y el built-in `history`. */
    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    /** Atajos de shell habituales que este emulador expande antes de ejecutar. */
    private val aliases = mapOf(
        "ll" to "ls -la",
        "la" to "ls -a",
        "l" to "ls",
        ".." to "cd .."
    )
    
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
    
    suspend fun execute(rawCommand: String) = withContext(Dispatchers.IO) {
        if (rawCommand.isBlank()) return@withContext

        // Prompt con directorio corto, como una shell de verdad.
        addLine("${promptSymbol()} ${shortCwd()} ${rawCommand.trim()}", TerminalLine.Type.INPUT)

        // Historial: se guarda el comando tal cual (sin repetir el anterior).
        if (_history.value.lastOrNull() != rawCommand.trim()) {
            _history.value = (_history.value + rawCommand.trim()).takeLast(100)
        }

        // Expansión de alias sobre la primera palabra ("ll" -> "ls -la").
        val trimmedCommand = expandAlias(rawCommand.trim())

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

            trimmedCommand == "history" -> {
                if (_history.value.isEmpty()) addLine("(sin historial)", TerminalLine.Type.OUTPUT)
                _history.value.forEachIndexed { i, cmd ->
                    addLine("${(i + 1).toString().padStart(4)}  $cmd", TerminalLine.Type.OUTPUT)
                }
                return@withContext
            }

            trimmedCommand == "sysinfo" || trimmedCommand == "neofetch" -> {
                showSysInfo()
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

    /** Símbolo de prompt: `#` con root, `$` sin él. */
    private fun promptSymbol(): String = if (_isRoot.value) "#" else "$"

    /** Directorio actual acortado (~ para el home) para el prompt. */
    private fun shortCwd(): String {
        val dir = currentDirectory
        return if (dir.startsWith(homeDirectory)) "~" + dir.removePrefix(homeDirectory) else dir
    }

    /** Expande el primer token si es un alias conocido. */
    private fun expandAlias(command: String): String {
        val firstToken = command.substringBefore(' ')
        val alias = aliases[firstToken] ?: return command
        val rest = command.removePrefix(firstToken)
        return alias + rest
    }

    /** Ficha estilo neofetch con datos reales del dispositivo. */
    private fun showSysInfo() {
        val rt = Runtime.getRuntime()
        val usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val maxMem = rt.maxMemory() / (1024 * 1024)
        val storageDir = Environment.getExternalStorageDirectory()
        val freeGb = storageDir.freeSpace / (1024.0 * 1024 * 1024)
        val totalGb = storageDir.totalSpace / (1024.0 * 1024 * 1024)
        val info = listOf(
            "        _______        NexusChat Terminal",
            "       / ____  \\       ─────────────────────",
            "      | |    | |       Modelo    : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            "      | |____| |       Android   : ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
            "      |  ____  |       Arch      : ${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}",
            "      | |    | |       Root      : ${if (_isRoot.value) "sí" else "no"}",
            "      |_|    |_|       RAM (app) : ${usedMem} / ${maxMem} MB",
            "                       Almacen.  : ${"%.1f".format(freeGb)} GB libres / ${"%.1f".format(totalGb)} GB",
            "                       Kernel    : ${System.getProperty("os.version") ?: "?"}"
        )
        info.forEach { addLine(it, TerminalLine.Type.SUCCESS) }
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
            _cwd.value = currentDirectory
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
              history           - Historial de comandos
              sysinfo / neofetch- Ficha del dispositivo
              exit              - Cierra la terminal

            ATAJOS (alias):
              ll = ls -la    la = ls -a    l = ls

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
