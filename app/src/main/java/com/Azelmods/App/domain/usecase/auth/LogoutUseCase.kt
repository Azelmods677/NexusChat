package com.Azelmods.App.domain.usecase.auth

import com.Azelmods.App.data.repository.AuthRepository
import com.Azelmods.App.util.Resource
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return authRepository.logout()
    }
}
