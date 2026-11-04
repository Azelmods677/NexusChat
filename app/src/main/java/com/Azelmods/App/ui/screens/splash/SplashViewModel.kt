package com.Azelmods.App.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.preferences.UserPreferences
import com.Azelmods.App.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashState(
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userPreferences: UserPreferences
) : ViewModel() {

    /** Quien prefiera entrar directo puede apagar la animación en Ajustes. */
    val animated: Boolean = userPreferences.splashAnimation.value

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        // La espera la marca la animación de la pantalla, no un retardo fijo aquí.
        viewModelScope.launch {
            _state.value = SplashState(
                isLoading = false,
                isAuthenticated = authRepository.isUserAuthenticated()
            )
        }
    }
}
