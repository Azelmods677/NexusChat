@file:Suppress("DEPRECATION")
package com.Azelmods.App.data.security.encryption

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import javax.crypto.KeyAgreement
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("DEPRECATION")
/**
 * E2EE: ECDH (curva elíptica del sistema) + AES-256-GCM.
 * Claves públicas en Firebase: users/{uid}/keys/identityPublic
 */
@Singleton
class E2EECryptoService @Inject constructor(
    private val context: Context,
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "E2EECrypto"
        private const val PREFS = "e2ee_crypto_prefs"
        private const val KEY_PRIVATE = "identity_private_pkcs8"
        private const val KEY_PUBLIC = "identity_public_x509"
        private const val GCM_TAG_BITS = 128
        private const val EC_ALGORITHM = "EC"
    }

    @Suppress("DEPRECATION")
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val sharedKeyCache = mutableMapOf<String, ByteArray>()

    suspend fun ensureLocalKeys(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (prefs.contains(KEY_PRIVATE)) return@withContext true
            val userId = auth.currentUser?.uid ?: return@withContext false
            val keyPair = KeyPairGenerator.getInstance(EC_ALGORITHM).apply {
                initialize(256)
            }.generateKeyPair()
            prefs.edit()
                .putString(KEY_PUBLIC, Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP))
                .putString(KEY_PRIVATE, Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP))
                .apply()
            database.reference.child("users").child(userId).child("keys")
                .updateChildren(
                    mapOf(
                        "identityPublic" to Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP),
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Log.d(TAG, "E2EE keys generated for $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "ensureLocalKeys failed", e)
            false
        }
    }

    fun hasLocalKeys(): Boolean = prefs.contains(KEY_PRIVATE)

    private fun getLocalKeyPair(): KeyPair {
        val pubStr = prefs.getString(KEY_PUBLIC, "") ?: ""
        val privStr = prefs.getString(KEY_PRIVATE, "") ?: ""
        if (pubStr.isEmpty() || privStr.isEmpty()) {
            throw IllegalStateException("E2EE key pair not initialized")
        }
        val pub = Base64.decode(pubStr, Base64.NO_WRAP)
        val priv = Base64.decode(privStr, Base64.NO_WRAP)
        val kf = KeyFactory.getInstance(EC_ALGORITHM)
        return KeyPair(
            kf.generatePublic(X509EncodedKeySpec(pub)),
            kf.generatePrivate(PKCS8EncodedKeySpec(priv))
        )
    }

    private suspend fun fetchRemotePublicKeyBytes(userId: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val snap = database.reference.child("users").child(userId)
                .child("keys").child("identityPublic").get().await()
            val b64 = snap.getValue(String::class.java) ?: return@withContext null
            Base64.decode(b64, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "fetchRemotePublicKey $userId", e)
            null
        }
    }

    private suspend fun sharedAesKey(peerId: String): ByteArray? {
        sharedKeyCache[peerId]?.let { return it }
        ensureLocalKeys()
        val remoteBytes = fetchRemotePublicKeyBytes(peerId) ?: return null
        val local = getLocalKeyPair()
        val kf = KeyFactory.getInstance(EC_ALGORITHM)
        val remotePublic = kf.generatePublic(X509EncodedKeySpec(remoteBytes))
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(local.private)
        agreement.doPhase(remotePublic, true)
        val key = MessageDigest.getInstance("SHA-256").digest(agreement.generateSecret())
        sharedKeyCache[peerId] = key
        return key
    }

    suspend fun encryptFor(peerId: String, plaintext: String): EncryptionResult = withContext(Dispatchers.IO) {
        try {
            ensureLocalKeys()
            val aesKey = sharedAesKey(peerId)
                ?: return@withContext EncryptionResult.Error("No existe clave pública del destinatario")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"))
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            EncryptionResult.Success(iv + encrypted, MessageType.WHISPER)
        } catch (e: Exception) {
            Log.e(TAG, "encryptFor failed", e)
            EncryptionResult.Error(e.message ?: "Error de cifrado")
        }
    }

    suspend fun decryptFrom(peerId: String, ciphertext: ByteArray): DecryptionResult = withContext(Dispatchers.IO) {
        try {
            val aesKey = sharedAesKey(peerId)
                ?: return@withContext DecryptionResult.Error("Sin clave compartida")
            if (ciphertext.size < 13) return@withContext DecryptionResult.Error("Payload inválido")
            val iv = ciphertext.copyOfRange(0, 12)
            val data = ciphertext.copyOfRange(12, ciphertext.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            val plain = cipher.doFinal(data)
            DecryptionResult.Success(String(plain, Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "decryptFrom failed", e)
            DecryptionResult.Error(e.message ?: "Error de descifrado")
        }
    }
}
