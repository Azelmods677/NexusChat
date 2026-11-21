package com.Azelmods.App.ui.screens.welcome

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Expresiones de Nexa: ojos + boca en reposo. */
enum class NexaMood(val eyes: String, val mouth: String, val bouncy: Boolean = false) {
    SMILE("neutral", "smile"),
    HAPPY("happy", "open", bouncy = true),
    WINK("wink", "cat"),
    SURPRISE("surprise", "o"),
    SMUG("smug", "cat"),
    THINK("think", "flat"),
    LOVE("love", "open", bouncy = true),
    EXCITED("sparkle", "open", bouncy = true)
}

/** Postura: quieta, señalando arriba (POINT) o abajo (DOWN), o saludando. */
enum class NexaPose(val key: String) { IDLE("idle"), POINT("point"), WAVE("wave"), DOWN("down") }

/** Una N del diseño (top, cascos): posición en el sprite, color de fondo y forma. */
private class NexaGlyph(val x: Int, val y: Int, val bg: Char, val rows: List<String>)
private val N_SMALL = listOf("n..n", "nn.n", "n.nn", "n..n")
private val NEXA_GLYPHS = listOf(
    NexaGlyph(26, 49, 'c', listOf("n...n", "nn..n", "n.n.n", "n..nn", "n...n")),
    NexaGlyph(6, 29, 'D', N_SMALL),
    NexaGlyph(46, 29, 'D', N_SMALL)
)

/** Dónde queda la mano que señala, como fracción del sprite sin girar. */
fun nexaHandFraction(pose: NexaPose): Offset = when (pose) {
    NexaPose.DOWN -> Offset(50f / 56f, 67f / 84f)
    NexaPose.WAVE -> Offset(45f / 56f, 32f / 84f)
    else -> Offset(54f / 56f, 36f / 84f)
}

/**
 * Nexa chibi en pixel art, de cuerpo entero. Respira, parpadea, da saltitos en las expresiones
 * alegres, mueve la boca con cada palabra mientras [talking] ([wordTick] cambia en cada una),
 * camina moviendo las piernas si [walking] y levanta el brazo según [pose]. [facingRight] gira
 * solo el dibujo: los efectos ("?", "!", corazones) nunca salen al revés.
 */
