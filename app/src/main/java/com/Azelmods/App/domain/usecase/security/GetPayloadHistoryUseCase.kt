package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.data.security.payload.GeneratedPayload
import com.Azelmods.App.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting payload history
 * 
 * Requirements: 36.1, 36.2
 */
class GetPayloadHistoryUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(): Flow<List<GeneratedPayload>> {
        return securityRepository.getPayloadHistory()
    }
}
