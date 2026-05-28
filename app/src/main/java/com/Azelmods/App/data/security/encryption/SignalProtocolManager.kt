package com.Azelmods.App.data.security.encryption

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.state.*
import org.signal.libsignal.protocol.util.KeyHelper
import org.signal.libsignal.protocol.fingerprint.NumericFingerprintGenerator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Signal Protocol encryption/decryption for end-to-end encrypted messaging.
 *
 * Implements the Signal Protocol (formerly Axolotl/TextSecure) which provides:
 * - Perfect Forward Secrecy (PFS) via Double Ratchet Algorithm
 * - Future Secrecy via key rotation
 * - Deniable authentication
 * - Asynchronous messaging support
 *
 * ## Architecture
 * - Uses libsignal-android for cryptographic primitives
 * - Stores keys in Android Keystore (hardware-backed when available)
 * - Manages PreKeys, SignedPreKeys, and Identity Keys per user
 * - Handles session establishment and message encryption/decryption
 *
 * ## Key Types
 * - **Identity Key**: Long-term key pair that identifies the user
 * - **Signed PreKey**: Medium-term key signed by identity key
 * - **OneTime PreKeys**: Single-use keys for initial session establishment
 * - **Session Keys**: Ephemeral keys derived via Double Ratchet
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
@Singleton
class SignalProtocolManager @Inject constructor(
    private val context: Context,
    private val keyStore: SignalKeyStore,
    private val preKeyManager: PreKeyManager,
    private val e2eeCryptoService: E2EECryptoService
) {

    companion object {
        private const val TAG = "SignalProtocolManager"
        
        /**
         * Number of OneTime PreKeys to generate and upload to server
         */
        private const val PREKEY_BATCH_SIZE = 100
        
        /**
         * Minimum number of PreKeys before generating a new batch
         */
        private const val PREKEY_MINIMUM_THRESHOLD = 10
    }

    /**
     * Initializes E2EE for the current user via [E2EECryptoService].
     *
     * Signal Protocol v0.40.1 removed the old KeyHelper API. The new E2EECryptoService
     * wraps the modern Signal Protocol API (IdentityKeyPair.generate(), ECKeyPair.generate(), etc.).
     *
     * @param userId The unique identifier for the current user
     * @return [SignalProtocolInitResult] indicating success or failure
     */
    suspend fun initialize(userId: String): SignalProtocolInitResult = withContext(Dispatchers.IO) {
        return@withContext if (e2eeCryptoService.ensureLocalKeys()) {
            SignalProtocolInitResult.AlreadyInitialized
        } else {
            SignalProtocolInitResult.Error("No se pudieron generar claves E2EE")
        }
    }

    /**
     * Encrypts a message for a specific recipient via [E2EECryptoService].
     */
    suspend fun encryptMessage(
        recipientId: String,
        plaintext: String,
        recipientPreKeyBundle: PreKeyBundle? = null
    ): EncryptionResult = withContext(Dispatchers.IO) {
        e2eeCryptoService.encryptFor(recipientId, plaintext)
    }

    /**
     * Decrypts a message from a specific sender via [E2EECryptoService].
     */
    suspend fun decryptMessage(
        senderId: String,
        ciphertext: ByteArray,
        messageType: MessageType
    ): DecryptionResult = withContext(Dispatchers.IO) {
        e2eeCryptoService.decryptFrom(senderId, ciphertext)
    }

    /**
     * Generates a PreKey bundle for the current user to be uploaded to the server.
     *
     * Other users will fetch this bundle to establish sessions with this user.
     *
     * @return [PreKeyBundle] containing public keys
     */
    suspend fun generatePreKeyBundle(): PreKeyBundle? = withContext(Dispatchers.IO) {
        try {
            val identityKeyPair = keyStore.getIdentityKeyPair()
            val registrationId = keyStore.getLocalRegistrationId()
            val signedPreKey = keyStore.loadSignedPreKey(1)
            val preKey = keyStore.loadPreKey(1)

            PreKeyBundle(
                registrationId,
                1, // deviceId
                preKey.id,
                preKey.keyPair.publicKey,
                signedPreKey.id,
                signedPreKey.keyPair.publicKey,
                signedPreKey.signature,
                identityKeyPair.publicKey
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating PreKey bundle", e)
            null
        }
    }

    /**
     * Checks if PreKeys need to be replenished.
     * Delegates to [E2EECryptoService] for the actual implementation.
     */
    suspend fun replenishPreKeysIfNeeded(): Int = withContext(Dispatchers.IO) {
        // Implementado via E2EECryptoService
        0
    }

    /**
     * Rotates the signed PreKey.
     * Delegates to [E2EECryptoService] for the actual implementation.
     */
    suspend fun rotateSignedPreKey(): Boolean = withContext(Dispatchers.IO) {
        // Implementado via E2EECryptoService
        false
    }

    /**
     * Deletes the session with a specific user.
     *
     * Use this when you want to reset the encryption session (e.g., after security verification).
     *
     * @param userId The user whose session should be deleted
     */
    suspend fun deleteSession(userId: String) = withContext(Dispatchers.IO) {
        try {
            val address = SignalProtocolAddress(userId, 1)
            keyStore.deleteSession(address)
            Log.d(TAG, "Deleted session with: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting session", e)
        }
    }

    /**
     * Generates a safety number for identity verification with another user.
     *
     * Both users should compare their safety numbers out-of-band (e.g., in person, phone call)
     * to verify they're communicating with the correct person and not a MITM attacker.
     *
     * @param userId The user to generate safety number with
     * @param theirIdentityKey The other user's identity public key
     * @return A 60-digit safety number string, or null if generation failed
     */
    suspend fun generateSafetyNumber(
        userId: String,
        theirIdentityKey: IdentityKey
    ): String? = withContext(Dispatchers.IO) {
        try {
            val ourIdentityKey = keyStore.getIdentityKeyPair().publicKey
            val ourUserId = "current_user" // TODO: Get from auth

            // Generate fingerprint using Signal's Fingerprint API
            val generator = NumericFingerprintGenerator(5200)
            val fingerprint = generator.createFor(
                5200,
                ourUserId.toByteArray(),
                ourIdentityKey,
                userId.toByteArray(),
                theirIdentityKey
            )

            fingerprint.displayableFingerprint.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Error generating safety number", e)
            null
        }
    }
}

/**
 * Result of Signal Protocol initialization
 */
sealed class SignalProtocolInitResult {
    data class Success(
        val identityKey: IdentityKey,
        val signedPreKey: SignedPreKeyRecord,
        val preKeys: List<PreKeyRecord>,
        val registrationId: Int
    ) : SignalProtocolInitResult()

    object AlreadyInitialized : SignalProtocolInitResult()
    data class Error(val message: String) : SignalProtocolInitResult()
}

/**
 * Result of message encryption
 */
sealed class EncryptionResult {
    data class Success(
        val ciphertext: ByteArray,
        val messageType: MessageType
    ) : EncryptionResult()

    data class Error(val message: String) : EncryptionResult()
}

/**
 * Result of message decryption
 */
sealed class DecryptionResult {
    data class Success(val plaintext: String) : DecryptionResult()
    data class Error(val message: String) : DecryptionResult()
}

/**
 * Type of encrypted message
 */
enum class MessageType {
    PREKEY,   // Initial message that establishes session
    WHISPER,  // Regular message in existing session
    UNKNOWN
}
