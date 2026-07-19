package com.Azelmods.App.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.ui.theme.NexusTokens
import com.Azelmods.App.ui.theme.DarkBubbleDeep

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

// ─── 2. GRADIENT BUTTON ─────────────────────────────────────────────
@Composable
fun NexusPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    gradient: List<Color> = NexusTokens.Gradient.Brand
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.45f),
        label = "btn_scale"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(52.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
            .background(
                if (enabled) Brush.horizontalGradient(gradient)
                else Brush.horizontalGradient(
                    listOf(NexusTokens.Color.BgElevated, DarkBubbleDeep)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "btn_content"
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ─── 3. SHIMMER LOADING ─────────────────────────────────────────────
@Composable
fun NexusShimmer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(NexusTokens.Radius.md)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offsetX by transition.animateFloat(
        initialValue = -1200f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearEasing)
        ),
        label = "shimmer_x"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = NexusTokens.Gradient.Shimmer,
                    start = Offset(offsetX, 0f),
                    end = Offset(offsetX + 600f, 300f)
                )
            )
    )
}

// ─── 4. STATUS BADGE ────────────────────────────────────────────────
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
