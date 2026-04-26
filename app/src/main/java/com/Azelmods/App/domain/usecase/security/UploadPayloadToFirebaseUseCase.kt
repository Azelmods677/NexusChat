package com.Azelmods.App.domain.usecase.security

import com.Azelmods.App.domain.repository.SecurityRepository
import javax.inject.Inject

/**
 * Use case for uploading payloads to Firebase
 * 
 * Requirements: 36.1, 36.2
 */
class UploadPayloadToFirebaseUseCase @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(payloadId: String): String {
        return securityRepository.uploadPayloadToFirebase(payloadId)
    }
}
