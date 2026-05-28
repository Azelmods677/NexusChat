package com.Azelmods.App.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.Azelmods.App.BuildConfig
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TamperDetection — runtime integrity guard for Azelgram.
 *
 * Checks performed:
 *  1. APK signature (SHA-256) against the expected release certificate hash.
 *  2. Debugger attached (Debug.isDebuggerConnected / waitingForDebugger).
 *  3. Emulator environment via hardware/system-property heuristics.
 *
 * Policy (intentionally non-fatal):
 *  - In DEBUG builds all checks pass transparently so development is never blocked.
 *  - In RELEASE builds failures are logged as warnings; the caller decides the response.
 *    Forcing a hard crash here would make the app fragile against false-positives on
 *    legitimate custom ROMs. Prefer server-side enforcement for critical actions.
 *
 * Usage:
 *  val result = tamperDetection.checkIntegrity()
 *  if (!result.isIntact) tamperDetection.logWarnings(result)
 */
@Singleton
class TamperDetection @Inject constructor(
    private val context: Context
) {

    // ── Public API ────────────────────────────────────────────────────────────

    data class TamperResult(
        val isIntact: Boolean,
        val issues: List<String>
    )

    /**
     * Runs all integrity checks and returns a consolidated [TamperResult].
     * Automatically calls [logWarnings] when issues are found.
     */
    fun checkIntegrity(): TamperResult {
        val issues = mutableListOf<String>()

        if (!verifySignature()) {
            issues += "Signature mismatch — APK certificate does not match the expected hash."
        }
        if (detectDebugger()) {
            issues += "Debugger detected — android.os.Debug reports an attached debugger."
        }
        if (detectEmulator()) {
            issues += "Emulator environment detected — hardware/system properties indicate a virtual device."
        }

        val result = TamperResult(isIntact = issues.isEmpty(), issues = issues.toList())
        logWarnings(result)
        return result
    }

    // ── Individual Checks ─────────────────────────────────────────────────────

    /**
     * Verifies the APK signing certificate against [EXPECTED_CERT_HASH].
     *
     * Always returns `true` in DEBUG builds so development keystore differences
     * never block the developer workflow.
     *
     * To obtain the hash for production:
     *   keytool -printcert -jarfile your-release.apk | grep SHA-256
     * or via ADB on a installed release build:
     *   adb shell pm get-app-signing-info --apk com.Azelmods.App
     */
    fun verifySignature(): Boolean {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "verifySignature: skipped in DEBUG build")
            return true
        }

        return try {
            val signatures = getSignatureBytes()
            if (signatures.isNullOrEmpty()) {
                Log.w(TAG, "verifySignature: no signatures found")
                return false
            }

            val digest = MessageDigest.getInstance("SHA-256")
            signatures.any { sigBytes ->
                val hash = digest.apply { reset() }
                    .digest(sigBytes)
                    .joinToString("") { byte -> "%02x".format(byte) }

                Log.d(TAG, "verifySignature: computed hash=$hash")
                hash.equals(EXPECTED_CERT_HASH, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifySignature: exception during check", e)
            // Fail-open on unexpected errors to avoid bricking the app; log for monitoring.
            false
        }
    }

    /**
     * Returns `true` when a debugger is actively connected or the VM is
     * waiting for one to attach.
     */
    fun detectDebugger(): Boolean {
        val connected = android.os.Debug.isDebuggerConnected()
        val waiting   = android.os.Debug.waitingForDebugger()
        if (connected || waiting) {
            Log.w(TAG, "detectDebugger: connected=$connected, waiting=$waiting")
        }
        return connected || waiting
    }

    /**
     * Returns `true` when the runtime environment shows strong signals of
     * being an emulator or virtual device.
     *
     * Always returns `false` in DEBUG builds — emulators are the primary
     * development target and should not be treated as hostile.
     */
    fun detectEmulator(): Boolean {
        if (BuildConfig.DEBUG) return false

        val checks = listOf(
            Build.FINGERPRINT.startsWith("generic")                              to "FINGERPRINT starts with 'generic'",
            Build.FINGERPRINT.startsWith("unknown")                              to "FINGERPRINT starts with 'unknown'",
            Build.FINGERPRINT.contains(":eng/")                                  to "FINGERPRINT contains ':eng/'",
            Build.MODEL.contains("Emulator", ignoreCase = true)                 to "MODEL contains 'Emulator'",
            Build.MODEL.contains("Android SDK built for x86", ignoreCase = true) to "MODEL is SDK emulator",
            Build.MANUFACTURER.equals("Genymotion", ignoreCase = true)          to "MANUFACTURER is Genymotion",
            Build.BRAND.startsWith("generic", ignoreCase = true)                to "BRAND starts with 'generic'",
            Build.DEVICE.startsWith("generic", ignoreCase = true)               to "DEVICE starts with 'generic'",
            Build.PRODUCT.startsWith("sdk", ignoreCase = true)                  to "PRODUCT starts with 'sdk'",
            Build.HARDWARE.equals("goldfish", ignoreCase = true)                to "HARDWARE is goldfish",
            Build.HARDWARE.equals("ranchu", ignoreCase = true)                  to "HARDWARE is ranchu",
            (System.getProperty("ro.kernel.qemu") == "1")                       to "ro.kernel.qemu=1"
        )

        val triggered = checks.filter { it.first }
        triggered.forEach { Log.w(TAG, "detectEmulator: ${it.second}") }
        return triggered.isNotEmpty()
    }

    /**
     * Logs all issues found in [result] as warnings.
     * No-ops when [TamperResult.isIntact] is `true`.
     */
    fun logWarnings(result: TamperResult) {
        if (result.isIntact) return

        Log.w(TAG, "╔══════════════════════════════════════╗")
        Log.w(TAG, "║   INTEGRITY CHECK FAILED — AZELGRAM  ║")
        Log.w(TAG, "╠══════════════════════════════════════╣")
        result.issues.forEachIndexed { index, issue ->
            Log.w(TAG, "║  ${index + 1}. $issue")
        }
        Log.w(TAG, "╚══════════════════════════════════════╝")
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Returns the raw byte arrays for every signing certificate present in the APK.
     * Uses the modern [PackageManager.GET_SIGNING_CERTIFICATES] API on API 28+,
     * and the legacy [PackageManager.GET_SIGNATURES] on older platforms.
     */
    private fun getSignatureBytes(): Array<out ByteArray>? {
        // minSdk 31 = Android 12, GET_SIGNING_CERTIFICATES disponible en todas las versiones soportadas
        return try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            // apkContentsSigners reflects the actual signers of the APK (not the history).
            info.signingInfo?.apkContentsSigners?.map { it.toByteArray() }?.toTypedArray()
        } catch (e: Exception) {
            Log.e(TAG, "getSignatureBytes failed", e)
            null
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "TamperDetection"

        /**
         * Expected SHA-256 fingerprint of the release signing certificate (hex, lowercase).
         *
         * ⚠️  Replace this placeholder before publishing to production.
         *
         * How to get the real value:
         *   1. Build a signed release APK.
         *   2. Run: keytool -printcert -jarfile app-release.apk | grep "SHA256"
         *   3. Copy the hex string (without colons) and paste it here.
         *
         * Example (do NOT use this):
         *   "a1b2c3d4e5f6...64hexchars"
         */
        const val EXPECTED_CERT_HASH: String = "BUILD_TIME_REPLACE_WITH_REAL_HASH"
    }
}
