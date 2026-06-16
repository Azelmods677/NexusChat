package com.Azelmods.App.ui.screens.security

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.data.security.tor.OrbotDetector
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.*
import kotlinx.coroutines.delay

/**
 * ðŸŒ OrbotWelcomeScreen â€” Pantalla de bienvenida que guÃ­a al usuario
 * a instalar y configurar Orbot para navegaciÃ³n anÃ³nima.
 *
 * Estados:
 * 1. Orbot no instalado â†’ BotÃ³n para descargar desde Guardian Project
 * 2. Orbot instalado pero no activo â†’ BotÃ³n para abrir Orbot
 * 3. Orbot conectado y funcionando â†’ ConfirmaciÃ³n + acceso a Tor Browser
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbotWelcomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var orbotStatus by remember { mutableStateOf(OrbotStatus.CHECKING) }

    // Auto-detect Orbot status on launch and every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val installed = OrbotDetector.isOrbotInstalled(context)
            val active = OrbotDetector.isTorAvailable()

            orbotStatus = when {
                !installed -> OrbotStatus.NOT_INSTALLED
                !active -> OrbotStatus.INACTIVE
                else -> OrbotStatus.ACTIVE
            }
            delay(3000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orbot Setup") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // â”€â”€ Animated Tor icon â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            TorOnionAnimation(status = orbotStatus)
            Spacer(Modifier.height(24.dp))

            // â”€â”€ Title â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(
                text = "NavegaciÃ³n AnÃ³nima",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Accede a sitios .onion y navega de forma privada usando la red Tor a travÃ©s de Orbot",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(32.dp))

            // â”€â”€ Status Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            OrbotStatusCard(status = orbotStatus)

            Spacer(Modifier.height(24.dp))

            // â”€â”€ Action buttons â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            when (orbotStatus) {
                OrbotStatus.CHECKING -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                OrbotStatus.NOT_INSTALLED -> {
                    OrbotActionButton(
                        icon = Icons.Default.Download,
                        text = "Descargar Orbot",
                        description = "Desde Play Store o F-Droid",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://play.google.com/store/apps/details?id=org.torproject.android")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val fdroidIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://f-droid.org/packages/org.torproject.android")
                                }
                                try {
                                    context.startActivity(fdroidIntent)
                                } catch (_: Exception) {
                                    // Fallback: open browser with Orbot info
                                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://orbot.app")
                                    }
                                    context.startActivity(webIntent)
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    InstallStepCard(
                        step = 1,
                        title = "Descarga e instala Orbot",
                        description = "Desde Play Store, F-Droid o orbot.app"
                    )
                    InstallStepCard(
                        step = 2,
                        title = "Abre Orbot y presiona 'Iniciar'",
                        description = "Espera a que se conecte a la red Tor"
                    )
                    InstallStepCard(
                        step = 3,
                        title = "Vuelve a esta pantalla",
                        description = "VerÃ¡s el estado 'Conectado' automÃ¡ticamente"
                    )
                }

                OrbotStatus.INACTIVE -> {
                    OrbotActionButton(
                        icon = Icons.Default.PlayArrow,
                        text = "Abrir Orbot",
                        description = "Orbot estÃ¡ instalado pero no conectado",
                        color = Warning,
                        onClick = { OrbotDetector.launchOrbot(context) }
                    )

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Warning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Consejo",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Una vez que Orbot se conecte, el estado cambiarÃ¡ automÃ¡ticamente.",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                OrbotStatus.ACTIVE -> {
                    OrbotActionButton(
                        icon = Icons.Default.Verified,
                        text = "Â¡Conectado a Tor!",
                        description = "Tu trÃ¡fico estÃ¡ siendo anonimizado",
                        color = Success,
                        enabled = false,
                        onClick = {}
                    )

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Â¿QuÃ© sigue?",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )

                            NextStepItem(
                                icon = Icons.Default.Language,
                                title = "Navegador Privado",
                                description = "Accede a sitios .onion con DuckDuckGo",
                                onClick = { navController.navigate(Screen.TorBrowser.route) }
                            )
                            NextStepItem(
                                icon = Icons.Default.Security,
                                title = "ConfiguraciÃ³n de Tor",
                                description = "Ajusta el modo anÃ³nimo y proxy selector",
                                onClick = { navController.navigate(Screen.TorControl.route) }
                            )
                            NextStepItem(
                                icon = Icons.Default.Shield,
                                title = "Seguridad",
                                description = "Explora todas las funciones de privacidad",
                                onClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // â”€â”€ Refresh button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            TextButton(
                onClick = {
                    orbotStatus = OrbotStatus.CHECKING
                    val installed = OrbotDetector.isOrbotInstalled(context)
                    val active = OrbotDetector.isTorAvailable()
                    orbotStatus = when {
                        !installed -> OrbotStatus.NOT_INSTALLED
                        !active -> OrbotStatus.INACTIVE
                        else -> OrbotStatus.ACTIVE
                    }
                }
            ) {
                Text("Verificar estado", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// TorOnionAnimation
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun TorOnionAnimation(status: OrbotStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "onion")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val arcProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arc"
    )

    val glowColor = when (status) {
        OrbotStatus.ACTIVE -> Success
        OrbotStatus.INACTIVE -> Warning
        OrbotStatus.NOT_INSTALLED -> Color.Red.copy(alpha = 0.6f)
        OrbotStatus.CHECKING -> MaterialTheme.colorScheme.primary
    }

    val alpha = if (status == OrbotStatus.ACTIVE) pulse else 1f

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 3.dp.toPx()
            val radius = size.minDimension / 2 - strokeWidth / 2
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0f),
                        glowColor.copy(alpha = 0.3f * pulse),
                        glowColor.copy(alpha = 0f)
                    )
                ),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Animated arc
        Canvas(modifier = Modifier.size(120.dp)) {
            val strokeWidth = 2.dp.toPx()
            val sweepAngle = arcProgress * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(glowColor.copy(alpha = 0.1f), glowColor)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center icon with rotation and pulse
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Tor",
            tint = when (status) {
                OrbotStatus.ACTIVE -> Success
                OrbotStatus.INACTIVE -> Warning
                OrbotStatus.NOT_INSTALLED -> Color.Red.copy(alpha = 0.7f)
                OrbotStatus.CHECKING -> MaterialTheme.colorScheme.primary
            }.copy(alpha = alpha),
            modifier = Modifier
                .size(48.dp)
                .rotate(rotation)
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// OrbotStatusCard
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun OrbotStatusCard(status: OrbotStatus) {
    val icon = when (status) {
        OrbotStatus.CHECKING -> Icons.Default.Search
        OrbotStatus.NOT_INSTALLED -> Icons.Default.ErrorOutline
        OrbotStatus.INACTIVE -> Icons.Default.PowerSettingsNew
        OrbotStatus.ACTIVE -> Icons.Default.CheckCircle
    }

    val title = when (status) {
        OrbotStatus.CHECKING -> "Verificando..."
        OrbotStatus.NOT_INSTALLED -> "Orbot no instalado"
        OrbotStatus.INACTIVE -> "Orbot inactivo"
        OrbotStatus.ACTIVE -> "Orbot conectado"
    }

    val description = when (status) {
        OrbotStatus.CHECKING -> "Detectando Orbot en tu dispositivo"
        OrbotStatus.NOT_INSTALLED -> "Necesitas Orbot para navegar por la red Tor"
        OrbotStatus.INACTIVE -> "Orbot estÃ¡ instalado pero no conectado a Tor"
        OrbotStatus.ACTIVE -> "Tu dispositivo estÃ¡ conectado a la red Tor"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            OrbotStatus.ACTIVE -> Success.copy(alpha = 0.15f)
                            OrbotStatus.INACTIVE -> Warning.copy(alpha = 0.15f)
                            OrbotStatus.NOT_INSTALLED -> Color.Red.copy(alpha = 0.15f)
                            OrbotStatus.CHECKING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (status) {
                        OrbotStatus.ACTIVE -> Success
                        OrbotStatus.INACTIVE -> Warning
                        OrbotStatus.NOT_INSTALLED -> Color.Red
                        OrbotStatus.CHECKING -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// OrbotActionButton
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun OrbotActionButton(
    icon: ImageVector,
    text: String,
    description: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// InstallStepCard
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun InstallStepCard(step: Int, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$step", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(text = description, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// NextStepItem
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun NextStepItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(description, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// OrbotStatus enum
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private enum class OrbotStatus {
    CHECKING,
    NOT_INSTALLED,
    INACTIVE,
    ACTIVE
}
