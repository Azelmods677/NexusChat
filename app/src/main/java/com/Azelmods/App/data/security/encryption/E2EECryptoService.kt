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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 *
 * ## Por qué las claves se guardan POR UID
 *
 * La versión anterior guardaba un único par de claves bajo claves fijas
 * (`identity_private_pkcs8`), compartidas por todas las cuentas que hubieran
 * iniciado sesión en el dispositivo. Con dos cuentas —algo normal al probar la
 * app— la segunda reutilizaba la clave privada de la primera mientras Firebase
 * seguía publicando bajo su uid una clave pública que ya no le correspondía. El
 * secreto ECDH que derivaba el otro extremo no coincidía, la etiqueta GCM
 * fallaba y todos los mensajes se leían como "No se pudo descifrar".
 *
 * Ahora cada uid tiene su propio par y su propia entrada de caché, así que
 * cambiar de cuenta —o cerrar y volver a abrir sesión— no puede envenenar el
 * material criptográfico de otra.
 *
 * ## Por qué se reconcilia la clave pública en cada arranque
 *
 * `ensureLocalKeys()` salía antes con `true` en cuanto existía la clave privada
 * local, sin comprobar que la pública estuviera realmente publicada. Si la
 * primera subida falló (sin red, reglas denegando, cierre de la app a medias),
 * quedaba una cuenta con clave privada pero sin clave pública en el servidor:
 * nadie podía escribirle nunca y el error tampoco se reintentaba jamás. La
 * reconciliación es una única lectura por sesión y cierra ese agujero.
 *
 * El formato en la base de datos NO cambia (iv de 12 bytes ‖ ciphertext+tag),
 * de modo que los mensajes ya enviados se siguen descifrando igual.
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
        private const val KEY_PRIVATE_PREFIX = "identity_private_pkcs8_"
        private const val KEY_PUBLIC_PREFIX = "identity_public_x509_"
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8   // 16
        private const val IV_BYTES = 12
        private const val EC_ALGORITHM = "EC"

        /** Payload mínimo válido: iv + etiqueta GCM + al menos 1 byte de texto. */
        private const val MIN_PAYLOAD_BYTES = IV_BYTES + GCM_TAG_BYTES + 1
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

    /**
     * Secretos ECDH ya derivados. La clave del mapa incluye el uid local a
     * propósito: si no lo incluyera, al cambiar de cuenta se reutilizaría el
     * secreto de la cuenta anterior para el mismo interlocutor.
     */
    private val sharedKeyCache = mutableMapOf<String, ByteArray>()

    /** Serializa la generación de claves: dos envíos simultáneos generaban dos pares. */
    private val keyMutex = Mutex()

    /** uids cuya clave pública ya se reconcilió con el servidor en esta sesión. */
    private val reconciledUids = mutableSetOf<String>()

    /** Último uid observado, para detectar el cambio de cuenta. */
    @Volatile
    private var lastKnownUid: String? = auth.currentUser?.uid

    init {
        // Se engancha al propio FirebaseAuth en lugar de pedirle a cada pantalla
        // de cierre de sesión que se acuerde de limpiar: cualquier camino que
        // cambie de cuenta (logout manual, token revocado, borrado de cuenta)
        // pasa por aquí y deja el material criptográfico de la cuenta anterior
        // fuera de juego.
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != lastKnownUid) {
                lastKnownUid = uid
                clearSessionCache()
                Log.d(TAG, "Cambio de cuenta detectado: caché E2EE invalidada")
            }
        }
    }

    private fun privateKeyPref(uid: String) = "$KEY_PRIVATE_PREFIX$uid"
    private fun publicKeyPref(uid: String) = "$KEY_PUBLIC_PREFIX$uid"

    suspend fun ensureLocalKeys(): Boolean = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext false
        keyMutex.withLock {
            try {
                val storedPublic = prefs.getString(publicKeyPref(userId), null)
                val hasLocal = prefs.contains(privateKeyPref(userId)) && !storedPublic.isNullOrEmpty()

                val publicB64 = if (hasLocal) {
                    storedPublic!!
                } else {
                    val keyPair = KeyPairGenerator.getInstance(EC_ALGORITHM).apply {
                        initialize(256)
                    }.generateKeyPair()
                    val pub = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
                    prefs.edit()
                        .putString(publicKeyPref(userId), pub)
                        .putString(
                            privateKeyPref(userId),
                            Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
                        )
                        .apply()
                    Log.d(TAG, "Par de claves E2EE generado para $userId")
                    pub
                }

                // Reconciliación: una sola vez por sesión y por cuenta.
                if (!hasLocal || !reconciledUids.contains(userId)) {
                    publishPublicKeyIfNeeded(userId, publicB64)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "ensureLocalKeys falló", e)
                false
            }
        }
    }

    /**
     * Sube la clave pública sólo si el servidor no tiene ya exactamente esa.
     * Marca el uid como reconciliado únicamente cuando la escritura (o la
     * comprobación) termina bien, para que un fallo de red se reintente en el
     * siguiente mensaje en lugar de quedarse colgado para siempre.
     */
    private suspend fun publishPublicKeyIfNeeded(userId: String, publicB64: String) {
        try {
            val remote = database.reference.child("users").child(userId)
                .child("keys").child("identityPublic").get().await()
                .getValue(String::class.java)

            if (remote == publicB64) {
                reconciledUids.add(userId)
                return
            }

            database.reference.child("users").child(userId).child("keys")
                .updateChildren(
                    mapOf(
                        "identityPublic" to publicB64,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            reconciledUids.add(userId)
            Log.d(TAG, "Clave pública publicada para $userId")
        } catch (e: Exception) {
            // No se marca como reconciliado: se reintentará.
            Log.e(TAG, "No se pudo publicar la clave pública de $userId", e)
        }
    }

    fun hasLocalKeys(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return prefs.contains(privateKeyPref(uid))
    }

    /**
     * Olvida los secretos derivados. Debe llamarse al cerrar sesión: si no, la
     * siguiente cuenta que entre en el mismo proceso seguiría usando los
     * secretos de la anterior.
     */
    fun clearSessionCache() {
        synchronized(sharedKeyCache) { sharedKeyCache.clear() }
        reconciledUids.clear()
    }

    /**
     * Invalida el secreto con un interlocutor concreto. Se usa cuando un
     * descifrado falla: si el otro extremo rotó su clave (reinstaló la app), el
     * secreto en caché quedó obsoleto y sin esto no se recuperaría nunca sin
     * matar el proceso.
     */
    private fun invalidatePeer(localUid: String, peerId: String) {
        synchronized(sharedKeyCache) { sharedKeyCache.remove("$localUid|$peerId") }
    }

    private fun getLocalKeyPair(userId: String): KeyPair {
        val pubStr = prefs.getString(publicKeyPref(userId), "") ?: ""
        val privStr = prefs.getString(privateKeyPref(userId), "") ?: ""
        if (pubStr.isEmpty() || privStr.isEmpty()) {
            throw IllegalStateException("Par de claves E2EE sin inicializar")
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

    private suspend fun sharedAesKey(localUid: String, peerId: String): ByteArray? {
        val cacheKey = "$localUid|$peerId"
        synchronized(sharedKeyCache) { sharedKeyCache[cacheKey] }?.let { return it }

        val remoteBytes = fetchRemotePublicKeyBytes(peerId) ?: return null
        val local = getLocalKeyPair(localUid)
        val kf = KeyFactory.getInstance(EC_ALGORITHM)
        val remotePublic = kf.generatePublic(X509EncodedKeySpec(remoteBytes))
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(local.private)
        agreement.doPhase(remotePublic, true)
        val key = MessageDigest.getInstance("SHA-256").digest(agreement.generateSecret())
        synchronized(sharedKeyCache) { sharedKeyCache[cacheKey] = key }
        return key
    }

    suspend fun encryptFor(peerId: String, plaintext: String): EncryptionResult = withContext(Dispatchers.IO) {
        try {
            if (!ensureLocalKeys()) {
                return@withContext EncryptionResult.Error("Claves locales no disponibles")
            }
            val localUid = auth.currentUser?.uid
                ?: return@withContext EncryptionResult.Error("Sesión no iniciada")
            val aesKey = sharedAesKey(localUid, peerId)
                ?: return@withContext EncryptionResult.Error("No existe clave pública del destinatario")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"))
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            EncryptionResult.Success(iv + encrypted, MessageType.WHISPER)
        } catch (e: Exception) {
            Log.e(TAG, "encryptFor falló", e)
            EncryptionResult.Error(e.message ?: "Error de cifrado")
        }
    }

    /**
     * Descifra un payload intercambiado con [peerId].
     *
     * [peerId] es **el otro extremo de la conversación**, no "quien envió el
     * mensaje": el secreto ECDH es simétrico, así que el mismo par de claves
     * descifra tanto lo que llega como lo que uno mismo escribió. Gracias a eso
     * el emisor puede releer sus propios mensajes en lugar de ver el marcador
     * "🔒 Mensaje cifrado de extremo a extremo" que se guarda en el servidor.
     */
    suspend fun decryptFrom(peerId: String, ciphertext: ByteArray): DecryptionResult = withContext(Dispatchers.IO) {
        val localUid = auth.currentUser?.uid
            ?: return@withContext DecryptionResult.Error("Sesión no iniciada")
        if (ciphertext.size < MIN_PAYLOAD_BYTES) {
            return@withContext DecryptionResult.Error("Payload inválido")
        }

        // Primer intento con lo que haya en caché; si falla, se descarta el
        // secreto y se vuelve a derivar desde la clave pública actual del otro
        // extremo (el caso de "reinstaló la app y rotó su clave").
        decryptOnce(localUid, peerId, ciphertext)?.let { return@withContext it }

        invalidatePeer(localUid, peerId)
        decryptOnce(localUid, peerId, ciphertext)
            ?: DecryptionResult.Error("No hay clave compartida con este contacto")
    }

    /**
     * @return el resultado, o `null` si conviene reintentar con el secreto
     *   renovado (clave ausente o etiqueta GCM inválida).
     */
    private suspend fun decryptOnce(
        localUid: String,
        peerId: String,
        ciphertext: ByteArray
    ): DecryptionResult? {
        return try {
            val aesKey = sharedAesKey(localUid, peerId) ?: return null
            val iv = ciphertext.copyOfRange(0, IV_BYTES)
            val data = ciphertext.copyOfRange(IV_BYTES, ciphertext.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            DecryptionResult.Success(String(cipher.doFinal(data), Charsets.UTF_8))
        } catch (e: javax.crypto.AEADBadTagException) {
            Log.w(TAG, "Etiqueta GCM inválida con $peerId; se renovará el secreto")
            null
        } catch (e: Exception) {
            Log.e(TAG, "decryptFrom falló", e)
            DecryptionResult.Error(e.message ?: "Error de descifrado")
        }
    }
}
