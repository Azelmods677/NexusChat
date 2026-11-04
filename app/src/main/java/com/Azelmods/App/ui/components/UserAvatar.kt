package com.Azelmods.App.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.theme.AvatarPalette
import com.Azelmods.App.ui.utils.UserProfileHelper

/** Colores de perfil: cada persona elige el suyo y se ve en su aro. */
object ProfileColors {
    /**
     * Quien no eligió recibe uno de los primeros. No cambiar este número: a todas esas personas
     * les cambiaría el color.
     */
    private const val AUTO_ASSIGNED_COUNT = 8

    private class Entry(val name: String, val premium: Boolean, vararg stops: Long) {
        val colors: List<Color> = stops.map { Color(it) }
    }

    /**
     * Cada cuenta guarda la POSICIÓN de su color: los nuevos van siempre al final, y uno ya
     * publicado no se mueve ni se quita.
     */
    private val entries: List<Entry> = listOf(
        Entry("Jade", false, 0xFF2FB38E, 0xFF7FD6BC),
        Entry("Océano", false, 0xFF3B82F6, 0xFF22D3EE),
        Entry("Aurora", false, 0xFF8B5CF6, 0xFFEC4899),
        Entry("Coral", false, 0xFFEC4899, 0xFFF97316),
        Entry("Ámbar", false, 0xFFF97316, 0xFFFACC15),
        Entry("Fuego", false, 0xFFEF4444, 0xFFF59E0B),
        Entry("Bosque", false, 0xFF14B8A6, 0xFF84CC16),
        Entry("Índigo", false, 0xFF6366F1, 0xFF06B6D4),
        Entry("Neón", true, 0xFF00F5D4, 0xFF7B2FF7),
        Entry("Sakura", true, 0xFFFF5EDB, 0xFF5D5FEF),
        Entry("Oro", true, 0xFFFFD86F, 0xFFB8860B),
        Entry("Grafito", true, 0xFF232526, 0xFF8E9EAB),
        Entry("Cielo", false, 0xFF0EA5E9, 0xFF7DD3FC),
        Entry("Menta", false, 0xFF10B981, 0xFF6EE7B7),
        Entry("Lavanda", false, 0xFF8B5CF6, 0xFFC4B5FD),
        Entry("Rosa", false, 0xFFEC4899, 0xFFF9A8D4),
        Entry("Durazno", false, 0xFFFB7185, 0xFFFDBA74),
        Entry("Cereza", false, 0xFFBE123C, 0xFFF43F5E),
        Entry("Lima", false, 0xFF65A30D, 0xFFBEF264),
        Entry("Turquesa", false, 0xFF0891B2, 0xFF2DD4BF),
        Entry("Vino", false, 0xFF7F1D1D, 0xFF9D174D),
        Entry("Noche", false, 0xFF1E3A8A, 0xFF4338CA),
        Entry("Arena", false, 0xFFB08968, 0xFFE6CCB2),
        Entry("Tormenta", false, 0xFF475569, 0xFF94A3B8),
        Entry("Uva", false, 0xFF6D28D9, 0xFFA21CAF),
        Entry("Selva", false, 0xFF14532D, 0xFF16A34A),
        Entry("Canela", false, 0xFF9A3412, 0xFFD97706),
        Entry("Hielo", false, 0xFF38BDF8, 0xFFE0F2FE),
        Entry("Galaxia", true, 0xFF1E1B4B, 0xFF7C3AED, 0xFFEC4899),
        Entry("Atardecer", true, 0xFFF43F5E, 0xFFF97316, 0xFFFDE047),
        Entry("Boreal", true, 0xFF00E5A8, 0xFF3B82F6, 0xFFA855F7),
        Entry("Arcoíris", true, 0xFFEF4444, 0xFFF59E0B, 0xFF22C55E, 0xFF3B82F6, 0xFFA855F7),
        Entry("Holograma", true, 0xFF67E8F9, 0xFFC4B5FD, 0xFFF9A8D4),
        Entry("Platino", true, 0xFF6B7280, 0xFFE5E7EB, 0xFF9CA3AF),
        Entry("Rubí", true, 0xFF7F1D1D, 0xFFE11D48, 0xFFFDA4AF),
        Entry("Zafiro", true, 0xFF1E3A8A, 0xFF2563EB, 0xFF67E8F9),
        Entry("Esmeralda", true, 0xFF064E3B, 0xFF059669, 0xFF6EE7B7),
        Entry("Lava", true, 0xFF292524, 0xFFDC2626, 0xFFFB923C),
        Entry("Cíber", true, 0xFF22D3EE, 0xFFD946EF, 0xFFFDE047),
        Entry("Eclipse", true, 0xFF0F172A, 0xFF6D28D9, 0xFFF59E0B)
    )

    val palettes: List<List<Color>> = entries.map { it.colors }
    val names: List<String> = entries.map { it.name }
    val freeIndices: List<Int> = entries.indices.filter { !entries[it].premium }
    val premiumIndices: List<Int> = entries.indices.filter { entries[it].premium }

    /** Índice del elegido o, si no eligió, uno fijo gratuito derivado de [seed]. */
    fun indexFor(seed: String, chosen: Int = -1): Int =
        chosen.takeIf { it in palettes.indices } ?: ((seed.hashCode() and 0x7FFFFFFF) % AUTO_ASSIGNED_COUNT)

    /** El elegido, o uno fijo derivado de [seed]: la misma persona siempre tiene el mismo. */
    fun forUser(seed: String, chosen: Int = -1): List<Color> = palettes[indexFor(seed, chosen)]
}

/** Aro de color alrededor del avatar. [rotating] se reserva para lo que merece atención. */
data class AvatarRing(
    val colors: List<Color>,
    val width: Dp = 2.dp,
    val rotating: Boolean = false
)

@Composable
fun UserAvatar(
    name: String,
    photoUrl: String?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    ring: AvatarRing? = null,
    content: (@Composable () -> Unit)? = null
) {
    val initials = remember(name) { UserProfileHelper.getInitials(name) }

    // Mismo color siempre para la misma persona, no uno al azar en cada recomposición.
    val avatarColor = remember(name, backgroundColor) {
        backgroundColor ?: AvatarPalette[(name.hashCode() and 0x7FFFFFFF) % AvatarPalette.size]
    }

    // El aro va dentro del tamaño pedido: activarlo no descoloca la fila.
    val inner = if (ring != null) size - (ring.width + 2.dp) * 2 else size

    Box(
        modifier = modifier
            .size(size)
            .then(if (ring != null) Modifier.avatarRing(ring) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            content != null -> Box(Modifier.size(inner).clip(CircleShape)) { content() }
            !photoUrl.isNullOrBlank() -> AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de perfil de $name",
                modifier = Modifier
                    .size(inner)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            else -> Box(
                modifier = Modifier
                    .size(inner)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (inner.value * 0.36f).sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun Modifier.avatarRing(ring: AvatarRing): Modifier {
    val brush = remember(ring.colors) { Brush.sweepGradient(ring.colors + ring.colors.first()) }
    val angle = if (ring.rotating) {
        rememberInfiniteTransition(label = "aro").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(RING_ROTATION_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "giro"
        )
    } else {
        null
    }
    return drawBehind {
        val stroke = ring.width.toPx()
        rotate(angle?.value ?: -90f) {
            drawCircle(
                brush = brush,
                radius = size.minDimension / 2f - stroke / 2f,
                style = Stroke(width = stroke)
            )
        }
    }
}

private const val RING_ROTATION_MS = 5_000
