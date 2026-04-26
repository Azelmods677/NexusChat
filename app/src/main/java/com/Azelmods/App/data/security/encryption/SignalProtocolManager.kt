package com.Azelmods.App.data.security.encryption

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.*
import org.signal.libsignal.protocol.util.KeyHelper
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
    private val preKeyManager: PreKeyManager
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
     * Initializes Signal Protocol for the current user.
     *
     * This should be called once during user registration or first app launch.
     * Generates:
     * - Identity key pair
     * - Signed PreKey
     * - Batch of OneTime PreKeys
     *
     * @param userId The unique identifier for the current user
     * @return [SignalProtocolInitResult] containing keys to upload to server
     */
    suspend fun initialize(userId: String): SignalProtocolInitResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Signal Protocol for user: $userId")

            // Check if already initialized
            if (keyStore.hasIdentityKeyPair()) {
                Log.w(TAG, "Signal Protocol already initialized for this user")
                return@withContext SignalProtocolInitResult.AlreadyInitialized
            }

            // Generate identity key pair
            val identityKeyPair = KeyHelper.generateIdentityKeyPair()
            val registrationId = KeyHelper.generateRegistrationId(false)

            // Store identity key pair
            keyStore.saveIdentityKeyPair(identityKeyPair)
            keyStore.saveLocalRegistrationId(registrationId)

            Log.d(TAG, "Generated identity key pair with registration ID: $registrationId")

            // Generate signed PreKey
            val signedPreKeyId = 1
            val signedPreKeyPair = KeyHelper.generateSignedPreKey(identityKeyPair, signedPreKeyId)
            keyStore.storeSignedPreKey(signedPreKeyId, SignedPreKeyRecord(
                signedPreKeyId,
                System.currentTimeMillis(),
                signedPreKeyPair
            ))

            Log.d(TAG, "Generated signed PreKey with ID: $signedPreKeyId")

            // Generate batch of OneTime PreKeys
            val preKeys = KeyHelper.generatePreKeys(1, PREKEY_BATCH_SIZE)
            preKeys.forEach { preKey ->
                keyStore.storePreKey(preKey.id, PreKeyRecord(preKey.id, preKey))
            }

            Log.d(TAG, "Generated ${preKeys.size} OneTime PreKeys")

            // Return keys that need to be uploaded to server
            SignalProtocolInitResult.Success(
                identityKey = identityKeyPair.publicKey,
                signedPreKey = signedPreKeyPair,
                preKeys = preKeys,
                registrationId = registrationId
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Signal Protocol", e)
            SignalProtocolInitResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Encrypts a message for a specific recipient.
     *
     * If no session exists with the recipient, this will establish one using
     * their PreKey bundle (which must be fetched from the server first).
     *
     * @param recipientId The unique identifier of the message recipient
     * @param plaintext The message content to encrypt
     * @param recipientPreKeyBundle Optional PreKey bundle if establishing new session
     * @return [EncryptionResult] containing the encrypted message
     */
    suspend fun encryptMessage(
        recipientId: String,
        plaintext: String,
        recipientPreKeyBundle: PreKeyBundle? = null
    ): EncryptionResult = withContext(Dispatchers.IO) {
        try {
            val recipientAddress = SignalProtocolAddress(recipientId, 1)

            // Check if session exists
            if (!keyStore.containsSession(recipientAddress)) {
                if (recipientPreKeyBundle == null) {
                    return@withContext EncryptionResult.Error(
                        "No session exists and no PreKey bundle provided"
                    )
                }

                // Establish new session using PreKey bundle
                val sessionBuilder = SessionBuilder(keyStore, recipientAddress)
                sessionBuilder.process(recipientPreKeyBundle)

                Log.d(TAG, "Established new session with: $recipientId")
            }

            // Encrypt message
            val sessionCipher = SessionCipher(keyStore, recipientAddress)
            val ciphertext = sessionCipher.encrypt(plaintext.toByteArray())

            Log.d(TAG, "Encrypted message for: $recipientId (type: ${ciphertext.type})")

            EncryptionResult.Success(
                ciphertext = ciphertext.serialize(),
                messageType = when (ciphertext.type) {
                    CiphertextMessage.PREKEY_TYPE -> MessageType.PREKEY
                    CiphertextMessage.WHISPER_TYPE -> MessageType.WHISPER
                    else -> MessageType.UNKNOWN
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting message", e)
            EncryptionResult.Error(e.message ?: "Encryption failed")
        }
    }

    /**
     * Decrypts a message from a specific sender.
     *
     * Automatically handles session establishment if this is a PreKey message.
     *
     * @param senderId The unique identifier of the message sender
     * @param ciphertext The encrypted message bytes
     * @param messageType The type of encrypted message (PreKey or Whisper)
     * @return [DecryptionResult] containing the decrypted plaintext
     */
    suspend fun decryptMessage(
        senderId: String,
        ciphertext: ByteArray,
        messageType: MessageType
    ): DecryptionResult = withContext(Dispatchers.IO) {
        try {
            val senderAddress = SignalProtocolAddress(senderId, 1)
            val sessionCipher = SessionCipher(keyStore, senderAddress)

            val plaintext = when (messageType) {
                MessageType.PREKEY -> {
                    // PreKey message - establishes new session
                    val preKeyMessage = PreKeySignalMessage(ciphertext)
                    sessionCipher.decrypt(preKeyMessage)
                }
                MessageType.WHISPER -> {
                    // Regular message in existing session
                    val signalMessage = SignalMessage(ciphertext)
                    sessionCipher.decrypt(signalMessage)
                }
                MessageType.UNKNOWN -> {
                    return@withContext DecryptionResult.Error("Unknown message type")
                }
            }

            Log.d(TAG, "Decrypted message from: $senderId")

            DecryptionResult.Success(String(plaintext))

        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting message", e)
            DecryptionResult.Error(e.message ?: "Decryption failed")
        }
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
     * Checks if PreKeys need to be replenished and generates new ones if needed.
     *
     * Should be called periodically (e.g., daily) to maintain a healthy pool of PreKeys.
     *
     * @return Number of new PreKeys generated, or 0 if no replenishment needed
     */
    suspend fun replenishPreKeysIfNeeded(): Int = withContext(Dispatchers.IO) {
        try {
            val currentPreKeyCount = keyStore.getPreKeyCount()

            if (currentPreKeyCount < PREKEY_MINIMUM_THRESHOLD) {
                Log.d(TAG, "PreKey count ($currentPreKeyCount) below threshold, generating new batch")

                val nextPreKeyId = keyStore.getNextPreKeyId()
                val identityKeyPair = keyStore.getIdentityKeyPair()

                val newPreKeys = KeyHelper.generatePreKeys(nextPreKeyId, PREKEY_BATCH_SIZE)
                newPreKeys.forEach { preKey ->
                    keyStore.storePreKey(preKey.id, PreKeyRecord(preKey.id, preKey))
                }

                Log.d(TAG, "Generated ${newPreKeys.size} new PreKeys")
                return@withContext newPreKeys.size
            }

            0

        } catch (e: Exception) {
            Log.e(TAG, "Error replenishing PreKeys", e)
            0
        }
    }

    /**
     * Rotates the signed PreKey.
     *
     * Should be called periodically (e.g., weekly) for forward secrecy.
     *
     * @return `true` if rotation succeeded, `false` otherwise
     */
    suspend fun rotateSignedPreKey(): Boolean = withContext(Dispatchers.IO) {
        try {
            val identityKeyPair = keyStore.getIdentityKeyPair()
            val nextSignedPreKeyId = keyStore.getNextSignedPreKeyId()

            val newSignedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, nextSignedPreKeyId)
            keyStore.storeSignedPreKey(nextSignedPreKeyId, SignedPreKeyRecord(
                nextSignedPreKeyId,
                System.currentTimeMillis(),
                newSignedPreKey
            ))

            Log.d(TAG, "Rotated signed PreKey to ID: $nextSignedPreKeyId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error rotating signed PreKey", e)
            false
        }
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
