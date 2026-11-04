package com.Azelmods.App.ui.screens.premium

import com.Azelmods.App.i18n.tr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.repository.PaymentsUnavailableException
import com.Azelmods.App.data.repository.PremiumPlan
import com.Azelmods.App.data.repository.PremiumRepository
import com.Azelmods.App.data.repository.PremiumStatus
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isLoadingPlans: Boolean = true,
    val plans: List<PremiumPlan> = emptyList(),
    val selectedPlanId: String? = null,
    /** Por qué no se pueden comprar planes ahora mismo; null si todo va bien. */
    val plansError: String? = null,
    val isOpeningCheckout: Boolean = false,
    val checkoutError: String? = null,
    /** Enlace de pago listo para abrir; la pantalla lo consume una vez. */
    val checkoutUrl: String? = null,
    val status: PremiumStatus = PremiumStatus(),
    /** Se acaba de activar con la pantalla abierta: toca celebrarlo. */
    val justActivated: Boolean = false
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repository: PremiumRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PremiumUiState())
    val state: StateFlow<PremiumUiState> = _state.asStateFlow()

    private var hadStatus = false

    init {
        loadPlans()
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                repository.observe(uid).collect { status ->
                    val activatedNow = hadStatus && status.isActiveNow() && !_state.value.status.isActiveNow()
                    hadStatus = true
                    _state.update { it.copy(status = status, justActivated = it.justActivated || activatedNow) }
                }
            }
        }
    }

    fun loadPlans() {
        _state.update { it.copy(isLoadingPlans = true, plansError = null) }
        viewModelScope.launch {
            runCatching { repository.loadPlans() }
                .onSuccess { plans ->
                    _state.update { current ->
                        current.copy(
                            isLoadingPlans = false,
                            plans = plans,
                            selectedPlanId = current.selectedPlanId
                                ?: plans.firstOrNull { !it.badge.isNullOrBlank() }?.id
                                ?: plans.firstOrNull()?.id
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoadingPlans = false, plansError = friendly(error)) }
                }
        }
    }

    fun selectPlan(id: String) = _state.update { it.copy(selectedPlanId = id, checkoutError = null) }

    fun startCheckout() {
        val planId = _state.value.selectedPlanId ?: return
        if (_state.value.isOpeningCheckout) return
        _state.update { it.copy(isOpeningCheckout = true, checkoutError = null) }
        viewModelScope.launch {
            runCatching { repository.createCheckout(planId) }
                .onSuccess { url -> _state.update { it.copy(isOpeningCheckout = false, checkoutUrl = url) } }
                .onFailure { error ->
                    _state.update { it.copy(isOpeningCheckout = false, checkoutError = friendly(error)) }
                }
        }
    }

    fun onCheckoutOpened() = _state.update { it.copy(checkoutUrl = null) }

    fun onCelebrationShown() = _state.update { it.copy(justActivated = false) }

    private fun friendly(error: Throwable): String =
        (error as? PaymentsUnavailableException)?.message ?: tr("No se pudo contactar con el servidor de pagos.")
}
