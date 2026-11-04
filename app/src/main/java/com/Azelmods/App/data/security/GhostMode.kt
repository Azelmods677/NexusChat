package com.Azelmods.App.data.security

import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.security.tor.AnonymityPolicy
import com.Azelmods.App.data.security.tor.TorPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modo fantasma: lees y escribes sin que nadie vea que estás conectado, que escribes o que has
 * leído. A diferencia del modo anónimo no pasa por Tor, así que tu IP no cambia.
 */
@Singleton
class GhostMode @Inject constructor(
    private val preferences: TorPreferences,
    private val repository: RealtimeDatabaseRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _enabled = MutableStateFlow(preferences.isGhostModeEnabled())
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun set(on: Boolean) {
        preferences.setGhostModeEnabled(on)
        AnonymityPolicy.ghost = on
        _enabled.value = on
        // Sin esto seguirías apareciendo "en línea" hasta cerrar la app.
        scope.launch { runCatching { repository.updatePresence(isOnline = !on) } }
    }
}
