package com.Azelmods.App.data.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.Azelmods.App.BuildConfig
import java.security.MessageDigest

/**
 * IntegrityGuard — firma de autoría y anti-manipulación de NexusChat (Azel Mods).
 *
 * ## Qué hace
 *
 * Comprueba, en el arranque, que el APK esté firmado con el certificado del autor.
 * Si alguien recompila la app, la reempaqueta o la vuelve a firmar con OTRA clave
 * (que es lo que hay que hacer para redistribuirla como propia), la firma cambia y,
 * en un build de RELEASE, la app se autodestruye con un fallo. Es una marca de
 * propiedad que viaja dentro del binario: dice "esto es de Azel Mods" de una forma
 * que no se puede borrar simplemente cambiando un texto en pantalla.
 *
 * ## La "criptografía oculta"
 *
 * La huella del certificado NO se guarda en claro. Se guarda **el SHA-256 de la
 * huella** ([DEBUG_MARK] / [RELEASE_MARK]). En tiempo de ejecución se calcula el
 * mismo hash sobre la firma real y se comparan los hashes. Así, ni buscando la
 * huella en el APK aparece: solo hay un hash irreversible.
 *
 * ## Por qué NO puede romper el trabajo del desarrollador
 *
 * La autodestrucción es deliberadamente conservadora:
 *  - En **debug** NUNCA se dispara (el autor compila y depura sin miedo).
 *  - En **release**, si [RELEASE_MARK] aún está vacío, tampoco se dispara: falla
 *    "en abierto" hasta que el autor fija su huella de release (ver README →
 *    Integridad). Un build de release sin configurar avisa por log, no crashea.
 *  - Solo se autodestruye un **release firmado por una clave desconocida** cuando el
 *    autor YA fijó su huella. Ese caso es, por definición, un reempaquetado ajeno.
 */
object IntegrityGuard {

    private const val TAG = "IntegrityGuard"

    /**
     * SHA-256 de la huella SHA-256 del certificado DEBUG del autor.
     * (Sirve para que los builds de depuración se reconozcan como propios.)
     */
    private const val DEBUG_MARK = "F697CE175486D691DA21BD570EC914FF8B16EBFE57F6929A49B8BD61DE8332C3"

    /**
     * SHA-256 de la huella SHA-256 del certificado de RELEASE del autor.
     *
     * Se deja vacío a propósito: rellénalo con el valor que imprime el comando del
     * README (sección Integridad) tras firmar con tu keystore real. Mientras esté
     * vacío, el anti-tamper NO actúa en release (fail-open), para no arriesgar un
     * "brick" del propio release antes de configurarlo.
     */
    private const val RELEASE_MARK = ""

    /**
     * Marca de propiedad, NUNCA en texto plano.
     *
     * El nombre del autor no está escrito en ningún sitio del binario: aquí solo vive
     * ofuscado (XOR con [OWNER_KEY]) y verificado contra su hash ([OWNER_HASH]).
     * Buscar "Azel Mods" con `strings` sobre el APK no devuelve nada — solo bytes que
     * no significan nada sin la clave. Se reconstruye en memoria bajo demanda.
     *
     * Por qué así: cambiar el crédito en pantalla ya no basta. Para quitar el nombre
     * hay que dar con el blob, con la clave y con el hash a la vez y recomputar los
     * tres de forma coherente; si se cambia uno sin los otros, [verify] lo detecta y,
     * en release, la app se autodestruye. Es un disuasor fuerte, no un candado
     * perfecto (nada compilado lo es): sube el coste de plagiar por encima del de
     * escribir la app desde cero.
     */
    private val OWNER_KEY = byteArrayOf(
        0x5A, 0x37, 0xC1.toByte(), 0x8E.toByte(), 0x2D, 0x71, 0xB4.toByte(), 0x0F
    )

