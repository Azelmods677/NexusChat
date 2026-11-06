package com.Azelmods.App.data.security.secret

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado de los chats secretos.
 *
 * - Cada chat acuerda su propio secreto con claves efímeras (ECDH P-256) que solo existen en los
 *   dos teléfonos, y la privada se destruye en cuanto se ha usado.
 * - Cada mensaje usa una clave distinta, derivada de una cadena que avanza y olvida la anterior
 *   (HMAC-SHA256): quien consiga el estado actual no puede leer los mensajes pasados.
 */
object SecretRatchet {
    private const val AES_KEY_BYTES = 32
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private val MESSAGE_KEY_LABEL = byteArrayOf(1)
    private val NEXT_CHAIN_LABEL = byteArrayOf(2)
    private val random = SecureRandom()

    fun newKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    fun sharedRoot(privateKey: ByteArray, peerPublicKey: ByteArray, chatId: String): ByteArray {
        val factory = KeyFactory.getInstance("EC")
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(factory.generatePrivate(PKCS8EncodedKeySpec(privateKey)))
        agreement.doPhase(factory.generatePublic(X509EncodedKeySpec(peerPublicKey)), true)
        // HKDF: extrae del secreto bruto una clave uniforme, ligada a este chat.
        val extracted = hmac(chatId.toByteArray(), agreement.generateSecret())
        return hmac(extracted, "nexus-secret-root".toByteArray() + byteArrayOf(1))
    }

    /** Cada participante cifra con su propia cadena, así nunca se reutiliza una clave. */
    fun chainFor(root: ByteArray, senderUid: String): ByteArray = hmac(root, "chain:$senderUid".toByteArray())

    data class Step(val messageKey: ByteArray, val nextChain: ByteArray)

    fun advance(chain: ByteArray): Step = Step(hmac(chain, MESSAGE_KEY_LABEL), hmac(chain, NEXT_CHAIN_LABEL))

    /** Devuelve IV + texto cifrado. [context] liga el mensaje a su chat, remitente y número. */
    fun seal(messageKey: ByteArray, plaintext: String, context: String): ByteArray {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(messageKey.copyOf(AES_KEY_BYTES), "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(context.toByteArray())
        return iv + cipher.doFinal(plaintext.toByteArray())
    }

    /** Lanza una excepción si el mensaje se alteró o la clave no es la suya. */
    fun open(messageKey: ByteArray, sealed: ByteArray, context: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(messageKey.copyOf(AES_KEY_BYTES), "AES"),
            GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES)
        )
        cipher.updateAAD(context.toByteArray())
        return String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES))
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
