package com.Azelmods.App.ui.screens.settings

import com.Azelmods.App.i18n.tr

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.DisplayMetrics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Azelmods.App.BuildConfig
import com.Azelmods.App.ui.screens.splash.drawNexusEmblem
import com.Azelmods.App.ui.components.NexusDetailRow
import com.Azelmods.App.ui.components.NexusDivider
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.components.pressScale
import com.Azelmods.App.ui.components.staggeredEnter
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val REPO_URL = "https://github.com/Azelmods677/NexusChat"

/**
 * Acerca de: la app y el dispositivo que la está ejecutando, con datos en vivo.
 *
 * Todo lo que aparece aquí se lee en el momento del sistema: la hora avanza, la batería
 * se actualiza con el aviso del sistema y la memoria se vuelve a medir cada pocos
 * segundos. No hay ningún valor escrito a mano.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val MaterialTheme_primary = MaterialTheme.colorScheme.primary

    val battery = rememberBatteryState(context)
    val now = rememberTicker(1_000L)
    val memory = rememberMemoryState(context)
    val locale = remember { Locale.getDefault() }
    val staticInfo = remember { readStaticDeviceInfo(context) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    if (showLicenses) LicensesDialog { showLicenses = false }
    if (showHistory) HistoryDialog { showHistory = false }

    Scaffold(
        topBar = { NexusTopBar(title = tr("Acerca de"), onBack = { navController.popBackStack() }) },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            AboutHero()

            NexusSectionHeader(tr("Sobre NexusChat"))
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AboutTile(Icons.Outlined.Gavel, tr("Licencia"), "Azel Mods 2027", 0, Modifier.weight(1f)) {
                        showLicenses = true
                    }
                    AboutTile(Icons.Outlined.NewReleases, tr("Versión"), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", 1, Modifier.weight(1f))
                }
                AboutTile(Icons.Outlined.History, tr("Historia"), tr("Cómo ha crecido NexusChat, versión a versión"), 2, Modifier.fillMaxWidth()) {
                    showHistory = true
                }
            }
            HighlightsGrid()

            NexusSectionHeader(tr("Novedades de esta versión"))
            ChangelogCard()

            NexusSectionHeader(tr("Información de la app"))
            NexusDetailRow(tr("Base"), tr("Android %s con Jetpack Compose", "17"))
            NexusDetailRow(tr("Créditos"), "Azel Mods")
            NexusDetailRow(tr("Paquete"), BuildConfig.APPLICATION_ID, monospace = true)
            NexusDetailRow(tr("Versión"), BuildConfig.VERSION_NAME)

            NexusSectionHeader(tr("Créditos"))
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NexusTokens.Radius.xl))
                    .background(NexusTokens.Color.BgSurface)
                    .clickable { uriHandler.openUri("https://t.me/AzelModsx7779") }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(50.dp)) { drawNexusEmblem(1f, MaterialTheme_primary) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Azel Mods", style = MaterialTheme.typography.titleSmall, color = NexusTokens.Color.TextPrimary)
                    Text(tr("Diseño y desarrollo de NexusChat"), style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted)
                }
            }

            NexusSectionHeader(tr("Comunidad"))
            CommunityRow(onOpen = uriHandler::openUri)

            NexusSectionHeader(tr("Sobre tu dispositivo"))
            LiveDeviceCard(now, battery, staticInfo, locale)
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AboutTile(Icons.Outlined.Android, tr("Android"), "Android ${Build.VERSION.RELEASE}", 3, Modifier.weight(1f))
                    AboutTile(Icons.Outlined.Wifi, tr("Red"), memory.network, 4, Modifier.weight(1f))
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AboutTile(Icons.Outlined.Sell, tr("Marca"), staticInfo.brand, 5, Modifier.weight(1f))
                    AboutTile(Icons.Outlined.PhoneAndroid, tr("Modelo"), staticInfo.model, 6, Modifier.weight(1f))
                }
            }

            DetailsToggle(expanded = showDetails, onClick = { showDetails = !showDetails })
            AnimatedVisibility(visible = showDetails) {
                Column {
                    NexusDetailRow(tr("Android"), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    NexusDetailRow(tr("Parche de seguridad"), staticInfo.securityPatch)
                    NexusDetailRow(tr("Procesador"), staticInfo.processor)
                    NexusDetailRow(tr("Arquitectura"), staticInfo.abi)
                    NexusDetailRow(tr("Pantalla"), staticInfo.display)
                    NexusDetailRow(tr("Memoria en uso"), memory.ram)
                    NexusDetailRow(tr("Almacenamiento libre"), memory.storage)
                    NexusDetailRow(tr("Encendido desde hace"), formatUptime(SystemClock.elapsedRealtime()))
                    NexusDetailRow(tr("Red"), memory.network)
                    NexusDetailRow(tr("Temperatura"), "%.1f °C".format(locale, battery.temperatureC))
                    NexusDetailRow(tr("Voltaje"), "%.2f V".format(locale, battery.voltageV))
                    NexusDetailRow(tr("Salud"), battery.healthLabel)
                    NexusDetailRow(tr("Compilación"), "${BuildConfig.VERSION_CODE} · ${if (BuildConfig.DEBUG) tr("depuración") else tr("producción")}")
                    NexusDetailRow(tr("Paquete"), BuildConfig.APPLICATION_ID, monospace = true)
                    Text(
                        text = tr("Los chats y los grupos se cifran en el dispositivo con ECDH P-256 y " +
                            "AES-256-GCM: la clave privada no sale del teléfono y el servidor solo guarda " +
                            "texto cifrado.\n\nLos chats secretos cambian de clave en cada mensaje. En el " +
                            "resto no hay secreto hacia adelante. El servidor ve los metadatos: quién habla " +
                            "con quién y cuándo."),
                        style = MaterialTheme.typography.bodySmall,
                        color = NexusTokens.Color.TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            NexusDivider(modifier = Modifier.padding(top = 12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    tr("Diseñada y desarrollada por Azel Mods"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    tr("NexusChat · Azel Mods 2027 · Todos los derechos reservados"),
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.TextMuted
                )
            }
        }
    }
}

/** La cara de la pantalla: el emblema gira despacio sobre un brillo que respira. */
@Composable
private fun AboutHero() {
    val accent = MaterialTheme.colorScheme.primary
    val me = com.Azelmods.App.data.profile.rememberMyProfile()
    val transition = rememberInfiniteTransition(label = "emblema")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "giro"
    )
    val glow by transition.animateFloat(
        0.16f, 0.38f,
        infiniteRepeatable(tween(2_800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brillo"
    )
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.xxl))
            .background(NexusTokens.Color.BgSurface)
            .drawBehind {
                val center = Offset(size.width / 2f, size.height * 0.32f)
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = glow), Color.Transparent),
                        center = center,
                        radius = size.width * 0.62f
                    )
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(Modifier.size(116.dp)) {
                rotate(angle) {
                    drawCircle(
                        Brush.sweepGradient(listOf(accent, Color.Transparent, accent)),
                        radius = size.minDimension * 0.46f,
                        style = Stroke(width = 2f)
                    )
                }
                drawNexusEmblem(1f, accent)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                tr("NexusChat"),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = NexusTokens.Color.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                tr("Mensajería privada con IA, Tor integrado y herramientas de desarrollo."),
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTokens.Color.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                tr("Versión %s", BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(NexusTokens.Radius.pill))
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            )
            if (me.name.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tu foto de 64 dp dentro de su marco, como una ficha de quien usa la app.
                    Box(
                        Modifier
                            .size(71.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        com.Azelmods.App.ui.components.UserAvatar(me.name, me.photoUrl, size = 64.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(me.name, style = MaterialTheme.typography.titleMedium, color = NexusTokens.Color.TextPrimary, maxLines = 1)
                }
            }
        }
    }
}

