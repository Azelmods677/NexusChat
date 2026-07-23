package com.Azelmods.App.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.ui.theme.NexusTokens

/**
 * COMPONENTES PREMIUM NEXUSCHAT 2026
 * Componentes reutilizables con glassmorphism y spring physics
 */

// ─── 0. SUPERFICIE GLASS (modificador canónico) ─────────────────────
/**
 * La superficie "vidrio" de NexusChat: relleno translúcido + borde degradado
 * sutil + esquinas del sistema. Es EL único lugar donde se define el glass;
 * cualquier componente que quiera verse de vidrio aplica este modificador
 * en vez de reconstruir fill/border/shape a mano.
 *
 * @param glow borde con el gradiente de marca (para superficies destacadas).
 * @param shape forma de la superficie; por defecto la tarjeta del sistema.
 */
fun Modifier.nexusGlass(
    glow: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(NexusTokens.Radius.lg)
): Modifier {
    val borderBrush = if (glow)
        Brush.linearGradient(NexusTokens.Gradient.Brand)
    else
        Brush.linearGradient(listOf(NexusTokens.Color.GlassBorder, NexusTokens.Color.GlassFill))

    return this
        .clip(shape)
        .background(NexusTokens.Color.GlassFill)
        .border(1.dp, borderBrush, shape)
}

// ─── 1. GLASS CARD ──────────────────────────────────────────────────
@Composable
fun NexusGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = NexusTokens.Anim.springBouncy(),
        label = "card_scale"
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .nexusGlass(glow = borderGlow)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .padding(NexusTokens.Space.md),
        content = content
    )
}

// ─── 2. STATUS BADGE ────────────────────────────────────────────────
@Composable
fun NexusStatusBadge(
    text: String,
    color: Color = NexusTokens.Color.Primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
            .background(color.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.4f),
                shape = RoundedCornerShape(NexusTokens.Radius.pill)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = NexusTokens.FontSize.xs,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}
