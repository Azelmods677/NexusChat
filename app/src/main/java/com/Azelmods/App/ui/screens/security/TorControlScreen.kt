package com.Azelmods.App.ui.screens.security

import com.Azelmods.App.i18n.tr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.security.tor.OrbotDetector
import com.Azelmods.App.data.security.tor.TorCircuitInfo
import com.Azelmods.App.data.security.tor.TorEngine
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.ui.components.NexusDetailRow
import com.Azelmods.App.ui.components.NexusListRow
import com.Azelmods.App.ui.components.NexusNotice
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.NexusSwitchRow
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.theme.NexusTokens

/**
 * Modo anónimo. Lo que aparece aquí sale del propio Tor (progreso y circuito), del Proyecto
 * Tor (IP de salida verificada) y del enrutado real (qué ha salido ya por Tor).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorControlScreen(
    navController: NavController,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val torState by viewModel.torState.collectAsState()
    val engine by viewModel.engine.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var identityRequest by remember { mutableIntStateOf(0) }

    val modeOn = torState !is TorState.Disconnected
    val info = (torState as? TorState.Connected)?.circuitInfo

    LaunchedEffect(identityRequest) {
        if (identityRequest > 0) snackbar.showSnackbar(tr("Buscando otra salida. La IP cambiará en unos segundos."))
    }

    Scaffold(
        topBar = { NexusTopBar(title = tr("Modo anónimo"), onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            NexusSwitchRow(
                title = tr("Modo anónimo"),
                subtitle = statusText(torState),
                checked = modeOn,
                onCheckedChange = { on ->
                    if (on) viewModel.enableAnonymousMode() else viewModel.disableAnonymousMode()
                }
            )

            val progress = (torState as? TorState.Connecting)?.progress
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = NexusTokens.Color.BgHigh,
                    drawStopIndicator = {}
                )
                Text(
                    text = "$progress %",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusTokens.Color.TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            (torState as? TorState.Error)?.let { error ->
                NexusNotice(
                    text = error.message,
                    tone = NexusTokens.Color.Error,
                    modifier = Modifier.padding(16.dp),
                    actionLabel = tr("Reintentar"),
                    onAction = {
                        viewModel.disableAnonymousMode()
                        viewModel.enableAnonymousMode()
                    }
                )
            }

            if (info != null) ConnectionDetails(info)

            if (info != null && !info.firebaseReachable) {
                NexusNotice(
                    text = if (info.engine == TorEngine.EMBEDDED) {
                        "Google está rechazando las salidas de Tor probadas, así que los mensajes " +
                            "no cargarán. Tu IP sigue oculta. Prueba con Nueva identidad."
                    } else {
                        "Google rechaza esta salida de Tor, así que los mensajes no cargarán. Tu IP " +
                            "sigue oculta. Con Orbot no se puede elegir la salida: usa el motor Integrado."
                    },
                    icon = Icons.Outlined.Info,
                    tone = NexusTokens.Color.Warning,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (info != null) {
                NexusListRow(
                    title = tr("Comprobar la conexión"),
                    subtitle = tr("Pregunta al Proyecto Tor con qué IP sales"),
                    icon = Icons.Outlined.VerifiedUser,
                    onClick = viewModel::verifyTor
                )
                if (info.engine == TorEngine.EMBEDDED) {
                    NexusListRow(
                        title = tr("Nueva identidad"),
                        subtitle = tr("Busca otra salida y crea circuitos nuevos"),
                        icon = Icons.Outlined.Autorenew,
                        onClick = { if (viewModel.newIdentity()) identityRequest++ }
                    )
                }
            }
            NexusListRow(
                title = tr("Navegador privado"),
                subtitle = tr("Sitios .onion y navegación sin rastro"),
                icon = Icons.Outlined.Language,
                onClick = { navController.navigate("tor_browser") }
            )

            NexusSectionHeader(tr("Motor de Tor"))
            EngineOption(
                title = tr("Integrado"),
                subtitle = "Tor ${viewModel.embeddedTorVersion} dentro de la app. No hace falta instalar nada.",
                selected = engine == TorEngine.EMBEDDED,
                onClick = { viewModel.setEngine(TorEngine.EMBEDDED) }
            )
            EngineOption(
                title = tr("Orbot"),
                subtitle = if (OrbotDetector.isOrbotInstalled(context)) {
                    "Usa la app Orbot instalada en el teléfono."
                } else {
                    "Requiere instalar la app Orbot."
                },
                selected = engine == TorEngine.ORBOT,
                onClick = { viewModel.setEngine(TorEngine.ORBOT) }
            )

            NexusSectionHeader(tr("Qué pasa por Tor"))
            val active = info != null
            CoverageRow("Navegador privado", active)
            CoverageRow("Azel IA y agentes", active)
            CoverageRow("Fotos, vídeos y archivos", active)
            CoverageRow(
                label = tr("Mensajes en tiempo real"),
                covered = info?.messagingViaTor == true,
                note = if (active && info?.messagingViaTor != true) tr("Firebase reconectará por Tor en unos segundos") else null
            )
            CoverageRow(
                label = tr("Llamadas"),
                covered = false,
                note = if (active) tr("Tor no admite llamadas. Van por un servidor intermedio, así el otro no ve tu IP.") else null
            )
            CoverageRow(
                label = tr("Notificaciones push"),
                covered = false,
                note = "Las gestiona Google Play Services, fuera de la app."
            )

            NexusSectionHeader(tr("Además, mientras esté activo"))
            Text(
                tr("No se publica si estás conectado ni tu última conexión, ni el aviso de " +
                    "«escribiendo», ni las confirmaciones de lectura."),
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTokens.Color.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

private fun statusText(state: TorState): String = when (state) {
    is TorState.Disconnected -> tr("Desactivado. Tu tráfico sale con tu IP normal.")
    is TorState.Connecting -> state.message
    is TorState.Bootstrapping -> state.message
    is TorState.Connected -> state.circuitInfo.exitIp?.let { "Conectado. Se te ve con la IP $it" } ?: tr("Conectado a la red Tor")
    is TorState.Error -> tr("Sin conexión. La app no sale a internet sin Tor.")
}

@Composable
private fun ConnectionDetails(info: TorCircuitInfo) {
    NexusSectionHeader(tr("Conexión"))
    NexusDetailRow(
        label = tr("Verificación"),
        value = if (info.verified) tr("Sale por Tor") else info.checkMessage.ifBlank { "Pendiente" },
        valueColor = if (info.verified) NexusTokens.Color.Online else NexusTokens.Color.TextSecondary
    )
    info.exitIp?.let { NexusDetailRow(tr("IP de salida"), it, monospace = true) }
    NexusDetailRow(
        label = tr("Servidores de mensajes"),
        value = if (info.firebaseReachable) tr("Accesibles") else tr("Bloqueados por Google"),
        valueColor = if (info.firebaseReachable) NexusTokens.Color.Online else NexusTokens.Color.Warning
    )
    NexusDetailRow(tr("Motor"), info.engine.label)
    NexusDetailRow(tr("Proxy local"), "127.0.0.1:${info.socksPort}", monospace = true)
    if (info.entryNode.isNotBlank()) {
        NexusSectionHeader(tr("Circuito actual"))
        NexusDetailRow(tr("Entrada"), info.entryNode, monospace = true)
        NexusDetailRow(tr("Intermedio"), info.middleNode, monospace = true)
        NexusDetailRow(tr("Salida"), info.exitNode, monospace = true)
    }
}

@Composable
private fun EngineOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    NexusListRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
        }
    )
}

@Composable
private fun CoverageRow(label: String, covered: Boolean, note: String? = null) {
    val icon: ImageVector = if (covered) Icons.Outlined.CheckCircle else Icons.Outlined.RemoveCircleOutline
    val tint: Color = if (covered) NexusTokens.Color.Online else NexusTokens.Color.TextMuted
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(NexusTokens.IconSize.sm))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextPrimary)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted)
            }
        }
    }
}