/** Una tarjeta de dato. Con [onClick] se puede abrir; sin él solo informa. */
@Composable
private fun AboutTile(
    icon: ImageVector,
    title: String,
    value: String,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxHeight()
            .staggeredEnter(index)
            .pressScale(interaction, 0.96f)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                else Modifier
            )
            .padding(20.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(NexusTokens.Color.BgDeep),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = NexusTokens.Color.TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted)
    }
}

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Licencia")) },
        text = {
            Text(
                tr("NexusChat · Azel Mods 2027 · Todos los derechos reservados"),
                style = MaterialTheme.typography.bodyLarge,
                color = NexusTokens.Color.TextPrimary
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

private val HISTORY = listOf(
    "8.0" to "Personalización completa, Kali Linux, stickers propios, chats secretos, grupos cifrados y canales",
    "7.0" to "Perfiles con fotos y vídeos, 40 colores y agentes de IA reales",
    "6.1" to "Llamadas fiables, agentes de IA que se ven trabajar y Tor verificado",
    "5.0" to "Versiones firmadas para publicar",
    "4.0" to "Gran ronda de correcciones y estabilidad",
    "3.0" to "Multimedia, llamadas y presencia reales, y un nuevo sistema de IA",
    "1.0" to "Primera versión de NexusChat"
)

@Composable
private fun HistoryDialog(onDismiss: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Historia")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                HISTORY.forEachIndexed { index, (version, text) ->
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        // La línea de tiempo une cada versión con la anterior.
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .padding(top = 4.dp)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            if (index != HISTORY.lastIndex) {
                                Box(
                                    Modifier
                                        .width(2.dp)
                                        .weight(1f)
                                        .background(accent.copy(alpha = 0.35f))
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.padding(bottom = 16.dp)) {
                            Text(version, style = MaterialTheme.typography.titleSmall, color = accent)
                            Text(tr(text), style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

private class Highlight(val icon: ImageVector, val title: String, val text: String)

private val HIGHLIGHTS = listOf(
    Highlight(Icons.Outlined.SmartToy, "Agentes de IA", "Investigan, programan y entregan resultados"),
    Highlight(Icons.Outlined.Security, "Tor integrado", "Navegación anónima sin instalar nada"),
    Highlight(Icons.Outlined.Lock, "Chats cifrados", "De extremo a extremo entre dos personas"),
    Highlight(Icons.Outlined.Videocam, "Llamadas", "Voz y vídeo a pantalla completa")
)

@Composable
private fun HighlightsGrid() {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HIGHLIGHTS.chunked(2).forEachIndexed { rowIndex, row ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { index, item ->
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .staggeredEnter(rowIndex * 2 + index)
                            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
                            .background(NexusTokens.Color.BgSurface)
                            .padding(14.dp)
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(tr(item.title), style = MaterialTheme.typography.titleSmall, color = NexusTokens.Color.TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text(tr(item.text), style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted)
                    }
                }
            }
        }
    }
}

private val CHANGELOG = listOf(
    "Estudio de diseño: filas, burbujas, campo de escribir, inicio y ajustes a tu gusto",
    "Filas en 3D, neón y cristal, también en llamadas, historias y contactos",
    "Stickers propios y animados, con paquetes para crear y compartir",
    "Kali Linux de verdad dentro de la terminal",
    "Chats secretos con una clave nueva en cada mensaje",
    "Grupos cifrados de extremo a extremo y canales",
    "Encuestas, mensajes guardados, fijados y programados",
    "Descarga vídeos de YouTube, Facebook, X e Instagram",
    "Aviso de actualización al momento y recuperación de contraseña",
    "Nueva animación de entrada y preparada para Android 17"
)

private const val CHANGELOG_PREVIEW = 4

@Composable
private fun ChangelogCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        (if (expanded) CHANGELOG else CHANGELOG.take(CHANGELOG_PREVIEW)).forEach { item ->
            Row {
                Box(
                    Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(10.dp))
                Text(tr(item), style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextPrimary)
            }
        }
        Text(
            if (expanded) tr("Ver menos") else tr("Ver todas"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CommunityRow(onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CommunityTile(Icons.Outlined.Code, "GitHub", 0, Modifier.weight(1f)) { onOpen(REPO_URL) }
        CommunityTile(Icons.Outlined.Send, "Telegram", 1, Modifier.weight(1f)) { onOpen("https://t.me/AzelModsx7779") }
        CommunityTile(Icons.Outlined.SmartDisplay, "YouTube", 2, Modifier.weight(1f)) { onOpen("https://youtube.com/@AzelModsx677") }
        CommunityTile(Icons.Outlined.MusicNote, "TikTok", 3, Modifier.weight(1f)) { onOpen("https://tiktok.com/@azelmods677") }
    }
}

@Composable
private fun CommunityTile(icon: ImageVector, label: String, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .staggeredEnter(index)
            .pressScale(interaction, 0.94f)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextPrimary, maxLines = 1)
    }
}

@Composable
private fun LiveDeviceCard(now: Long, battery: BatteryState, info: StaticDeviceInfo, locale: Locale) {
    val datePattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "EEEEdMMMMyyyy") }
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .padding(16.dp)
    ) {
        Text(
            SimpleDateFormat("HH:mm:ss", locale).format(Date(now)),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = NexusTokens.Color.TextPrimary
        )
        Text(
            SimpleDateFormat(datePattern, locale).format(Date(now)).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = NexusTokens.Color.TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Spacer(Modifier.height(16.dp))
        BatteryMeter(battery)
    }
}

@Composable
private fun DetailsToggle(expanded: Boolean, onClick: () -> Unit) {
    val turn by animateFloatAsState(if (expanded) 180f else 0f, label = "flecha")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            tr("Detalles técnicos"),
            style = MaterialTheme.typography.bodyLarge,
            color = NexusTokens.Color.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = NexusTokens.Color.TextMuted,
            modifier = Modifier.graphicsLayer { rotationZ = turn }
        )
    }
}

