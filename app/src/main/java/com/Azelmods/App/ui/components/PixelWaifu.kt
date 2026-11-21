package com.Azelmods.App.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * Personajes originales en pixel art (chicas y chicos) para las marcas de enviado, entregado y leído.
 * No son de ninguna serie. Los sprites están en [PixelSprites].
 *
 * Enviado: duerme y suelta "z". Entregado: despierta y guiña un ojo de vez en cuando.
 * Leído: ojos de corazón, boca abierta, un saltito y un corazón que sube. [frame] va de 0 a 7.
 */
enum class PixelCharacter(val key: String) {
    AKARI("akari"), YUME("yume"), RIN("rin"), SORA("sora"), MIKA("mika"),
    KAI("kai"), REN("ren"), HARU("haru")
}

private val INK = Color(0xFF2A1830)
private val HEART = Color(0xFFFF3D7F)

@Composable
fun PixelWaifu(character: PixelCharacter, double: Boolean, read: Boolean, frame: Int, modifier: Modifier = Modifier) {
    val state = when {
        read -> "read"
        !double -> "sent"
        frame == 5 -> "wink"
        else -> "delivered"
    }
    val grid = PixelSprites.grids["${character.key}_$state"] ?: return
    val palette = PixelSprites.palettes[character.key] ?: return
    Canvas(modifier) {
        val rows = grid.size
        val cols = grid.first().length
        // Se deja una columna a la derecha para la "z" y el corazón.
        val cell = minOf(size.width / (cols + 3), size.height / rows)
        val left = (size.width - cell * (cols + 3)) / 2f
        val top = (size.height - cell * rows) / 2f
        val bob = if (read && frame % 2 == 1) -cell else 0f
        fun px(x: Int, y: Int, color: Color, dy: Float = 0f) =
            drawRect(color, Offset(left + x * cell, top + y * cell + dy), Size(cell + 0.5f, cell + 0.5f))

        grid.forEachIndexed { y, line ->
            line.forEachIndexed { x, c -> palette[c]?.let { px(x, y, it, bob) } }
        }
        val rise = frame % 4
        when (state) {
            "sent" -> {
                // "z" que sube junto a la cabeza.
                val zy = 4 - rise
                px(cols, zy, INK); px(cols + 1, zy, INK); px(cols + 1, zy + 1, INK); px(cols, zy + 2, INK); px(cols + 1, zy + 2, INK)
            }
            "read" -> {
                val hy = 5 - rise
                px(cols, hy, HEART); px(cols + 2, hy, HEART)
                px(cols, hy + 1, HEART); px(cols + 1, hy + 1, HEART); px(cols + 2, hy + 1, HEART)
                px(cols + 1, hy + 2, HEART)
            }
        }
    }
}
