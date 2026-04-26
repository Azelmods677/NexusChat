package com.Azelmods.App.domain.usecase.auth

import com.Azelmods.App.data.model.User
import com.Azelmods.App.data.repository.AuthRepository
import com.Azelmods.App.util.Resource
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<User> {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(password.isNotBlank()) { "Password cannot be blank" }
        
        return authRepository.login(email, password)
    }
}
