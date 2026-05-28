package com.Azelmods.App.domain.repository

import com.Azelmods.App.data.security.tor.TorState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for security operations including Tor integration
 *
 * Esta interfaz usa Orbot para proporcionar conectividad Tor
 * en lugar de un binario Tor embebido.
 */
interface SecurityRepository {
    /**
     * Inicia la detección de Orbot y establece conexión Tor
     * @return Flow<TorState> emitiendo estados durante la conexión
     */
    fun startTor(): Flow<TorState>

    /**
     * Detiene el servicio Tor
     */
    suspend fun stopTor()

    /**
     * Obtiene el estado actual de Tor
     */
    fun getTorState(): StateFlow<TorState>

    /**
     * Obtiene información de conectividad Tor (Orbot disponible/activo)
     */
    fun getTorStatus(): String
}
