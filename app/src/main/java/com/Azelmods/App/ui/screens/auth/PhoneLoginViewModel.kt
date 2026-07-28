package com.Azelmods.App.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.domain.usecase.auth.PhoneLoginUseCase
import com.Azelmods.App.util.Resource
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
// Vive en com.google.firebase, no en com.google.firebase.auth: la lanza el
// backend de Firebase (limite de SMS por dispositivo/numero), no el modulo Auth.
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Paso del flujo de acceso por teléfono. */
enum class PhoneAuthStep { PHONE, CODE }

data class PhoneLoginState(
    val countryCode: String = "+57",
    val phoneNumber: String = "",
    val code: String = "",
    val step: PhoneAuthStep = PhoneAuthStep.PHONE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val isSuccess: Boolean = false,
    /** Segundos que faltan para poder reenviar el SMS. 0 = se puede reenviar. */
    val resendCooldown: Int = 0
) {
    /** Número en formato E.164, que es el único que Firebase acepta. */
    val fullNumber: String
        get() = countryCode.trim() + phoneNumber.filter { it.isDigit() }

    val canSubmitPhone: Boolean
        get() = !isLoading && phoneNumber.filter { it.isDigit() }.length >= 6 &&
            countryCode.startsWith("+") && countryCode.length >= 2

    val canSubmitCode: Boolean
        get() = !isLoading && code.length == CODE_LENGTH

    companion object {
        const val CODE_LENGTH = 6
    }
}

/**
 * Acceso con número de teléfono mediante SMS.
 *
 * Firebase exige una `Activity` para `verifyPhoneNumber` (necesita adjuntar el
 * reCAPTCHA y el auto-relleno del SMS), así que la pantalla la pasa en cada
 * llamada en lugar de guardarla aquí: retener una Activity en un ViewModel la
 * mantendría viva más allá de su ciclo de vida.
 */
@HiltViewModel
class PhoneLoginViewModel @Inject constructor(
    private val phoneLoginUseCase: PhoneLoginUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(PhoneLoginState())
    val state: StateFlow<PhoneLoginState> = _state.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var cooldownJob: Job? = null

    fun onCountryCodeChange(value: String) {
        // Se fuerza el "+" inicial: sin él Firebase rechaza el número y el
        // mensaje que devuelve no explica que falta el prefijo internacional.
        val cleaned = "+" + value.filter { it.isDigit() }.take(4)
        _state.value = _state.value.copy(countryCode = cleaned, error = null)
    }

    fun onPhoneNumberChange(value: String) {
        _state.value = _state.value.copy(
            phoneNumber = value.filter { it.isDigit() }.take(15),
            error = null
        )
    }

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(PhoneLoginState.CODE_LENGTH)
        _state.value = _state.value.copy(code = digits, error = null)
    }

    fun backToPhoneStep() {
        cooldownJob?.cancel()
        _state.value = _state.value.copy(
            step = PhoneAuthStep.PHONE,
            code = "",
            error = null,
            info = null,
            resendCooldown = 0
        )
    }

    /** Pide el SMS. [resend] fuerza un envío nuevo reusando el token de reenvío. */
    fun sendVerificationCode(activity: Activity, resend: Boolean = false) {
        val current = _state.value
        if (!resend && !current.canSubmitPhone) return

        _state.value = current.copy(isLoading = true, error = null, info = null)

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(current.fullNumber)
            .setTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resend) {
            resendToken?.let { builder.setForceResendingToken(it) }
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    /** Comprueba el código que ha escrito el usuario. */
    fun verifyCode() {
        val current = _state.value
        if (!current.canSubmitCode) return
        val id = verificationId
        if (id == null) {
            _state.value = current.copy(error = "Pide el código otra vez, por favor.")
            return
        }
        _state.value = current.copy(isLoading = true, error = null)
        signIn(PhoneAuthProvider.getCredential(id, current.code))
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        /**
         * Verificación instantánea: en algunos dispositivos Android lee el SMS o
         * reconoce el número sin que el usuario escriba nada.
         */
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            credential.smsCode?.let { code -> _state.value = _state.value.copy(code = code) }
            signIn(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            android.util.Log.e("PhoneLogin", "verifyPhoneNumber falló", e)
            val raw = e.message.orEmpty()
            // La causa nº1 de "el SMS no llega nunca" NO es el número: es que el
            // proyecto Firebase no puede verificar la app (falta la huella SHA-256
            // del certificado de firma en la consola, o Play Integrity/reCAPTCHA no
            // se completó). Firebase devuelve estos fallos con textos reconocibles;
            // los traducimos a algo que el desarrollador pueda accionar en vez de un
            // genérico "no se pudo enviar".
            val looksLikeAppVerification = raw.contains("app verification", ignoreCase = true) ||
                raw.contains("Integrity", ignoreCase = true) ||
                raw.contains("reCAPTCHA", ignoreCase = true) ||
                raw.contains("SafetyNet", ignoreCase = true) ||
                raw.contains("missing", ignoreCase = true) && raw.contains("SHA", ignoreCase = true)

            _state.value = _state.value.copy(
                isLoading = false,
                error = when {
                    e is FirebaseAuthInvalidCredentialsException ->
                        "El número no es válido. Revisa el prefijo del país y los dígitos."
                    e is FirebaseTooManyRequestsException ->
                        "Demasiados intentos desde este dispositivo. Espera un rato antes de volver a probar."
                    looksLikeAppVerification ->
                        "Verificación de la app rechazada por Firebase. Registra la huella SHA-256 " +
                        "de tu certificado de firma en la consola (Authentication → Sign-in → Teléfono) " +
                        "y descarga de nuevo google-services.json."
                    else -> raw.ifBlank { "No se pudo enviar el SMS" }
                }
            )
        }

        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = id
            resendToken = token
            _state.value = _state.value.copy(
                isLoading = false,
                step = PhoneAuthStep.CODE,
                info = "Te hemos enviado un código a ${_state.value.fullNumber}",
                error = null
            )
            startResendCooldown()
        }
    }

    private fun signIn(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            when (val result = phoneLoginUseCase(credential)) {
                is Resource.Success -> {
                    runCatching { com.Azelmods.App.utils.FCMTokenManager.saveFCMToken() }
                    _state.value = _state.value.copy(isLoading = false, isSuccess = true, error = null)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message ?: "No se pudo iniciar sesión"
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    /**
     * Cuenta atrás antes de permitir reenviar. Sin ella el usuario pulsa
     * "Reenviar" varias veces seguidas y Firebase acaba bloqueando el número por
     * abuso, que es un error del que no se sale en horas.
     */
    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in RESEND_COOLDOWN_SECONDS downTo 1) {
                _state.value = _state.value.copy(resendCooldown = remaining)
                delay(1_000)
            }
            _state.value = _state.value.copy(resendCooldown = 0)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }

    private companion object {
        const val TIMEOUT_SECONDS = 60L
        const val RESEND_COOLDOWN_SECONDS = 45
    }
}
