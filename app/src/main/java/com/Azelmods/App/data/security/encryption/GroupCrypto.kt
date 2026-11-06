package com.Azelmods.App.data.security.encryption

import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** Mensaje de grupo ya cifrado, listo para guardar. */
data class GroupCiphertext(val keyId: String, val payload: String)

/**
 * Cifrado de extremo a extremo para grupos: AES-256-GCM con una clave por grupo.
 *
 * La clave se reparte cifrada por separado para cada miembro (con el mismo cifrado de los chats
 * de dos personas), en `groupKeys/{chatId}/{keyId}/{uid}`: el servidor nunca la ve. Si el grupo
 * cambia de miembros se crea una clave nueva, y quien salió no puede leer lo siguiente.
 */
@Singleton
class GroupCrypto @Inject constructor(
    private val e2ee: E2EECryptoService
) {
    private val db = FirebaseDatabase.getInstance()
    private val random = SecureRandom()
    private val keys = ConcurrentHashMap<String, ByteArray>()

    private fun cacheName(chatId: String, keyId: String) = "$chatId/$keyId"

    /** null si algún miembro aún no tiene claves: entonces el mensaje no se puede cifrar para todos. */
    suspend fun encrypt(chatId: String, text: String): GroupCiphertext? = runCatching {
        val (keyId, key) = currentKey(chatId) ?: return null
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(chatId.toByteArray())
        GroupCiphertext(keyId, Base64.encodeToString(iv + cipher.doFinal(text.toByteArray()), Base64.NO_WRAP))
    }.onFailure { Log.w(TAG, "Grupo $chatId: no se pudo cifrar: ${it.message}") }.getOrNull()

    suspend fun decrypt(chatId: String, keyId: String, payload: String): String? = runCatching {
        val key = fetchKey(chatId, keyId) ?: return null
        val sealed = Base64.decode(payload, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
        cipher.updateAAD(chatId.toByteArray())
        String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES))
    }.onFailure { Log.w(TAG, "Grupo $chatId: no se pudo descifrar: ${it.message}") }.getOrNull()

    private suspend fun currentKey(chatId: String): Pair<String, ByteArray>? {
        val chat = db.getReference("chats/$chatId")
        val members = chat.child("members").get().await().children.mapNotNull { it.key }
        val current = chat.child("groupKeyId").get().await().getValue(String::class.java)
        if (current != null) {
            val holders = db.getReference("groupKeys/$chatId/$current").get().await().children.mapNotNull { it.key }.toSet()
            // Solo sirve si la tienen exactamente los miembros actuales.
            if (holders == members.toSet()) fetchKey(chatId, current)?.let { return current to it }
        }
        return createKey(chatId, members)
    }

    private suspend fun createKey(chatId: String, members: List<String>): Pair<String, ByteArray>? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val key = ByteArray(KEY_BYTES).also(random::nextBytes)
        val keyId = db.getReference("groupKeys/$chatId").push().key ?: return null
        val encoded = Base64.encodeToString(key, Base64.NO_WRAP)
        val copies = mutableMapOf<String, Any>()
        for (member in members) {
            val sealed = e2ee.encryptFor(member, encoded) as? EncryptionResult.Success ?: return null
            copies[member] = mapOf("from" to uid, "key" to Base64.encodeToString(sealed.ciphertext, Base64.NO_WRAP))
        }
        db.reference.updateChildren(
            mapOf("groupKeys/$chatId/$keyId" to copies, "chats/$chatId/groupKeyId" to keyId)
        ).await()
        keys[cacheName(chatId, keyId)] = key
        return keyId to key
    }

    private suspend fun fetchKey(chatId: String, keyId: String): ByteArray? {
        keys[cacheName(chatId, keyId)]?.let { return it }
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val copy = db.getReference("groupKeys/$chatId/$keyId/$uid").get().await()
        val from = copy.child("from").getValue(String::class.java) ?: return null
        val sealed = copy.child("key").getValue(String::class.java) ?: return null
        val opened = e2ee.decryptFrom(from, Base64.decode(sealed, Base64.NO_WRAP)) as? DecryptionResult.Success ?: return null
        return Base64.decode(opened.plaintext, Base64.NO_WRAP).also { keys[cacheName(chatId, keyId)] = it }
    }

    private companion object {
        const val TAG = "GroupCrypto"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val KEY_BYTES = 32
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
