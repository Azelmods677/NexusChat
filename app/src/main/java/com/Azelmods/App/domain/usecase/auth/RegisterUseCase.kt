package com.Azelmods.App.domain.usecase.auth

import com.Azelmods.App.data.model.User
import com.Azelmods.App.data.repository.AuthRepository
import com.Azelmods.App.util.Resource
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): Resource<User> {
        require(email.isNotBlank() && email.contains("@")) { "Invalid email" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
        require(displayName.isNotBlank()) { "Display name cannot be blank" }
        
        return authRepository.register(email, password, displayName)
    }
}
