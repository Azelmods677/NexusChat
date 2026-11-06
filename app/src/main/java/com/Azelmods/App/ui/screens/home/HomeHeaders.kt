package com.Azelmods.App.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.Azelmods.App.data.profile.MyProfileData
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.AvatarRing
import com.Azelmods.App.ui.components.HomeStyle
import com.Azelmods.App.ui.components.ProfileColors
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.pressScale
import com.Azelmods.App.ui.theme.NexusTokens

/** El título de la barra superior según el diseño de inicio elegido. */
@Composable
fun HomeTitle(style: HomeStyle, me: MyProfileData, showAvatar: Boolean, onOpenProfile: () -> Unit) {
    if (style == HomeStyle.PROFILE || style == HomeStyle.NAME) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(43.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenProfile),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(name = me.name, photoUrl = me.photoUrl, size = 38.dp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                tr("Chats"),
                style = MaterialTheme.typography.headlineMedium,
                color = NexusTokens.Color.TextPrimary
            )
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showAvatar && style == HomeStyle.CLASSIC) {
            UserAvatar(
                name = me.name,
                photoUrl = me.photoUrl,
                size = 36.dp,
                modifier = Modifier.clickable(onClick = onOpenProfile)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            tr("Chats"),
            style = MaterialTheme.typography.headlineMedium,
            color = NexusTokens.Color.TextPrimary
        )
    }
}

/** Lo que el diseño elegido coloca entre la barra superior y la lista. */
@Composable
fun HomeHeader(
    style: HomeStyle,
    me: MyProfileData,
    ghost: Boolean,
    anonymous: Boolean,
    onOpenProfile: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenStories: () -> Unit,
    onOpenCalls: () -> Unit
) {
    when (style) {
        HomeStyle.CLASSIC, HomeStyle.PROFILE -> Unit
        HomeStyle.NAME -> NamePanel(me)
        HomeStyle.ARC -> ArcHeader(me, onOpenProfile)
        HomeStyle.CARD -> ProfileCard(me, ghost, anonymous, onOpenProfile)
        HomeStyle.COVER -> CoverBanner(me, onOpenProfile)
        HomeStyle.STRIP -> TabStrip(me, onOpenContacts, onOpenStories, onOpenCalls, onOpenProfile)
    }
}

@Composable
private fun ProfileCard(me: MyProfileData, ghost: Boolean, anonymous: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = me.name,
            photoUrl = me.photoUrl,
            size = 52.dp,
            ring = AvatarRing(ProfileColors.forUser(me.uid.ifBlank { me.name }, me.color), width = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                me.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (me.handle.isNotBlank()) {
                Text(
                    "@${me.handle.removePrefix("@")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Solo aparece si cambia cómo te ven los demás; "normal" no necesita aviso.
        if (ghost || anonymous) {
            Icon(
                if (anonymous) Icons.Outlined.Security else Icons.Outlined.VisibilityOff,
                if (anonymous) tr("Modo anónimo") else tr("Modo fantasma"),
                tint = NexusTokens.Color.Online,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CoverBanner(me: MyProfileData, onClick: () -> Unit) {
    val palette = ProfileColors.forUser(me.uid.ifBlank { me.name }, me.color)
    Box(
        Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(136.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(Brush.linearGradient(palette))
            .clickable(onClick = onClick)
    ) {
        me.coverUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        // El nombre tiene que leerse sobre cualquier portada.
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = me.name,
                photoUrl = me.photoUrl,
                size = 56.dp,
                ring = AvatarRing(palette, width = 2.5.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    me.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (me.handle.isNotBlank()) {
                    Text(
                        "@${me.handle.removePrefix("@")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Panel con las esquinas de arriba redondeadas y una píldora con tu nombre. */
@Composable
private fun NamePanel(me: MyProfileData) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(NexusTokens.Color.BgSurface)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            me.name.ifBlank { tr("Chats") },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NexusTokens.Color.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(NexusTokens.Color.BgElevated)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/** Una banda con el borde de arriba curvado hacia abajo y tu foto en el centro, sobre la curva. */
@Composable
private fun ArcHeader(me: MyProfileData, onClick: () -> Unit) {
    val band = NexusTokens.Color.BgSurface
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val curve = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, size.height * 0.28f)
                quadraticTo(size.width / 2f, size.height * 0.95f, size.width, size.height * 0.28f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(curve, band)
        }
        Box(
            Modifier
                .size(56.dp)
                .border(1.4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(name = me.name, photoUrl = me.photoUrl, size = 50.dp)
        }
    }
}

/** Barra de 50 dp sin textos: los iconos a los lados y tu foto en el centro. */
@Composable
private fun TabStrip(
    me: MyProfileData,
    onOpenContacts: () -> Unit,
    onOpenStories: () -> Unit,
    onOpenCalls: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(NexusTokens.Color.BgSurface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StripAction(Icons.Outlined.Contacts, tr("Contactos"), onOpenContacts, Modifier.weight(1f))
        StripAction(Icons.Outlined.AutoStories, tr("Historias"), onOpenStories, Modifier.weight(1f))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .padding(bottom = 3.dp)
                    .size(41.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenProfile),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(name = me.name, photoUrl = me.photoUrl, size = 34.dp)
            }
        }
        StripAction(Icons.Outlined.Call, tr("Llamadas"), onOpenCalls, Modifier.weight(1f))
    }
}

@Composable
private fun StripAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}
