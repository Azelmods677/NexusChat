package com.Azelmods.App.data.security.tor

/**
 * 🧭 OrbotStatusMapper — Mapeo puro del estado de Orbot a UI clara y accionable.
 *
 * Convierte el par de booleanos `(installed, active)` en un [OrbotUiStatus]
 * con mensaje en español, etiqueta de acción coherente y un estado de toggle
 * consistente que NUNCA queda "roto" / en error permanente.
 *
 * La función [mapOrbotStatus] es PURA y TOTAL: definida para las cuatro
 * combinaciones posibles de `(installed, active)`.
 *
 * Property 3 (design.md): el mapeo de estado de Orbot es total y consistente.
 */

/** Estado lógico de Orbot derivado de (installed, active). */
enum class OrbotState { ACTIVE, INSTALLED_INACTIVE, NOT_INSTALLED }

/**
 * Estado de UI de Orbot listo para consumir por la pantalla y el toggle.
 *
 * @property state estado lógico de Orbot.
 * @property message mensaje claro en español para el usuario.
 * @property actionLabel etiqueta de la acción sugerida ("Descargar Orbot" |
 *   "Abrir Orbot") o `null` cuando no hace falta ninguna acción.
 * @property toggleEnabled estado consistente del toggle; siempre operable,
 *   nunca en estado de error permanente.
 */
data class OrbotUiStatus(
    val state: OrbotState,
    val message: String,
    val actionLabel: String?,
    val toggleEnabled: Boolean
)

/**
 * Mapea el estado de Orbot a un [OrbotUiStatus] consistente.
 *
 * Reglas:
 * - `active == true` → [OrbotState.ACTIVE], sin acción, toggle operable.
 * - `installed && !active` → [OrbotState.INSTALLED_INACTIVE], acción "Abrir Orbot".
 * - `!installed && !active` → [OrbotState.NOT_INSTALLED], acción "Descargar Orbot".
 *
 * El toggle queda siempre operable (`toggleEnabled = true`) para permitir
 * reintentar; nunca refleja un estado de error permanente.
 */
fun mapOrbotStatus(installed: Boolean, active: Boolean): OrbotUiStatus = when {
    active -> OrbotUiStatus(
        state = OrbotState.ACTIVE,
        message = "Tor está activo vía Orbot",
        actionLabel = null,
        toggleEnabled = true
    )
    installed -> OrbotUiStatus(
        state = OrbotState.INSTALLED_INACTIVE,
        message = "Orbot está instalado pero no activo. Abre Orbot y presiona 'Iniciar' para conectar a Tor.",
        actionLabel = "Abrir Orbot",
        toggleEnabled = true
    )
    else -> OrbotUiStatus(
        state = OrbotState.NOT_INSTALLED,
        message = "Orbot no está instalado. Descárgalo desde Play Store o F-Droid (org.torproject.android)",
        actionLabel = "Descargar Orbot",
        toggleEnabled = true
    )
}
