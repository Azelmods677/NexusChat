package com.Azelmods.App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Azelmods.App.ui.theme.NexusTokens

/**
 * Superficie plana con un filo de 1 dp. Conserva el nombre antiguo para las pantallas
 * que aún la usan, pero ya no hay vidrio, degradado ni rebote al pulsar.
 *
 * @param glow marca la superficie con el acento (solo para lo verdaderamente destacado).
 */
fun Modifier.nexusGlass(
    glow: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(NexusTokens.Radius.lg)
): Modifier = this
    .clip(shape)
    .background(NexusTokens.Color.BgSurface)
    .border(
        width = 1.dp,
        color = if (glow) NexusTokens.Color.Primary.copy(alpha = 0.5f) else NexusTokens.Color.Hairline,
        shape = shape
    )

/** Tarjeta plana. Úsala con moderación: la mayoría del contenido va mejor en listas. */
@Composable
fun NexusGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .nexusGlass(glow = borderGlow)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(NexusTokens.Space.md),
        content = content
    )
}

/** Etiqueta de estado: texto del tono indicado sobre un fondo apenas teñido. */
@Composable
fun NexusStatusBadge(
    text: String,
    color: Color = NexusTokens.Color.TextSecondary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.xs))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = NexusTokens.FontSize.xs,
            fontWeight = FontWeight.Medium
        )
    }
}
