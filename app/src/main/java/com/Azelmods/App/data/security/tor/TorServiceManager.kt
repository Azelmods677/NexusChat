package com.Azelmods.App.data.security.tor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for managing the Tor service and SOCKS5 proxy
 */
interface TorServiceManager {
    /**
     * Starts the Tor service and establishes SOCKS5 proxy
     * @return Flow<TorState> emitting connection states during bootstrap
     */
    fun startTor(): Flow<TorState>
    
    /**
     * Stops the Tor service and cleans up resources
     */
    suspend fun stopTor()
    
    /**
     * Gets current Tor connection state
     * @return StateFlow of the current TorState
     */
    fun getTorState(): StateFlow<TorState>
    
    /**
     * Enables obfs4 bridges for censorship bypass
     * @param bridges List of obfs4 bridge addresses
     */
    suspend fun enableObfs4Bridges(bridges: List<String>)
    
    /**
     * Gets Tor circuit information
     * @return Current circuit information or null if not connected
     */
    suspend fun getCircuitInfo(): TorCircuitInfo?
    
    /**
     * Creates new Tor identity (new circuit)
     */
    suspend fun newIdentity()
}
