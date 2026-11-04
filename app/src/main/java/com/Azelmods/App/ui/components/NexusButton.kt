package com.Azelmods.App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Botón de la app. Una sola fuente de verdad para color, altura y estado de carga.
 *
 * Accesibilidad: rol semántico de botón, ripple, y alturas ≥ 48dp en las tallas
 * Large/Medium (objetivo táctil). La talla Small (40dp) es para contextos densos.
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
    Large(50.dp, 15.sp, 24.dp, 20.dp),
    Medium(44.dp, 14.sp, 20.dp, 18.dp),
    Small(36.dp, 13.sp, 14.dp, 16.dp)
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
    val active = enabled && !loading

    val shape = RoundedCornerShape(NexusTokens.Radius.md)
    val scheme = MaterialTheme.colorScheme

    // Color de contenido por variante: sigue al acento elegido por el usuario.
    val contentColor: Color = when (variant) {
        NexusButtonVariant.Primary -> scheme.onPrimary
        NexusButtonVariant.Secondary -> NexusTokens.Color.TextPrimary
        NexusButtonVariant.Destructive -> scheme.onError
        NexusButtonVariant.Tinted -> scheme.onPrimaryContainer
        NexusButtonVariant.Ghost -> scheme.primary
    }

    // Relleno/borde por variante.
    val surface: Modifier = when (variant) {
        NexusButtonVariant.Primary -> Modifier.background(scheme.primary, shape)
        NexusButtonVariant.Secondary -> Modifier.background(NexusTokens.Color.BgHigh, shape)
        NexusButtonVariant.Tinted -> Modifier.background(scheme.primaryContainer, shape)
        NexusButtonVariant.Destructive -> Modifier.background(scheme.error, shape)
        NexusButtonVariant.Ghost -> Modifier.border(1.dp, scheme.outline, shape)
    }

    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
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
