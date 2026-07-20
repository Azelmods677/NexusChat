package com.Azelmods.App.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
        animationSpec = spring(
            stiffness = NexusTokens.Anim.SPRING_STIFFNESS,
            dampingRatio = NexusTokens.Anim.SPRING_DAMPING_LOW
        ),
        label = "card_scale"
    )

    val borderBrush = if (borderGlow)
        Brush.linearGradient(NexusTokens.Gradient.Brand)
    else
        Brush.linearGradient(listOf(NexusTokens.Color.GlassBorder, NexusTokens.Color.GlassFill))

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(NexusTokens.Radius.lg))
            .background(NexusTokens.Color.GlassFill)
            .border(1.dp, borderBrush, RoundedCornerShape(NexusTokens.Radius.lg))
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
