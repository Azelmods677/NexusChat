package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Use case for enabling obfs4 bridges for censorship bypass
 * 
 * This use case configures the Tor service to use obfs4 pluggable transports,
 * which obfuscate Tor traffic to bypass network censorship and deep packet inspection.
 * 
 * @param securityRepository Repository for security operations
 */
class EnableObfs4BridgesUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    /**
     * Enables obfs4 bridges with the provided bridge addresses
     * 
     * @param bridges List of obfs4 bridge addresses in the format:
     *                "obfs4 IP:PORT FINGERPRINT cert=CERT iat-mode=MODE"
     * @throws IllegalArgumentException if bridge list is empty or format is invalid
     */
    suspend operator fun invoke(bridges: List<String>) {
        require(bridges.isNotEmpty()) { "Bridge list cannot be empty" }
        securityRepository.enableObfs4Bridges(bridges)
    }
}
