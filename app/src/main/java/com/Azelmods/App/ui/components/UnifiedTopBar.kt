package com.Azelmods.App.ui.components

import com.Azelmods.App.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.theme.NexusTokens

/**
 * Barra superior compartida: opaca y sin animaciones. El contenido de la pantalla pasa
 * por debajo de un fondo sólido, así que el título siempre se lee.
 *
 * Los parámetros antiguos de efectos ya no hacen nada; se conservan para no romper a
 * quien los pasa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    userName: String? = null,
    userPhotoUrl: String? = null,
    userSubtitle: String? = null,
    onUserClick: (() -> Unit)? = null,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = NexusTokens.Color.BgSurface,
    contentColor: Color = NexusTokens.Color.TextPrimary,
    @Suppress("UNUSED_PARAMETER") enableGlassmorphism: Boolean = false,
    @Suppress("UNUSED_PARAMETER") enableAnimations: Boolean = false
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (userName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (onUserClick != null) Modifier.clickable(onClick = onUserClick) else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NexusTokens.Color.BgHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = tr("Foto de perfil"),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = contentColor,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            color = contentColor,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!userSubtitle.isNullOrBlank()) {
                            Text(
                                text = userSubtitle,
                                color = NexusTokens.Color.TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = title,
                    color = contentColor,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (showBackButton && onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Atrás"),
                        tint = contentColor
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            scrolledContainerColor = backgroundColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = NexusTokens.Color.TextSecondary
        )
    )
}

/** Botón de icono de barra. */
@Composable
fun ModernIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = NexusTokens.Color.TextSecondary,
    @Suppress("UNUSED_PARAMETER") enableAnimation: Boolean = false
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(NexusTokens.IconSize.md)
        )
    }
}

/** Barra de las pantallas de perfil. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedProfileTopBar(
    userName: String,
    userPhotoUrl: String? = null,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = userName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Atrás"),
                    tint = Color.White
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier
    )
}

/** Icono de acción de barra. */
@Composable
fun TopBarActionIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = NexusTokens.Color.TextSecondary
) {
    ModernIconButton(icon = icon, contentDescription = contentDescription, onClick = onClick, tint = tint)
}
