package com.Azelmods.App.data.security.encryption

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Punto de entrada del cifrado de extremo a extremo de la app.
 *
 * ## Qué pasó con el Signal Protocol
 *
 * Este archivo describía una implementación del Signal Protocol completa —Double
 * Ratchet, PreKeys, secreto hacia adelante— y arrastraba dos clases de apoyo
 * (`SignalKeyStore`, 418 líneas, y `PreKeyManager`, 323) más la dependencia
 * `libsignal-android`. Nada de eso llegó a ejecutarse: el cifrado real siempre
 * lo hizo [E2EECryptoService] con ECDH P-256 + AES-256-GCM, y los métodos de
 * PreKeys devolvían `0`, `false` o `null` sin hacer nada. Se han eliminado en la
 * v6, porque un andamiaje que sólo se referencia a sí mismo confunde a quien lee
 * el código y, peor, sugiere garantías criptográficas que la app no da.
 *
 * Lo que la app ofrece de verdad, y así está escrito en Ajustes → Acerca de:
 * cifrado de extremo a extremo en chats 1:1, **sin** secreto hacia adelante y
 * **sin** cubrir los grupos.
 *
 * La clase se conserva —en vez de llamar a [E2EECryptoService] directamente—
 * porque aquí viven los tipos de resultado que usa toda la app.
 */
@Singleton
class SignalProtocolManager @Inject constructor(
    private val e2eeCryptoService: E2EECryptoService
) {

    /** Genera y publica el par de claves del usuario si aún no existe. */
    suspend fun initialize(userId: String): SignalProtocolInitResult = withContext(Dispatchers.IO) {
        if (e2eeCryptoService.ensureLocalKeys()) {
            SignalProtocolInitResult.Ready
        } else {
            SignalProtocolInitResult.Error("No se pudieron generar claves E2EE")
        }
    }

    /** Cifra [plaintext] para [recipientId]. */
    suspend fun encryptMessage(
        recipientId: String,
        plaintext: String
    ): EncryptionResult = withContext(Dispatchers.IO) {
        e2eeCryptoService.encryptFor(recipientId, plaintext)
    }

    /**
     * Descifra un payload intercambiado con [senderId].
     *
     * Ojo: [senderId] es el **otro extremo del chat**, no necesariamente quien
     * envió el mensaje. Ver [E2EECryptoService.decryptFrom].
     */
    suspend fun decryptMessage(
        senderId: String,
        ciphertext: ByteArray,
        messageType: MessageType
    ): DecryptionResult = withContext(Dispatchers.IO) {
        e2eeCryptoService.decryptFrom(senderId, ciphertext)
    }
}

/** Resultado de preparar el cifrado para la sesión actual. */
sealed class SignalProtocolInitResult {
    object Ready : SignalProtocolInitResult()
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
