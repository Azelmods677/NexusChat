package com.Azelmods.App.ui.screens.auth

import com.Azelmods.App.i18n.tr

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.domain.usecase.auth.GoogleLoginUseCase
import com.Azelmods.App.domain.usecase.auth.RegisterUseCase
import com.Azelmods.App.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val registerUseCase: RegisterUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val googleIdTokenProvider: GoogleIdTokenProvider
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
        val current = _state.value
        val problem = when {
            current.displayName.isBlank() -> tr("Escribe tu nombre")
            current.displayName.length < 2 -> tr("El nombre tiene al menos 2 caracteres")
            current.email.isBlank() -> tr("Escribe tu correo")
            !current.email.contains("@") -> tr("Ese correo no parece válido")
            current.password.isBlank() -> tr("Escribe una contraseña")
            current.password.length < 6 -> tr("La contraseña debe tener al menos 6 caracteres")
            current.password != current.confirmPassword -> tr("Las contraseñas no coinciden")
            else -> null
        }
        if (problem != null) {
            _state.value = current.copy(error = problem)
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)
            when (val result = registerUseCase(current.email, current.password, current.displayName)) {
                is Resource.Success -> onSignedIn()
                is Resource.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.message ?: tr("No se pudo crear la cuenta")
                )
                is Resource.Loading -> Unit
            }
        }
    }

    /** [activity] debe ser la pantalla: el selector de cuentas se muestra sobre ella. */
    fun signInWithGoogle(activity: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val token = googleIdTokenProvider.request(activity)) {
                is GoogleTokenResult.Cancelled -> _state.value = _state.value.copy(isLoading = false)
                is GoogleTokenResult.Failed -> _state.value = _state.value.copy(isLoading = false, error = token.message)
                is GoogleTokenResult.Token -> when (val result = googleLoginUseCase(token.idToken)) {
                    is Resource.Success -> onSignedIn()
                    is Resource.Error -> _state.value = _state.value.copy(
                        isLoading = false,
                        error = "No se pudo iniciar sesión: ${result.message}"
                    )
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    private suspend fun onSignedIn() {
        // Sin el token FCM no llegarían las notificaciones de mensajes ni de llamadas.
        runCatching { com.Azelmods.App.utils.FCMTokenManager.saveFCMToken() }
        _state.value = _state.value.copy(isLoading = false, isSuccess = true)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
