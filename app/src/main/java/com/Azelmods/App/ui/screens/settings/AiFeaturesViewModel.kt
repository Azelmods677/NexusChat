package com.Azelmods.App.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.ai.AiAssistService
import com.Azelmods.App.data.preferences.AiFeaturePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiFeaturesState(
    /** `false` si no hay proveedor de IA configurado. */
    val providerReady: Boolean = false,
    val smartReplies: Boolean = false,
    val autoTranslate: Boolean = false,
    val conversationSummary: Boolean = false,
    val toneSuggestions: Boolean = false,
    val photoEnhance: Boolean = false,
    val voiceTranscription: Boolean = false
)

/**
 * Estado de los interruptores de funciones de IA.
 *
 * Cada `set*` escribe en DataStore y el flujo devuelve el valor ya guardado, así
 * que la interfaz refleja lo que hay en disco y no un estado local que se pierde
 * al salir de la pantalla —que es exactamente lo que hacían estos interruptores
 * antes de retirarlos—.
 */
@HiltViewModel
class AiFeaturesViewModel @Inject constructor(
    private val preferences: AiFeaturePreferences,
    private val aiAssistService: AiAssistService
) : ViewModel() {

    private val _state = MutableStateFlow(AiFeaturesState())
    val state: StateFlow<AiFeaturesState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(providerReady = aiAssistService.isAvailable())
        viewModelScope.launch {
            launch { preferences.smartReplies.collect { _state.value = _state.value.copy(smartReplies = it) } }
            launch { preferences.autoTranslate.collect { _state.value = _state.value.copy(autoTranslate = it) } }
            launch { preferences.conversationSummary.collect { _state.value = _state.value.copy(conversationSummary = it) } }
            launch { preferences.toneSuggestions.collect { _state.value = _state.value.copy(toneSuggestions = it) } }
            launch { preferences.photoEnhance.collect { _state.value = _state.value.copy(photoEnhance = it) } }
            launch { preferences.voiceTranscription.collect { _state.value = _state.value.copy(voiceTranscription = it) } }
        }
    }

    /** Relee si hay proveedor: el usuario puede pegar la clave en esta misma pantalla. */
    fun refreshProviderState() {
        _state.value = _state.value.copy(providerReady = aiAssistService.isAvailable())
    }

    fun setSmartReplies(enabled: Boolean) = viewModelScope.launch { preferences.setSmartReplies(enabled) }
    fun setAutoTranslate(enabled: Boolean) = viewModelScope.launch { preferences.setAutoTranslate(enabled) }
    fun setConversationSummary(enabled: Boolean) = viewModelScope.launch { preferences.setConversationSummary(enabled) }
    fun setToneSuggestions(enabled: Boolean) = viewModelScope.launch { preferences.setToneSuggestions(enabled) }
    fun setPhotoEnhance(enabled: Boolean) = viewModelScope.launch { preferences.setPhotoEnhance(enabled) }
    fun setVoiceTranscription(enabled: Boolean) = viewModelScope.launch { preferences.setVoiceTranscription(enabled) }
}
