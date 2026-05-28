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
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.ui.theme.Purple

/**
 * Toggle switch for Anonymous Mode with connection status display
 *
 * Displays:
 * - Toggle switch for enabling/disabling Tor
 * - Connection status (Disabled, Connecting X%, Connected, Error)
 * - Disables toggle during bootstrap process
 *
 * Requirements: 17.1, 17.2, 17.3, 17.4, 17.5
 */
@Composable
fun AnonymousModeToggle(
    torState: TorState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = torState is TorState.Connected
    val isConnecting = torState is TorState.Connecting
    val isError = torState is TorState.Error

    // Disable toggle while connecting
    val toggleEnabled = !isConnecting

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
                    StatusText(torState = torState)
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
                        checkedTrackColor = Purple,
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
                        color = Purple,
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

            // Error message
            if (isError) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = (torState as TorState.Error).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Status text based on Tor state
 */
@Composable
private fun StatusText(torState: TorState) {
    val (text, color) = when (torState) {
        is TorState.Disconnected -> {
            "Disabled" to Color.Gray
        }
        is TorState.Connecting -> {
            "Connecting..." to Purple
        }
        is TorState.Bootstrapping -> {
            "Connecting..." to Purple
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
        color = Purple.copy(alpha = alpha),
        fontSize = 14.sp
    )
}
