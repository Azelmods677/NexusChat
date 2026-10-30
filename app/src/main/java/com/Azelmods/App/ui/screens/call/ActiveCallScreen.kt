package com.Azelmods.App.ui.screens.call

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.model.CallType
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.VideoRenderer
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay

/**
 * Pantalla de llamada en curso (emisor y receptor).
 *
 * @param contactId para el emisor, el uid del contacto; para el receptor, el id
 *   de la llamada en Firebase.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActiveCallScreen(
    contactId: String,
    callType: String,
    navController: NavController,
    isCaller: Boolean = true,
    viewModel: CallViewModel = hiltViewModel()
) {
    if (contactId.isEmpty()) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    // El tipo llega en minúsculas desde la navegación interna y en MAYÚSCULAS
    // desde el push (CallType.name). Antes la interfaz comparaba con "audio" a
    // secas y una llamada de voz entrante se pintaba como videollamada vacía.
    val isVideoCall = callType.equals("video", ignoreCase = true)

    // ── Permisos: solo los que la llamada necesita de verdad ──
    val requiredPermissions = remember(isVideoCall) {
        if (isVideoCall) {
            listOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA)
        } else {
            listOf(android.Manifest.permission.RECORD_AUDIO)
        }
    }
    val permissions = rememberMultiplePermissionsState(requiredPermissions)

    LaunchedEffect(Unit) {
        if (!permissions.allPermissionsGranted) permissions.launchMultiplePermissionRequest()
    }

    if (!permissions.allPermissionsGranted) {
        CallPermissionGate(
            isVideoCall = isVideoCall,
            onGrant = { permissions.launchMultiplePermissionRequest() },
            onCancel = {
                // El receptor ya había pulsado "Aceptar": si no da permisos, la
                // llamada se rechaza para que el emisor no se quede sonando.
                if (!isCaller) viewModel.declineCall(contactId)
                navController.popBackStack()
            }
        )
        return
    }

    LaunchedEffect(contactId) {
        val type = if (isVideoCall) CallType.VIDEO else CallType.AUDIO
        if (isCaller) {
            viewModel.startCall(contactId, type)
        } else {
            viewModel.loadContactProfileFromCall(contactId)
            viewModel.acceptCall(contactId, type)
        }
    }

    val contact by viewModel.contactProfile.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val endReason by viewModel.endReason.collectAsState()
    val connectedAt by viewModel.connectedAt.collectAsState()
    val isRelayOnly by viewModel.isRelayOnly.collectAsState()
    val isAudioEnabled by viewModel.isAudioEnabled.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val localVideoTrack by viewModel.localVideoTrack.collectAsState()
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()

    val view = LocalView.current
    var controlsVisible by remember { mutableStateOf(true) }
    var showHangupDialog by remember { mutableStateOf(false) }

    // ── Cronómetro: cuenta desde que el medio conecta, no desde que suena ──
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(connectedAt) {
        while (connectedAt != null) {
            now = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val durationText = connectedAt?.let { formatDuration(((now - it) / 1000).toInt()) }

    val statusText = when {
        endReason != null -> endReason!!.label
        phase == CallPhase.STARTING -> "Preparando llamada…"
        phase == CallPhase.RINGING -> "Llamando…"
        phase == CallPhase.CONNECTING -> "Conectando…"
        phase == CallPhase.RECONNECTING -> "Reconectando…"
        else -> durationText ?: "Conectado"
    }

    // ── Al terminar (por cualquier motivo) se enseña el motivo y se sale ──
    LaunchedEffect(endReason) {
        if (endReason != null) {
            delay(1_300)
            navController.popBackStack()
        }
    }

    // Atrás no deja la llamada huérfana en segundo plano: pregunta.
    BackHandler(enabled = endReason == null) { showHangupDialog = true }

    // Pantalla siempre encendida durante la llamada.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Llamada de voz: apaga la pantalla al acercar el teléfono a la oreja (y evita
    // que la mejilla pulse botones), salvo con el altavoz puesto.
    ProximityScreenOff(enabled = !isVideoCall && !isSpeakerOn && endReason == null)

    // En vídeo, los controles se esconden solos cuando ya hay imagen.
    LaunchedEffect(controlsVisible, phase) {
        if (isVideoCall && controlsVisible && phase == CallPhase.CONNECTED) {
            delay(5_000)
            controlsVisible = false
        }
    }

    val name = contact?.name?.takeIf { it.isNotBlank() } ?: "…"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(NexusTokens.Gradient.Background))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (isVideoCall) controlsVisible = !controlsVisible }
    ) {
        if (isVideoCall && remoteVideoTrack != null) {
            VideoRenderer(
                videoTrack = remoteVideoTrack,
                modifier = Modifier.fillMaxSize(),
                mirror = false
            )
            // Velo inferior para que los controles se lean sobre cualquier imagen.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.45f),
                            0.25f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.6f)
                        )
                    )
            )
        } else {
            CallerHero(
                name = name,
                photoUrl = contact?.photoUrl,
                statusText = statusText,
                pulsing = endReason == null && phase != CallPhase.CONNECTED,
                isVideoCall = isVideoCall
            )
        }

        // ── Cabecera (sobre el vídeo) ──
        if (isVideoCall && remoteVideoTrack != null) {
            AnimatedVisibility(
                visible = controlsVisible || phase != CallPhase.CONNECTED,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(statusText, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
        }

        // ── Vista previa propia (PiP) ──
        if (isVideoCall) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(width = 104.dp, height = 150.dp)
                    .clip(RoundedCornerShape(NexusTokens.Radius.lg))
                    .background(NexusTokens.Color.BgElevated)
                    .border(1.dp, NexusTokens.Color.GlassBorder, RoundedCornerShape(NexusTokens.Radius.lg))
            ) {
                if (localVideoTrack != null && isVideoEnabled) {
                    VideoRenderer(
                        videoTrack = localVideoTrack,
                        modifier = Modifier.fillMaxSize(),
                        // Solo se espeja la cámara frontal, como un espejo; la
                        // trasera espejada mostraría el texto al revés.
                        mirror = isFrontCamera
                    )
                } else {
                    Icon(
                        Icons.Default.VideocamOff,
                        contentDescription = "Cámara apagada",
                        tint = NexusTokens.Color.TextMuted,
                        modifier = Modifier.align(Alignment.Center).size(28.dp)
                    )
                }
            }
        }

        // ── Insignias de seguridad ──
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isVideoCall && remoteVideoTrack != null) 64.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!(isVideoCall && remoteVideoTrack != null && !controlsVisible)) {
                SecurityBadge(icon = Icons.Default.Lock, text = "Cifrada de extremo a extremo")
                if (isRelayOnly) SecurityBadge(icon = Icons.Default.Shield, text = "IP oculta")
            }
        }

        // ── Controles ──
        AnimatedVisibility(
            visible = controlsVisible || !isVideoCall || phase != CallPhase.CONNECTED,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CallControlsPanel(
                isVideoCall = isVideoCall,
                isAudioEnabled = isAudioEnabled,
                isVideoEnabled = isVideoEnabled,
                isSpeakerOn = isSpeakerOn,
                enabled = endReason == null,
                onToggleMic = {
                    viewModel.toggleAudio()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onToggleSpeaker = {
                    viewModel.toggleSpeaker()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onToggleVideo = {
                    viewModel.toggleVideo()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onFlipCamera = {
                    viewModel.switchCamera()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onHangUp = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    viewModel.endCall()
                }
            )
        }
    }

    if (showHangupDialog) {
        AlertDialog(
            onDismissRequest = { showHangupDialog = false },
            containerColor = NexusTokens.Color.BgSurface,
            title = { Text("¿Colgar la llamada?", color = NexusTokens.Color.TextPrimary) },
            text = {
                Text(
                    "Si sales de esta pantalla la llamada terminará.",
                    color = NexusTokens.Color.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHangupDialog = false
                        viewModel.endCall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusTokens.Color.Error)
                ) { Text("Colgar") }
            },
            dismissButton = {
                TextButton(onClick = { showHangupDialog = false }) {
                    Text("Seguir en la llamada", color = NexusTokens.Color.PrimaryLight)
                }
            }
        )
    }
}

@Composable
private fun CallPermissionGate(isVideoCall: Boolean, onGrant: () -> Unit, onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(NexusTokens.Gradient.Background)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(NexusTokens.Color.Primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isVideoCall) Icons.Default.Videocam else Icons.Default.Mic,
                    contentDescription = null,
                    tint = NexusTokens.Color.PrimaryLight,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Permisos necesarios",
                color = NexusTokens.Color.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isVideoCall) "Para la videollamada hacen falta el micrófono y la cámara."
                else "Para la llamada de voz hace falta el micrófono.",
                color = NexusTokens.Color.TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = NexusTokens.Color.Primary),
                shape = RoundedCornerShape(NexusTokens.Radius.pill)
            ) { Text("Conceder permisos") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel) {
                Text("Cancelar", color = NexusTokens.Color.TextSecondary)
            }
        }
    }
}

/** Avatar grande con anillos de pulso mientras suena o conecta. */
@Composable
private fun CallerHero(
    name: String,
    photoUrl: String?,
    statusText: String,
    pulsing: Boolean,
    isVideoCall: Boolean
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            if (pulsing) {
                repeat(2) { i ->
                    val progress = (ring + i * 0.5f) % 1f
                    Box(
                        Modifier
                            .size(140.dp)
                            .scale(1f + progress * 0.55f)
                            .clip(CircleShape)
                            .background(NexusTokens.Color.Primary.copy(alpha = (1f - progress) * 0.28f))
                    )
                }
            }
            Box(
                Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.linearGradient(NexusTokens.Gradient.Brand), CircleShape)
                    .padding(4.dp)
            ) {
                UserAvatar(name = name, photoUrl = photoUrl, size = 132.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            name,
            color = NexusTokens.Color.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            statusText,
            color = NexusTokens.Color.TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.semantics { contentDescription = "Estado de la llamada: $statusText" }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (isVideoCall) "Videollamada NexusChat" else "Llamada de voz NexusChat",
            color = NexusTokens.Color.TextMuted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SecurityBadge(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, NexusTokens.Color.GlassBorder, RoundedCornerShape(NexusTokens.Radius.pill))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NexusTokens.Color.Success, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = NexusTokens.Color.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun CallControlsPanel(
    isVideoCall: Boolean,
    isAudioEnabled: Boolean,
    isVideoEnabled: Boolean,
    isSpeakerOn: Boolean,
    enabled: Boolean,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onHangUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.xxl))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, NexusTokens.Color.GlassBorder, RoundedCornerShape(NexusTokens.Radius.xxl))
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallControlButton(
                icon = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                label = if (isAudioEnabled) "Silenciar" else "Activar mic",
                isActive = !isAudioEnabled,
                enabled = enabled,
                onClick = onToggleMic
            )
            CallControlButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "Altavoz",
                isActive = isSpeakerOn,
                enabled = enabled,
                onClick = onToggleSpeaker
            )
            if (isVideoCall) {
                CallControlButton(
                    icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (isVideoEnabled) "Cámara" else "Cámara off",
                    isActive = !isVideoEnabled,
                    enabled = enabled,
                    onClick = onToggleVideo
                )
                CallControlButton(
                    icon = Icons.Default.Cameraswitch,
                    label = "Girar",
                    isActive = false,
                    enabled = enabled && isVideoEnabled,
                    onClick = onFlipCamera
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        FilledIconButton(
            onClick = onHangUp,
            enabled = enabled,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = NexusTokens.Color.Error,
                contentColor = Color.White,
                disabledContainerColor = NexusTokens.Color.Error.copy(alpha = 0.4f)
            )
        ) {
            Icon(Icons.Default.CallEnd, contentDescription = "Colgar", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(58.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) Color.White else Color.White.copy(alpha = 0.14f),
                contentColor = if (isActive) NexusTokens.Color.BgBase else Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.06f),
                disabledContentColor = Color.White.copy(alpha = 0.35f)
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

/**
 * Bloqueo de proximidad: apaga la pantalla cuando el sensor detecta la oreja,
 * como la app de teléfono. Se suelta en cuanto [enabled] pasa a false.
 */
@Composable
private fun ProximityScreenOff(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val wakeLock = if (enabled) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
                pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "NexusChat:CallProximity")
                    .apply {
                        setReferenceCounted(false)
                        // Tope de seguridad: si algo fallase al soltarlo, no se queda
                        // retenido para siempre.
                        acquire(4 * 60 * 60 * 1000L)
                    }
            } else null
        } else null
        onDispose {
            runCatching { if (wakeLock?.isHeld == true) wakeLock.release() }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, secs)
    }
}