// ── Batería ──────────────────────────────────────────────────────────────────

/** Estado de la batería en vivo (mismo dato que ve la barra del sistema). */
data class BatteryState(
    val percent: Int = 0,
    val isCharging: Boolean = false,
    val statusLabel: String = "Leyendo…",
    val healthLabel: String = "—",
    val temperatureC: Float = 0f,
    val voltageV: Float = 0f
)

@Composable
fun rememberBatteryState(context: Context): BatteryState {
    var state by remember { mutableStateOf(BatteryState()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                state = intent.toBatteryState()
            }
        }
        // Es un aviso "sticky": registrarse devuelve de inmediato el último valor.
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { state = it.toBatteryState() }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

private fun Intent.toBatteryState(): BatteryState {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val percent = if (level >= 0) (level * 100 / scale) else 0
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    val source = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "cargador"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> tr("inalámbrica")
        else -> null
    }
    val statusLabel = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Cargando" + (source?.let { " por $it" } ?: "")
        BatteryManager.BATTERY_STATUS_FULL -> tr("Carga completa")
        BatteryManager.BATTERY_STATUS_DISCHARGING -> tr("Descargando")
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> tr("Conectada, sin cargar")
        else -> tr("Desconocido")
    }
    val healthLabel = when (getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
        BatteryManager.BATTERY_HEALTH_GOOD -> tr("Buena")
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> tr("Sobrecalentada")
        BatteryManager.BATTERY_HEALTH_DEAD -> tr("Agotada")
        BatteryManager.BATTERY_HEALTH_COLD -> tr("Fría")
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> tr("Sobretensión")
        else -> tr("Desconocida")
    }
    return BatteryState(
        percent = percent,
        isCharging = charging,
        statusLabel = statusLabel,
        healthLabel = healthLabel,
        temperatureC = getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
        voltageV = getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
    )
}

