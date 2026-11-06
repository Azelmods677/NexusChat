package com.Azelmods.App.ui.screens.splash

import com.Azelmods.App.i18n.tr

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens
import kotlin.math.cos
import kotlinx.coroutines.launch
import kotlin.math.sin

private const val WORDMARK = "NexusChat"
private const val DURATION_MS = 2_500
private const val EXIT_MS = 420
private val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** Progreso de 0 a 1 de un tramo [start]..[end] de la animación completa. */
private fun phase(t: Float, start: Float, end: Float): Float =
    EaseOut.transform(((t - start) / (end - start)).coerceIn(0f, 1f))

/**
 * La entrada de la app. "Nexus" es conexión, y eso es lo que se ve: puntos dispersos que se
 * buscan y se enlazan, un anillo que se cierra a su alrededor y, dentro, la N trazándose.
 */
@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val progress = remember { Animatable(if (viewModel.animated) 0f else 1f) }
    var animationDone by remember { mutableStateOf(!viewModel.animated) }
    val exit = remember { Animatable(0f) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (viewModel.animated) {
            launch {
                // Un toque leve justo cuando el anillo se cierra: se siente que encaja.
                kotlinx.coroutines.delay((DURATION_MS * RING_CLOSES).toLong())
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
            progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        }
        animationDone = true
    }

    LaunchedEffect(state.isAuthenticated, animationDone) {
        val isAuth = state.isAuthenticated ?: return@LaunchedEffect
        if (!animationDone) return@LaunchedEffect
        // Si una notificación ya llevó a otra pantalla (p. ej. una llamada entrante con la app
        // cerrada), no se navega: el popUpTo(splash) inclusive cerraría también esa pantalla y
        // se colgaría la llamada. Cuando el usuario vuelva atrás, el splash reaparece y navega.
        if (navController.currentDestination?.route != Screen.Splash.route) return@LaunchedEffect
        // La salida atraviesa el emblema: la app parece abrirse desde dentro de él.
        if (viewModel.animated) exit.animateTo(1f, tween(EXIT_MS, easing = EaseOut))
        navController.navigate(if (isAuth) Screen.Home.route else Screen.Login.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val t = progress.value
    val leaving = exit.value

    Box(
        Modifier
            .fillMaxSize()
            .background(NexusTokens.Color.BgDeep)
            .graphicsLayer { alpha = 1f - leaving },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) { drawSplashField(t, accent) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val zoom = 1f + leaving * leaving * 2.4f
                scaleX = zoom
                scaleY = zoom
            }
        ) {
            Canvas(Modifier.size(180.dp)) { drawNexusEmblem(t, accent) }
            Spacer(Modifier.height(20.dp))
            Wordmark(t)
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Mensajería privada con IA"),
                color = NexusTokens.Color.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.graphicsLayer { alpha = phase(t, 0.82f, 0.96f) }
            )
        }
    }
}

/** Letra a letra, subiendo: el nombre aparece cuando el emblema ya está completo. */
@Composable
private fun Wordmark(t: Float) {
    Row {
        WORDMARK.forEachIndexed { index, letter ->
            val start = 0.6f + index * 0.022f
            Text(
                letter.toString(),
                color = NexusTokens.Color.TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    val value = phase(t, start, start + 0.16f)
                    alpha = value
                    translationY = (1f - value) * 22.dp.toPx()
                }
            )
        }
    }
}

/**
 * Lo que rodea al emblema: motas que caen hacia el centro con distinta velocidad, lo que da
 * profundidad, y una onda que sale cuando el anillo se cierra.
 */
private fun DrawScope.drawSplashField(t: Float, accent: Color) {
    val center = Offset(size.width / 2f, size.height * 0.44f)
    val reach = maxOf(size.width, size.height) * 0.75f
    val fadeOut = 1f - phase(t, 0.7f, 0.98f)
    if (fadeOut > 0f) {
        FIELD.forEach { mote ->
            // Las más lejanas (depth bajo) tardan más: así se percibe la distancia.
            val travel = phase(t, 0f, 0.42f + 0.4f * (1f - mote.depth))
            val distance = reach * mote.distance * (1f - travel)
            val radians = Math.toRadians((mote.angle + travel * 40f * mote.spin).toDouble())
            val position = Offset(
                center.x + (cos(radians) * distance).toFloat(),
                center.y + (sin(radians) * distance).toFloat()
            )
            val appear = phase(t, 0f, 0.14f)
            drawCircle(
                if (mote.bright) Color.White else accent,
                radius = 1.2f + mote.depth * 3f,
                center = position,
                alpha = (0.25f + mote.depth * 0.6f) * appear * fadeOut
            )
        }
    }
    // Onda expansiva: dos anillos que se alejan y se apagan.
    listOf(0f, 0.09f).forEach { delayed ->
        val wave = phase(t, RING_CLOSES + delayed, RING_CLOSES + delayed + 0.3f)
        if (wave > 0f && wave < 1f) {
            drawCircle(
                accent.copy(alpha = (1f - wave) * 0.5f),
                radius = size.minDimension * (0.2f + wave * 0.75f),
                center = center,
                style = Stroke(width = 3f * (1f - wave) + 0.5f)
            )
        }
    }
}

