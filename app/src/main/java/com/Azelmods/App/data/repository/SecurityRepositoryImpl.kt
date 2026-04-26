package com.Azelmods.App.data.repository

import com.Azelmods.App.data.security.tor.TorCircuitInfo
import com.Azelmods.App.data.security.tor.TorServiceManager
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SecurityRepository that delegates operations
 * to TorServiceManager and PayloadGenerator
 * 
 * This class follows Clean Architecture by implementing the domain layer
 * interface and delegating to data layer components.
 * 
 * @param torServiceManager Manager for Tor service operations
 * @param payloadGenerator Generator for security testing payloads
 */
@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val torServiceManager: TorServiceManager,
    private val payloadGenerator: com.Azelmods.App.data.security.payload.PayloadGenerator
) : SecurityRepository {
    
    override fun startTorService(): Flow<TorState> {
        return torServiceManager.startTor()
    }
    
    override suspend fun stopTorService() {
        torServiceManager.stopTor()
    }
    
    override fun getTorState(): StateFlow<TorState> {
        return torServiceManager.getTorState()
    }
    
    override suspend fun enableObfs4Bridges(bridges: List<String>) {
        torServiceManager.enableObfs4Bridges(bridges)
    }
    
    override suspend fun getCircuitInfo(): TorCircuitInfo? {
        return torServiceManager.getCircuitInfo()
    }
    
    override suspend fun newIdentity() {
        torServiceManager.newIdentity()
    }
    
    // Payload Generator Methods
    override fun generatePayload(config: com.Azelmods.App.data.security.payload.PayloadConfig): Flow<com.Azelmods.App.data.security.payload.PayloadGenerationState> {
        return payloadGenerator.generatePayload(config)
    }
    
    override fun getPayloadHistory(): Flow<List<com.Azelmods.App.data.security.payload.GeneratedPayload>> {
        return payloadGenerator.getPayloadHistory()
    }
    
    override suspend fun deletePayload(payloadId: String) {
        payloadGenerator.deletePayload(payloadId)
    }
    
    override suspend fun sharePayload(payloadId: String) {
        payloadGenerator.sharePayload(payloadId)
    }
    
    override suspend fun uploadPayloadToFirebase(payloadId: String): String {
        return payloadGenerator.uploadPayloadToFirebase(payloadId)
    }
}
