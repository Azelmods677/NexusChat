package com.Azelmods.App.data.security

import android.content.Context
import com.Azelmods.App.data.preferences.AppLockPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppLockManager - Gestiona el estado de bloqueo de la aplicación
 * 
 * Funcionalidades:
 * - Verificación de PIN
 * - Control de sesión de desbloqueo
 * - Auto-bloqueo basado en tiempo
 * - Estado de bloqueo reactivo
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLockPreferences: AppLockPreferences
) {
    
    // Estado actual de bloqueo (true = bloqueado, false = desbloqueado)
    private val _isLocked = MutableStateFlow(false)
    val isLocked: Flow<Boolean> = _isLocked.asStateFlow()
    
    // Timestamp de la última vez que la app estuvo en primer plano
    private var lastActiveTimestamp: Long = 0L
    
    /**
     * Verifica si la app debe estar bloqueada cuando vuelve al primer plano
     * 
     * @return true si debe mostrarse la pantalla de bloqueo
     */
    suspend fun shouldLockOnResume(): Boolean {
        val isEnabled = appLockPreferences.isLockEnabled.first()
        if (!isEnabled) return false
        
        // Si ya está bloqueada, mantener bloqueada
        if (_isLocked.value) return true
        
        // Primera vez que se abre la app
        if (lastActiveTimestamp == 0L) {
            lastActiveTimestamp = System.currentTimeMillis()
            return true // Siempre pedir PIN al abrir la app
        }
        
        // Verificar si pasó el tiempo de auto-bloqueo
        val autoLockMinutes = appLockPreferences.autoLockMinutes.first()
        if (autoLockMinutes == 0) {
            // Auto-bloqueo inmediato: siempre bloquear al volver
            return true
        }
        
        val elapsedMinutes = (System.currentTimeMillis() - lastActiveTimestamp) / 60_000
        return elapsedMinutes >= autoLockMinutes
    }
    
    /**
     * Marca la app como bloqueada
     */
    fun lock() {
        _isLocked.value = true
    }
    
    /**
     * Desbloquea la app después de verificar el PIN correctamente
     */
    fun unlock() {
        _isLocked.value = false
        lastActiveTimestamp = System.currentTimeMillis()
    }
    
    /**
     * Actualiza el timestamp cuando la app pasa a segundo plano
     */
    fun updateLastActiveTime() {
        lastActiveTimestamp = System.currentTimeMillis()
    }
    
    /**
     * Verifica si un PIN es correcto
     * 
     * @param pin El PIN ingresado por el usuario
     * @return true si el PIN es correcto
     */
    suspend fun verifyPin(pin: String): Boolean {
        return appLockPreferences.verifyPin(pin)
    }
    
    /**
     * Verifica si el bloqueo está habilitado en la configuración
     */
    suspend fun isLockEnabled(): Boolean {
        return appLockPreferences.isLockEnabled.first()
    }
    
    /**
     * Verifica si la biometría está habilitada
     */
    suspend fun isBiometricEnabled(): Boolean {
        return appLockPreferences.isBiometricEnabled.first()
    }
    
    /**
     * Resetea el estado de bloqueo (usado al cerrar sesión)
     */
    fun reset() {
        _isLocked.value = false
        lastActiveTimestamp = 0L
    }
}
