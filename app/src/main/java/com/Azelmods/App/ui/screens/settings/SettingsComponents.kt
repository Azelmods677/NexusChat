package com.Azelmods.App.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.Azelmods.App.ui.components.NexusListRow
import com.Azelmods.App.ui.theme.NexusTokens

/** Entrada de ajustes con título primero (firma usada por las subpantallas). */
@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = NexusTokens.Color.TextSecondary,
    onClick: () -> Unit
) {
    NexusListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = iconTint,
        onClick = onClick
    )
}
