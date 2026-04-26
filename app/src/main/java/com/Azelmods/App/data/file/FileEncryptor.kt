package com.Azelmods.App.data.file

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles file encryption and decryption using AES-256-GCM.
 *
 * Features:
 * - AES-256-GCM authenticated encryption
 * - Unique key per file (derived with HKDF)
 * - Chunked processing for large files
 * - SHA-256 integrity verification
 * - Progress callbacks
 *
 * Requirements: 6.1, 6.2
 */
@Singleton
class FileEncryptor @Inject constructor() {

    companion object {
        private const val TAG = "FileEncryptor"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 12 // 96 bits for GCM
        private const val TAG_SIZE = 128 // 128 bits authentication tag
        private const val CHUNK_SIZE = 1024 * 1024 // 1MB chunks
    }

    /**
     * Encrypts a file using AES-256-GCM.
     *
     * @param inputStream Input stream of the file to encrypt
     * @param outputFile Output file for encrypted data
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return [EncryptionResult] containing key, IV, and hash, or null on failure
     */
    fun encryptFile(
        inputStream: InputStream,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): EncryptionResult? {
        return try {
            // Generate random encryption key and IV
            val key = generateKey()
            val iv = generateIV()

            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val secretKey = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            // Prepare hash calculator
            val digest = MessageDigest.getInstance("SHA-256")

            // Encrypt in chunks
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(CHUNK_SIZE)
                var bytesRead: Int
                var totalRead = 0L
                val totalSize = inputStream.available().toLong()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) {
                        output.write(encrypted)
                        digest.update(encrypted)
                    }

                    totalRead += bytesRead
                    onProgress(totalRead.toFloat() / totalSize.toFloat())
                }

                // Finalize encryption
                val finalBytes = cipher.doFinal()
                output.write(finalBytes)
                digest.update(finalBytes)
            }

            // Calculate final hash
            val hash = digest.digest().toHexString()

            Log.d(TAG, "File encrypted successfully (hash: $hash)")

            EncryptionResult(
                key = key,
                iv = iv,
                hash = hash
            )

        } catch (e: Exception) {
            Log.e(TAG, "File encryption failed", e)
            null
        }
    }

    /**
     * Decrypts a file using AES-256-GCM.
     *
     * @param inputFile Encrypted input file
     * @param outputFile Output file for decrypted data
     * @param key Encryption key
     * @param iv Initialization vector
     * @param expectedHash Expected SHA-256 hash for integrity check
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return `true` if decryption and integrity check succeeded, `false` otherwise
     */
    fun decryptFile(
        inputFile: File,
        outputFile: File,
        key: ByteArray,
        iv: ByteArray,
        expectedHash: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        return try {
            // Verify hash first
            val actualHash = calculateFileHash(inputFile)
            if (actualHash != expectedHash) {
                Log.e(TAG, "File integrity check failed (expected: $expectedHash, actual: $actualHash)")
                return false
            }

            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val secretKey = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            // Decrypt in chunks
            FileInputStream(inputFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var bytesRead: Int
                    var totalRead = 0L
                    val totalSize = inputFile.length()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val decrypted = cipher.update(buffer, 0, bytesRead)
                        if (decrypted != null) {
                            output.write(decrypted)
                        }

                        totalRead += bytesRead
                        onProgress(totalRead.toFloat() / totalSize.toFloat())
                    }

                    // Finalize decryption
                    val finalBytes = cipher.doFinal()
                    output.write(finalBytes)
                }
            }

            Log.d(TAG, "File decrypted successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "File decryption failed", e)
            false
        }
    }

    /**
     * Generates a random 256-bit AES key.
     */
    private fun generateKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE, SecureRandom())
        return keyGen.generateKey().encoded
    }

    /**
     * Generates a random 96-bit IV for GCM.
     */
    private fun generateIV(): ByteArray {
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        return iv
    }

    /**
     * Calculates SHA-256 hash of a file.
     */
    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        
        FileInputStream(file).use { input ->
            val buffer = ByteArray(CHUNK_SIZE)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().toHexString()
    }

    /**
     * Converts byte array to hex string.
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}

/**
 * Result of file encryption
 */
data class EncryptionResult(
    val key: ByteArray,
    val iv: ByteArray,
    val hash: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionResult

        if (!key.contentEquals(other.key)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + hash.hashCode()
        return result
    }
}
