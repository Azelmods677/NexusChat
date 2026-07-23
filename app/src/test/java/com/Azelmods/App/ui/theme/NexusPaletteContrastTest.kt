package com.Azelmods.App.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual

/**
 * Nexus Design System — contrato de contraste de la paleta (Principio A2).
 *
 * El contraste es una propiedad del token, no una auditoría posterior: si un
 * par rol-sobre-rol del sistema deja de cumplir su mínimo, este test rompe y
 * el cambio de color no entra.
 *
 * Umbrales (WCAG 2.1):
 *  - 4.5:1 texto normal
 *  - 3.0:1 texto grande, iconos funcionales y texto de pistas/captions
 */
class NexusPaletteContrastTest : StringSpec({

    fun Color.relativeLuminance(): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }

    /** Ratio de contraste WCAG; los colores con alpha se compositan sobre el fondo. */
    fun contrast(fg: Color, bg: Color): Double {
        val fgOpaque = if (fg.alpha < 1f) fg.compositeOver(bg) else fg
        val l1 = fgOpaque.relativeLuminance()
        val l2 = bg.relativeLuminance()
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    val surfaces = mapOf(
        "BgBase" to NexusTokens.Color.BgBase,
        "BgSurface" to NexusTokens.Color.BgSurface,
        "BgElevated" to NexusTokens.Color.BgElevated
    )

    "TextPrimary alcanza AAA (7:1) sobre todas las superficies" {
        surfaces.forEach { (name, surface) ->
            withClue_(name) {
                contrast(NexusTokens.Color.TextPrimary, surface) shouldBeGreaterThanOrEqual 7.0
            }
        }
    }

    "TextSecondary alcanza AA (4.5:1) sobre todas las superficies" {
        surfaces.forEach { (name, surface) ->
            withClue_(name) {
                contrast(NexusTokens.Color.TextSecondary, surface) shouldBeGreaterThanOrEqual 4.5
            }
        }
    }

    "TextMuted (pistas/captions) alcanza 3:1 sobre todas las superficies" {
        surfaces.forEach { (name, surface) ->
            withClue_(name) {
                contrast(NexusTokens.Color.TextMuted, surface) shouldBeGreaterThanOrEqual 3.0
            }
        }
    }

    "Error es legible (4.5:1) sobre las superficies donde se muestra" {
        surfaces.forEach { (name, surface) ->
            withClue_(name) {
                contrast(NexusTokens.Color.Error, surface) shouldBeGreaterThanOrEqual 4.5
            }
        }
    }

    "Texto blanco sobre el violeta de marca cumple 3:1 (texto de botones)" {
        contrast(NexusTokens.Color.TextPrimary, NexusTokens.Color.Primary) shouldBeGreaterThanOrEqual 3.0
    }

    "Los acentos de los 25 temas cumplen 3:1 sobre el fondo base (iconos/indicadores)" {
        AppTheme.ACCENT_SWATCHES.forEach { swatch ->
            withClue_(swatch.id) {
                contrast(swatch.color, NexusTokens.Color.BgBase) shouldBeGreaterThanOrEqual 3.0
            }
        }
    }

    "El estado Online cumple 3:1 sobre superficies (dot de presencia)" {
        surfaces.forEach { (name, surface) ->
            withClue_(name) {
                contrast(NexusTokens.Color.Online, surface) shouldBeGreaterThanOrEqual 3.0
            }
        }
    }
})

/** Mensaje de contexto simple sin dependencia extra de kotest-assertions-shared. */
private inline fun withClue_(clue: String, block: () -> Unit) {
    try {
        block()
    } catch (e: AssertionError) {
        throw AssertionError("[$clue] ${e.message}", e)
    }
}
