package com.Azelmods.App.ui.screens.security

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.data.security.tor.OrbotState
import com.Azelmods.App.data.security.tor.OrbotUiStatus
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.ui.theme.Warning

/**
 * Toggle switch for Anonymous Mode with connection status display
 *
 * Displays:
 * - Toggle switch for enabling/disabling Tor
 * - Connection status (Disabled, Connecting X%, Connected, Error)
 * - Disables toggle during bootstrap process
 *
 * Cuando se proporciona [orbotStatus] (derivado de `OrbotDetector` vía
 * `mapOrbotStatus`), el estado de Orbot se presenta de forma clara y accionable
 * y el toggle NUNCA queda en un estado de error permanente: ante un
 * `TorState.Error` se muestra el mensaje/acción de Orbot y el toggle vuelve a un
 * estado consistente pero operable para reintentar (degradación elegante).
 *
 * Requirements: 6.4, 7.1, 7.3, 17.1, 17.2, 17.3, 17.4, 17.5
 */
@Composable
fun AnonymousModeToggle(
    torState: TorState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    orbotStatus: OrbotUiStatus? = null,
    onOrbotAction: (() -> Unit)? = null
) {
    val isEnabled = torState is TorState.Connected
    val isConnecting = torState is TorState.Connecting
    // Cuando hay estado de Orbot accionable lo presentamos en lugar del error
    // duro de Tor, de modo que el toggle nunca aparezca "roto".
    val isError = torState is TorState.Error && orbotStatus == null

    // El toggle se deshabilita sólo mientras conecta. Si hay estado de Orbot,
    // respetamos `toggleEnabled` (siempre operable) para permitir reintentar.
    val toggleEnabled = !isConnecting && (orbotStatus?.toggleEnabled ?: true)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anonymous Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status text
                    StatusText(torState = torState, orbotStatus = orbotStatus)
                }

                Switch(
                    checked = isEnabled || isConnecting,
                    onCheckedChange = { checked ->
                        if (toggleEnabled) {
                            onToggle(checked)
                        }
                    },
                    enabled = toggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }

            // Progress bar while connecting
            if (isConnecting) {
                Spacer(modifier = Modifier.height(12.dp))

                val connectingState = torState as TorState.Connecting
                val progress = connectingState.progress / 100f

                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (connectingState.message.isNotEmpty())
                            "Connecting: ${connectingState.progress}% – ${connectingState.message}"
                        else
                            "Connecting: ${connectingState.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Error message (sólo cuando no hay estado de Orbot accionable)
            if (isError) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = (torState as TorState.Error).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            // Orbot status message + action (degradación elegante, nunca "roto")
            if (orbotStatus != null && orbotStatus.state != OrbotState.ACTIVE) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = orbotStatus.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                val actionLabel = orbotStatus.actionLabel
                if (actionLabel != null && onOrbotAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onOrbotAction) {
                        Text(text = actionLabel, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Status text based on Tor state.
 *
 * Cuando se proporciona [orbotStatus] y Tor está en error, mostramos un estado
 * derivado del estado real de Orbot (no un "Error" permanente).
 */
@Composable
private fun StatusText(torState: TorState, orbotStatus: OrbotUiStatus? = null) {
    // Si Tor reporta error pero tenemos un estado de Orbot accionable, lo
    // presentamos como estado informativo en lugar de "Error".
    if (torState is TorState.Error && orbotStatus != null) {
        val (text, color) = when (orbotStatus.state) {
            OrbotState.ACTIVE -> "Connected" to Color.Green
            OrbotState.INSTALLED_INACTIVE -> "Orbot inactivo" to Warning
            OrbotState.NOT_INSTALLED -> "Orbot no instalado" to Warning
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontSize = 14.sp
        )
        return
    }

    val (text, color) = when (torState) {
        is TorState.Disconnected -> {
            "Disabled" to Color.Gray
        }
        is TorState.Connecting -> {
            "Connecting..." to MaterialTheme.colorScheme.primary
        }
        is TorState.Bootstrapping -> {
            "Connecting..." to MaterialTheme.colorScheme.primary
        }
        is TorState.Connected -> {
            "Connected" to Color.Green
        }
        is TorState.Error -> {
            "Error" to MaterialTheme.colorScheme.error
        }
    }

    // Animated dot for connecting state
    if (torState is TorState.Connecting) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            AnimatedConnectingDots()
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontSize = 14.sp
        )
    }
}

/**
 * Animated dots for connecting state
 */
@Composable
private fun AnimatedConnectingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Text(
        text = "...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        fontSize = 14.sp
    )
}