    /** "Azel Mods" cifrado por XOR con [OWNER_KEY]. Sin la clave, no dice nada. */
    private val OWNER_BLOB = byteArrayOf(
        0x1B, 0x4D, 0xA4.toByte(), 0xE2.toByte(), 0x0D, 0x3C, 0xDB.toByte(), 0x6B, 0x29
    )

    /** SHA-256 del nombre correcto. El ancla que hace que patchear el blob no cuele. */
    private const val OWNER_HASH = "99C5DA246DB3DD3C99F9C95B29A95C1D0BFE43552976E72294D179A5BD78E239"

    /** Reconstruye el nombre del autor en memoria a partir del blob ofuscado. */
    fun owner(): String {
        val out = ByteArray(OWNER_BLOB.size)
        for (i in OWNER_BLOB.indices) {
            out[i] = (OWNER_BLOB[i].toInt() xor OWNER_KEY[i % OWNER_KEY.size].toInt()).toByte()
        }
        return String(out, Charsets.US_ASCII)
    }

    /** `true` si el nombre reconstruido casa con su hash: nadie tocó el blob ni la clave. */
    private fun ownershipIntact(): Boolean =
        sha256Hex(owner().toByteArray(Charsets.US_ASCII)).equals(OWNER_HASH, ignoreCase = true)

    /**
     * Punto de entrada. Verifica DOS cosas —la firma del APK y la marca de autoría— y,
     * si alguna no cuadra en un release configurado, programa la autodestrucción.
     * Cualquier error de lectura se traga: un fallo del guard nunca debe tumbar la app
     * por sí mismo (solo lo hace una manipulación real en release).
     */
    fun verify(context: Context) {
        try {
            // 1) La marca de autoría: barata y siempre disponible. Si el blob o la clave
            //    fueron alterados para escribir otro nombre, el hash deja de casar.
            val ownershipOk = ownershipIntact()

            // 2) La firma del certificado: quién empaquetó realmente este APK.
            val actualMarks = signingFingerprints(context).map { mark(it) }
            val trusted = buildList {
                add(DEBUG_MARK)
                if (RELEASE_MARK.isNotBlank()) add(RELEASE_MARK)
            }
            val signatureOk = actualMarks.any { it in trusted }

            if (ownershipOk && signatureOk) return // build íntegro del autor: todo en orden

            // Salvaguardas: nunca romper depuración ni un release aún sin configurar.
            if (BuildConfig.DEBUG || RELEASE_MARK.isBlank()) {
                if (!ownershipOk) android.util.Log.w(TAG, "Marca de autoría alterada (aviso, sin acción en este build).")
                if (!signatureOk) android.util.Log.w(TAG, "Firma no reconocida (aviso, sin acción en este build).")
                return
            }

            // Release configurado + (firma ajena O marca de autoría manipulada) → plagio.
            selfDestruct()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "No se pudo verificar la integridad: ${e.message}")
        }
    }

    /** Huellas SHA-256 (hex, mayúsculas) de los certificados con los que se firmó el APK. */
    private fun signingFingerprints(context: Context): List<String> {
        val pm = context.packageManager
        val pkg = context.packageName

        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo
            when {
                signingInfo == null -> emptyArray()
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                else -> signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION", "PackageManagerGetSignatures")
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures ?: emptyArray()
        }

        return signatures.map { sig -> sha256Hex(sig.toByteArray()) }
    }

    /** Hash de la huella: lo que realmente se guarda y compara (la huella nunca en claro). */
    private fun mark(fingerprint: String): String =
        sha256Hex(fingerprint.toByteArray(Charsets.UTF_8))

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02X".format(it) }

    /**
     * Autodestrucción: un fallo fatal diferido. Se lanza desde el bucle principal con
     * un retardo para que el crash no apunte de inmediato a esta comprobación.
     */
    private fun selfDestruct() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Excepción no capturada → el manejador global reenvía al del sistema → crash.
            throw IllegalStateException("Integrity verification failed (E-0x41A2)")
        }, 1500L)
    }
}
