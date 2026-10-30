package com.Azelmods.App.ui.screens.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.security.tor.OrbotDetector
import com.Azelmods.App.data.security.tor.OrbotState
import com.Azelmods.App.data.security.tor.OrbotUiStatus
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.data.security.tor.mapOrbotStatus
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pantalla de control de Tor (modo anónimo vía Orbot).
 *
 * - Estado REAL: verificado contra check.torproject.org, con la IP de salida y si
 *   Orbot va en modo proxy o VPN. Antes "Conectado" solo significaba que un puerto
 *   local respondía.
 * - Cobertura honesta: qué partes de la app pasan por Tor y cuáles no.
 * - Acciones: iniciar Orbot sin salir de la app, comprobar, abrir el navegador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorControlScreen(
    navController: NavController,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val torState by viewModel.torState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Estado real de Orbot, comprobado en segundo plano (abre sockets).
    var orbotStatus by remember { mutableStateOf<OrbotUiStatus?>(null) }
    LaunchedEffect(torState) {
        while (true) {
            orbotStatus = withContext(Dispatchers.IO) {
                runCatching {
                    mapOrbotStatus(
                        installed = OrbotDetector.isOrbotUsable(context),
                        active = OrbotDetector.isTorAvailable()
                    )
                }.getOrElse { mapOrbotStatus(installed = false, active = false) }
            }
            kotlinx.coroutines.delay(4000)
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SecurityUiState.Success -> {
                snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short)
                viewModel.clearUiState()
            }
            is SecurityUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "${state.message}\n${state.suggestion}",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modo anónimo (Tor)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NexusTokens.Color.BgBase,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NexusTokens.Color.BgBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TorStatusHero(torState = torState)

            AnonymousModeToggle(
                torState = torState,
                onToggle = { enabled ->
                    if (enabled) viewModel.enableAnonymousMode() else viewModel.disableAnonymousMode()
                },
                orbotStatus = orbotStatus,
                onOrbotAction = {
                    when (orbotStatus?.state) {
                        OrbotState.NOT_INSTALLED -> openOrbotStore(context)
                        OrbotState.INSTALLED_INACTIVE -> {
                            if (!viewModel.requestOrbotStart(context)) OrbotDetector.launchOrbot(context)
                        }
                        else -> Unit
                    }
                }
            )

            // Acciones rápidas
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction(
                    icon = Icons.Default.VerifiedUser,
                    label = "Comprobar",
                    enabled = torState is TorState.Connected,
                    modifier = Modifier.weight(1f),
                    onClick = viewModel::verifyTor
                )
                QuickAction(
                    icon = Icons.Default.PowerSettingsNew,
                    label = if (orbotStatus?.state == OrbotState.NOT_INSTALLED) "Instalar Orbot" else "Abrir Orbot",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (orbotStatus?.state == OrbotState.NOT_INSTALLED) openOrbotStore(context)
                        else OrbotDetector.launchOrbot(context)
                    }
                )
                QuickAction(
                    icon = Icons.Default.Language,
                    label = "Navegador",
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("tor_browser") }
                )
            }

            CoverageCard(torState = torState)

            if (torState is TorState.Error && orbotStatus?.state != OrbotState.ACTIVE) {
                Surface(
                    color = NexusTokens.Color.Error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(NexusTokens.Radius.lg),
                    border = BorderStroke(1.dp, NexusTokens.Color.Error.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("No hay conexión con Tor", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            (torState as TorState.Error).message,
                            color = NexusTokens.Color.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.enableAnonymousMode() },
                            colors = ButtonDefaults.buttonColors(containerColor = NexusTokens.Color.Error)
                        ) { Text("Reintentar") }
                    }
                }
            }

            InfoCard(
                title = "Toda la app por Tor",
                description = "Mensajes en tiempo real y notificaciones usan conexiones propias de Firebase " +
                    "que no respetan el proxy. Para que también vayan por Tor, activa en Orbot el modo VPN y " +
                    "añade NexusChat a sus apps: la pantalla lo detectará y lo verás como «Toda la app»."
            )
        }
    }
}

private fun openOrbotStore(context: android.content.Context) {
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=org.torproject.android")
            )
        )
    }
}

