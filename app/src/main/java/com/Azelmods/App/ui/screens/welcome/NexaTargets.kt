package com.Azelmods.App.ui.screens.welcome

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Dónde está cada elemento que Nexa puede señalar. Las pantallas marcan sus botones con
 * [nexaTarget] y la visita los ilumina, camina hasta ellos y los "toca".
 * Las coordenadas son de la raíz de la ventana, las mismas que usa la capa de Nexa.
 */
object NexaTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

/** Marca este elemento para que Nexa pueda señalarlo con la clave [key]. */
fun Modifier.nexaTarget(key: String): Modifier = onGloballyPositioned { coords ->
    if (coords.isAttached) {
        val r = coords.boundsInRoot()
        if (r.width > 0f && r.height > 0f && NexaTargets.bounds[key] != r) NexaTargets.bounds[key] = r
    }
}
