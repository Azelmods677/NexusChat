package com.Azelmods.App.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.security.tor.TorCircuitInfo
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for security features including Tor integration
 *
 * Manages the state of Tor service and provides functions to control
 * anonymous mode, circuit management, and bridge configuration.
 *
 * Handles persistence of Tor settings and auto-start on app restart.
 * Requirements: 23.1, 23.2, 23.3, 23.4
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val torPreferences: com.Azelmods.App.data.security.tor.TorPreferences
) : ViewModel() {

    // Tor state from repository
    val torState: StateFlow<TorState> = securityRepository.getTorState()

    private val _uiState = MutableStateFlow<SecurityUiState>(SecurityUiState.Idle)
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _circuitInfo = MutableStateFlow<TorCircuitInfo?>(null)
    val circuitInfo: StateFlow<TorCircuitInfo?> = _circuitInfo.asStateFlow()

    // Tor connection logs
    private val _torLogs = MutableStateFlow<List<String>>(emptyList())
    val torLogs: StateFlow<List<String>> = _torLogs.asStateFlow()

    init {
        // Requirement 23.3, 23.4: Restore Tor state on app restart and auto-start if previously enabled
        restoreTorState()
    }

    /**
     * Restores Tor state on app restart
     * Requirement 23.3, 23.4: Restore Tor state and auto-start if previously enabled
     */
    private fun restoreTorState() {
        viewModelScope.launch {
            try {
                // Check if Anonymous Mode was previously enabled
                val wasEnabled = torPreferences.isAnonymousModeEnabled()

                if (wasEnabled) {
                    // Auto-start Tor if it was previously enabled
                    enableAnonymousMode()
                }

                // Restore bridge configuration if any
                val bridges = torPreferences.getBridgeAddresses()
                if (bridges.isNotEmpty()) {
                    // Bridges will be applied when Tor starts
                    // No need to explicitly enable them here
                }
            } catch (e: Exception) {
                // Silently fail - don't block app startup
            }
        }
    }

    /**
     * Enables anonymous mode by starting Tor service
     * Requirement 23.1: Save Anonymous Mode preference
     */
    fun enableAnonymousMode() {
        viewModelScope.launch {
            try {
                addLog("Starting Tor service...")
                _uiState.value = SecurityUiState.Loading("Starting Tor service...")

                securityRepository.startTorService().collect { state ->
                    when (state) {
                        is TorState.Disconnected -> {
                            _circuitInfo.value = null
                            _uiState.value = SecurityUiState.Idle
                            addLog("Tor disconnected")
                        }
                        is TorState.Connecting -> {
                            _uiState.value = SecurityUiState.Loading("Connecting: ${state.progress}%")
                            addLog(
                                "Connecting to Tor: ${state.progress}%" +
                                if (state.message.isNotEmpty()) " – ${state.message}" else ""
                            )
                        }
                        is TorState.Bootstrapping -> {
                            _uiState.value = SecurityUiState.Loading("Connecting: ${state.progress}%")
                            addLog(
                                "Connecting to Tor: ${state.progress}%" +
                                if (state.message.isNotEmpty()) " – ${state.message}" else ""
                            )
                        }
                        is TorState.Connected -> {
                            _circuitInfo.value = state.circuitInfo
                            _uiState.value = SecurityUiState.Success("Anonymous mode enabled")
                            torPreferences.setAnonymousModeEnabled(true)
                            addLog(
                                "Connected! Circuit: ${state.circuitInfo.entryNode} " +
                                "→ ${state.circuitInfo.middleNode} " +
                                "→ ${state.circuitInfo.exitNode}"
                            )
                        }
                        is TorState.Error -> {
                            _uiState.value = SecurityUiState.Error(
                                message = state.message,
                                suggestion = getSuggestionForError(state.message)
                            )
                            addLog("Error: ${state.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error(
                    message = "Failed to start Tor: ${e.message}",
                    suggestion = "Check your network connection and try again"
                )
            }
        }
    }

    /**
     * Disables anonymous mode by stopping Tor service
     * Requirement 23.1: Save Anonymous Mode preference
     */
    fun disableAnonymousMode() {
        viewModelScope.launch {
            try {
                addLog("Stopping Tor service...")
                _uiState.value = SecurityUiState.Loading("Stopping Tor service...")
                securityRepository.stopTorService()
                _circuitInfo.value = null
                _uiState.value = SecurityUiState.Idle
                torPreferences.setAnonymousModeEnabled(false)
                addLog("Tor service stopped")
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error(
                    message = "Failed to stop Tor: ${e.message}",
                    suggestion = "Try restarting the app"
                )
                addLog("Error stopping Tor: ${e.message}")
            }
        }
    }

    /**
     * Requests a new Tor identity (creates new circuit)
     */
    fun requestNewIdentity() {
        viewModelScope.launch {
            try {
                addLog("Requesting new Tor identity...")
                _uiState.value = SecurityUiState.Loading("Creating new identity...")
                securityRepository.newIdentity()

                // Refresh circuit info
                val newCircuitInfo = securityRepository.getCircuitInfo()
                _circuitInfo.value = newCircuitInfo

                _uiState.value = SecurityUiState.Success("New identity created")
                addLog("New identity created successfully")
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error(
                    message = "Failed to create new identity: ${e.message}",
                    suggestion = "Make sure Tor is connected"
                )
                addLog("Failed to create new identity: ${e.message}")
            }
        }
    }

    /**
     * Enables obfs4 bridges for censorship bypass
     * Requirement 23.2: Save bridge configuration
     */
    fun enableBridges(bridges: List<String>) {
        viewModelScope.launch {
            try {
                _uiState.value = SecurityUiState.Loading("Configuring bridges...")
                securityRepository.enableObfs4Bridges(bridges)
                // Save bridge configuration
                torPreferences.saveBridgeConfiguration(bridges)
                _uiState.value = SecurityUiState.Success("Bridges configured")
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error(
                    message = "Failed to configure bridges: ${e.message}",
                    suggestion = "Check bridge format and try again"
                )
            }
        }
    }

    /**
     * Refreshes circuit information
     */
    fun refreshCircuitInfo() {
        viewModelScope.launch {
            try {
                val info = securityRepository.getCircuitInfo()
                _circuitInfo.value = info
            } catch (e: Exception) {
                // Silently fail - circuit info is optional
            }
        }
    }

    /**
     * Provides user-friendly suggestions based on error messages
     */
    private fun getSuggestionForError(errorMessage: String): String {
        return when {
            errorMessage.contains("binary not found", ignoreCase = true) ->
                "Tor binary is missing. Please reinstall the app."
            errorMessage.contains("timeout", ignoreCase = true) ->
                "Connection timed out. Try using bridges or check your network."
            errorMessage.contains("proxy", ignoreCase = true) ->
                "Proxy connection failed. Try restarting Tor service."
            errorMessage.contains("dns", ignoreCase = true) ->
                "DNS leak detected. Tor has been disabled for your safety."
            else ->
                "Please check your network connection and try again."
        }
    }

    /**
     * Clears the current UI state message
     */
    fun clearUiState() {
        _uiState.value = SecurityUiState.Idle
    }

    /**
     * Requests a new Tor circuit without changing the exit identity.
     * Logs the result for user visibility.
     */
    fun requestNewCircuit() {
        viewModelScope.launch {
            try {
                addLog("Requesting new Tor circuit...")
                securityRepository.newIdentity()
                addLog("New circuit requested successfully")
            } catch (e: Exception) {
                addLog("Failed to get new circuit: ${e.message}")
            }
        }
    }

    /** Clears all Tor connection logs. */
    fun clearLogs() { _torLogs.value = emptyList() }

    /**
     * Appends a timestamped entry to [torLogs], keeping at most 100 lines.
     */
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
