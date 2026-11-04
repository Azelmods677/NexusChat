package com.Azelmods.App.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.security.tor.TorEngine
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del modo anónimo para las pantallas de seguridad.
 *
 * La preferencia se guarda al pulsar el interruptor, no al terminar de conectar: si el
 * usuario lo activó, al reabrir la app debe seguir activo aunque Tor no llegara a conectar,
 * porque es lo que evita que algo salga sin protección sin que se dé cuenta.
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val torPreferences: com.Azelmods.App.data.security.tor.TorPreferences,
    private val torService: com.Azelmods.App.data.security.tor.TorService,
    private val ghostMode: com.Azelmods.App.data.security.GhostMode
) : ViewModel() {

    val ghostModeEnabled: StateFlow<Boolean> = ghostMode.enabled

    fun setGhostMode(on: Boolean) = ghostMode.set(on)

    val torState: StateFlow<TorState> = securityRepository.getTorState()

    private val _engine = MutableStateFlow(torService.engine)
    val engine: StateFlow<TorEngine> = _engine.asStateFlow()

    val embeddedTorVersion: String get() = torService.embeddedVersion

    fun enableAnonymousMode() {
        torPreferences.setAnonymousModeEnabled(true)
        torService.startTor()
    }

    fun disableAnonymousMode() {
        torPreferences.setAnonymousModeEnabled(false)
        viewModelScope.launch { securityRepository.stopTor() }
    }

    fun setEngine(engine: TorEngine) {
        torService.setEngine(engine)
        _engine.value = engine
    }

    /** Vuelve a preguntar a check.torproject.org si salimos por Tor y con qué IP. */
    fun verifyTor() = torService.verifyConnection()

    /** Pide circuitos nuevos. Devuelve false si el motor en uso no lo permite. */
    fun newIdentity(): Boolean = torService.newIdentity()

    /** Pide a Orbot que arranque Tor sin salir de NexusChat. */
    fun requestOrbotStart(context: android.content.Context): Boolean =
        com.Azelmods.App.data.security.tor.OrbotIntents.requestStart(context)
}
