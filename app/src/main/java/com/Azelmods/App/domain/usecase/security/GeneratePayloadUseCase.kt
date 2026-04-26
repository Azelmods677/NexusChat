package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.data.security.payload.PayloadConfig
import com.Azelmods.App.data.security.payload.PayloadGenerationState
import com.Azelmods.App.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for generating payloads
 * 
 * Requirements: 36.1, 36.2
 */
class GeneratePayloadUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(config: PayloadConfig): Flow<PayloadGenerationState> {
        return securityRepository.generatePayload(config)
    }
}
