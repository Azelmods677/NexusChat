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
        // Se conserva el nombre histórico para no perder la clave ya guardada de los
        // usuarios que actualicen; ahora almacena la clave del proveedor activo.
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_PROVIDER = "ai_provider_id"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_MODEL = "ai_model"
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

    // ── Proveedor ────────────────────────────────────────────────────────────

    // Se inicializa aquí, no en el bloque init: init se ejecuta en orden de
    // declaración y está declarado más arriba, así que no puede tocar esta propiedad.
    private val _provider = MutableStateFlow(getProvider())

    /** Estado reactivo del proveedor activo, para que los ajustes se refresquen solos. */
    val provider: StateFlow<AiProvider> = _provider.asStateFlow()

    /** Proveedor elegido por el usuario; [AiProvider.DEFAULT] si nunca eligió. */
    fun getProvider(): AiProvider = AiProvider.fromId(
        runCatching { prefs.getString(KEY_PROVIDER, null) }.getOrNull()
    )

    /**
     * Cambia el proveedor. La URL base y el modelo se resetean a los del preset:
     * arrastrar la URL de OpenAI a Ollama solo produce errores desconcertantes.
     * La API key NO se borra, porque volver al proveedor anterior es lo normal.
     */
    fun setProvider(newProvider: AiProvider) {
        runCatching {
            prefs.edit()
                .putString(KEY_PROVIDER, newProvider.id)
                .remove(KEY_BASE_URL)
                .remove(KEY_MODEL)
                .apply()
        }.onFailure { Log.e(TAG, "No se pudo guardar el proveedor", it) }
        _provider.value = newProvider
        Log.d(TAG, "Proveedor de IA: ${newProvider.displayName}")
    }

    /** URL base efectiva: la que puso el usuario o, si no, la del preset. */
    fun getBaseUrl(): String {
        val stored = runCatching { prefs.getString(KEY_BASE_URL, null) }.getOrNull()?.trim()
        val effective = stored?.takeIf { it.isNotEmpty() } ?: getProvider().defaultBaseUrl
        // Sin barra final: los endpoints se concatenan como "$baseUrl/chat/completions"
        // y una doble barra devuelve 404 en varios proveedores.
        return effective.trimEnd('/')
    }

    fun setBaseUrl(url: String) {
        runCatching {
            val sanitized = url.trim()
            if (sanitized.isEmpty()) prefs.edit().remove(KEY_BASE_URL).apply()
            else prefs.edit().putString(KEY_BASE_URL, sanitized).apply()
        }.onFailure { Log.e(TAG, "No se pudo guardar la URL base", it) }
    }

    /** Modelo efectivo: el que puso el usuario o, si no, el del preset. */
    fun getModel(): String {
        val stored = runCatching { prefs.getString(KEY_MODEL, null) }.getOrNull()?.trim()
        return stored?.takeIf { it.isNotEmpty() } ?: getProvider().defaultModel
    }

    fun setModel(model: String) {
        runCatching {
            val sanitized = model.trim()
            if (sanitized.isEmpty()) prefs.edit().remove(KEY_MODEL).apply()
            else prefs.edit().putString(KEY_MODEL, sanitized).apply()
        }.onFailure { Log.e(TAG, "No se pudo guardar el modelo", it) }
    }

    /**
     * `true` si el proveedor activo puede usarse ya: tiene lo que necesita.
     * Los servidores locales no requieren clave, pero sí una URL base.
     */
    fun isProviderUsable(): Boolean {
        val p = getProvider()
        val keyOk = p.allowsEmptyKey || hasApiKey()
        val urlOk = !p.isOpenAiCompatible || getBaseUrl().isNotBlank()
        return keyOk && urlOk
    }

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
