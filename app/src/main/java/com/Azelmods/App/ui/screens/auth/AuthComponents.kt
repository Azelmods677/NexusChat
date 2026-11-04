package com.Azelmods.App.ui.screens.auth

import com.Azelmods.App.i18n.tr

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.Azelmods.App.ui.components.NexusNotice
import com.Azelmods.App.ui.screens.splash.drawNexusEmblem
import com.Azelmods.App.ui.theme.NexusTokens

/** Marca en las pantallas de entrada. */
@Composable
fun AuthBrand(subtitle: String, modifier: Modifier = Modifier) {
    // El mismo emblema de la animación de entrada, trazándose al llegar a la pantalla.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1_500, easing = LinearEasing)) }
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(104.dp)) { drawNexusEmblem(progress.value, accent) }
        Spacer(Modifier.height(10.dp))
        Text(tr("NexusChat"), style = MaterialTheme.typography.headlineMedium, color = NexusTokens.Color.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextMuted)
    }
}

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = NexusTokens.Color.Hairline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = NexusTokens.Color.TextMuted,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = NexusTokens.Color.TextPrimary,
    unfocusedTextColor = NexusTokens.Color.TextPrimary,
    focusedContainerColor = NexusTokens.Color.BgSurface,
    unfocusedContainerColor = NexusTokens.Color.BgSurface,
    focusedLeadingIconColor = NexusTokens.Color.TextSecondary,
    unfocusedLeadingIconColor = NexusTokens.Color.TextMuted,
    focusedTrailingIconColor = NexusTokens.Color.TextSecondary,
    unfocusedTrailingIconColor = NexusTokens.Color.TextMuted
)

@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) tr("Ocultar contraseña") else tr("Mostrar contraseña")
                    )
                }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = RoundedCornerShape(NexusTokens.Radius.md),
        colors = authFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AuthOutlinedAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(NexusTokens.Radius.md),
        border = BorderStroke(1.dp, NexusTokens.Color.BgHigh),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NexusTokens.Color.BgSurface,
            contentColor = NexusTokens.Color.TextPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Letra de Google a modo de icono, sin reproducir su logotipo. */
@Composable
fun GoogleMark() {
    Text("G", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun AuthDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = NexusTokens.Color.Hairline)
        Text(
            "o",
            color = NexusTokens.Color.TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = NexusTokens.Color.Hairline)
    }
}

@Composable
fun AuthError(message: String?) {
    if (message != null) {
        NexusNotice(text = message, tone = NexusTokens.Color.Error, icon = Icons.Outlined.Info)
    }
}

/**
 * Con el modo anónimo activo la sesión pasa por Tor, que puede estar conectando todavía.
 * Sin esta salida, alguien que no pudiera entrar no tendría cómo apagarlo.
 */
@Composable
fun AnonymousModeLoginNotice(onDisable: () -> Unit) {
    NexusNotice(
        text = tr("El modo anónimo está activo y puede tardar en conectar. Si no consigues entrar, desactívalo."),
        icon = Icons.Outlined.Info,
        tone = NexusTokens.Color.Warning,
        actionLabel = tr("Desactivar modo anónimo"),
        onAction = onDisable
    )
}
