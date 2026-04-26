package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Use case for sharing payloads
 * 
 * Requirements: 36.1, 36.2
 */
class SharePayloadUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(payloadId: String) {
        securityRepository.sharePayload(payloadId)
    }
}
