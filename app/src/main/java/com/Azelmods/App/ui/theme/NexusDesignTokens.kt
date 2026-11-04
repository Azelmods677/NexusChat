package com.Azelmods.App.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sistema de diseño de NexusChat (v7).
 *
 * Dirección: grafito neutro + un único acento. Es una app de mensajería y
 * privacidad: la interfaz tiene que desaparecer para que se lea la conversación.
 *
 * Reglas que evitan volver al aspecto de plantilla:
 *  - Sin degradados: los [Gradient] son planos a propósito. El código antiguo que
 *    todavía pide un gradiente recibe un color sólido.
 *  - Sin vidrio ni brillos: [Glass] son superficies opacas con un filo de 1 dp.
 *  - Sin animaciones infinitas decorativas: solo comunican estado real (escribiendo,
 *    timbre, conectando).
 *  - Los iconos son Material y nunca emojis; los emojis son contenido del usuario.
 *  - El acento marca lo importante (no leídos, acción primaria). Si todo tiene
 *    color, nada destaca.
 */
object NexusTokens {

    // ── COLORES ─────────────────────────────────────
    object Color {
        // Marca
        val Primary       = Color(0xFF2FB38E)   // Jade: acento por defecto
        val PrimaryLight  = Color(0xFF7FD6BC)   // Jade claro (texto sobre oscuro)
        val PrimaryDeep   = Color(0xFF14503F)   // Jade profundo (burbuja enviada)
        val Secondary     = Color(0xFF8FA3B5)   // Pizarra: rol secundario neutro
        val Accent        = Color(0xFFE07A5F)   // Coral: avisos cálidos puntuales
        val Gold          = Color(0xFFD9A441)   // Ámbar apagado

        // Superficies: escalera neutra, sin tinte azul ni violeta.
        val BgDeep        = Color(0xFF0B0C0E)   // Fondo de pantallas completas (medios)
        val BgBase        = Color(0xFF101214)   // Fondo de pantalla
        val BgSurface     = Color(0xFF171A1D)   // Barras, hojas, filas destacadas
        val BgElevated    = Color(0xFF1F2327)   // Menús, campos, burbuja recibida
        val BgHigh        = Color(0xFF2A2F34)   // Pulsado, chips activos

        val Hairline      = Color(0xFF24282C)   // Separadores de 1 dp

        // Heredados del antiguo glass: ahora son superficies opacas.
        val GlassFill     = BgSurface
        val GlassBorder   = Hairline
        val GlassStrong   = BgElevated

        // Texto
        val TextPrimary   = Color(0xFFECEEF0)
        val TextSecondary = Color(0xFFA6ACB3)
        val TextMuted     = Color(0xFF7C828A)
        val TextDisabled  = Color(0xFF4F545A)

        // Estados
        val Online        = Color(0xFF3CC68A)
        val Away          = Color(0xFFD9A441)
        val Offline       = Color(0xFF5F666E)
        val Error         = Color(0xFFE5534B)
        val Success       = Color(0xFF3CC68A)
        val Warning       = Color(0xFFD9A441)

        // Doble check de "visto": rol propio, lenguaje visual que el usuario ya conoce.
        val ReadReceipt   = Color(0xFF53A6E6)
    }

    // ── "GRADIENTES" ────────────────────────────────
    // Planos a propósito. Se conservan para que el código que aún los consume
    // compile y se vea sólido; el código nuevo no debe usarlos.
    object Gradient {
        val Brand         = listOf(Color.Primary, Color.Primary)
        val Warm          = listOf(Color.Accent, Color.Accent)
        val Cool          = listOf(Color.Primary, Color.Primary)
        val Background    = listOf(Color.BgBase, Color.BgBase, Color.BgBase)
        val Shimmer       = listOf(Color.BgSurface, Color.BgElevated, Color.BgSurface)
        val AzelAI        = listOf(Color.Primary, Color.Primary, Color.Primary)
    }

    // ── ESPACIADO (rejilla de 4 dp) ─────────────────
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
    // Contenidos: las esquinas enormes son otra marca de plantilla.
    object Radius {
        val xs   = 4.dp
        val sm   = 8.dp
        val md   = 10.dp
        val lg   = 12.dp
        val xl   = 16.dp
        val xxl  = 20.dp
        val pill = 999.dp
    }

    // ── ELEVACIÓN ───────────────────────────────────
    // La jerarquía la da el tono de la superficie, no la sombra.
    object Elevation {
        val none   = 0.dp
        val sm     = 0.dp
        val md     = 1.dp
        val lg     = 2.dp
        val xl     = 4.dp
    }

    // ── TAMAÑOS DE ICONO ────────────────────────────
    object IconSize {
        val xs = 16.dp
        val sm = 18.dp
        val md = 22.dp
        val lg = 28.dp
        val xl = 44.dp
    }

    // ── TIPOGRAFÍA ──────────────────────────────────
    object FontSize {
        val xs   = 11.sp
        val sm   = 12.sp
        val md   = 14.sp
        val lg   = 16.sp
        val xl   = 18.sp
        val xxl  = 22.sp
        val h2   = 26.sp
        val h1   = 30.sp
    }

    // ── ANIMACIONES ─────────────────────────────────
    // Movimiento corto y sin rebote: comunica un cambio, no llama la atención.
    object Anim {
        const val FAST      = 120
        const val NORMAL    = 220
        const val SLOW      = 320
        const val VERY_SLOW = 480

        const val SPRING_STIFFNESS   = 700f
        const val SPRING_DAMPING     = 1f
        const val SPRING_DAMPING_LOW = 1f

        fun <T> springDefault(): SpringSpec<T> =
            spring(dampingRatio = SPRING_DAMPING, stiffness = SPRING_STIFFNESS)

        fun <T> springBouncy(): SpringSpec<T> =
            spring(dampingRatio = SPRING_DAMPING_LOW, stiffness = SPRING_STIFFNESS)
    }

    // ── SUPERFICIES SOBRE FONDO DE USUARIO ──────────
    // Con un fondo elegido por el usuario las filas necesitan algo de respaldo para
    // leerse. Sin fondo propio estas capas no se aprecian.
    object Glass {
        val Rest = Color(0x00000000)
        val Raised = Color(0x14FFFFFF)
        val Overlay = Color(0xF2171A1D)

        val BorderRest = Color(0x00000000)
        val BorderRaised = Color.Hairline

        val BorderWidth = 1.dp
        val AccentRailWidth = 3.dp
    }

    // ── OPACIDAD DE TEXTO ───────────────────────────
    object Alpha {
        const val Full = 1.0f
        const val High = 0.92f
        const val Medium = 0.70f
        const val Low = 0.50f
        const val Faint = 0.30f
    }

    // ── DECISIONES DE SUPERFICIE ────────────────────
    object Surface {
        val CardRest = Elevation.none
        val CardRaised = Elevation.none
        val Sheet = Elevation.none
        val CardRadius = Radius.lg
    }
}