/** Cabecera grande con el estado real de la conexión. */
@Composable
private fun TorStatusHero(torState: TorState) {
    val connected = torState as? TorState.Connected
    val info = connected?.circuitInfo
    val (title, subtitle, color) = when {
        info != null && info.verified && info.vpnMode ->
            Triple("Protegido: toda la app por Tor", "IP visible: ${info.exitIp ?: "—"}", NexusTokens.Color.Success)
        info != null && info.verified ->
            Triple("Protegido por Tor", "IP visible: ${info.exitIp ?: "—"} · modo proxy", NexusTokens.Color.Success)
        info != null ->
            Triple("Conectado a Orbot", info.checkMessage.ifBlank { "Comprobando la salida…" }, NexusTokens.Color.Secondary)
        torState is TorState.Connecting ->
            Triple("Conectando… ${torState.progress}%", torState.message, NexusTokens.Color.Warning)
        torState is TorState.Bootstrapping ->
            Triple("Conectando… ${torState.progress}%", torState.message, NexusTokens.Color.Warning)
        torState is TorState.Error ->
            Triple("Sin Tor", "El modo anónimo no está funcionando", NexusTokens.Color.Error)
        else ->
            Triple("Modo anónimo desactivado", "Tu tráfico sale con tu IP normal", NexusTokens.Color.TextMuted)
    }

    val working = torState is TorState.Connecting || torState is TorState.Bootstrapping
    val t = rememberInfiniteTransition(label = "hero")
    val pulse by t.animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")

    Surface(
        color = NexusTokens.Color.BgSurface,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(84.dp)
                    .scale(if (working) pulse else 1f)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.05f))))
                    .border(2.dp, color.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🧅", fontSize = 38.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = NexusTokens.Color.TextSecondary, fontSize = 13.sp)
            if (working) {
                val progress = when (torState) {
                    is TorState.Connecting -> torState.progress
                    is TorState.Bootstrapping -> torState.progress
                    else -> 0
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            }
            if (info != null) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TorChip("SOCKS ${info.socksPort}")
                    TorChip(if (info.vpnMode) "VPN de Orbot" else "Proxy")
                    TorChip(if (info.verified) "Verificado ✓" else "Sin verificar")
                }
                if (info.verified && info.checkMessage.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(info.checkMessage, color = NexusTokens.Color.TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TorChip(text: String) {
    Text(
        text,
        color = NexusTokens.Color.TextSecondary,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(NexusTokens.Radius.lg),
        border = BorderStroke(1.dp, NexusTokens.Color.GlassBorder),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (enabled) NexusTokens.Color.PrimaryLight else NexusTokens.Color.TextDisabled)
            Spacer(Modifier.height(4.dp))
            Text(label, color = if (enabled) Color.White else NexusTokens.Color.TextDisabled, fontSize = 12.sp)
        }
    }
}

/** Qué pasa por Tor y qué no, sin promesas falsas. */
@Composable
private fun CoverageCard(torState: TorState) {
    val info = (torState as? TorState.Connected)?.circuitInfo
    val active = info != null
    val vpn = info?.vpnMode == true

    Surface(
        color = NexusTokens.Color.BgSurface,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(0.5.dp, NexusTokens.Color.GlassBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Qué pasa por Tor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            CoverageRow("Navegador y sitios .onion", if (active) Coverage.YES else Coverage.OFF)
            CoverageRow("Azel IA y agentes", if (active) Coverage.YES else Coverage.OFF)
            CoverageRow("Fotos, vídeos y archivos", if (active) Coverage.YES else Coverage.OFF)
            CoverageRow(
                "Mensajes en tiempo real (Firebase)",
                when {
                    vpn -> Coverage.YES
                    active -> Coverage.PARTIAL
                    else -> Coverage.OFF
                },
                note = if (active && !vpn) "Solo con el modo VPN de Orbot" else null
            )
            CoverageRow(
                "Llamadas",
                if (active) Coverage.PARTIAL else Coverage.OFF,
                note = if (active) "Van por un servidor relay: el otro no ve tu IP (Tor no admite llamadas)" else null
            )
            AnimatedVisibility(visible = !active) {
                Text(
                    "Activa el modo anónimo para proteger estas conexiones.",
                    color = NexusTokens.Color.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private enum class Coverage { YES, PARTIAL, OFF }

@Composable
private fun CoverageRow(label: String, coverage: Coverage, note: String? = null) {
    val (icon, color) = when (coverage) {
        Coverage.YES -> Icons.Default.CheckCircle to NexusTokens.Color.Success
        Coverage.PARTIAL -> Icons.Default.Info to NexusTokens.Color.Warning
        Coverage.OFF -> Icons.Default.RemoveCircleOutline to NexusTokens.Color.TextMuted
    }
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = Color.White, fontSize = 14.sp)
            if (note != null) Text(note, color = NexusTokens.Color.TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    Surface(
        color = NexusTokens.Color.BgSurface,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(0.5.dp, NexusTokens.Color.GlassBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Info, null, tint = NexusTokens.Color.PrimaryLight, modifier = Modifier.size(22.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(description, color = NexusTokens.Color.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
