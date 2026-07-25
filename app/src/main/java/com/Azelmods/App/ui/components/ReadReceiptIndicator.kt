package com.Azelmods.App.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.ui.theme.MessageDelivered
import com.Azelmods.App.ui.theme.MessageRead
import com.Azelmods.App.ui.theme.MessageSent

/**
 * ✓✓ Indicador de estado de mensaje.
 *
 * Antes usaba los iconos genéricos de Material (`Check` / `DoneAll`): trazos
 * gruesos, separación fija y el mismo violeta de marca para "leído", que no se
 * distinguía de cualquier otro acento de la app.
 *
 * Ahora los checks se DIBUJAN a medida, lo que permite controlar grosor, remates
 * redondeados y el solape exacto entre ambos ticks (como en las apps de mensajería
 * de referencia), y el estado "leído" usa el azul de confirmación del design system
 * ([MessageRead]) con un halo suave y un pequeño "pop" al pasar a leído.
 */
@Composable
fun ReadReceiptIndicator(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val targetColor = when (status) {
        MessageStatus.SENDING -> MessageSent.copy(alpha = 0.55f)
        MessageStatus.SENT -> MessageSent
        MessageStatus.DELIVERED -> MessageDelivered
        MessageStatus.READ -> MessageRead
        MessageStatus.FAILED -> Color.Red
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 420, easing = EaseInOutQuart),
        label = "receipt_color"
    )

    // Pequeño "pop" al confirmarse la lectura: comunica el cambio sin ser ruidoso.
    val pop by animateFloatAsState(
        targetValue = if (status == MessageStatus.READ) 1f else 0.92f,
        animationSpec = tween(durationMillis = 320, easing = EaseOutBack),
        label = "receipt_pop"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (status) {
            MessageStatus.SENDING -> {
                val infiniteTransition = rememberInfiniteTransition(label = "receipt")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "sending_rotation"
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Enviando",
                    tint = animatedColor,
                    modifier = Modifier
                        .size(12.dp)
                        .rotate(rotation)
                )
            }

            MessageStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "No enviado",
                    tint = Color.Red,
                    modifier = Modifier.size(13.dp)
                )
            }

            MessageStatus.SENT -> {
                CheckMarks(
                    color = animatedColor,
                    double = false,
                    glow = false,
                    modifier = Modifier
                        .size(width = 14.dp, height = 12.dp)
                        .scale(pop)
                )
            }

            MessageStatus.DELIVERED, MessageStatus.READ -> {
                CheckMarks(
                    color = animatedColor,
                    double = true,
                    glow = status == MessageStatus.READ,
                    modifier = Modifier
                        .size(width = 18.dp, height = 12.dp)
                        .scale(pop)
                )
            }
        }
    }
}

/**
 * Dibuja uno o dos ticks con trazo redondeado.
 *
 * Se dibuja en vez de usar iconos para poder ajustar el solape entre ticks: los
 * `DoneAll` de Material dejan los dos checks demasiado separados a tamaños pequeños
 * y se leen como un borrón. Cuando [glow] es cierto se pinta primero una copia
 * difusa y más gruesa que actúa de halo.
 */
@Composable
private fun CheckMarks(
    color: Color,
    double: Boolean,
    glow: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width
        val strokeWidth = h * 0.16f

        // Un tick ocupa ~11/18 del ancho cuando son dos, para que solapen.
        val tickWidth = if (double) w * 0.62f else w

        fun tickPath(startX: Float): Path = Path().apply {
            moveTo(startX, h * 0.52f)
            lineTo(startX + tickWidth * 0.34f, h * 0.82f)
            lineTo(startX + tickWidth * 0.96f, h * 0.18f)
        }

        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        if (glow) {
            val glowStroke = Stroke(
                width = strokeWidth * 2.4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            drawPath(tickPath(0f), color = color.copy(alpha = 0.22f), style = glowStroke)
            if (double) {
                drawPath(tickPath(w - tickWidth), color = color.copy(alpha = 0.22f), style = glowStroke)
            }
        }

        // El segundo tick se dibuja primero para que el primero quede por encima:
        // así el solape se ve limpio y no como dos trazos cortados.
        if (double) {
            drawPath(tickPath(w - tickWidth), color = color, style = stroke)
        }
        drawPath(tickPath(0f), color = color, style = stroke)
    }
}
