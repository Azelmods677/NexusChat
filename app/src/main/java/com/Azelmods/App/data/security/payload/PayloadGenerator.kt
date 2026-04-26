package com.Azelmods.App.data.security.payload

import kotlinx.coroutines.flow.Flow

/**
 * Interface for generating Android payloads for security testing
 * 
 * Provides methods for:
 * - Generating custom payloads with various configurations
 * - Managing payload history
 * - Sharing and uploading payloads
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7
 */
interface PayloadGenerator {
    
    /**
     * Generates a payload with the specified configuration
     * 
     * @param config Payload configuration including type, host, port, obfuscation level
     * @return Flow emitting generation state updates
     */
    fun generatePayload(config: PayloadConfig): Flow<PayloadGenerationState>
    
    /**
     * Gets list of available payload types
     * 
     * @return List of supported payload types
     */
    fun getAvailablePayloadTypes(): List<PayloadType>
    
    /**
     * Gets payload generation history
     * 
     * @return Flow of generated payloads
     */
    fun getPayloadHistory(): Flow<List<GeneratedPayload>>
    
    /**
     * Deletes a payload from history and storage
     * 
     * @param payloadId ID of the payload to delete
     */
    suspend fun deletePayload(payloadId: String)
    
    /**
     * Shares a payload via Android share intent
     * 
     * @param payloadId ID of the payload to share
     */
    suspend fun sharePayload(payloadId: String)
    
    /**
     * Uploads a payload to Firebase Storage
     * 
     * @param payloadId ID of the payload to upload
     * @return Download URL of the uploaded payload
     */
    suspend fun uploadPayloadToFirebase(payloadId: String): String
}

/**
 * Configuration for payload generation
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7
 */
@kotlinx.parcelize.Parcelize
data class PayloadConfig(
    val type: PayloadType,
    val lhost: String,
    val lport: Int,
    val obfuscationLevel: ObfuscationLevel = ObfuscationLevel.NONE,
    val customPackageName: String? = null,
    val enableAntiEmulator: Boolean = false,
    val enableAntiDebug: Boolean = false,
    val enablePersistence: Boolean = false,
    val templateApkPath: String? = null
) : android.os.Parcelable

/**
 * Available payload types based on msfvenom
 * 
 * Requirements: 11.1, 11.2, 11.3
 */
enum class PayloadType(
    val msfvenomName: String,
    val displayName: String,
    val description: String
) {
    METERPRETER_REVERSE_TCP(
        msfvenomName = "android/meterpreter/reverse_tcp",
        displayName = "Meterpreter Reverse TCP",
        description = "Standard reverse TCP connection with full Meterpreter functionality"
    ),
    METERPRETER_REVERSE_HTTP(
        msfvenomName = "android/meterpreter/reverse_http",
        displayName = "Meterpreter Reverse HTTP",
        description = "HTTP-based reverse connection, better for firewall bypass"
    ),
    METERPRETER_REVERSE_HTTPS(
        msfvenomName = "android/meterpreter/reverse_https",
        displayName = "Meterpreter Reverse HTTPS",
        description = "HTTPS-based reverse connection with encryption"
    ),
    SHELL_REVERSE_TCP(
        msfvenomName = "android/shell/reverse_tcp",
        displayName = "Shell Reverse TCP",
        description = "Lightweight reverse shell without Meterpreter"
    ),
    SHELL_REVERSE_HTTP(
        msfvenomName = "android/shell/reverse_http",
        displayName = "Shell Reverse HTTP",
        description = "HTTP-based reverse shell"
    )
}

/**
 * Obfuscation levels for payload generation
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
enum class ObfuscationLevel(
    val displayName: String,
    val description: String,
    val encodingIterations: Int
) {
    NONE(
        displayName = "None",
        description = "No obfuscation applied",
        encodingIterations = 0
    ),
    LOW(
        displayName = "Low",
        description = "Basic string encoding",
        encodingIterations = 1
    ),
    MEDIUM(
        displayName = "Medium",
        description = "String encoding + control flow obfuscation",
        encodingIterations = 3
    ),
    HIGH(
        displayName = "High",
        description = "Full obfuscation with anti-detection",
        encodingIterations = 5
    ),
    EXTREME(
        displayName = "Extreme",
        description = "Maximum obfuscation (slow generation)",
        encodingIterations = 10
    )
}

/**
 * State of payload generation process
 * 
 * Requirements: 7.3, 24.1, 24.2, 24.3, 24.4, 24.5, 24.6
 */
sealed class PayloadGenerationState {
    object Idle : PayloadGenerationState()
    
    data class Validating(val message: String) : PayloadGenerationState()
    
    data class Generating(
        val progress: Int,
        val message: String
    ) : PayloadGenerationState()
    
    data class Obfuscating(
        val progress: Int,
        val message: String
    ) : PayloadGenerationState()
    
    data class Signing(
        val progress: Int,
        val message: String
    ) : PayloadGenerationState()
    
    data class Success(val payload: GeneratedPayload) : PayloadGenerationState()
    
    data class Error(
        val message: String,
        val suggestion: String? = null
    ) : PayloadGenerationState()
}

/**
 * Generated payload information
 * 
 * Requirements: 11.4, 11.5, 11.6, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7
 */
data class GeneratedPayload(
    val id: String,
    val type: PayloadType,
    val lhost: String,
    val lport: Int,
    val obfuscationLevel: ObfuscationLevel,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val sha256Hash: String,
    val timestamp: Long,
    val firebaseUrl: String? = null,
    val customPackageName: String? = null,
    val hasAntiEmulator: Boolean = false,
    val hasAntiDebug: Boolean = false,
    val hasPersistence: Boolean = false
)

/**
 * Validation result for payload configuration
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    
    data class Invalid(
        val errors: List<ValidationError>
    ) : ValidationResult()
}

/**
 * Validation error details
 */
data class ValidationError(
    val field: String,
    val message: String
)
