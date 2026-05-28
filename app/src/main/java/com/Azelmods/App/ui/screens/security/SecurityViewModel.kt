package com.Azelmods.App.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for security features including Tor integration via Orbot
 *
 * Maneja el estado de Tor y proporciona funciones para activar/desactivar
 * el modo anónimo usando Orbot.
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val torPreferences: com.Azelmods.App.data.security.tor.TorPreferences
) : ViewModel() {

    val torState: StateFlow<TorState> = securityRepository.getTorState()

    private val _uiState = MutableStateFlow<SecurityUiState>(SecurityUiState.Idle)
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _torLogs = MutableStateFlow<List<String>>(emptyList())
    val torLogs: StateFlow<List<String>> = _torLogs.asStateFlow()

    init {
        restoreTorState()
    }

    /**
     * Restaura el estado de Tor al iniciar la app
     */
    private fun restoreTorState() {
        viewModelScope.launch {
            try {
                val wasEnabled = torPreferences.isAnonymousModeEnabled()
                if (wasEnabled) {
                    enableAnonymousMode()
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Activa el modo anónimo iniciando Tor vía Orbot
     */
    fun enableAnonymousMode() {
        viewModelScope.launch {
            try {
                addLog("Iniciando Tor vía Orbot...")
                _uiState.value = SecurityUiState.Loading("Buscando Orbot...")

                securityRepository.startTor().collect { state ->
                    when (state) {
                        is TorState.Disconnected -> {
                            _uiState.value = SecurityUiState.Idle
                            addLog("Tor desconectado")
                        }
                        is TorState.Connecting -> {
                            _uiState.value = SecurityUiState.Loading("Conectando: ${state.progress}%")
                            addLog(
                                "Conectando a Tor: ${state.progress}%" +
                                if (state.message.isNotEmpty()) " – ${state.message}" else ""
                            )
                        }
                        is TorState.Bootstrapping -> {
                            _uiState.value = SecurityUiState.Loading("Conectando: ${state.progress}%")
                            addLog(
                                "Conectando a Tor: ${state.progress}%" +
                                if (state.message.isNotEmpty()) " – ${state.message}" else ""
                            )
                        }
                        is TorState.Connected -> {
                            _uiState.value = SecurityUiState.Success("Modo anónimo activado")
                            torPreferences.setAnonymousModeEnabled(true)
                            addLog("✓ Conectado a la red Tor vía Orbot")
                        }
                        is TorState.Error -> {
                            _uiState.value = SecurityUiState.Error(
                                message = state.message,
                                suggestion = "Asegúrate de que Orbot esté instalado y ejecutándose"
                            )
                            addLog("Error: ${state.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error(
                    message = "Error al iniciar Tor: ${e.message}",
                    suggestion = "Verifica tu conexión de red"
                )
            }
        }
    }

    /**
     * Desactiva el modo anónimo deteniendo Tor
     */
    fun disableAnonymousMode() {
        viewModelScope.launch {
            try {
                addLog("Deteniendo Tor...")
                _uiState.value = SecurityUiState.Loading("Deteniendo Tor...")
                securityRepository.stopTor()
                _uiState.value = SecurityUiState.Idle
                torPreferences.setAnonymousModeEnabled(false)
                addLog("Tor detenido")
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error(
                    message = "Error al detener Tor: ${e.message}",
                    suggestion = "Reintenta o reinicia la app"
                )
                addLog("Error: ${e.message}")
            }
        }
    }

    /**
     * Limpia el estado actual de UI
     */
    fun clearUiState() {
        _uiState.value = SecurityUiState.Idle
    }

    fun clearLogs() { _torLogs.value = emptyList() }

    private fun addLog(entry: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val newLogs = (_torLogs.value + "[$timestamp] $entry").takeLast(100)
        _torLogs.value = newLogs
    }
}

/**
 * UI state for security screen
 */
sealed class SecurityUiState {
    object Idle : SecurityUiState()
    data class Loading(val message: String) : SecurityUiState()
    data class Success(val message: String) : SecurityUiState()
    data class Error(val message: String, val suggestion: String) : SecurityUiState()
}
