package com.Azelmods.App.ui.screens.call

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.service.CallNotifications
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.delay

/**
 * Llamada entrante a pantalla completa.
 *
 * Cambios respecto a la versión anterior:
 * - Ya no pide la cámara en una llamada de VOZ (los permisos los pide la
 *   pantalla de la llamada al contestar, y solo los necesarios).
 * - Se cierra sola si quien llama cuelga: antes se quedaba "sonando" y vibrando
 *   para siempre aunque la llamada ya no existiera.
 * - Al contestar o rechazar se retira la notificación (y su tono).
 * - Se quitó "Recordarme", que en realidad solo rechazaba la llamada.
 */
@Composable
fun IncomingCallScreen(
    callId: String,
    callType: String, // "audio" or "video"
    navController: NavController,
    viewModel: CallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isVideoCall = callType.equals("video", ignoreCase = true)

    val contact by viewModel.contactProfile.collectAsState()
    val endReason by viewModel.endReason.collectAsState()

    LaunchedEffect(callId) {
        if (callId.isNotBlank()) {
            viewModel.loadContactProfileFromCall(callId)
            viewModel.observeIncomingCall(callId)
        }
    }

    // Quien llama colgó (o se rechazó desde la notificación): fuera.
    LaunchedEffect(endReason) {
        if (endReason != null) {
            CallNotifications.cancelIncoming(context, callId)
            delay(1_200)
            navController.popBackStack()
        }
    }

    // Vibración suave mientras suena (la notificación ya pone el tono).
    LaunchedEffect(endReason) {
        while (endReason == null) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            delay(2000)
        }
    }

    val transition = rememberInfiniteTransition(label = "incoming")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "bob"
    )

    val name = contact?.name?.takeIf { it.isNotBlank() } ?: "…"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        NexusTokens.Color.Primary.copy(alpha = 0.35f),
                        NexusTokens.Color.BgBase,
                        NexusTokens.Color.BgDeep
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isVideoCall) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = NexusTokens.Color.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = endReason?.label
                        ?: if (isVideoCall) "Videollamada entrante" else "Llamada de voz entrante",
                    color = NexusTokens.Color.TextSecondary,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = name,
                color = NexusTokens.Color.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = NexusTokens.Color.Success, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cifrada de extremo a extremo", color = NexusTokens.Color.TextMuted, fontSize = 12.sp)
            }

            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                repeat(3) { i ->
                    val progress = (ring + i / 3f) % 1f
                    Box(
                        Modifier
                            .size(150.dp)
                            .scale(1f + progress * 0.6f)
                            .clip(CircleShape)
                            .background(NexusTokens.Color.PrimaryLight.copy(alpha = (1f - progress) * 0.22f))
                    )
                }
                Box(
                    Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(2.dp, Brush.linearGradient(NexusTokens.Gradient.Brand), CircleShape)
                        .padding(4.dp)
                ) {
                    UserAvatar(name = name, photoUrl = contact?.photoUrl, size = 142.dp)
                }
            }

            Spacer(Modifier.weight(1f))

            // Responder con un mensaje: rechaza y abre el chat.
            IncomingSideAction(
                icon = Icons.AutoMirrored.Filled.Message,
                label = "Responder con mensaje",
                onClick = {
                    if (callId.isNotBlank()) viewModel.declineCall(callId)
                    val peerId = contact?.uid?.takeIf { it.isNotBlank() }
                    if (peerId != null) {
                        navController.navigate("chat/$peerId") {
                            popUpTo("incoming_call/$callId/$callType") { inclusive = true }
                        }
                    } else {
                        navController.navigateUp()
                    }
                }
            )

            Spacer(Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IncomingMainAction(
                    icon = Icons.Default.CallEnd,
                    label = "Rechazar",
                    color = NexusTokens.Color.Error,
                    enabled = endReason == null,
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        if (callId.isNotBlank()) viewModel.declineCall(callId)
                    }
                )
                IncomingMainAction(
                    icon = if (isVideoCall) Icons.Default.Videocam else Icons.Default.Call,
                    label = "Contestar",
                    color = NexusTokens.Color.Success,
                    enabled = endReason == null,
                    modifier = Modifier.offset(y = (-6 * bob).dp),
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        if (callId.isNotBlank()) {
                            CallNotifications.cancelIncoming(context, callId)
                            // La pantalla de llamada (que es la que muestra el medio)
                            // es la que contesta y es dueña de la conexión.
                            navController.navigate("active_call/$callId/${if (isVideoCall) "video" else "audio"}?isCaller=false") {
                                popUpTo("incoming_call/$callId/$callType") { inclusive = true }
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IncomingMainAction(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(76.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = color,
                contentColor = Color.White,
                disabledContainerColor = color.copy(alpha = 0.35f)
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = NexusTokens.Color.TextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun IncomingSideAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = NexusTokens.Color.TextSecondary)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp)
    }
}
