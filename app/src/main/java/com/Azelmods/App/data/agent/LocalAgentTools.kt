package com.Azelmods.App.data.agent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Local Agent Tools - Herramientas que Ollama puede usar
 * Convierte Ollama en un agente con capacidades reales
 */
class LocalAgentTools(private val context: Context) {
    
    companion object {
        private const val TAG = "LocalAgentTools"
    }
    
    /**
     * Ejecutar comando shell (sin root)
     */
    suspend fun executeShellCommand(command: String): ToolResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing: $command")
            
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = StringBuilder()
            val error = StringBuilder()
            
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { output.append(it).append("\n") }
            }
            
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.forEachLine { error.append(it).append("\n") }
            }
            
            val exitCode = process.waitFor()
            
            ToolResult.Success(
                tool = "shell",
                output = if (output.isNotEmpty()) output.toString() else error.toString(),
                metadata = mapOf("exitCode" to exitCode.toString())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Shell command error", e)
            ToolResult.Error("shell", e.message ?: "Unknown error")
        }
    }
    
    /**
     * Obtener información del sistema
     */
    suspend fun getSystemInfo(): ToolResult = withContext(Dispatchers.IO) {
        try {
            val info = buildString {
                appendLine("📱 SYSTEM INFO")
                appendLine("═══════════════")
                appendLine("Android Version: ${android.os.Build.VERSION.RELEASE}")
                appendLine("SDK: ${android.os.Build.VERSION.SDK_INT}")
                appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("Architecture: ${System.getProperty("os.arch")}")
                appendLine("Cores: ${Runtime.getRuntime().availableProcessors()}")
                
                val runtime = Runtime.getRuntime()
                val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                val maxMemory = runtime.maxMemory() / 1024 / 1024
                appendLine("Memory: ${usedMemory}MB / ${maxMemory}MB")
                
                appendLine("\n📂 STORAGE")
                appendLine("═══════════════")
                val internalDir = context.filesDir
                appendLine("Internal: ${internalDir.absolutePath}")
                appendLine("Free Space: ${internalDir.freeSpace / 1024 / 1024}MB")
            }
            
            ToolResult.Success("system_info", info)
        } catch (e: Exception) {
            ToolResult.Error("system_info", e.message ?: "Unknown error")
        }
    }
    
    /**
     * Escanear red local (sin root)
     */
    suspend fun scanNetwork(): ToolResult = withContext(Dispatchers.IO) {
        try {
            val devices = mutableListOf<String>()
            
            // Get local IP
            val localIp = getLocalIpAddress()
            if (localIp == null) {
                return@withContext ToolResult.Error("network_scan", "No network connection")
            }
            
            devices.add("🌐 LOCAL NETWORK SCAN")
            devices.add("═══════════════════════")
            devices.add("Your IP: $localIp")
            devices.add("")
            
            // Scan common gateway IPs
            val subnet = localIp.substringBeforeLast(".")
            val commonIps = listOf("$subnet.1", "$subnet.254", "$subnet.100")
            
            devices.add("Scanning common devices...")
            commonIps.forEach { ip ->
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(1000)) {
                        val hostname = try {
                            address.hostName
                        } catch (e: Exception) {
                            "Unknown"
                        }
                        devices.add("✅ $ip - $hostname")
                    }
                } catch (e: Exception) {
                    // Skip unreachable
                }
            }
            
            ToolResult.Success("network_scan", devices.joinToString("\n"))
        } catch (e: Exception) {
            ToolResult.Error("network_scan", e.message ?: "Unknown error")
        }
    }
    
    /**
     * Listar archivos en directorio
     */
    suspend fun listFiles(path: String = ""): ToolResult = withContext(Dispatchers.IO) {
        try {
            val targetDir = if (path.isEmpty()) {
                context.filesDir
            } else {
                File(context.filesDir, path)
            }
            
            if (!targetDir.exists()) {
                return@withContext ToolResult.Error("list_files", "Directory not found: $path")
            }
            
            val files = targetDir.listFiles()?.sortedBy { it.name } ?: emptyList()
            
            val output = buildString {
                appendLine("📂 ${targetDir.absolutePath}")
                appendLine("═══════════════════════")
                appendLine("Total: ${files.size} items")
                appendLine("")
                
                files.forEach { file ->
                    val icon = if (file.isDirectory) "📁" else "📄"
                    val size = if (file.isFile) " (${file.length() / 1024}KB)" else ""
                    appendLine("$icon ${file.name}$size")
                }
            }
            
            ToolResult.Success("list_files", output)
        } catch (e: Exception) {
            ToolResult.Error("list_files", e.message ?: "Unknown error")
        }
    }
    
    /**
     * Leer archivo
     */
    suspend fun readFile(filename: String): ToolResult = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, filename)
            if (!file.exists()) {
                return@withContext ToolResult.Error("read_file", "File not found: $filename")
            }
            
            val content = file.readText()
            ToolResult.Success("read_file", content, mapOf("size" to "${file.length()} bytes"))
        } catch (e: Exception) {
            ToolResult.Error("read_file", e.message ?: "Unknown error")
        }
    }
    
    /**
     * Escribir archivo
     */
    suspend fun writeFile(filename: String, content: String): ToolResult = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, filename)
            file.writeText(content)
            ToolResult.Success("write_file", "File written: ${file.absolutePath}", 
                mapOf("size" to "${file.length()} bytes"))
        } catch (e: Exception) {
            ToolResult.Error("write_file", e.message ?: "Unknown error")
        }
    }
    
    /**
     * Obtener procesos en ejecución
     */
    suspend fun getProcesses(): ToolResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("ps")
            val output = StringBuilder()
            
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { output.append(it).append("\n") }
            }
            
            ToolResult.Success("processes", output.toString())
        } catch (e: Exception) {
            ToolResult.Error("processes", e.message ?: "Unknown error")
        }
    }
    
    /**
     * Ping a host
     */
    suspend fun ping(host: String): ToolResult = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(host)
            val startTime = System.currentTimeMillis()
            val reachable = address.isReachable(5000)
            val latency = System.currentTimeMillis() - startTime
            
            val result = if (reachable) {
                "✅ $host is reachable\nLatency: ${latency}ms\nIP: ${address.hostAddress}"
            } else {
                "❌ $host is not reachable"
            }
            
            ToolResult.Success("ping", result)
        } catch (e: Exception) {
            ToolResult.Error("ping", e.message ?: "Unknown error")
        }
    }
    
    /**
     * Obtener IP local
     */
    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress.indexOf(':') < 0) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP", e)
        }
        return null
    }
    
    /**
     * Listar herramientas disponibles
     */
    fun listAvailableTools(): List<AgentTool> {
        return listOf(
            AgentTool(
                name = "shell",
                description = "Execute shell commands (no root required)",
                usage = "shell: <command>",
                example = "shell: ls -la"
            ),
            AgentTool(
                name = "system_info",
                description = "Get system information (Android version, memory, storage)",
                usage = "system_info",
                example = "system_info"
            ),
            AgentTool(
                name = "network_scan",
                description = "Scan local network for devices",
                usage = "network_scan",
                example = "network_scan"
            ),
            AgentTool(
                name = "list_files",
                description = "List files in directory",
                usage = "list_files: [path]",
                example = "list_files: /data"
            ),
            AgentTool(
                name = "read_file",
                description = "Read file content",
                usage = "read_file: <filename>",
                example = "read_file: config.txt"
            ),
            AgentTool(
                name = "write_file",
                description = "Write content to file",
                usage = "write_file: <filename> | <content>",
                example = "write_file: test.txt | Hello World"
            ),
            AgentTool(
                name = "processes",
                description = "List running processes",
                usage = "processes",
                example = "processes"
            ),
            AgentTool(
                name = "ping",
                description = "Ping a host to check connectivity",
                usage = "ping: <host>",
                example = "ping: google.com"
            )
        )
    }
}

/**
 * Resultado de ejecución de herramienta
 */
sealed class ToolResult {
    data class Success(
        val tool: String,
        val output: String,
        val metadata: Map<String, String> = emptyMap()
    ) : ToolResult()
    
    data class Error(
        val tool: String,
        val message: String
    ) : ToolResult()
}

/**
 * Definición de herramienta del agente
 */
data class AgentTool(
    val name: String,
    val description: String,
    val usage: String,
    val example: String
)
