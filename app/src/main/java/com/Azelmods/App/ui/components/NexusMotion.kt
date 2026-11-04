package com.Azelmods.App.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/*
 * Movimiento de la app. Todo es corto y responde a algo que acaba de pasar: pulsar, entrar en
 * una pantalla o recibir un mensaje. Nada se mueve solo para llamar la atención.
 */

/** Respuesta física al pulsar: el elemento cede un poco bajo el dedo. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Entrada escalonada para las filas visibles al abrir una lista.
 * Solo la primera vez: repetirla al desplazarse se notaría como parpadeo.
 */
@Composable
fun Modifier.staggeredEnter(index: Int, enabled: Boolean = true): Modifier {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    val distance = with(LocalDensity.current) { 18.dp.toPx() }
    LaunchedEffect(Unit) {
        if (enabled) {
            delay(index.coerceAtMost(MAX_STAGGERED) * STAGGER_MS)
            progress.animateTo(1f, tween(ENTER_MS, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * distance
    }
}

/** Aparición de un mensaje recién llegado: sube y crece un poco desde su esquina. */
@Composable
fun Modifier.messageEnter(animate: Boolean, fromEnd: Boolean): Modifier {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    val distance = with(LocalDensity.current) { 14.dp.toPx() }
    LaunchedEffect(Unit) {
        if (animate) {
            progress.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }
    return graphicsLayer {
        val value = progress.value
        alpha = value.coerceIn(0f, 1f)
        translationY = (1f - value) * distance
        val scale = 0.9f + 0.1f * value
        scaleX = scale
        scaleY = scale
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (fromEnd) 1f else 0f, 1f)
    }
}

private const val MAX_STAGGERED = 10
private const val STAGGER_MS = 28L
private const val ENTER_MS = 280
