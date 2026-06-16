package com.Azelmods.App.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Conjunto de colores derivados de un color primario para un tema de NexusChat.
 *
 * Cada tema define su [primary] y sus variantes (claras/oscuras/contenedor) más
 * el color de la burbuja de mensajes enviados ([bubbleSent]). Las variantes se
 * generan automáticamente en [NexusColorSchemes.buildScheme].
 */
data class NexusColors(
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val primaryContainer: Color,
    val bubbleSent: Color
)
