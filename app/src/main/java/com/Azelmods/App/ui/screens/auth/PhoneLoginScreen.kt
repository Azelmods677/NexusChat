package com.Azelmods.App.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.NexusButton
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens

/**
 * Acceso con número de teléfono.
 *
 * Dos pasos en una sola pantalla (número → código) en lugar de dos destinos de
 * navegación: volver atrás desde el código debe llevar al número sin perder lo
 * escrito, y con dos rutas eso obliga a subir el estado o a duplicarlo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(
    navController: NavController,
    viewModel: PhoneLoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = NexusTokens.Color.BgBase,
        topBar = {
            TopAppBar(
                title = { Text("Entrar con teléfono") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.step == PhoneAuthStep.CODE) viewModel.backToPhoneStep()
                            else navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = NexusTokens.Color.TextPrimary,
                    navigationIconContentColor = NexusTokens.Color.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = NexusTokens.Space.lg)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(NexusTokens.Space.xl))

            Icon(
                imageVector = if (state.step == PhoneAuthStep.PHONE) Icons.Default.Phone else Icons.Default.Sms,
                contentDescription = null,
                tint = NexusTokens.Color.Primary,
                modifier = Modifier.size(NexusTokens.IconSize.xl)
            )

            Spacer(Modifier.height(NexusTokens.Space.md))

            Text(
                text = if (state.step == PhoneAuthStep.PHONE) {
                    "Escribe tu número"
                } else {
                    "Escribe el código"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NexusTokens.Color.TextPrimary
            )

            Spacer(Modifier.height(NexusTokens.Space.sm))

            Text(
                text = if (state.step == PhoneAuthStep.PHONE) {
                    "Te enviaremos un SMS con un código de ${PhoneLoginState.CODE_LENGTH} dígitos. Pueden aplicarse tarifas de tu operador."
                } else {
                    state.info ?: "Revisa tus mensajes"
                },
                fontSize = 14.sp,
                color = NexusTokens.Color.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(NexusTokens.Space.xl))

            when (state.step) {
                PhoneAuthStep.PHONE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.sm)
                    ) {
                        OutlinedTextField(
                            value = state.countryCode,
                            onValueChange = viewModel::onCountryCodeChange,
                            label = { Text("País") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.width(110.dp),
                            shape = RoundedCornerShape(NexusTokens.Radius.md),
                            colors = nexusFieldColors()
                        )
                        OutlinedTextField(
                            value = state.phoneNumber,
                            onValueChange = viewModel::onPhoneNumberChange,
                            label = { Text("Número de teléfono") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { activity?.let { viewModel.sendVerificationCode(it) } }
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(NexusTokens.Radius.md),
                            colors = nexusFieldColors()
                        )
                    }

                    Spacer(Modifier.height(NexusTokens.Space.md))

                    Text(
                        text = "Se enviará a ${state.fullNumber}",
                        fontSize = 13.sp,
                        color = NexusTokens.Color.TextMuted
                    )

                    Spacer(Modifier.height(NexusTokens.Space.lg))

                    NexusButton(
                        text = "Enviar código",
                        onClick = {
                            if (activity == null) return@NexusButton
                            viewModel.sendVerificationCode(activity)
                        },
                        loading = state.isLoading,
                        enabled = state.canSubmitPhone && activity != null
                    )
                }

                PhoneAuthStep.CODE -> {
                    OutlinedTextField(
                        value = state.code,
                        onValueChange = viewModel::onCodeChange,
                        label = { Text("Código de ${PhoneLoginState.CODE_LENGTH} dígitos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { viewModel.verifyCode() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(NexusTokens.Radius.md),
                        colors = nexusFieldColors()
                    )

                    Spacer(Modifier.height(NexusTokens.Space.lg))

                    NexusButton(
                        text = "Verificar y entrar",
                        onClick = viewModel::verifyCode,
                        loading = state.isLoading,
                        enabled = state.canSubmitCode
                    )

                    Spacer(Modifier.height(NexusTokens.Space.md))

                    TextButton(
                        onClick = {
                            activity?.let { viewModel.sendVerificationCode(it, resend = true) }
                        },
                        enabled = state.resendCooldown == 0 && !state.isLoading && activity != null
                    ) {
                        Text(
                            text = if (state.resendCooldown > 0) {
                                "Reenviar en ${state.resendCooldown} s"
                            } else {
                                "Reenviar código"
                            },
                            color = if (state.resendCooldown > 0) {
                                NexusTokens.Color.TextMuted
                            } else {
                                NexusTokens.Color.Primary
                            }
                        )
                    }
                }
            }

            state.error?.let { message ->
                Spacer(Modifier.height(NexusTokens.Space.md))
                Surface(
                    color = NexusTokens.Color.Error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(NexusTokens.Radius.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        color = NexusTokens.Color.Error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(NexusTokens.Space.md)
                    )
                }
            }

            if (activity == null) {
                Spacer(Modifier.height(NexusTokens.Space.md))
                Text(
                    text = "No se pudo acceder a la ventana de la app; reinicia NexusChat.",
                    color = NexusTokens.Color.Error,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun nexusFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NexusTokens.Color.Primary,
    unfocusedBorderColor = NexusTokens.Color.GlassBorder,
    focusedLabelColor = NexusTokens.Color.Primary,
    unfocusedLabelColor = NexusTokens.Color.TextMuted,
    cursorColor = NexusTokens.Color.Primary,
    focusedTextColor = NexusTokens.Color.TextPrimary,
    unfocusedTextColor = NexusTokens.Color.TextPrimary
)
