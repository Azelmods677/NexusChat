package com.Azelmods.App.ui.screens.auth

import com.Azelmods.App.i18n.tr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.NexusButton
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Scaffold(containerColor = NexusTokens.Color.BgBase) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            AuthBrand(
                subtitle = tr("Crea tu cuenta"),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            AuthField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = tr("Nombre visible"),
                icon = Icons.Outlined.Person,
                keyboardActions = KeyboardActions(onNext = { emailFocus.requestFocus() })
            )
            AuthField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = tr("Correo"),
                icon = Icons.Outlined.Mail,
                keyboardType = KeyboardType.Email,
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                modifier = Modifier.focusRequester(emailFocus)
            )
            AuthField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = tr("Contraseña"),
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                keyboardActions = KeyboardActions(onNext = { confirmFocus.requestFocus() }),
                isPassword = true,
                modifier = Modifier.focusRequester(passwordFocus)
            )
            AuthField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = tr("Confirmar contraseña"),
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { viewModel.register() }),
                isPassword = true,
                modifier = Modifier.focusRequester(confirmFocus)
            )

            AuthError(state.error)

            NexusButton(
                text = tr("Crear cuenta"),
                onClick = viewModel::register,
                loading = state.isLoading
            )

            AuthDivider()

            AuthOutlinedAction(
                text = tr("Continuar con Google"),
                onClick = { viewModel.signInWithGoogle(context) },
                enabled = !state.isLoading,
                leading = { GoogleMark() }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tr("¿Ya tienes cuenta?"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextMuted
                )
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(tr("Entrar"))
                }
            }
        }
    }
}
