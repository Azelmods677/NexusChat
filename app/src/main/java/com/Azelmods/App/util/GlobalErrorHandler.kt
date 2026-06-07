package com.Azelmods.App.util

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GLOBAL ERROR HANDLER — ENTERPRISE GRADE 2026
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Captura crashes no manejados y guarda logs para diagnóstico
 * Previene pérdida total de la app al crashear
 * 
 * @since 2026
 * @version 3.0.0 Enterprise Edition
 * @author AzelMods677
 * ═══════════════════════════════════════════════════════════════════════════
 */

class NexusCrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val PREFS_NAME = "nexus_crash_logs"
        private const val KEY_LAST_CRASH = "last_crash_log"
        private const val KEY_CRASH_COUNT = "crash_count"
        private const val KEY_CRASH_TIMESTAMP = "crash_timestamp"
        
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val crashHandler = NexusCrashHandler(context.applicationContext, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashLog = buildCrashLog(thread, throwable)
            saveCrashLog(crashLog)
            logToAndroid(throwable)
        } catch (e: Exception) {
            // Nunca crashear en el crash handler
            android.util.Log.e("NexusCrashHandler", "Error in crash handler", e)
        } finally {
            // Delegar al handler original de Android
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun buildCrashLog(thread: Thread, throwable: Throwable): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timestamp = dateFormat.format(Date())
        
        return buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("NEXUS CHAT CRASH REPORT")
            appendLine("═══════════════════════════════════════")
            appendLine("Time: $timestamp")
            appendLine("Thread: ${thread.name} (ID: ${thread.id})")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message ?: "No message"}")
            appendLine()
            appendLine("Stack Trace:")
            throwable.stackTrace.take(15).forEach { element ->
                appendLine("  at $element")
            }
            
            // Causa raíz si existe
            throwable.cause?.let { cause ->
                appendLine()
                appendLine("Caused by: ${cause.javaClass.name}")
                appendLine("Message: ${cause.message ?: "No message"}")
                cause.stackTrace.take(10).forEach { element ->
                    appendLine("  at $element")
                }
            }
            
            appendLine()
            appendLine("Device Info:")
            appendLine("  Model: ${android.os.Build.MODEL}")
            appendLine("  Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("  Manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("═══════════════════════════════════════")
        }
    }
    
    private fun saveCrashLog(log: String) {
        try {
            prefs.edit().apply {
                putString(KEY_LAST_CRASH, log)
                putLong(KEY_CRASH_TIMESTAMP, System.currentTimeMillis())
                
                val count = prefs.getInt(KEY_CRASH_COUNT, 0)
                putInt(KEY_CRASH_COUNT, count + 1)
                
                apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("NexusCrashHandler", "Failed to save crash log", e)
        }
    }
    
    private fun logToAndroid(throwable: Throwable) {
        android.util.Log.e(
            "NexusCrashHandler",
            "UNCAUGHT EXCEPTION: ${throwable.javaClass.simpleName}",
            throwable
        )
    }
    
    /**
     * Obtiene el último crash log guardado
     */
    fun getLastCrashLog(): String? {
        return try {
            prefs.getString(KEY_LAST_CRASH, null)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Obtiene el conteo total de crashes
     */
    fun getCrashCount(): Int {
        return try {
            prefs.getInt(KEY_CRASH_COUNT, 0)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Limpia los logs de crashes
     */
    fun clearCrashLogs() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.e("NexusCrashHandler", "Failed to clear crash logs", e)
        }
    }
}
