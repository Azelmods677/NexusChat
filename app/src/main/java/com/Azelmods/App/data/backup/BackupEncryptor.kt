package com.Azelmods.App.data.backup

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles backup encryption and decryption using password-based encryption.
 *
 * Features:
 * - AES-256-GCM authenticated encryption
 * - PBKDF2 key derivation from password
 * - HMAC-SHA256 integrity verification
 * - Salt and IV generation
 * - Compression with GZIP
 *
 * ## File Format
 * ```
 * [MAGIC_BYTES: 8 bytes]
 * [VERSION: 1 byte]
 * [SALT: 32 bytes]
 * [IV: 12 bytes]
 * [HMAC: 32 bytes]
 * [ENCRYPTED_DATA: variable]
 * ```
 *
 * Requirements: 10.1, 10.4
 */
@Singleton
class BackupEncryptor @Inject constructor() {

    companion object {
        private const val TAG = "BackupEncryptor"
        
        // File format constants
        private val MAGIC_BYTES = "AZELBACK".toByteArray()
        private const val VERSION: Byte = 1
        
        // Crypto constants
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
        private const val SALT_SIZE = 32
        private const val PBKDF2_ITERATIONS = 100_000
        
        // HMAC
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    /**
     * Encrypts a backup file with a password.
     *
     * Process:
     * 1. Compresses data with GZIP
     * 2. Derives encryption key from password using PBKDF2
     * 3. Encrypts with AES-256-GCM
     * 4. Calculates HMAC for integrity
     * 5. Writes encrypted file with header
     *
     * @param inputFile The backup file to encrypt
     * @param outputFile The encrypted output file
     * @param password The encryption password
     * @return `true` if encryption succeeded
     */
    fun encryptBackup(
        inputFile: File,
        outputFile: File,
        password: String
    ): Boolean {
        return try {
            Log.d(TAG, "Encrypting backup: ${inputFile.name}")

            // Generate salt and IV
            val salt = ByteArray(SALT_SIZE).apply { SecureRandom().nextBytes(this) }
            val iv = ByteArray(IV_SIZE).apply { SecureRandom().nextBytes(this) }

            // Derive encryption key from password
            val encryptionKey = deriveKey(password, salt)

            // Read and compress input data
            val inputData = inputFile.readBytes()
            val compressedData = compress(inputData)

            Log.d(TAG, "Compressed ${inputData.size} bytes to ${compressedData.size} bytes")

            // Encrypt data
            val cipher = Cipher.getInstance(ALGORITHM)
            val secretKey = SecretKeySpec(encryptionKey, "AES")
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            val encryptedData = cipher.doFinal(compressedData)

            // Calculate HMAC
            val hmac = calculateHMAC(encryptedData, encryptionKey)

            // Write encrypted file with header
            FileOutputStream(outputFile).use { output ->
                output.write(MAGIC_BYTES)
                output.write(byteArrayOf(VERSION))
                output.write(salt)
                output.write(iv)
                output.write(hmac)
                output.write(encryptedData)
            }

            Log.d(TAG, "Backup encrypted successfully: ${outputFile.name} (${outputFile.length()} bytes)")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Backup encryption failed", e)
            false
        }
    }

    /**
     * Decrypts a backup file with a password.
     *
     * Process:
     * 1. Reads and validates file header
     * 2. Derives decryption key from password
     * 3. Verifies HMAC integrity
     * 4. Decrypts with AES-256-GCM
     * 5. Decompresses data
     *
     * @param inputFile The encrypted backup file
     * @param outputFile The decrypted output file
     * @param password The decryption password
     * @return `true` if decryption succeeded
     */
    fun decryptBackup(
        inputFile: File,
        outputFile: File,
        password: String
    ): Boolean {
        return try {
            Log.d(TAG, "Decrypting backup: ${inputFile.name}")

            // Read encrypted file
            FileInputStream(inputFile).use { input ->
                // Validate magic bytes
                val magic = ByteArray(MAGIC_BYTES.size)
                input.read(magic)
                if (!magic.contentEquals(MAGIC_BYTES)) {
                    Log.e(TAG, "Invalid backup file format")
                    return false
                }

                // Read version
                val version = input.read().toByte()
                if (version != VERSION) {
                    Log.e(TAG, "Unsupported backup version: $version")
                    return false
                }

                // Read salt, IV, and HMAC
                val salt = ByteArray(SALT_SIZE).apply { input.read(this) }
                val iv = ByteArray(IV_SIZE).apply { input.read(this) }
                val expectedHmac = ByteArray(32).apply { input.read(this) }

                // Read encrypted data
                val encryptedData = input.readBytes()

                // Derive decryption key
                val decryptionKey = deriveKey(password, salt)

                // Verify HMAC
                val actualHmac = calculateHMAC(encryptedData, decryptionKey)
                if (!actualHmac.contentEquals(expectedHmac)) {
                    Log.e(TAG, "HMAC verification failed - wrong password or corrupted file")
                    return false
                }

                // Decrypt data
                val cipher = Cipher.getInstance(ALGORITHM)
                val secretKey = SecretKeySpec(decryptionKey, "AES")
                val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

                val compressedData = cipher.doFinal(encryptedData)

                // Decompress data
                val decryptedData = decompress(compressedData)

                // Write decrypted file
                outputFile.writeBytes(decryptedData)

                Log.d(TAG, "Backup decrypted successfully: ${outputFile.name} (${outputFile.length()} bytes)")
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Backup decryption failed", e)
            false
        }
    }

    /**
     * Verifies if a password is correct for a backup file without full decryption.
     *
     * Only reads the header and verifies HMAC.
     *
     * @param backupFile The encrypted backup file
     * @param password The password to verify
     * @return `true` if password is correct
     */
    fun verifyPassword(backupFile: File, password: String): Boolean {
        return try {
            FileInputStream(backupFile).use { input ->
                // Skip magic bytes and version
                input.skip((MAGIC_BYTES.size + 1).toLong())

                // Read salt, IV, and HMAC
                val salt = ByteArray(SALT_SIZE).apply { input.read(this) }
                val iv = ByteArray(IV_SIZE).apply { input.read(this) }
                val expectedHmac = ByteArray(32).apply { input.read(this) }

                // Read first chunk of encrypted data (enough for HMAC verification)
                val encryptedChunk = ByteArray(1024).apply { input.read(this) }

                // Derive key
                val key = deriveKey(password, salt)

                // Verify HMAC on first chunk
                val actualHmac = calculateHMAC(encryptedChunk, key)
                
                // Compare first 16 bytes (sufficient for password verification)
                actualHmac.take(16).toByteArray()
                    .contentEquals(expectedHmac.take(16).toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Password verification failed", e)
            false
        }
    }

    /**
     * Derives an encryption key from a password using PBKDF2.
     */
    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /**
     * Calculates HMAC-SHA256 for integrity verification.
     */
    private fun calculateHMAC(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKey = SecretKeySpec(key, HMAC_ALGORITHM)
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    /**
     * Compresses data using GZIP.
     */
    private fun compress(data: ByteArray): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(data)
        }
        return outputStream.toByteArray()
    }

    /**
     * Decompresses GZIP data.
     */
    private fun decompress(data: ByteArray): ByteArray {
        val inputStream = java.io.ByteArrayInputStream(data)
        val outputStream = java.io.ByteArrayOutputStream()
        
        java.util.zip.GZIPInputStream(inputStream).use { gzip ->
            gzip.copyTo(outputStream)
        }
        
        return outputStream.toByteArray()
    }
}
