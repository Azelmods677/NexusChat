package com.Azelmods.App.data.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * TOTP (RFC 6238) — segundo factor compatible con Google Authenticator, Aegis, etc.
 *
 * Es criptografía estándar, sin dependencias ni servidor: el secreto se genera y se
 * guarda en el dispositivo, y el código de 6 dígitos se deriva del reloj. La misma
 * semilla en la app de autenticación del usuario produce el mismo código, así que
 * cualquier app TOTP sirve como segundo factor. Por eso el 2FA de NexusChat deja de
 * ser un adorno: hay un secreto real que verificar.
 *
 * - HMAC-SHA1 (el algoritmo por defecto de las apps de autenticación).
 * - Ventana de 30 s.
 * - 6 dígitos.
 * - Verificación con tolerancia de ±1 ventana para absorber el desfase de reloj.
 */
object TotpAuthenticator {

    private const val TIME_STEP_SECONDS = 30L
    private const val DIGITS = 6
    private const val ALGORITHM = "HmacSHA1"

    /** Genera un secreto nuevo en Base32 (160 bits), el formato que esperan las apps TOTP. */
    fun generateSecret(): String {
        val bytes = ByteArray(20)
        SecureRandom().nextBytes(bytes)
        return Base32.encode(bytes)
    }

    /** Código actual para [secretBase32]. Cadena de 6 dígitos con ceros a la izquierda. */
    fun currentCode(secretBase32: String, atMillis: Long = System.currentTimeMillis()): String {
        val counter = atMillis / 1000L / TIME_STEP_SECONDS
        return codeForCounter(secretBase32, counter)
    }

    /**
     * `true` si [code] es válido ahora mismo, admitiendo la ventana anterior y la
     * siguiente (±30 s) para que un reloj ligeramente desajustado no bloquee al usuario.
     */
    fun verify(secretBase32: String, code: String, atMillis: Long = System.currentTimeMillis()): Boolean {
        val cleaned = code.filter { it.isDigit() }
        if (cleaned.length != DIGITS) return false
        val counter = atMillis / 1000L / TIME_STEP_SECONDS
        for (offset in -1..1) {
            if (constantTimeEquals(codeForCounter(secretBase32, counter + offset), cleaned)) return true
        }
        return false
    }

    /** Segundos que le quedan de vida al código actual (para la barra de cuenta atrás). */
    fun secondsRemaining(atMillis: Long = System.currentTimeMillis()): Int =
        (TIME_STEP_SECONDS - (atMillis / 1000L % TIME_STEP_SECONDS)).toInt()

    /**
     * URI `otpauth://` que las apps de autenticación leen desde un QR o al pegarla.
     * [account] suele ser el usuario/teléfono; [issuer] es el nombre de la app.
     */
    fun provisioningUri(secretBase32: String, account: String, issuer: String = "NexusChat"): String {
        val label = java.net.URLEncoder.encode("$issuer:$account", "UTF-8")
        val iss = java.net.URLEncoder.encode(issuer, "UTF-8")
        return "otpauth://totp/$label?secret=$secretBase32&issuer=$iss&algorithm=SHA1&digits=$DIGITS&period=$TIME_STEP_SECONDS"
    }

    private fun codeForCounter(secretBase32: String, counter: Long): String {
        val key = Base32.decode(secretBase32)
        val msg = ByteBuffer.allocate(8).putLong(counter).array()
        val hmac = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(key, ALGORITHM)) }.doFinal(msg)
        // Truncamiento dinámico (RFC 4226 §5.3).
        val offset = hmac[hmac.size - 1].toInt() and 0x0f
        val binary = ((hmac[offset].toInt() and 0x7f) shl 24) or
            ((hmac[offset + 1].toInt() and 0xff) shl 16) or
            ((hmac[offset + 2].toInt() and 0xff) shl 8) or
            (hmac[offset + 3].toInt() and 0xff)
        val otp = binary % 1_000_000
        return otp.toString().padStart(DIGITS, '0')
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}

/** Base32 (RFC 4648) sin relleno, en mayúsculas: el formato de las semillas TOTP. */
private object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1f
                sb.append(ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1f
            sb.append(ALPHABET[index])
        }
        return sb.toString()
    }

    fun decode(encoded: String): ByteArray {
        val clean = encoded.trim().uppercase().replace("=", "").replace(" ", "")
        if (clean.isEmpty()) return ByteArray(0)
        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bitsLeft = 0
        for (c in clean) {
            val index = ALPHABET.indexOf(c)
            if (index < 0) continue // ignora caracteres no Base32
            buffer = (buffer shl 5) or index
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.write((buffer shr (bitsLeft - 8)) and 0xff)
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }
}
