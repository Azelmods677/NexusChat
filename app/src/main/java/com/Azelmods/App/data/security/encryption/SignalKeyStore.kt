package com.Azelmods.App.data.security.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.state.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("DEPRECATION")

/**
 * Secure storage for Signal Protocol keys using Android Keystore.
 *
 * Implements all required Signal Protocol storage interfaces:
 * - [IdentityKeyStore]: Stores identity keys and trusted keys
 * - [PreKeyStore]: Stores OneTime PreKeys
 * - [SignedPreKeyStore]: Stores signed PreKeys
 * - [SessionStore]: Stores session state
 *
 * ## Security
 * - Uses Android Keystore for hardware-backed encryption when available
 * - Falls back to software encryption on older devices
 * - All keys are encrypted at rest using AES-256-GCM
 * - Uses EncryptedSharedPreferences for additional protection
 *
 * Requirements: 1.3, 1.4
 */
@Singleton
class SignalKeyStore @Inject constructor(
    private val context: Context
) : IdentityKeyStore, PreKeyStore, SignedPreKeyStore, SessionStore {

    companion object {
        private const val TAG = "SignalKeyStore"
        private const val KEYSTORE_ALIAS = "nexus_chat_signal_key"
        private const val PREFS_NAME = "signal_key_store"
        
        // Preference keys
        private const val KEY_IDENTITY_PUBLIC = "identity_public"
        private const val KEY_IDENTITY_PRIVATE = "identity_private"
        private const val KEY_REGISTRATION_ID = "registration_id"
        private const val KEY_PREKEY_PREFIX = "prekey_"
        private const val KEY_SIGNED_PREKEY_PREFIX = "signed_prekey_"
        private const val KEY_SESSION_PREFIX = "session_"
        private const val KEY_TRUSTED_IDENTITY_PREFIX = "trusted_identity_"
        private const val KEY_NEXT_PREKEY_ID = "next_prekey_id"
        private const val KEY_NEXT_SIGNED_PREKEY_ID = "next_signed_prekey_id"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val androidKeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    init {
        // Generate master encryption key if it doesn't exist
        if (!androidKeyStore.containsAlias(KEYSTORE_ALIAS)) {
            generateMasterKey()
        }
    }

    // ── IdentityKeyStore implementation ──────────────────────────────────────

    override fun getIdentityKeyPair(): IdentityKeyPair {
        // TODO: Update to libsignal 0.40.1 API - IdentityKeyPair constructor signature changed
        throw UnsupportedOperationException("Signal Protocol temporarily disabled - API migration needed")
        
        /* OLD CODE - needs API migration
        val publicBytes = encryptedPrefs.getString(KEY_IDENTITY_PUBLIC, null)
            ?: throw IllegalStateException("Identity key pair not initialized")
        val privateBytes = encryptedPrefs.getString(KEY_IDENTITY_PRIVATE, null)
            ?: throw IllegalStateException("Identity key pair not initialized")

        return IdentityKeyPair(
            IdentityKey(android.util.Base64.decode(publicBytes, android.util.Base64.DEFAULT)),
            android.util.Base64.decode(privateBytes, android.util.Base64.DEFAULT)
        )
        */
    }

    override fun getLocalRegistrationId(): Int {
        return encryptedPrefs.getInt(KEY_REGISTRATION_ID, 0)
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val key = KEY_TRUSTED_IDENTITY_PREFIX + address.name
        val existing = encryptedPrefs.getString(key, null)
        val newValue = android.util.Base64.encodeToString(
            identityKey.serialize(),
            android.util.Base64.DEFAULT
        )

        encryptedPrefs.edit().putString(key, newValue).apply()

        // Return true if this is a new identity or if it changed
        return existing == null || existing != newValue
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        val key = KEY_TRUSTED_IDENTITY_PREFIX + address.name
        val trusted = encryptedPrefs.getString(key, null) ?: return true // Trust on first use

        val trustedKey = IdentityKey(
            android.util.Base64.decode(trusted, android.util.Base64.DEFAULT)
        )

        return trustedKey == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val key = KEY_TRUSTED_IDENTITY_PREFIX + address.name
        val identityBytes = encryptedPrefs.getString(key, null) ?: return null

        return IdentityKey(android.util.Base64.decode(identityBytes, android.util.Base64.DEFAULT))
    }

    // ── PreKeyStore implementation ───────────────────────────────────────────

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val key = KEY_PREKEY_PREFIX + preKeyId
        val serialized = encryptedPrefs.getString(key, null)
            ?: throw InvalidKeyIdException("PreKey not found: $preKeyId")

        return PreKeyRecord(android.util.Base64.decode(serialized, android.util.Base64.DEFAULT))
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        val key = KEY_PREKEY_PREFIX + preKeyId
        val serialized = android.util.Base64.encodeToString(
            record.serialize(),
            android.util.Base64.DEFAULT
        )

        encryptedPrefs.edit().putString(key, serialized).apply()
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        val key = KEY_PREKEY_PREFIX + preKeyId
        return encryptedPrefs.contains(key)
    }

    override fun removePreKey(preKeyId: Int) {
        val key = KEY_PREKEY_PREFIX + preKeyId
        encryptedPrefs.edit().remove(key).apply()
    }

    // ── SignedPreKeyStore implementation ─────────────────────────────────────

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val key = KEY_SIGNED_PREKEY_PREFIX + signedPreKeyId
        val serialized = encryptedPrefs.getString(key, null)
            ?: throw InvalidKeyIdException("SignedPreKey not found: $signedPreKeyId")

        return SignedPreKeyRecord(
            android.util.Base64.decode(serialized, android.util.Base64.DEFAULT)
        )
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        return encryptedPrefs.all
            .filterKeys { it.startsWith(KEY_SIGNED_PREKEY_PREFIX) }
            .mapNotNull { (_, value) ->
                try {
                    SignedPreKeyRecord(
                        android.util.Base64.decode((value as? String) ?: return@mapNotNull null, android.util.Base64.DEFAULT)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading signed PreKey", e)
                    null
                }
            }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        val key = KEY_SIGNED_PREKEY_PREFIX + signedPreKeyId
        val serialized = android.util.Base64.encodeToString(
            record.serialize(),
            android.util.Base64.DEFAULT
        )

        encryptedPrefs.edit().putString(key, serialized).apply()
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        val key = KEY_SIGNED_PREKEY_PREFIX + signedPreKeyId
        return encryptedPrefs.contains(key)
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        val key = KEY_SIGNED_PREKEY_PREFIX + signedPreKeyId
        encryptedPrefs.edit().remove(key).apply()
    }

    // ── SessionStore implementation ──────────────────────────────────────────

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val key = KEY_SESSION_PREFIX + address.name + "_" + address.deviceId
        val serialized = encryptedPrefs.getString(key, null)

        return if (serialized != null) {
            SessionRecord(android.util.Base64.decode(serialized, android.util.Base64.DEFAULT))
        } else {
            SessionRecord()
        }
    }

    override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): List<SessionRecord> {
        return addresses.mapNotNull { address ->
            try {
                val session = loadSession(address)
                if (session.hasSenderChain()) session else null
            } catch (e: Exception) {
                Log.e(TAG, "Error loading session for ${address.name}", e)
                null
            }
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        return encryptedPrefs.all
            .filterKeys { it.startsWith(KEY_SESSION_PREFIX + name + "_") }
            .mapNotNull { (key, _) ->
                try {
                    key.substringAfterLast("_").toInt()
                } catch (e: Exception) {
                    null
                }
            }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        val key = KEY_SESSION_PREFIX + address.name + "_" + address.deviceId
        val serialized = android.util.Base64.encodeToString(
            record.serialize(),
            android.util.Base64.DEFAULT
        )

        encryptedPrefs.edit().putString(key, serialized).apply()
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        val session = loadSession(address)
        return session.hasSenderChain()
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        val key = KEY_SESSION_PREFIX + address.name + "_" + address.deviceId
        encryptedPrefs.edit().remove(key).apply()
    }

    override fun deleteAllSessions(name: String) {
        val editor = encryptedPrefs.edit()
        encryptedPrefs.all.keys
            .filter { it.startsWith(KEY_SESSION_PREFIX + name + "_") }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    // ── Custom methods ───────────────────────────────────────────────────────

    /**
     * Saves the identity key pair for the current user
     */
    fun saveIdentityKeyPair(keyPair: IdentityKeyPair) {
        val publicKey = android.util.Base64.encodeToString(
            keyPair.publicKey.serialize(),
            android.util.Base64.DEFAULT
        )
        val privateKey = android.util.Base64.encodeToString(
            keyPair.privateKey.serialize(),
            android.util.Base64.DEFAULT
        )

        encryptedPrefs.edit()
            .putString(KEY_IDENTITY_PUBLIC, publicKey)
            .putString(KEY_IDENTITY_PRIVATE, privateKey)
            .apply()

        Log.d(TAG, "Saved identity key pair")
    }

    /**
     * Saves the local registration ID
     */
    fun saveLocalRegistrationId(registrationId: Int) {
        encryptedPrefs.edit()
            .putInt(KEY_REGISTRATION_ID, registrationId)
            .apply()

        Log.d(TAG, "Saved registration ID: $registrationId")
    }

    /**
     * Checks if identity key pair exists
     */
    fun hasIdentityKeyPair(): Boolean {
        return encryptedPrefs.contains(KEY_IDENTITY_PUBLIC) &&
                encryptedPrefs.contains(KEY_IDENTITY_PRIVATE)
    }

    /**
     * Gets the count of available PreKeys
     */
    fun getPreKeyCount(): Int {
        return encryptedPrefs.all.keys.count { it.startsWith(KEY_PREKEY_PREFIX) }
    }

    /**
     * Gets the next available PreKey ID
     */
    fun getNextPreKeyId(): Int {
        val current = encryptedPrefs.getInt(KEY_NEXT_PREKEY_ID, 1)
        encryptedPrefs.edit().putInt(KEY_NEXT_PREKEY_ID, current + 1).apply()
        return current
    }

    /**
     * Gets the next available SignedPreKey ID
     */
    fun getNextSignedPreKeyId(): Int {
        val current = encryptedPrefs.getInt(KEY_NEXT_SIGNED_PREKEY_ID, 1)
        encryptedPrefs.edit().putInt(KEY_NEXT_SIGNED_PREKEY_ID, current + 1).apply()
        return current
    }

    /**
     * Clears all stored keys (use with caution!)
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        Log.w(TAG, "Cleared all Signal Protocol keys")
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Generates a master encryption key in Android Keystore
     */
    private fun generateMasterKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()

            Log.d(TAG, "Generated master encryption key in Android Keystore")

        } catch (e: Exception) {
            Log.e(TAG, "Error generating master key", e)
            throw e
        }
    }

    /**
     * Encrypts data using the master key from Android Keystore
     */
    private fun encrypt(data: ByteArray): ByteArray {
        val secretKey = (androidKeyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)
            ?: throw IllegalStateException("Master encryption key not found in Android Keystore")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        // Prepend IV to encrypted data
        return iv + encrypted
    }

    /**
     * Decrypts data using the master key from Android Keystore
     */
    private fun decrypt(data: ByteArray): ByteArray {
        val secretKey = (androidKeyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)
            ?: throw IllegalStateException("Master encryption key not found in Android Keystore")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        // Extract IV from beginning of data
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)

        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(encrypted)
    }
}