private class Mote(val angle: Float, val distance: Float, val depth: Float, val spin: Float, val bright: Boolean)

/** Siempre las mismas posiciones: la entrada se ve igual cada vez, como una marca. */
private val FIELD: List<Mote> = kotlin.random.Random(7).let { random ->
    List(70) {
        Mote(
            angle = random.nextFloat() * 360f,
            distance = 0.35f + random.nextFloat() * 0.65f,
            depth = random.nextFloat(),
            spin = if (random.nextBoolean()) 1f else -1f,
            bright = random.nextInt(4) == 0
        )
    }
}

/** En este punto de la animación el anillo termina de cerrarse. */
private const val RING_CLOSES = 0.66f

/** El emblema de NexusChat. Con [t] = 1 se ve completo, sin animar. */
internal fun DrawScope.drawNexusEmblem(t: Float, accent: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val ringRadius = size.minDimension * 0.36f

    // Halo: crece cuando la N está casi trazada y se queda respirando.
    val glow = phase(t, 0.52f, 0.86f)
    if (glow > 0f) {
        drawCircle(
            Brush.radialGradient(
                listOf(accent.copy(alpha = 0.32f * glow), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.5f * (0.7f + 0.3f * glow)
            ),
            radius = size.minDimension * 0.5f,
            center = center
        )
    }

    // Nodos: llegan de lejos, se enlazan entre sí y terminan sobre el anillo.
    val gather = phase(t, 0f, 0.42f)
    val settle = phase(t, 0.36f, 0.62f)
    val nodeAlpha = 1f - phase(t, 0.58f, 0.74f)
    val nodes = NODE_ANGLES.mapIndexed { index, angle ->
        val radians = Math.toRadians(angle.toDouble())
        val swirl = Math.toRadians((1f - gather) * 70.0 * (if (index % 2 == 0) 1 else -1))
        val far = size.minDimension * (0.5f + 0.42f * (index % 3) / 2f)
        val distance = far + (ringRadius - far) * gather
        Offset(
            center.x + (cos(radians + swirl) * distance).toFloat(),
            center.y + (sin(radians + swirl) * distance).toFloat()
        )
    }
    val linkAlpha = phase(t, 0.14f, 0.36f) * nodeAlpha
    if (linkAlpha > 0f) {
        nodes.forEachIndexed { i, a ->
            val b = nodes[(i + 1) % nodes.size]
            val c = nodes[(i + 3) % nodes.size]
            drawLine(accent.copy(alpha = 0.55f * linkAlpha), a, b, strokeWidth = 2.5f, cap = StrokeCap.Round)
            drawLine(accent.copy(alpha = 0.22f * linkAlpha), a, c, strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }
    if (nodeAlpha > 0f) {
        nodes.forEach { node ->
            drawCircle(accent.copy(alpha = nodeAlpha), radius = 5f + 3f * gather * (1f - settle), center = node)
            drawCircle(Color.White.copy(alpha = 0.85f * nodeAlpha), radius = 2.2f, center = node)
        }
    }

    // Anillo: se cierra a partir del punto en que los nodos ya han llegado.
    val sweep = phase(t, 0.32f, 0.66f)
    if (sweep > 0f) {
        rotate(-90f + (1f - sweep) * -120f, center) {
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.25f), accent, Color.White, accent.copy(alpha = 0.25f)), center),
                startAngle = 0f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )
        }
    }

    // La N, trazada de un solo gesto.
    val stroke = phase(t, 0.44f, 0.74f)
    if (stroke > 0f) {
        val unit = ringRadius / 50f
        val origin = Offset(center.x - 50f * unit, center.y - 50f * unit)
        val path = Path().apply {
            moveTo(origin.x + 26f * unit, origin.y + 72f * unit)
            lineTo(origin.x + 26f * unit, origin.y + 28f * unit)
            lineTo(origin.x + 74f * unit, origin.y + 72f * unit)
            lineTo(origin.x + 74f * unit, origin.y + 28f * unit)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * stroke, partial, true)
        drawPath(
            partial,
            Brush.linearGradient(
                listOf(Color.White, accent),
                start = Offset(origin.x, origin.y),
                end = Offset(origin.x + 100f * unit, origin.y + 100f * unit)
            ),
            style = Stroke(width = 10f * unit.coerceAtLeast(0.6f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val sheen = phase(t, 0.76f, 0.95f)
        if (sheen > 0f && sheen < 1f) {
            val x = origin.x + (sheen * 160f - 30f) * unit
            drawPath(
                partial,
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.Transparent),
                    start = Offset(x - 22f * unit, origin.y),
                    end = Offset(x + 22f * unit, origin.y + 44f * unit)
                ),
                style = Stroke(width = 11f * unit.coerceAtLeast(0.6f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

private val NODE_ANGLES = listOf(-90f, -38f, 14f, 66f, 118f, 170f, 222f)