@Composable
private fun BatteryMeter(battery: BatteryState) {
    val tone = when {
        battery.isCharging -> NexusTokens.Color.Online
        battery.percent <= 15 -> NexusTokens.Color.Error
        battery.percent <= 30 -> NexusTokens.Color.Warning
        else -> MaterialTheme.colorScheme.primary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when {
                battery.isCharging -> Icons.Outlined.BatteryChargingFull
                battery.percent <= 15 -> Icons.Outlined.BatteryAlert
                else -> Icons.Outlined.BatteryFull
            },
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(NexusTokens.IconSize.lg)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${battery.percent} %",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = NexusTokens.Color.TextPrimary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = battery.statusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(NexusTokens.Color.BgHigh)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction = (battery.percent / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(tone)
                )
            }
        }
    }
}

// ── Datos en vivo ────────────────────────────────────────────────────────────

@Composable
internal fun rememberTicker(periodMs: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(periodMs - (now % periodMs))
        }
    }
    return now
}

private data class MemoryState(
    val ram: String = "…",
    val storage: String = "…",
    val network: String = "…"
)

@Composable
private fun rememberMemoryState(context: Context): MemoryState {
    var state by remember { mutableStateOf(MemoryState()) }
    LaunchedEffect(Unit) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        while (true) {
            val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val stat = StatFs(Environment.getDataDirectory().path)
            state = MemoryState(
                ram = "${formatBytes(info.totalMem - info.availMem)} de ${formatBytes(info.totalMem)}",
                storage = "${formatBytes(stat.availableBytes)} de ${formatBytes(stat.totalBytes)}",
                network = currentNetworkLabel(context)
            )
            delay(3_000L)
        }
    }
    return state
}

private fun currentNetworkLabel(context: Context): String = runCatching {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "Sin conexión"
    val parts = buildList {
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> add("Wi-Fi")
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> add("Datos móviles")
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> add("Ethernet")
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
    }
    parts.joinToString(" + ").ifEmpty { "Otra" }
}.getOrDefault("Desconocida")

private data class StaticDeviceInfo(
    val brand: String,
    val model: String,
    val securityPatch: String,
    val processor: String,
    val abi: String,
    val display: String
)

private fun readStaticDeviceInfo(context: Context): StaticDeviceInfo {
    val metrics: DisplayMetrics = context.resources.displayMetrics
    return StaticDeviceInfo(
        brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            .let { if (Build.BRAND.equals(it, ignoreCase = true)) it else "$it (${Build.BRAND})" },
        model = Build.MODEL,
        securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { "—" },
        processor = Build.SOC_MODEL.takeUnless { it.isBlank() || it == Build.UNKNOWN } ?: Build.HARDWARE,
        abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "—",
        display = "${metrics.widthPixels} × ${metrics.heightPixels} · ${metrics.densityDpi} ppp"
    )
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024 * 1024)
    return if (gb >= 1) "%.1f GB".format(gb) else "%.0f MB".format(bytes / (1024.0 * 1024))
}

private fun formatUptime(millis: Long): String {
    val totalMinutes = millis / 60_000
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return buildString {
        if (days > 0) append("$days d ")
        if (hours > 0 || days > 0) append("$hours h ")
        append("$minutes min")
    }
}
