package com.Azelmods.App.ui.theme

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
        
        // Superficies oscuras
        val BgDeep        = Color(0xFF070714)   // Fondo más oscuro
        val BgBase        = Color(0xFF0D0D1E)   // Fondo base
        val BgSurface     = Color(0xFF141428)   // Tarjetas
        val BgElevated    = Color(0xFF1C1C38)   // Elementos elevados
        
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
            Color(0xFF0D0D1E),
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
    object Anim {
        const val FAST      = 150
        const val NORMAL    = 300
        const val SLOW      = 500
        const val VERY_SLOW = 800
        
        const val SPRING_STIFFNESS   = 500f
        const val SPRING_DAMPING     = 0.75f
        const val SPRING_DAMPING_LOW = 0.5f
    }
}
