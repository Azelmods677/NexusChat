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
    
    init {
        viewModelScope.launch {
            _isBiometricEnabled.value = appLockManager.isBiometricEnabled()
        }
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
    
    suspend fun verifyPin(): Boolean {
        val isCorrect = appLockManager.verifyPin(_pin.value)
        
        return if (isCorrect) {
            appLockManager.unlock()
            _pin.value = ""
            _isError.value = false
            true
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
                                appLockManager.unlock()
                                onSuccess()
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
