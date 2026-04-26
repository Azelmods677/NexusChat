package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Use case for stopping the Tor service
 * 
 * This use case encapsulates the business logic for disabling Anonymous Mode
 * by stopping the Tor service and cleaning up resources.
 * 
 * @param securityRepository Repository for security operations
 */
class StopTorUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    /**
     * Stops the Tor service and cleans up all resources
     */
    suspend operator fun invoke() {
        securityRepository.stopTorService()
    }
}
