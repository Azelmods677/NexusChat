package com.Azelmods.App.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Safe clickable modifier that prevents ACTION_HOVER_EXIT crashes
 * Use this instead of .clickable { } throughout the app
 */
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(),
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * Safe clickable modifier with long click support
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.safeClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    if (onLongClick != null) {
        this.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        this.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            enabled = enabled,
            onClick = onClick
        )
    }
}
