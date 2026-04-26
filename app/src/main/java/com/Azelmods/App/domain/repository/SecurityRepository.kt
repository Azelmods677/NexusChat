package com.Azelmods.App.domain.repository

import com.Azelmods.App.data.security.tor.TorCircuitInfo
import com.Azelmods.App.data.security.tor.TorState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for security operations including Tor integration
 * and payload generation (future implementation)
 * 
 * This interface follows Clean Architecture principles by abstracting
 * the data layer from the domain layer.
 */
interface SecurityRepository {
    /**
     * Starts the Tor service and establishes SOCKS5 proxy
     * @return Flow<TorState> emitting connection states during bootstrap
     */
    fun startTorService(): Flow<TorState>
    
    /**
     * Stops the Tor service and cleans up resources
     */
    suspend fun stopTorService()
    
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
    
    // Payload Generator Methods
    /**
     * Generates a payload with the specified configuration
     * @param config Payload configuration
     * @return Flow emitting generation state updates
     */
    fun generatePayload(config: com.Azelmods.App.data.security.payload.PayloadConfig): Flow<com.Azelmods.App.data.security.payload.PayloadGenerationState>
    
    /**
     * Gets payload generation history
     * @return Flow of generated payloads
     */
    fun getPayloadHistory(): Flow<List<com.Azelmods.App.data.security.payload.GeneratedPayload>>
    
    /**
     * Deletes a payload from history and storage
     * @param payloadId ID of the payload to delete
     */
    suspend fun deletePayload(payloadId: String)
    
    /**
     * Shares a payload via Android share intent
     * @param payloadId ID of the payload to share
     */
    suspend fun sharePayload(payloadId: String)
    
    /**
     * Uploads a payload to Firebase Storage
     * @param payloadId ID of the payload to upload
     * @return Download URL of the uploaded payload
     */
    suspend fun uploadPayloadToFirebase(payloadId: String): String
}
