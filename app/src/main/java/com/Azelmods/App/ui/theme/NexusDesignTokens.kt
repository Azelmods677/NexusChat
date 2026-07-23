package com.Azelmods.App.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NexusChat Design System 2026
 * Tokens globales — NO modificar directamente en pantallas.
 * Usar siempre estos valores para mantener consistencia.
 */
object NexusTokens {
    
    // ── COLORES PRINCIPALES ─────────────────────────
    object Color {
        // Brand
        val Primary       = Color(0xFF7C6FE0)   // Violeta profundo
        val PrimaryLight  = Color(0xFFAB9EFF)   // Violeta claro
        val Secondary     = Color(0xFF00D4FF)   // Cyan eléctrico
        val Accent        = Color(0xFFFF6B9D)   // Rosa neón
        val Gold          = Color(0xFFFFD700)   // Dorado premium
        
        // Superficies oscuras — escalera canónica de la app.
        // Unificadas con las superficies de facto (las que el 90% de las
        // pantallas ya usaba vía Color.kt/MaterialTheme): un solo juego de
        // superficies para toda la app. BgDeep se reserva como extremo
        // profundo de gradientes de fondo.
        val BgDeep        = Color(0xFF070714)   // Extremo profundo (solo gradientes)
        val BgBase        = Color(0xFF0F0F1A)   // Fondo base de pantallas
        val BgSurface     = Color(0xFF1A1A2E)   // Tarjetas / superficies
        val BgElevated    = Color(0xFF252538)   // Elementos elevados (sheets, menús)
        
        // Vidrio (Glassmorphism)
        val GlassFill     = Color(0x14FFFFFF)   // 8% blanco
        val GlassBorder   = Color(0x29FFFFFF)   // 16% blanco
        val GlassStrong   = Color(0x1FFFFFFF)   // 12% blanco
        
        // Texto
        val TextPrimary   = Color(0xFFFFFFFF)
        val TextSecondary = Color(0xB3FFFFFF)   // 70%
        val TextMuted     = Color(0x66FFFFFF)   // 40%
        val TextDisabled  = Color(0x33FFFFFF)   // 20%
        
        // Estados
        val Online        = Color(0xFF00E676)
        val Away          = Color(0xFFFFB74D)
        val Offline       = Color(0xFF546E7A)
        val Error         = Color(0xFFFF5252)
        val Success       = Color(0xFF00E676)
        val Warning       = Color(0xFFFFD740)
    }
    
    // ── GRADIENTES ──────────────────────────────────
    object Gradient {
        val Brand         = listOf(Color(0xFF7C6FE0), Color(0xFF00D4FF))
        val Warm          = listOf(Color(0xFFFF6B9D), Color(0xFFFFD700))
        val Cool          = listOf(Color(0xFF00D4FF), Color(0xFF7C6FE0))
        val Background    = listOf(
            Color(0xFF070714),
            Color(0xFF0F0F1A),
            Color(0xFF12122A)
        )
        val Shimmer       = listOf(
            Color(0xFF141428),
            Color(0xFF1F1F45),
            Color(0xFF141428)
        )
        val AzelAI        = listOf(
            Color(0xFF7C6FE0),
            Color(0xFFFF6B9D),
            Color(0xFF00D4FF)
        )
    }
    
    // ── ESPACIADO (8pt grid) ────────────────────────
    object Space {
        val xxs  = 2.dp
        val xs   = 4.dp
        val sm   = 8.dp
        val md   = 16.dp
        val lg   = 24.dp
        val xl   = 32.dp
        val xxl  = 48.dp
        val xxxl = 64.dp
    }
    
    // ── RADIOS ──────────────────────────────────────
    object Radius {
        val xs   = 4.dp
        val sm   = 8.dp
        val md   = 12.dp
        val lg   = 16.dp
        val xl   = 24.dp
        val xxl  = 32.dp
        val pill = 999.dp
    }
    
    // ── ELEVACIÓN / BLUR ────────────────────────────
    object Elevation {
        val none   = 0.dp
        val sm     = 2.dp
        val md     = 4.dp
        val lg     = 8.dp
        val xl     = 16.dp
    }
    
    // ── TAMAÑOS DE ICONO ────────────────────────────
    // La app usa UNA familia (Material Symbols); el ritmo visual lo dan estos
    // 5 tamaños. Regla: un icono nunca lleva un .dp literal — siempre un token.
    //   xs 16 → iconos inline en texto/chips densos
    //   sm 18 → iconos secundarios en filas y botones Small
    //   md 24 → tamaño base (nav, toolbars, filas) — el default de Material
    //   lg 32 → iconos protagonistas (headers de sección, diálogos)
    //   xl 48 → iconos hero (estados vacíos, onboarding)
    object IconSize {
        val xs = 16.dp
        val sm = 18.dp
        val md = 24.dp
        val lg = 32.dp
        val xl = 48.dp
    }

    // ── TIPOGRAFÍA ──────────────────────────────────
    object FontSize {
        val xs   = 10.sp
        val sm   = 12.sp
        val md   = 14.sp
        val lg   = 16.sp
        val xl   = 20.sp
        val xxl  = 24.sp
        val h2   = 28.sp
        val h1   = 32.sp
    }
    
    // ── ANIMACIONES ─────────────────────────────────
    // La curva spring de NexusChat es parte de la identidad: las pantallas
    // consumen estos patrones nombrados, no inventan tweens/springs locales.
    object Anim {
        const val FAST      = 150
        const val NORMAL    = 300
        const val SLOW      = 500
        const val VERY_SLOW = 800

        const val SPRING_STIFFNESS   = 500f
        const val SPRING_DAMPING     = 0.75f
        const val SPRING_DAMPING_LOW = 0.5f

        /** Spring estándar Nexus: firme, sin rebote perceptible (transiciones, offsets). */
        fun <T> springDefault(): SpringSpec<T> =
            spring(dampingRatio = SPRING_DAMPING, stiffness = SPRING_STIFFNESS)

        /** Spring con rebote Nexus: feedback táctil de press/scale en superficies. */
        fun <T> springBouncy(): SpringSpec<T> =
            spring(dampingRatio = SPRING_DAMPING_LOW, stiffness = SPRING_STIFFNESS)
    }
}
