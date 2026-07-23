package com.Azelmods.App.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.ui.theme.NexusTokens

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * NEXUS BUTTON — botón insignia del Nexus Design System.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Una sola fuente de verdad para las acciones de NexusChat. Reemplaza los
 * `Button(...)` sueltos de Material que hoy repiten a mano color/altura/estado
 * de carga en cada pantalla. Es 100% token-driven: no admite colores, radios,
 * tamaños ni tipografías hardcodeados.
 *
 * Identidad Nexus incorporada:
 *  · [NexusButtonVariant.Primary] usa el gradiente de marca (violeta→cyan).
 *  · Microinteracción de press con la curva spring del sistema (springBouncy).
 *  · Píldora completa, coherente con los CTA de la app.
 *
 * Accesibilidad: rol semántico de botón, ripple, y alturas ≥ 48dp en las tallas
 * Large/Medium (objetivo táctil). La talla Small (40dp) es para contextos densos
 * (chips de acción dentro de tarjetas), no para CTA primarios.
 *
 * @param loading muestra un spinner y bloquea el click sin cambiar el layout.
 * @param leadingIcon icono opcional a la izquierda del texto.
 * @param fillWidth por defecto ocupa el ancho (patrón CTA); false para inline.
 */
enum class NexusButtonVariant { Primary, Secondary, Tinted, Destructive, Ghost }

enum class NexusButtonSize(
    val height: Dp,
    val fontSize: TextUnit,
    val horizontalPadding: Dp,
    val iconSize: Dp
) {
    Large(56.dp, 16.sp, 24.dp, 20.dp),
    Medium(48.dp, 15.sp, 20.dp, 18.dp),
    Small(40.dp, 13.sp, 16.dp, 16.dp)
}

@Composable
fun NexusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NexusButtonVariant = NexusButtonVariant.Primary,
    size: NexusButtonSize = NexusButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fillWidth: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading

    val scale by animateFloatAsState(
        targetValue = if (pressed && active) 0.97f else 1f,
        animationSpec = NexusTokens.Anim.springBouncy(),
        label = "nexus_button_scale"
    )

    val shape = RoundedCornerShape(NexusTokens.Radius.pill)

    // Color de contenido por variante (tokens; nunca literal).
    val contentColor: Color = when (variant) {
        NexusButtonVariant.Primary,
        NexusButtonVariant.Secondary,
        NexusButtonVariant.Destructive -> NexusTokens.Color.TextPrimary
        NexusButtonVariant.Tinted,
        NexusButtonVariant.Ghost -> NexusTokens.Color.Primary
    }

    // Relleno/borde por variante.
    val surface: Modifier = when (variant) {
        NexusButtonVariant.Primary ->
            Modifier.background(Brush.linearGradient(NexusTokens.Gradient.Brand), shape)
        NexusButtonVariant.Secondary ->
            Modifier.background(NexusTokens.Color.BgElevated, shape)
        NexusButtonVariant.Tinted ->
            Modifier.background(NexusTokens.Color.Primary.copy(alpha = 0.15f), shape)
        NexusButtonVariant.Destructive ->
            Modifier.background(NexusTokens.Color.Error, shape)
        NexusButtonVariant.Ghost ->
            Modifier.border(1.dp, NexusTokens.Color.GlassBorder, shape)
    }

    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .clip(shape)
            .then(surface)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = contentColor),
                enabled = active,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = size.horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size.iconSize),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(size.iconSize)
                )
                Spacer(modifier = Modifier.width(NexusTokens.Space.sm))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = size.fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
