package com.Azelmods.App.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.ai.AiKeyStore
import com.Azelmods.App.data.ai.AiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🔑 AI KEY VIEW MODEL
 *
 * ViewModel mínimo para la sección "API Key de Gemini" de [AiFeaturesScreen].
 * Encapsula el acceso a [AiKeyStore] (almacenamiento cifrado) y expone:
 *  - [hasKey]: estado reactivo que indica si hay una clave activa.
 *  - [feedback]: mensaje efímero de confirmación/validación para la UI.
 *
 * Toda operación está protegida (el AiKeyStore nunca lanza), por lo que la UI
 * jamás crashea al guardar/borrar la clave.
 */
@HiltViewModel
class AiKeyViewModel @Inject constructor(
    private val keyStore: AiKeyStore
) : ViewModel() {

    companion object {
        private const val TAG = "AiKeyViewModel"
    }

    /** `true` si hay una API key del usuario almacenada de forma segura. */
    val hasKey: StateFlow<Boolean> = keyStore.hasKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), keyStore.hasApiKey())

    private val _feedback = MutableStateFlow<String?>(null)

    /** Mensaje de confirmación/validación para mostrar en la UI (se limpia con [clearFeedback]). */
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    /**
     * Guarda la API key tras validación mínima (no vacía). Devuelve feedback en [feedback].
     */
    fun saveApiKey(rawKey: String) {
        val key = rawKey.trim()
        if (key.isEmpty()) {
            _feedback.value = "La API key no puede estar vacía."
            return
        }
        viewModelScope.launch {
            runCatching { keyStore.setApiKey(key) }
                .onSuccess {
                    Log.d(TAG, "API key guardada correctamente")
                    _feedback.value = "API key guardada correctamente."
                }
                .onFailure {
                    Log.e(TAG, "No se pudo guardar la API key", it)
                    _feedback.value = "No se pudo guardar la API key. Inténtalo de nuevo."
                }
        }
    }

    /**
     * Borra la API key almacenada.
     */
    fun clearApiKey() {
        viewModelScope.launch {
            runCatching { keyStore.clearApiKey() }
                .onSuccess {
                    Log.d(TAG, "API key borrada")
                    _feedback.value = "API key borrada."
                }
                .onFailure {
                    Log.e(TAG, "No se pudo borrar la API key", it)
                    _feedback.value = "No se pudo borrar la API key."
                }
        }
    }

    // ── Proveedor de IA ──────────────────────────────────────────────────────

    /** Proveedor activo, reactivo para que la UI se redibuje al cambiarlo. */
    val provider: StateFlow<AiProvider> = keyStore.provider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), keyStore.getProvider())

    /** Todos los proveedores seleccionables. */
    val availableProviders: List<AiProvider> = AiProvider.entries

    /** URL base efectiva del proveedor activo (la del usuario o la del preset). */
    fun currentBaseUrl(): String = keyStore.getBaseUrl()

    /** Modelo efectivo del proveedor activo. */
    fun currentModel(): String = keyStore.getModel()

    /**
     * Cambia de proveedor. Resetea URL y modelo a los del preset, porque arrastrar
     * la URL del anterior solo produce errores difíciles de entender.
     */
    fun selectProvider(newProvider: AiProvider) {
        viewModelScope.launch {
            runCatching { keyStore.setProvider(newProvider) }
                .onSuccess {
                    _feedback.value = if (newProvider.allowsEmptyKey) {
                        "Proveedor: ${newProvider.displayName}. Revisa la URL del servidor."
                    } else {
                        "Proveedor: ${newProvider.displayName}. Necesita su propia API key."
                    }
                }
                .onFailure {
                    Log.e(TAG, "No se pudo cambiar el proveedor", it)
                    _feedback.value = "No se pudo cambiar el proveedor."
                }
        }
    }

    /** Guarda la URL base y el modelo del proveedor activo. */
    fun saveEndpoint(baseUrl: String, model: String) {
        viewModelScope.launch {
            runCatching {
                keyStore.setBaseUrl(baseUrl)
                keyStore.setModel(model)
            }.onSuccess {
                _feedback.value = if (keyStore.isProviderUsable()) {
                    "Configuración guardada."
                } else {
                    // Guardar algo incompleto y no avisar deja al usuario con un chat
                    // que falla sin explicación.
                    "Guardado, pero falta algún dato: revisa URL, modelo y API key."
                }
            }.onFailure {
                Log.e(TAG, "No se pudo guardar el endpoint", it)
                _feedback.value = "No se pudo guardar la configuración."
            }
        }
    }

    /** Limpia el mensaje de feedback tras mostrarlo. */
    fun clearFeedback() {
        _feedback.value = null
    }
}
