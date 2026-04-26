package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use case for getting the current Tor connection state
 * 
 * This use case provides access to the current state of the Tor service,
 * allowing the UI to reactively display connection status.
 * 
 * @param securityRepository Repository for security operations
 */
class GetTorStateUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    /**
     * Gets the current Tor connection state as a StateFlow
     * 
     * @return StateFlow<TorState> that emits the current state and updates
     */
    operator fun invoke(): StateFlow<TorState> {
        return securityRepository.getTorState()
    }
}