@Composable
fun NexaCharacter(
    mood: NexaMood,
    talking: Boolean,
    wordTick: Int,
    modifier: Modifier = Modifier,
    pose: NexaPose = NexaPose.IDLE,
    walking: Boolean = false,
    facingRight: Boolean = true
) {
    val time = rememberInfiniteTransition(label = "nexa")
    val breath by time.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse), label = "breath")
    val sway by time.animateFloat(-1f, 1f, infiniteRepeatable(tween(3400, easing = EaseInOutSine), RepeatMode.Reverse), label = "sway")
    val hop by time.animateFloat(0f, 1f, infiniteRepeatable(tween(520, easing = EaseOutQuad), RepeatMode.Reverse), label = "hop")
    val fx by time.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "fx")

    // Paso al caminar: pie izquierdo, quieta, pie derecho, quieta; con rebote.
    var stepFrame by remember { mutableStateOf(0) }
    LaunchedEffect(walking) {
        if (!walking) { stepFrame = 0; return@LaunchedEffect }
        var i = 0
        while (true) { stepFrame = intArrayOf(1, 0, 2, 0)[i % 4]; i++; delay(130) }
    }
    // Saludo: la mano va y viene.
    var waveTilt by remember { mutableStateOf(0f) }
    LaunchedEffect(pose) {
        if (pose != NexaPose.WAVE) { waveTilt = 0f; return@LaunchedEffect }
        var right = true
        while (true) { waveTilt = if (right) 1f else -1f; right = !right; delay(240) }
    }
    // Parpadeo natural cada 2,5–4,5 s; a veces doble.
    var blinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((2500..4500).random().toLong())
            blinking = true; delay(120); blinking = false
            if ((0..3).random() == 0) { delay(150); blinking = true; delay(100); blinking = false }
        }
    }
    // Boca al hablar: abierta o media en cada palabra, con un cierre corto entre medias.
    var mouthOpen by remember { mutableStateOf(0) }
    LaunchedEffect(wordTick, talking) {
        if (!talking) { mouthOpen = 0; return@LaunchedEffect }
        mouthOpen = if (wordTick % 2 == 0) 2 else 1
        delay(140); mouthOpen = 1
        delay(90); mouthOpen = 0
    }
    // Al cambiar de ánimo, un saltito de reacción.
    val react = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        react.snapTo(0f); react.animateTo(1f, tween(160, easing = EaseOutQuad))
        react.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 320f))
    }

    val eyes = when {
        blinking && mood.eyes in setOf("neutral", "think", "smug", "sparkle", "surprise") -> "closed"
        else -> mood.eyes
    }
    val mouth = when {
        talking && mouthOpen == 2 -> "open"
        talking && mouthOpen == 1 -> "half"
        talking -> "smile"
        else -> mood.mouth
    }
    val bounce = if (mood.bouncy && !walking) hop else 0f
    val walkBob = if (walking && stepFrame != 0) 1f else 0f
    val body = NexaSprites.bodies["${pose.key}_$stepFrame"] ?: NexaSprites.bodies.getValue("idle_0")

    Canvas(
        modifier.graphicsLayer {
            translationY = -bounce * 10f - breath * 2f - walkBob * 5f - react.value * 12f
            rotationZ = sway * 1.2f + waveTilt * 0.8f
            scaleY = 1f + breath * 0.012f
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
        }
    ) {
        val cols = body.first().length
        val rows = body.size
        val cell = minOf(size.width / cols, size.height / rows)
        val left = (size.width - cell * cols) / 2f
        val top = size.height - cell * rows
        // Solo el dibujo se gira; los efectos se colocan sin invertirse.
        fun sx(x: Float) = if (facingRight) x else size.width - x
        scale(scaleX = if (facingRight) 1f else -1f, scaleY = 1f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            fun px(x: Int, y: Int, c: Char) {
                val color = NexaSprites.palette[c] ?: return
                drawRect(color, Offset(left + x * cell, top + y * cell), Size(cell + 0.6f, cell + 0.6f))
            }
            body.forEachIndexed { y, line -> line.forEachIndexed { x, c -> px(x, y, c) } }
            // Girada, la N saldría al revés (И): se repinta invertida dentro del lienzo girado,
            // así se lee bien. Solo donde se ve la letra (no encima de una mano o un brazo).
            if (!facingRight) NEXA_GLYPHS.forEach { gl ->
                val w = gl.rows.first().length
                gl.rows.forEachIndexed { dy, row ->
                    for (dx in 0 until w) {
                        val x = gl.x + dx; val y = gl.y + dy
                        val here = body.getOrNull(y)?.getOrNull(x) ?: continue
                        if (here != gl.bg && here != 'n' && here != 'c' && here != 'D') continue
                        px(x, y, if (row[w - 1 - dx] == 'n') 'n' else gl.bg)
                    }
                }
            }
            val ex = NexaSprites.EYE_X; val erx = NexaSprites.EYE_RX; val ey = NexaSprites.EYE_Y
            val ew = NexaSprites.EYE_W; val eh = NexaSprites.EYE_H
            for (y in ey until ey + eh) for (x in 0 until ew) { px(ex + x, y, 'S'); px(erx + x, y, 'S') }
            val leftEye = NexaSprites.eyes[eyes] ?: NexaSprites.eyes.getValue("neutral")
            val rightEye = NexaSprites.eyes[if (eyes == "wink") "happy" else eyes] ?: leftEye
            for (dy in 0 until eh) for (dx in 0 until ew) {
                leftEye[dy][dx].let { if (it != '.') px(ex + dx, ey + dy, it) }
                rightEye[dy][ew - 1 - dx].let { if (it != '.') px(erx + dx, ey + dy, it) }
            }
            if (mood.bouncy || mood == NexaMood.SMUG || mood == NexaMood.WINK) for (i in 0 until 3) {
                px(NexaSprites.BLUSH_LX + i, NexaSprites.BLUSH_Y, 'B'); px(NexaSprites.BLUSH_RX + i, NexaSprites.BLUSH_Y, 'B')
            }
            val mx = NexaSprites.MOUTH_X; val my = NexaSprites.MOUTH_Y
            for (y in my until my + NexaSprites.MOUTH_H) for (x in mx until mx + NexaSprites.MOUTH_W) px(x, y, 'S')
            (NexaSprites.mouths[mouth] ?: NexaSprites.mouths.getValue("smile")).forEachIndexed { dy, line ->
                line.forEachIndexed { dx, c -> if (c != '.') px(mx + dx, my + dy, c) }
            }
        }
        // Efectos anime alrededor de la cabeza.
        val headX = sx(left + cell * 44f)
        val headY = top + cell * 8f
        when (mood) {
            NexaMood.LOVE -> repeat(3) { n ->
                val t = (fx + n / 3f) % 1f
                nexaHeart(Offset(headX - cell * (20f - n * 9f), headY + cell * 8f - t * cell * 22f), cell * (2.6f + n * 0.4f), Color(0xFFFF4F8B).copy(alpha = 1f - t))
            }
            NexaMood.HAPPY, NexaMood.EXCITED -> repeat(3) { n ->
                val t = (fx + n / 3f) % 1f
                nexaSparkle(Offset(sx(left + cell * (4f + n * 24f)), top + cell * (10f + n * 6f)), kotlin.math.sin(t * Math.PI.toFloat()) * cell * 2.6f, Color(0xFFC9A6FF))
            }
            NexaMood.THINK -> nexaMark("?", Offset(headX, headY - fx * 0f - kotlin.math.sin(fx * 6.28f) * cell), cell * 10f)
            NexaMood.SURPRISE -> nexaMark("!", Offset(headX, headY - react.value * cell * 3f), cell * 11f)
            else -> {}
        }
    }
}

