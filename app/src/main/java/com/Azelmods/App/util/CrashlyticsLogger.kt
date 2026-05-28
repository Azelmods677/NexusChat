package com.Azelmods.App.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * 🛡️ CrashlyticsLogger — Utility for structured error logging
 *
 * Provides consistent crash reporting across the app with:
 * - Custom keys for context (feature, operation, user ID)
 * - Log statements that sync with crash reports
 * - Non-fatal exception recording
 * - Sensitive data filtering (auto-strips tokens, passwords)
 */
object CrashlyticsLogger {

    private const val TAG = "Crashlytics"
    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    /**
     * Set user ID for crash reporting.
     * Call after login/logout.
     */
    fun setUserId(uid: String) {
        try {
            crashlytics.setUserId(uid)
        } catch (_: Exception) { }
    }

    /**
     * Clear user ID on logout.
     */
    fun clearUserId() {
        try {
            crashlytics.setUserId("")
        } catch (_: Exception) { }
    }

    /**
     * Set a custom key for crash context.
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics.setCustomKey(key, sanitize(value))
        } catch (_: Exception) { }
    }

    fun setCustomKey(key: String, value: Int) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (_: Exception) { }
    }

    fun setCustomKey(key: String, value: Boolean) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (_: Exception) { }
    }

    fun setCustomKey(key: String, value: Long) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (_: Exception) { }
    }

    /**
     * Log a message that will be attached to the next crash report.
     */
    fun log(message: String) {
        try {
            crashlytics.log(sanitize(message))
        } catch (_: Exception) { }
        Log.d(TAG, message)
    }

    /**
     * Record a non-fatal exception with context.
     *
     * @param throwable The exception to report
     * @param feature The feature/module where it occurred (e.g. "chat", "ai", "calls")
     * @param operation The operation being performed (e.g. "sendMessage", "loadChat")
     * @param message Optional user-friendly message for logs
     */
    fun recordException(
        throwable: Throwable,
        feature: String = "general",
        operation: String = "unknown",
        message: String? = null
    ) {
        try {
            crashlytics.setCustomKey("feature", feature)
            crashlytics.setCustomKey("operation", operation)
            if (message != null) {
                crashlytics.log(sanitize(message))
            }
            crashlytics.recordException(throwable)
        } catch (_: Exception) { }

        Log.e(TAG, "[$feature/$operation] ${message ?: throwable.message}", throwable)
    }

    /**
     * Record a recoverable error (non-fatal, with user-friendly message).
     *
     * @param throwable The exception
     * @param feature Feature/module name
     * @param operation Operation name
     * @param userMessage Message to show to the user
     */
    fun recordRecoverableError(
        throwable: Throwable,
        feature: String = "general",
        operation: String = "unknown",
        userMessage: String = "Ocurrió un error inesperado"
    ) {
        recordException(throwable, feature, operation, "Recoverable: $userMessage")
    }

    /**
     * Log a warning (non-critical, not sent as crash).
     */
    fun warn(feature: String, operation: String, message: String) {
        val msg = "[$feature/$operation] $message"
        Log.w(TAG, msg)
        try {
            crashlytics.log(sanitize(msg))
        } catch (_: Exception) { }
    }

    /**
     * Sanitize sensitive data before sending to Crashlytics.
     * Strips tokens, passwords, API keys from log messages.
     */
    private fun sanitize(input: String): String {
        return input
            .replace(Regex("""(?i)(api[_-]?key|token|password|secret|auth)[=:]\s*\S+"""), "$1=***")
            .replace(Regex("""sk-[a-zA-Z0-9]{20,}"""), "sk-***")
            .replace(Regex("""Bearer\s+\S+"""), "Bearer ***")
    }
}
