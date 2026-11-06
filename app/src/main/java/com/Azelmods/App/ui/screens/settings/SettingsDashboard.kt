package com.Azelmods.App.ui.screens.settings

import com.Azelmods.App.i18n.tr

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Azelmods.App.data.security.tor.AnonymityPolicy
import com.Azelmods.App.ui.components.AvatarRing
import com.Azelmods.App.ui.components.ProfileColors
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.pressScale
import com.Azelmods.App.ui.components.staggeredEnter
import com.Azelmods.App.ui.theme.NexusTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cabecera de Ajustes: quién eres y cómo está el teléfono de un vistazo, con datos en vivo.
 * Tocar una tarjeta lleva a su sección.
 */
@Composable
fun SettingsDashboard(
    userName: String,
    userHandle: String,
    userPhoto: String?,
    userColor: Int,
    userId: String,
    onOpenProfile: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSecurity: () -> Unit
) {
    val context = LocalContext.current
    val battery = rememberBatteryState(context)
    val now = rememberTicker(1_000L)
    val deviceName = remember {
        val brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL else "$brand ${Build.MODEL}"
    }
    val storageUsed = remember {
        runCatching {
            val disk = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            ((disk.totalBytes - disk.availableBytes) * 100 / disk.totalBytes).toInt()
        }.getOrDefault(0)
    }
    val accent = MaterialTheme.colorScheme.primary
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    // Se anima solo al abrir la pantalla.
    val animateEntry = remember { mutableStateOf(true) }.value

    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DashboardCard(
            onClick = onOpenProfile,
            modifier = Modifier
                .fillMaxWidth()
                .staggeredEnter(0, enabled = animateEntry)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = userName.ifBlank { "?" },
                    photoUrl = userPhoto,
                    size = 58.dp,
                    ring = AvatarRing(ProfileColors.forUser(userId.ifBlank { userName }, userColor), width = 2.5.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        userName.ifBlank { tr("Tu perfil") },
                        style = MaterialTheme.typography.titleLarge,
                        color = NexusTokens.Color.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (userHandle.isNotBlank()) {
                        Text(
                            "@${userHandle.removePrefix("@")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexusTokens.Color.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NexusTokens.Color.TextMuted
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                icon = if (battery.isCharging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryStd,
                iconColor = batteryColor(battery.percent, battery.isCharging),
                title = tr("Batería"),
                value = "${battery.percent} %",
                detail = battery.statusLabel,
                progress = battery.percent / 100f,
                progressColor = if (battery.percent <= 15) NexusTokens.Color.Error else accent,
                onClick = onOpenAbout,
                modifier = Modifier
                    .weight(1f)
                    .staggeredEnter(1, enabled = animateEntry)
            )
            MetricCard(
                icon = Icons.Outlined.Smartphone,
                iconColor = accent,
                title = tr("Dispositivo"),
                value = deviceName,
                detail = "$storageUsed % " + tr("de memoria usada"),
                progress = storageUsed / 100f,
                progressColor = accent,
                onClick = onOpenAbout,
                modifier = Modifier
                    .weight(1f)
                    .staggeredEnter(2, enabled = animateEntry)
            )
        }

        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                icon = Icons.Outlined.Schedule,
                iconColor = accent,
                title = tr("Hora"),
                value = timeFormat.format(Date(now)),
                detail = dateFormat.format(Date(now)),
                onClick = onOpenAbout,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .staggeredEnter(3, enabled = animateEntry)
            )
            val anonymous = AnonymityPolicy.active
            val ghost = AnonymityPolicy.ghost
            DashboardCard(
                onClick = onOpenSecurity,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .staggeredEnter(4, enabled = animateEntry)
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    UserAvatar(
                        name = userName.ifBlank { "?" },
                        photoUrl = userPhoto,
                        size = 44.dp,
                        ring = AvatarRing(listOf(accent, accent), width = 2.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        userName.ifBlank { tr("Tu perfil") },
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        when {
                            anonymous -> tr("Modo anónimo")
                            ghost -> tr("Modo fantasma")
                            else -> tr("Conexión normal")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (anonymous || ghost) NexusTokens.Color.Online else NexusTokens.Color.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun batteryColor(percent: Int, charging: Boolean): Color = when {
    charging -> NexusTokens.Color.Online
    percent <= 15 -> NexusTokens.Color.Error
    percent <= 30 -> NexusTokens.Color.Warning
    else -> NexusTokens.Color.Online
}

@Composable
private fun DashboardCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    progressColor: Color = NexusTokens.Color.Online
) {
    DashboardCard(onClick = onClick, modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextMuted)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            if (progress != null) {
                val animated by animateFloatAsState(progress, tween(700), label = "barraTarjeta")
                Box(
                    Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(NexusTokens.Color.BgHigh)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(animated.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(progressColor)
                    )
                }
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = NexusTokens.Color.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Icono de una entrada de ajustes sobre su color: ayuda a encontrar cada sección de un vistazo. */
@Composable
fun SettingsIconTile(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.md))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}
