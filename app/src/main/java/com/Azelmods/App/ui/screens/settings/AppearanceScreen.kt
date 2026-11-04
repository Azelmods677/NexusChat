package com.Azelmods.App.ui.screens.settings

import com.Azelmods.App.i18n.tr

import androidx.compose.material.icons.outlined.Palette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.NexusListRow
import com.Azelmods.App.ui.components.NexusRowValue
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.theme.AppTheme
import com.Azelmods.App.ui.theme.NexusTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val accentColor by viewModel.accentColor.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val wallpaperType by viewModel.wallpaperType.collectAsState()

    val wallpaperLabel = when (wallpaperType) {
        "image" -> tr("Imagen")
        "video" -> tr("Vídeo")
        "color" -> tr("Color")
        else -> tr("Predeterminado")
    }
    val fontSizeLabel = when (fontSize.uppercase().replace(" ", "").replace("_", "")) {
        "SMALL", "PEQUEÑO", "PEQUENO" -> tr("Pequeña")
        "LARGE", "GRANDE" -> tr("Grande")
        "EXTRALARGE", "XLARGE", "EXTRAGRANDE", "MUYGRANDE" -> tr("Muy grande")
        else -> tr("Normal")
    }
    val activeAccent = AppTheme.canonicalId(accentColor)
    val activeAccentName = AppTheme.ACCENT_SWATCHES.firstOrNull { it.id == activeAccent }?.displayName.orEmpty()

    Scaffold(
        topBar = { NexusTopBar(title = tr("Apariencia"), onBack = { navController.popBackStack() }) },
        containerColor = NexusTokens.Color.BgBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            NexusSectionHeader(tr("Color de acento"), trailing = {
                Text(
                    activeAccentName,
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.TextMuted
                )
            })
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                maxItemsInEachRow = 6
            ) {
                AppTheme.ACCENT_SWATCHES.forEach { swatch ->
                    val selected = swatch.id == activeAccent
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatch.color)
                            .then(
                                if (selected) Modifier.border(2.dp, NexusTokens.Color.TextPrimary, CircleShape)
                                else Modifier
                            )
                            .clickable { viewModel.setAccentColor(swatch.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Seleccionado: ${swatch.displayName}",
                                tint = if (swatch.color.luminance() > 0.34f) Color(0xFF0B0F0D) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            NexusSectionHeader(tr("Avatares"))
            val context = androidx.compose.ui.platform.LocalContext.current
            val themePreferences = remember { com.Azelmods.App.data.preferences.ThemePreferences(context) }
            var avatarRings by remember { mutableStateOf(themePreferences.areAvatarRingsEnabled()) }
            com.Azelmods.App.ui.components.NexusSwitchRow(
                title = tr("Aros de color"),
                subtitle = tr("Cada contacto con su color en la lista de chats. Las historias siempre lo llevan."),
                icon = Icons.Outlined.Palette,
                checked = avatarRings,
                onCheckedChange = {
                    avatarRings = it
                    themePreferences.setAvatarRingsEnabled(it)
                }
            )

            NexusSectionHeader(tr("Pantalla"))
            val splashAnimation by viewModel.splashAnimation.collectAsState()
            com.Azelmods.App.ui.components.NexusSwitchRow(
                title = tr("Animación de entrada"),
                subtitle = tr("Al abrir la app. Apágala para entrar directo."),
                icon = Icons.Outlined.AutoAwesome,
                checked = splashAnimation,
                onCheckedChange = viewModel::setSplashAnimation
            )
            NexusListRow(
                title = tr("Tamaño de letra"),
                icon = Icons.Outlined.FormatSize,
                onClick = { navController.navigate("font_size") },
                trailing = { NexusRowValue(fontSizeLabel) }
            )
            NexusListRow(
                title = tr("Fondo"),
                subtitle = tr("Imagen, vídeo o color detrás de los chats"),
                icon = Icons.Outlined.Wallpaper,
                onClick = { navController.navigate("wallpaper") },
                trailing = { NexusRowValue(wallpaperLabel) }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NexusListRow(
        title = title,
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
