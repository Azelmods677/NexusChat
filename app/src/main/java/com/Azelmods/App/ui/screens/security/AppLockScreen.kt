package com.Azelmods.App.ui.screens.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.security.AppLockManager
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AppLockScreen - Pantalla de desbloqueo con PIN y biometría
 * 
 * Features:
 * - Entrada de PIN numérico (4-6 dígitos)
 * - Autenticación biométrica (huella/Face ID)
 * - Animación de error al ingresar PIN incorrecto
 * - Indicadores visuales de PIN ingresado
 * - Teclado numérico personalizado
 * 
 * Requirements: 9.1, 9.2, 9.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pinValue by viewModel.pin.collectAsState()
    val isErrorValue by viewModel.isError.collectAsState()
    val errorMessageValue by viewModel.errorMessage.collectAsState()
    val isBiometricEnabledValue by viewModel.isBiometricEnabled.collectAsState()
    val requires2fa by viewModel.requires2fa.collectAsState()
    val totpCode by viewModel.totpCode.collectAsState()

    // Paso del segundo factor: el PIN/huella ya se validó y ahora se pide el código.
    if (requires2fa) {
        TwoFactorStep(
            code = totpCode,
            isError = isErrorValue,
            errorMessage = errorMessageValue,
            onCodeChange = viewModel::onTotpChange,
            onVerify = { viewModel.verifyTotp(onUnlocked) }
        )
        return
    }
    
    // Animación de shake para error
    val shakeOffset = remember { Animatable(0f) }
    
    LaunchedEffect(isErrorValue) {
        if (isErrorValue) {
            // Animación de shake
            repeat(3) {
                shakeOffset.animateTo(20f, animationSpec = tween(50))
                shakeOffset.animateTo(-20f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }
    
    // Intentar biometría automáticamente al abrir
    LaunchedEffect(Unit) {
        if (isBiometricEnabledValue) {
            delay(300) // Pequeño delay para que la UI se cargue
            viewModel.authenticateWithBiometrics(context as FragmentActivity, onUnlocked)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            
            // Icono de bloqueo
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Título
            Text(
                text = "Aplicación Bloqueada",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Subtítulo
            Text(
                text = "Ingresa tu PIN para desbloquear",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Indicadores de PIN
            Row(
                modifier = Modifier
                    .offset(x = shakeOffset.value.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(6) { index ->
                    PinIndicator(
                        isFilled = index < pinValue.length,
                        isError = isErrorValue
                    )
                }
            }
            
            // Mensaje de error
            AnimatedVisibility(
                visible = isErrorValue,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessageValue,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Teclado numérico
            NumericKeypad(
                onNumberClick = { number ->
                    viewModel.addDigit(number)
                },
                onBackspaceClick = {
                    viewModel.removeLastDigit()
                },
                onBiometricClick = if (isBiometricEnabledValue) {
                    { viewModel.authenticateWithBiometrics(context as FragmentActivity, onUnlocked) }
                } else null
            )
            
            // Verificar PIN automáticamente cuando tiene 4-6 dígitos
            LaunchedEffect(pinValue) {
                if (pinValue.length >= 4) {
                    delay(800) // Delay aumentado para evitar verificación prematura
                    val success = viewModel.verifyPin()
                    if (success) {
                        onUnlocked()
                    }
                }
            }
        }
    }
}

/**
 * Segundo factor: entrada del código TOTP de 6 dígitos. Aparece SOLO después de que
 * el PIN o la huella hayan sido correctos, de modo que exige de verdad los dos
 * factores para desbloquear.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoFactorStep(
    code: String,
    isError: Boolean,
    errorMessage: String,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    // Verifica solo cuando el usuario ha escrito los 6 dígitos.
    LaunchedEffect(code) {
        if (code.length == 6) {
            delay(150)
            onVerify()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Verificación en dos pasos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Introduce el código de 6 dígitos de tu app de autenticación",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                singleLine = true,
                isError = isError,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 28.sp,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                ),
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(visible = isError, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onVerify,
                enabled = code.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Verificar") }
        }
    }
}

@Composable
private fun PinIndicator(
    isFilled: Boolean,
    isError: Boolean
) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                when {
                    isError -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> Color.Gray.copy(alpha = 0.3f)
                }
            )
            .then(
                if (!isFilled && !isError) {
                    Modifier.border(1.dp, Color.Gray, CircleShape)
                } else Modifier
            )
    )
}

@Composable
private fun NumericKeypad(
    onNumberClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Filas 1-3
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                for (col in 1..3) {
                    val number = row * 3 + col
                    NumericKey(
                        text = number.toString(),
                        onClick = { onNumberClick(number) }
                    )
                }
            }
        }
        
        // Fila 4: Biometría / 0 / Backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Botón de biometría (si está habilitado)
            if (onBiometricClick != null) {
                IconKey(
                    icon = Icons.Default.Fingerprint,
                    onClick = onBiometricClick
                )
            } else {
                Spacer(modifier = Modifier.size(72.dp))
            }
            
            // Botón 0
            NumericKey(
                text = "0",
                onClick = { onNumberClick(0) }
            )
            
            // Botón backspace
            IconKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                onClick = onBackspaceClick
            )
        }
    }
}

@Composable
private fun NumericKey(
    text: String,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale.value)
            .clip(CircleShape)
            .background(DarkSurface)
            .clickable {
                scope.launch {
                    scale.animateTo(0.9f, animationSpec = tween(50))
                    scale.animateTo(1f, animationSpec = tween(50))
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 28.sp
        )
    }
}

@Composable
private fun IconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale.value)
            .clip(CircleShape)
            .background(DarkSurface)
            .clickable {
                scope.launch {
                    scale.animateTo(0.9f, animationSpec = tween(50))
                    scale.animateTo(1f, animationSpec = tween(50))
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}

// ViewModel
@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager
) : ViewModel() {
    
    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin
    
    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage
    
    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    // Segundo factor: cuando el PIN/huella es correcto pero el 2FA está activo, el
    // desbloqueo NO se concede todavía; la pantalla pasa a pedir el código TOTP.
    private val _requires2fa = MutableStateFlow(false)
    val requires2fa: StateFlow<Boolean> = _requires2fa

    private val _totpCode = MutableStateFlow("")
    val totpCode: StateFlow<String> = _totpCode

    private var twoFactorEnabled = false

    init {
        viewModelScope.launch {
            _isBiometricEnabled.value = appLockManager.isBiometricEnabled()
            twoFactorEnabled = appLockManager.is2faEnabled()
        }
    }

    fun onTotpChange(value: String) {
        _totpCode.value = value.filter { it.isDigit() }.take(6)
        _isError.value = false
    }
    
    fun addDigit(digit: Int) {
        if (_pin.value.length < 6) {
            _pin.value += digit.toString()
            _isError.value = false
        }
    }
    
    fun removeLastDigit() {
        if (_pin.value.isNotEmpty()) {
            _pin.value = _pin.value.dropLast(1)
            _isError.value = false
        }
    }
    
    /**
     * Verifica el PIN. Devuelve `true` SOLO si el desbloqueo es total. Si el PIN es
     * correcto pero el 2FA está activo, no desbloquea: activa [requires2fa] para que
     * la pantalla pida el código TOTP, y devuelve `false`.
     */
    suspend fun verifyPin(): Boolean {
        val isCorrect = appLockManager.verifyPin(_pin.value)

        return if (isCorrect) {
            _pin.value = ""
            _isError.value = false
            if (twoFactorEnabled) {
                _requires2fa.value = true
                false
            } else {
                appLockManager.unlock()
                true
            }
        } else {
            _isError.value = true
            _errorMessage.value = "PIN incorrecto. Inténtalo de nuevo."
            viewModelScope.launch {
                delay(1500)
                _isError.value = false
                _pin.value = ""
            }
            false
        }
    }

    /** Comprueba el segundo factor. Desbloquea (y llama a [onSuccess]) solo si es válido. */
    fun verifyTotp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (appLockManager.verifyTotp(_totpCode.value)) {
                appLockManager.unlock()
                _totpCode.value = ""
                _requires2fa.value = false
                _isError.value = false
                onSuccess()
            } else {
                _isError.value = true
                _errorMessage.value = "Código de verificación incorrecto."
                launch {
                    delay(1500)
                    _isError.value = false
                    _totpCode.value = ""
                }
            }
        }
    }
    
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
                val biometricPrompt = androidx.biometric.BiometricPrompt(
                    activity,
                    executor,
                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: androidx.biometric.BiometricPrompt.AuthenticationResult
                        ) {
                            viewModelScope.launch {
                                // La huella es el primer factor: si hay 2FA, todavía
                                // falta el código TOTP antes de conceder el acceso.
                                if (twoFactorEnabled) {
                                    _requires2fa.value = true
                                } else {
                                    appLockManager.unlock()
                                    onSuccess()
                                }
                            }
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            _isError.value = true
                            _errorMessage.value = errString.toString()
                            viewModelScope.launch {
                                delay(2000)
                                _isError.value = false
                            }
                        }
                        
                        override fun onAuthenticationFailed() {
                            _isError.value = true
                            _errorMessage.value = "Autenticación biométrica fallida"
                            viewModelScope.launch {
                                delay(2000)
                                _isError.value = false
                            }
                        }
                    }
                )
                
                val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Desbloquear Aplicación")
                    .setSubtitle("Usa tu huella digital o reconocimiento facial")
                    .setNegativeButtonText("Usar PIN")
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
                
            } catch (e: Exception) {
                _isError.value = true
                _errorMessage.value = "Error al usar biometría"
            }
        }
    }
}
