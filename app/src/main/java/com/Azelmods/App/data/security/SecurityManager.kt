package com.Azelmods.App.data.security

import android.content.Context
import com.Azelmods.App.BuildConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SECURITY MANAGER — ENTERPRISE GRADE 2026
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Gestiona la seguridad de la aplicación:
 * • Detección de root/emulador
 * • Limpieza de datos sensibles en memoria
 * • Validación de tokens
 * • Cliente HTTP seguro con timeouts
 * 
 * @since 2026
 * @version 3.0.0 Enterprise Edition
 * @author AzelMods677
 * ═══════════════════════════════════════════════════════════════════════════
 */

@Singleton
class SecurityManager @Inject constructor(
    private val context: Context
) {
    
    /**
     * Verifica si el dispositivo es seguro para ejecutar la app
     * En modo debug siempre retorna true
     */
    fun isDeviceSecure(): Boolean {
        if (BuildConfig.DEBUG) {
            return true // Permitir desarrollo en emuladores
        }
        
        return !isRooted() && !isEmulator()
    }
    
    /**
     * Detecta si el dispositivo tiene root
     */
    private fun isRooted(): Boolean {
        return try {
            // Verificar binario su
            val process = Runtime.getRuntime().exec("which su")
            val result = process.inputStream.bufferedReader().readText()
            result.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Detecta si está corriendo en un emulador
     */
    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK") ||
                android.os.Build.MANUFACTURER.contains("Genymotion") ||
                (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == android.os.Build.PRODUCT)
    }
    
    /**
     * Limpia datos sensibles de memoria (contraseñas, tokens, etc)
     * Sobrescribe con ceros para prevenir lectura de memoria
     */
    fun wipeSecureData(data: CharArray) {
        try {
            data.fill('\u0000')
        } catch (e: Exception) {
            android.util.Log.e("SecurityManager", "Failed to wipe data", e)
        }
    }
    
    /**
     * Sobrecarga para ByteArray
     */
    fun wipeSecureData(data: ByteArray) {
        try {
            data.fill(0)
        } catch (e: Exception) {
            android.util.Log.e("SecurityManager", "Failed to wipe data", e)
        }
    }
    
    /**
     * Valida que un token tenga formato correcto
     * Previene inyección de tokens malformados
     */
    fun isTokenValid(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        if (token.length < 20) return false
        
        // Token debe ser alfanumérico con algunos caracteres especiales permitidos
        return token.all { char ->
            char.isLetterOrDigit() || char in "-_."
        }
    }
    
    /**
     * Valida formato de email
     */
    fun isEmailValid(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
    
    /**
     * Crea un cliente HTTP seguro con timeouts apropiados
     * Previene ANR y hanging requests
     */
    fun getSecureOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Valida longitud de contraseña
     */
    fun isPasswordSecure(password: String?): Boolean {
        if (password.isNullOrBlank()) return false
        if (password.length < 6) return false
        
        // Opcional: verificar complejidad
        // val hasUpperCase = password.any { it.isUpperCase() }
        // val hasLowerCase = password.any { it.isLowerCase() }
        // val hasDigit = password.any { it.isDigit() }
        
        return true
    }
    
    /**
     * Sanitiza input del usuario para prevenir inyección
     */
    fun sanitizeInput(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        return input.trim()
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }
    
    /**
     * Genera un nonce aleatorio para operaciones criptográficas
     */
    fun generateNonce(length: Int = 32): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
