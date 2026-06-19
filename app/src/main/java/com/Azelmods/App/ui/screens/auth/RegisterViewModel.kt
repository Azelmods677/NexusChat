package com.Azelmods.App.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.domain.usecase.auth.RegisterUseCase
import com.Azelmods.App.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class RegisterState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()
    
    fun onDisplayNameChange(displayName: String) {
        _state.value = _state.value.copy(displayName = displayName, error = null)
    }
    
    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }
    
    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }
    
    fun onConfirmPasswordChange(confirmPassword: String) {
        _state.value = _state.value.copy(confirmPassword = confirmPassword, error = null)
    }
    
    fun register() {
        val currentState = _state.value
        
        // Validation
        if (currentState.displayName.isBlank()) {
            _state.value = currentState.copy(error = "Display name is required")
            return
        }
        
        if (currentState.displayName.length < 2) {
            _state.value = currentState.copy(error = "Display name must be at least 2 characters")
            return
        }
        
        if (currentState.email.isBlank()) {
            _state.value = currentState.copy(error = "Email is required")
            return
        }
        
        if (!currentState.email.contains("@")) {
            _state.value = currentState.copy(error = "Invalid email format")
            return
        }
        
        if (currentState.password.isBlank()) {
            _state.value = currentState.copy(error = "Password is required")
            return
        }
        
        if (currentState.password.length < 6) {
            _state.value = currentState.copy(error = "Password must be at least 6 characters")
            return
        }
        
        if (currentState.password != currentState.confirmPassword) {
            _state.value = currentState.copy(error = "Passwords don't match")
            return
        }
        
        viewModelScope.launch {
            _state.value = currentState.copy(isLoading = true, error = null)
            
            when (val result = registerUseCase(
                currentState.email,
                currentState.password,
                currentState.displayName
            )) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message ?: "Registration failed"
                    )
                }
                is Resource.Loading -> {
                    // Already handled
                }
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun registerWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential).await()
                _state.value = _state.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Google Sign-In failed: ${e.message}")
            }
        }
    }
}
