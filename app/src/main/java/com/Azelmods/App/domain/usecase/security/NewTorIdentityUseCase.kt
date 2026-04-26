package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Use case for creating a new Tor identity
 * 
 * This use case requests a new Tor circuit, effectively changing the exit node
 * and providing a fresh identity for enhanced anonymity. This is useful when
 * the user wants to appear as a different user or from a different location.
 * 
 * @param securityRepository Repository for security operations
 */
class NewTorIdentityUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    /**
     * Creates a new Tor identity by establishing a new circuit
     * 
     * This will change the entry, middle, and exit nodes, providing
     * a completely new path through the Tor network.
     */
    suspend operator fun invoke() {
        securityRepository.newIdentity()
    }
}
