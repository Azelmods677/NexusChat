package com.Azelmods.App.ui.screens.auth

import com.Azelmods.App.i18n.tr

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.security.tor.AnonymityPolicy
import com.Azelmods.App.data.security.tor.TorPreferences
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.domain.usecase.auth.GoogleLoginUseCase
import com.Azelmods.App.domain.usecase.auth.LoginUseCase
import com.Azelmods.App.util.Resource
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val isSuccess: Boolean = false,
    val anonymousModeOn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val torPreferences: TorPreferences,
    private val torService: Lazy<TorService>
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState(anonymousModeOn = AnonymityPolicy.active))
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
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
                        error = tr("No se pudo iniciar sesión") + ": ${result.message}\n" + com.Azelmods.App.data.repository.UPDATE_HINT()
                    )
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun login() {
        val current = _state.value
        val problem = when {
            current.email.isBlank() -> tr("Escribe tu correo")
            !current.email.contains("@") -> tr("Ese correo no parece válido")
            current.password.isBlank() -> tr("Escribe tu contraseña")
            current.password.length < 6 -> tr("La contraseña tiene al menos 6 caracteres")
            else -> null
        }
        if (problem != null) {
            _state.value = current.copy(error = problem)
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)
            when (val result = loginUseCase(current.email, current.password)) {
                is Resource.Success -> onSignedIn()
                is Resource.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = (result.message ?: tr("No se pudo iniciar sesión")) + "\n" + com.Azelmods.App.data.repository.UPDATE_HINT()
                )
                is Resource.Loading -> Unit
            }
        }
    }

    /**
     * Envía el enlace para crear una contraseña nueva. Sirve también para quien entró
     * siempre con Google y nunca llegó a tener una.
     */
    fun sendPasswordReset() {
        val email = _state.value.email.trim()
        if (!email.contains("@")) {
            _state.value = _state.value.copy(error = tr("Escribe tu correo arriba y vuelve a tocar aquí"), notice = null)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, notice = null)
            _state.value = runCatching {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            }.fold(
                onSuccess = {
                    _state.value.copy(
                        isLoading = false,
                        notice = tr("Listo. Si ese correo tiene una cuenta, te llegó un enlace para crear tu contraseña nueva. Mira también en spam.")
                    )
                },
                onFailure = {
                    _state.value.copy(isLoading = false, error = tr("No se pudo enviar el enlace. Revisa el correo y tu conexión."))
                }
            )
        }
    }

    private suspend fun onSignedIn() {
        // Sin el token FCM no llegarían las notificaciones de mensajes ni de llamadas.
        runCatching { com.Azelmods.App.utils.FCMTokenManager.saveFCMToken() }
        _state.value = _state.value.copy(isLoading = false, isSuccess = true)
    }

    fun disableAnonymousMode() {
        torPreferences.setAnonymousModeEnabled(false)
        torService.get().stopTor()
        _state.value = _state.value.copy(anonymousModeOn = false, error = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
