package com.Azelmods.App.ui.screens.settings

import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.toArgb
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
    var showAccentPicker by remember { mutableStateOf(false) }
    if (showAccentPicker) {
        CustomAccentDialog(
            initial = AppTheme.getPrimaryColor(accentColor),
            onDismiss = { showAccentPicker = false },
            onPick = { hex ->
                viewModel.setAccentColor(hex)
                showAccentPicker = false
            }
        )
    }
    val activeAccentName = AppTheme.ACCENT_SWATCHES.firstOrNull { it.id == activeAccent }?.displayName.orEmpty()

    Scaffold(
        topBar = { NexusTopBar(title = tr("Color y fondo"), onBack = { navController.popBackStack() }) },
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
                // El último hueco abre el selector libre: cualquier color, no solo los de la lista.
                val customActive = AppTheme.isCustom(activeAccent)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (customActive) {
                                androidx.compose.ui.graphics.SolidColor(AppTheme.getPrimaryColor(activeAccent))
                            } else {
                                androidx.compose.ui.graphics.Brush.sweepGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                        Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            }
                        )
                        .then(if (customActive) Modifier.border(2.dp, NexusTokens.Color.TextPrimary, CircleShape) else Modifier)
                        .clickable { showAccentPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (customActive) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = tr("Elegir cualquier color"),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
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

/** Cualquier color: tono, intensidad y brillo, viendo el resultado mientras se mueve. */
@Composable
private fun CustomAccentDialog(initial: Color, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val start = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember { mutableStateOf(start[0]) }
    var saturation by remember { mutableStateOf(start[1]) }
    var brightness by remember { mutableStateOf(start[2]) }
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
    val hex = "#%06X".format(argb and 0xFFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Elegir cualquier color")) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(NexusTokens.Radius.lg))
                        .background(Color(argb)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(hex, color = if (Color(argb).luminance() > 0.34f) Color(0xFF0B0F0D) else Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text(tr("Tono"), style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextMuted)
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text(tr("Intensidad"), style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextMuted)
                Slider(value = saturation, onValueChange = { saturation = it })
                Text(tr("Brillo"), style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextMuted)
                Slider(value = brightness, onValueChange = { brightness = it })
            }
        },
        confirmButton = { TextButton(onClick = { onPick(hex) }) { Text(tr("Usar este color")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}
