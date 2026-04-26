package com.Azelmods.App.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class PhoneAuthState(
    val countryCode: String = "+1",
    val phoneNumber: String = "",
    val verificationCode: String = "",
    val codeSent: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _state = MutableStateFlow(PhoneAuthState())
    val state: StateFlow<PhoneAuthState> = _state.asStateFlow()
    
    private var verificationId: String? = null
    
    fun onCountryCodeChange(code: String) {
        _state.value = _state.value.copy(countryCode = code, error = null)
    }
    
    fun onPhoneNumberChange(number: String) {
        _state.value = _state.value.copy(phoneNumber = number, error = null)
    }
    
    fun onVerificationCodeChange(code: String) {
        if (code.length <= 6 && code.all { it.isDigit() }) {
            _state.value = _state.value.copy(verificationCode = code, error = null)
        }
    }
    
    fun sendVerificationCode() {
        val currentState = _state.value
        
        // Validation
        if (currentState.phoneNumber.isBlank()) {
            _state.value = currentState.copy(error = "Phone number is required")
            return
        }
        
        if (currentState.phoneNumber.length < 10) {
            _state.value = currentState.copy(error = "Invalid phone number")
            return
        }
        
        viewModelScope.launch {
            _state.value = currentState.copy(isLoading = true, error = null)
            
            try {
                // TODO: Implement Firebase Phone Auth
                // For now, simulate sending code
                kotlinx.coroutines.delay(1500)
                
                // Simulate verification ID
                verificationId = "simulated_verification_id"
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    codeSent = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to send code: ${e.message}"
                )
            }
        }
    }
    
    fun verifyCode() {
        val currentState = _state.value
        
        if (currentState.verificationCode.length != 6) {
            _state.value = currentState.copy(error = "Please enter 6-digit code")
            return
        }
        
        if (verificationId == null) {
            _state.value = currentState.copy(error = "Verification ID not found")
            return
        }
        
        viewModelScope.launch {
            _state.value = currentState.copy(isLoading = true, error = null)
            
            try {
                // TODO: Implement Firebase Phone Auth verification
                // For now, simulate verification
                kotlinx.coroutines.delay(1500)
                
                // Simulate successful verification
                _state.value = _state.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Verification failed: ${e.message}"
                )
            }
        }
    }
    
    fun resendCode() {
        _state.value = _state.value.copy(
            codeSent = false,
            verificationCode = "",
            error = null
        )
        sendVerificationCode()
    }
    
    private suspend fun signInWithCredential(credential: PhoneAuthCredential) {
        try {
            auth.signInWithCredential(credential).await()
            _state.value = _state.value.copy(
                isLoading = false,
                isSuccess = true
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Sign in failed: ${e.message}"
            )
        }
    }
}
