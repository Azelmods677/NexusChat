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
                // Try with authorized accounts first for better UX
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true) // First try with saved accounts
                    .setServerClientId(googleClientId)
                    .setAutoSelectEnabled(true) // Auto-select if only one account
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                val result = try {
                    credentialManager.getCredential(
                        request = request,
                        context = context
                    )
                } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                    // No authorized accounts, try again without filter
                    android.util.Log.d("GoogleSignIn", "No authorized accounts, trying all accounts")
                    val googleIdOptionAll = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false) // Show all Google accounts
                        .setServerClientId(googleClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    
                    val requestAll = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOptionAll)
                        .build()
                    
                    credentialManager.getCredential(
                        request = requestAll,
                        context = context
                    )
                }
                
                val credential = result.credential
                
                // Try to handle GoogleIdTokenCredential
                val googleIdTokenCredential = try {
                    GoogleIdTokenCredential.createFrom(credential.data)
                } catch (e: Exception) {
                    android.util.Log.e("GoogleSignIn", "Failed to create GoogleIdTokenCredential: ${e.message}")
                    android.util.Log.e("GoogleSignIn", "Credential type: ${credential.type}")
                    android.util.Log.e("GoogleSignIn", "Credential class: ${credential::class.java.simpleName}")
                    null
                }
                
                if (googleIdTokenCredential != null) {
                    val idToken = googleIdTokenCredential.idToken
                    // Call GoogleLoginUseCase to authenticate with Firebase
                    when (val loginResult = googleLoginUseCase(idToken)) {
                        is Resource.Success -> {
                            // Guardar el token FCM para poder recibir push de mensajes y llamadas.
                            runCatching { com.Azelmods.App.utils.FCMTokenManager.saveFCMToken() }
                            _state.value = _state.value.copy(
                                isLoading = false,
                                isSuccess = true
                            )
                        }
                        is Resource.Error -> {
                            android.util.Log.e("GoogleSignIn", "FULL ERROR: ${loginResult.message}")
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "Error de autenticación: ${loginResult.message}"
                            )
                        }
                        is Resource.Loading -> {
                            // Already handled
                        }
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "No se pudo procesar la respuesta de Google. Intenta de nuevo."
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
                    error = "No se encontró cuenta de Google. Agrega una cuenta en Configuración del dispositivo."
                )
            } catch (e: GetCredentialException) {
                android.util.Log.e("GoogleSignIn", "FULL ERROR: GetCredentialException: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error de Google Sign-In. Verifica tu conexión a internet y que tengas una cuenta de Google activa."
                )
            } catch (e: Exception) {
                android.util.Log.e("GoogleSignIn", "FULL ERROR: ${e::class.java.simpleName}: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.message ?: "Intenta de nuevo"}"
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
                    // Guardar el token FCM para poder recibir push de mensajes y llamadas.
                    runCatching { com.Azelmods.App.utils.FCMTokenManager.saveFCMToken() }
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
