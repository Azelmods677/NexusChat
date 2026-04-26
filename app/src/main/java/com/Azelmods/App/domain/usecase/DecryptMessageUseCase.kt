package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.security.encryption.DecryptionResult
import com.Azelmods.App.data.security.encryption.MessageType
import com.Azelmods.App.data.security.encryption.SignalProtocolManager
import javax.inject.Inject

/**
 * Use case for decrypting messages using Signal Protocol E2EE.
 *
 * Handles:
 * - Decrypting received encrypted messages
 * - Establishing sessions from PreKey messages
 * - Returning decrypted plaintext
 *
 * Requirements: 1.1, 1.2
 */
class DecryptMessageUseCase @Inject constructor(
    private val signalProtocolManager: SignalProtocolManager
) {

    /**
     * Decrypts a message from a specific sender.
     *
     * @param senderId The sender's user ID
     * @param ciphertext The encrypted message bytes
     * @param messageType The type of encrypted message (PreKey or Whisper)
     * @return [DecryptionResult] containing decrypted plaintext or error
     */
    suspend operator fun invoke(
        senderId: String,
        ciphertext: ByteArray,
        messageType: MessageType
    ): DecryptionResult {
        return signalProtocolManager.decryptMessage(
            senderId = senderId,
            ciphertext = ciphertext,
            messageType = messageType
        )
    }
}
