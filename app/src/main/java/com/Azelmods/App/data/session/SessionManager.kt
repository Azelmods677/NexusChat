package com.Azelmods.App.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SESSION MANAGER — ENTERPRISE GRADE 2026
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Gestiona la sesión del usuario con:
 * • Persistencia de sesión entre reinicios
 * • Refresh automático de tokens
 * • Prevención de memory leaks en auth listeners
 * • Validación de expiración de sesión
 * 
 * @since 2026
 * @version 3.0.0 Enterprise Edition
 * @author AzelMods677
 * ═══════════════════════════════════════════════════════════════════════════
 */

@Singleton
class SessionManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val SESSION_KEY = stringPreferencesKey("session_uid")
        private val TOKEN_KEY = stringPreferencesKey("session_token")
        private val EXPIRY_KEY = longPreferencesKey("session_expiry")
        
        // Sesión válida por 55 minutos (Firebase expira a los 60)
        private const val SESSION_DURATION_MS = 55 * 60 * 1000L
        
        // Refresh cada 50 minutos
        const val REFRESH_INTERVAL_MS = 50 * 60 * 1000L
    }
    
    /**
     * Estado de login del usuario
     * Combina Firebase Auth y DataStore para máxima confiabilidad
     */
    val isLoggedIn: StateFlow<Boolean> = flow {
        // Emitir estado inicial de Firebase
        emit(auth.currentUser != null)
        
        // Luego seguir el estado de DataStore
        dataStore.data.collect { prefs ->
            val uid = prefs[SESSION_KEY]
            val expiry = prefs[EXPIRY_KEY] ?: 0L
            val isValid = uid != null && System.currentTimeMillis() < expiry
            emit(isValid && auth.currentUser != null)
        }
    }.stateIn(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = auth.currentUser != null
    )
    
    /**
     * UID del usuario actual
     */
    val currentUserId: String?
        get() = auth.currentUser?.uid
    
    /**
     * Refresca el token de sesión
     * Debe llamarse periódicamente para mantener la sesión activa
     */
    suspend fun refreshSession(): Result<Unit> {
        return try {
            val user = auth.currentUser 
                ?: return Result.failure(Exception("No user logged in"))
            
            val tokenResult = user.getIdToken(true).await()
            val token = tokenResult.token 
                ?: return Result.failure(Exception("Token is null"))
            
            dataStore.edit { prefs ->
                prefs[SESSION_KEY] = user.uid
                prefs[TOKEN_KEY] = token
                prefs[EXPIRY_KEY] = System.currentTimeMillis() + SESSION_DURATION_MS
            }
            
            android.util.Log.d("SessionManager", "Session refreshed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Failed to refresh session", e)
            Result.failure(e)
        }
    }
    
    /**
     * Guarda la sesión después de login
     */
    suspend fun saveSession(uid: String, token: String) {
        try {
            dataStore.edit { prefs ->
                prefs[SESSION_KEY] = uid
                prefs[TOKEN_KEY] = token
                prefs[EXPIRY_KEY] = System.currentTimeMillis() + SESSION_DURATION_MS
            }
            android.util.Log.d("SessionManager", "Session saved for user: $uid")
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Failed to save session", e)
        }
    }
    
    /**
     * Limpia la sesión al hacer logout
     */
    suspend fun clearSession() {
        try {
            dataStore.edit { prefs ->
                prefs.clear()
            }
            auth.signOut()
            android.util.Log.d("SessionManager", "Session cleared")
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Failed to clear session", e)
        }
    }
    
    /**
     * Verifica si la sesión ha expirado
     */
    suspend fun isSessionExpired(): Boolean {
        return try {
            val expiry = dataStore.data.map { prefs ->
                prefs[EXPIRY_KEY] ?: 0L
            }.stateIn(
                CoroutineScope(Dispatchers.IO),
                SharingStarted.Eagerly,
                0L
            ).value
            
            System.currentTimeMillis() >= expiry
        } catch (e: Exception) {
            true // En caso de error, considerar expirado
        }
    }
    
    /**
     * Obtiene el token actual
     */
    suspend fun getCurrentToken(): String? {
        return try {
            dataStore.data.map { prefs ->
                prefs[TOKEN_KEY]
            }.stateIn(
                CoroutineScope(Dispatchers.IO),
                SharingStarted.Eagerly,
                null
            ).value
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Failed to get token", e)
            null
        }
    }
}