/**
 * Voz de Nexa con el motor de voz del teléfono. Elige una voz en español que funcione sin
 * internet, de la mejor calidad disponible, y le sube un poco el tono para un timbre más anime.
 * [wordTick] avanza con cada palabra para sincronizar la boca.
 */
class NexaVoice(context: Context) {
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking
    private val _wordTick = MutableStateFlow(0)
    val wordTick: StateFlow<Int> = _wordTick
    private var ready = false
    private var pending: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            configure()
            ready = true
            pending?.let { speak(it) }
            pending = null
        }
    }

    private fun configure() {
        val spanish = tts.voices.orEmpty().filter { it.locale.language == "es" && !it.isNetworkConnectionRequired }
        // Las voces neuronales de Google llevan nombres con "x-…" y calidad alta; se prefieren
        // las de América Latina, que suelen ser femeninas por defecto.
        val best: Voice? = spanish.sortedWith(
            compareByDescending<Voice> { it.quality }
                .thenByDescending { it.locale.country in setOf("US", "MX", "CO", "AR") }
                .thenByDescending { !it.name.contains("male", ignoreCase = true) || it.name.contains("female", ignoreCase = true) }
        ).firstOrNull()
        if (best != null) tts.voice = best else tts.language = Locale("es", "US")
        // Suena como asistente: baja un poco la música en vez de mezclarse con ella.
        tts.setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        tts.setPitch(1.18f)
        tts.setSpeechRate(1.02f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = true }
            override fun onDone(utteranceId: String?) { _speaking.value = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { _speaking.value = false }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                _wordTick.value = _wordTick.value + 1
            }
        })
    }

    fun speak(text: String) {
        if (!ready) { pending = text; return }
        // Sin emojis ni símbolos: la voz los leería en alto.
        val clean = text
            .replace("NexusChat", "Nexus Chat") // se pronuncia como dos palabras
            .replace(Regex("[^\\p{L}\\p{N}\\s.,;:!?¡¿'\"()-]"), "")
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "nexa_${System.nanoTime()}")
    }

    fun stop() {
        if (ready) tts.stop()
        _speaking.value = false
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
