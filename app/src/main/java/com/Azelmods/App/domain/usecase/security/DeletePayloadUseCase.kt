package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Use case for deleting payloads
 * 
 * Requirements: 36.1, 36.2
 */
class DeletePayloadUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(payloadId: String) {
        securityRepository.deletePayload(payloadId)
    }
}
