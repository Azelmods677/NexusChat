package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for starting the Tor service
 * 
 * This use case encapsulates the business logic for enabling Anonymous Mode
 * by starting the Tor service and establishing a SOCKS5 proxy connection.
 * 
 * @param securityRepository Repository for security operations
 */
class StartTorUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    /**
     * Starts the Tor service and returns a flow of connection states
     * 
     * @return Flow<TorState> emitting bootstrap progress and connection status
     */
    operator fun invoke(): Flow<TorState> {
        return securityRepository.startTorService()
    }
}
