package com.Azelmods.App.ui.screens.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.domain.usecase.auth.LoginUseCase
import com.Azelmods.App.util.Resource
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val googleLoginUseCase: com.Azelmods.App.domain.usecase.auth.GoogleLoginUseCase,
    private val credentialManager: CredentialManager,
    private val googleClientId: String,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }
    
    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }
    
    // MANUAL: Run: ./gradlew signingReport
    // Copy the SHA-1 and add it in Firebase Console
    // → Project Settings → Your App → Add fingerprint
    
    fun signInWithGoogle() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(googleClientId)
                    .setAutoSelectEnabled(false)
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                
                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    // Call GoogleLoginUseCase to authenticate with Firebase
                    when (val loginResult = googleLoginUseCase(idToken)) {
                        is Resource.Success -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                isSuccess = true
                            )
                        }
                        is Resource.Error -> {
                            android.util.Log.e("GoogleSignIn", "FULL ERROR: ${loginResult.message}")
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "Google Sign-In failed: ${loginResult.message}"
                            )
                        }
                        is Resource.Loading -> {
                            // Already handled
                        }
                    }
                } else {
                    android.util.Log.e("GoogleSignIn", "FULL ERROR: Invalid credential type: ${credential::class.java.simpleName}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Google Sign-In failed: Invalid credential type"
                    )
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                // User cancelled, don't show error
                android.util.Log.d("GoogleSignIn", "User cancelled Google Sign-In")
                _state.value = _state.value.copy(
                    error = null,
                    isLoading = false
                )
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                android.util.Log.e("GoogleSignIn", "FULL ERROR: NoCredentialException: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "No Google account found. Add a Google account in device Settings."
                )
            } catch (e: GetCredentialException) {
                android.util.Log.e("GoogleSignIn", "FULL ERROR: GetCredentialException: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Google Sign-In unavailable: ${e.message}"
                )
            } catch (e: Exception) {
                android.util.Log.e("GoogleSignIn", "FULL ERROR: ${e::class.java.simpleName}: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }
    
    fun login() {
        val currentState = _state.value
        
        // Validation
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
        
        viewModelScope.launch {
            _state.value = currentState.copy(isLoading = true, error = null)
            
            when (val result = loginUseCase(currentState.email, currentState.password)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message ?: "Login failed"
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
}
