package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.security.encryption.EncryptionResult
import com.Azelmods.App.data.security.encryption.PreKeyManager
import com.Azelmods.App.data.security.encryption.SignalProtocolManager
import javax.inject.Inject

/**
 * Use case for encrypting messages using Signal Protocol E2EE.
 *
 * Handles:
 * - Fetching recipient's PreKey bundle if needed
 * - Encrypting message content
 * - Returning encrypted ciphertext with metadata
 *
 * Requirements: 1.1, 1.2
 */
class EncryptMessageUseCase @Inject constructor(
    private val signalProtocolManager: SignalProtocolManager,
    private val preKeyManager: PreKeyManager
) {

    /**
     * Encrypts a message for a specific recipient.
     *
     * @param recipientId The recipient's user ID
     * @param plaintext The message content to encrypt
     * @return [EncryptionResult] containing encrypted message or error
     */
    suspend operator fun invoke(
        recipientId: String,
        plaintext: String
    ): EncryptionResult {
        // Try to encrypt with existing session first
        var result = signalProtocolManager.encryptMessage(
            recipientId = recipientId,
            plaintext = plaintext,
            recipientPreKeyBundle = null
        )

        // If no session exists, fetch PreKey bundle and try again
        if (result is EncryptionResult.Error && 
            result.message.contains("No session exists")) {
            
            val preKeyBundle = preKeyManager.fetchPreKeyBundle(recipientId)
                ?: return EncryptionResult.Error("Failed to fetch recipient's PreKey bundle")

            result = signalProtocolManager.encryptMessage(
                recipientId = recipientId,
                plaintext = plaintext,
                recipientPreKeyBundle = preKeyBundle
            )
        }

        return result
    }
}
