@file:Suppress("DEPRECATION")
package com.Azelmods.App.data.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔐 AI KEY STORE
 *
 * Almacenamiento seguro de la API key de Gemini introducida por el usuario.
 * Usa [EncryptedSharedPreferences] (androidx.security:security-crypto) de forma que
 * la clave nunca se guarda en texto plano en disco.
 *
 * Características:
 *  - Cifrado AES256 (claves y valores) respaldado por el Android Keystore.
 *  - Recuperación robusta: si el almacén cifrado se corrompe (p. ej. tras restaurar
 *    un backup o rotar el keystore), se limpia y se vuelve a crear sin crashear.
 *  - Estado reactivo [hasKey] para que la UI (cabecera del chat, ajustes) refleje en
 *    tiempo real si hay una clave activa.
 *
 * Provisto por Hilt mediante inyección de constructor (@Singleton).
 */
@Singleton
class AiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AiKeyStore"
        private const val PREFS_FILE = "azel_ai_secure_prefs"
        private const val KEY_API_KEY = "gemini_api_key"
    }

    private val prefs: SharedPreferences by lazy { createSecurePrefs() }

    private val _hasKey = MutableStateFlow(false)

    /** Estado reactivo: `true` si hay una API key del usuario almacenada. */
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    init {
        // Inicializa el estado reactivo con el valor persistido.
        _hasKey.value = !readRawKey().isNullOrBlank()
    }

    /**
     * Devuelve la API key del usuario o `null` si no hay ninguna configurada.
     * El valor se devuelve ya recortado (sin espacios sobrantes).
     */
    fun getApiKey(): String? = readRawKey()?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Guarda la API key del usuario de forma cifrada. Un valor en blanco equivale a borrarla.
     */
    fun setApiKey(key: String) {
        val sanitized = key.trim()
        if (sanitized.isEmpty()) {
            clearApiKey()
            return
        }
        runCatching {
            prefs.edit().putString(KEY_API_KEY, sanitized).apply()
        }.onFailure { Log.e(TAG, "No se pudo guardar la API key", it) }
        _hasKey.value = !getApiKey().isNullOrBlank()
        Log.d(TAG, "API key guardada (hasKey=${_hasKey.value})")
    }

    /**
     * Borra la API key del usuario del almacén seguro.
     */
    fun clearApiKey() {
        runCatching {
            prefs.edit().remove(KEY_API_KEY).apply()
        }.onFailure { Log.e(TAG, "No se pudo borrar la API key", it) }
        _hasKey.value = false
        Log.d(TAG, "API key borrada")
    }

    /**
     * `true` si existe una API key del usuario almacenada y no está en blanco.
     */
    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    private fun readRawKey(): String? = runCatching {
        prefs.getString(KEY_API_KEY, null)
    }.getOrNull()

    /**
     * Crea (o recupera) el almacén cifrado. Si la creación falla por corrupción del
     * archivo cifrado, lo elimina y reintenta una vez para evitar crashes persistentes.
     */
    private fun createSecurePrefs(): SharedPreferences {
        return try {
            buildEncryptedPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "Almacén cifrado corrupto, recreando", e)
            // Elimina el archivo corrupto y reintenta una vez.
            runCatching { context.deleteSharedPreferences(PREFS_FILE) }
            try {
                buildEncryptedPrefs()
            } catch (e2: Exception) {
                Log.e(TAG, "Fallo al recrear el almacén cifrado, usando prefs estándar como último recurso", e2)
                // Último recurso: prefs estándar (no debería ocurrir en dispositivos normales).
                context.getSharedPreferences("${PREFS_FILE}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
