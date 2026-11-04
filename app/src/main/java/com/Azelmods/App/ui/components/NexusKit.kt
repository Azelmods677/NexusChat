package com.Azelmods.App.ui.components

import com.Azelmods.App.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Azelmods.App.ui.theme.NexusTokens

/*
 * Kit de componentes de NexusChat.
 *
 * Las pantallas se construyen con listas planas a todo el ancho y separadores finos,
 * como las apps de mensajería maduras. Las tarjetas flotantes se reservan para lo que
 * de verdad es un objeto aparte. Cada texto va en castellano y en mayúscula inicial
 * normal: nada de TÍTULOS EN MAYÚSCULAS con espaciado, que es otro rasgo de plantilla.
 */

/** Barra superior opaca. Al ser opaca, el contenido nunca se superpone al título al desplazarse. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    containerColor: Color = NexusTokens.Color.BgBase,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = NexusTokens.Color.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NexusTokens.Color.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Atrás"),
                        tint = NexusTokens.Color.TextPrimary
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = NexusTokens.Color.TextPrimary,
            actionIconContentColor = NexusTokens.Color.TextSecondary,
            navigationIconContentColor = NexusTokens.Color.TextPrimary
        )
    )
}

/** Cabecera de sección en una lista. */
@Composable
fun NexusSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke(this)
    }
}

/** Separador fino, alineado con el texto de la fila y no con su icono. */
@Composable
fun NexusDivider(modifier: Modifier = Modifier, startInset: Dp = 0.dp) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset),
        thickness = 1.dp,
        color = NexusTokens.Color.Hairline
    )
}

/**
 * Fila de lista: icono monocromo, título, subtítulo opcional y contenido al final.
 * Es la pieza principal de Ajustes, Seguridad, Acerca de y cualquier listado.
 */
@Composable
fun NexusListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = NexusTokens.Color.TextSecondary,
    titleColor: Color = NexusTokens.Color.TextPrimary,
    enabled: Boolean = true,
    singleLineSubtitle: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) iconTint else NexusTokens.Color.TextDisabled,
                modifier = Modifier.size(NexusTokens.IconSize.md)
            )
            Spacer(Modifier.width(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) titleColor else NexusTokens.Color.TextDisabled
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) NexusTokens.Color.TextMuted else NexusTokens.Color.TextDisabled,
                    maxLines = if (singleLineSubtitle) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** Valor corto al final de una fila ("Jade", "5 min"). */
@Composable
fun NexusRowValue(text: String, color: Color = NexusTokens.Color.TextMuted) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 170.dp),
        textAlign = TextAlign.End
    )
}

/** Fila con interruptor: toda la fila conmuta, no solo el control. */
@Composable
fun NexusSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    NexusListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = NexusTokens.Color.TextMuted,
                    uncheckedTrackColor = NexusTokens.Color.BgHigh,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    )
}

/** Dato en una línea: etiqueta a la izquierda, valor a la derecha. */
@Composable
fun NexusDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = NexusTokens.Color.TextPrimary,
    monospace: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = NexusTokens.Color.TextSecondary
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.SansSerif
            ),
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/** Estado vacío: una frase que dice qué pasa y, si tiene sentido, qué hacer. */
@Composable
fun NexusEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NexusTokens.Color.TextMuted,
                modifier = Modifier.size(NexusTokens.IconSize.xl)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NexusTokens.Color.TextPrimary,
                textAlign = TextAlign.Center
            )
            if (!body.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextMuted,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(20.dp))
                NexusButton(
                    text = actionLabel,
                    onClick = onAction,
                    size = NexusButtonSize.Medium,
                    fillWidth = false
                )
            }
        }
    }
}

/** Chip de filtro compacto. */
@Composable
fun NexusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int = 0
) {
    val shape = RoundedCornerShape(NexusTokens.Radius.pill)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else NexusTokens.Color.BgElevated
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else NexusTokens.Color.TextSecondary
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else NexusTokens.Color.TextMuted
            )
        }
    }
}

/** Contador de no leídos. */
@Composable
fun NexusCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    muted: Boolean = false
) {
    Box(
        modifier = modifier
            .heightIn(min = 20.dp)
            .widthIn(min = 20.dp)
            .clip(CircleShape)
            .background(if (muted) NexusTokens.Color.BgHigh else MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (muted) NexusTokens.Color.TextSecondary else MaterialTheme.colorScheme.onPrimary
        )
    }
}

/** Aviso dentro de una pantalla (no un Toast que desaparece antes de leerlo). */
@Composable
fun NexusNotice(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Color = NexusTokens.Color.TextSecondary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tone.copy(alpha = 0.10f),
        shape = RoundedCornerShape(NexusTokens.Radius.md)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(NexusTokens.IconSize.sm)
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.TextPrimary
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = tone,
                        modifier = Modifier.clickable(onClick = onAction)
                    )
                }
            }
        }
    }
}
