package com.Azelmods.App.data.security.tor

/**
 * Estado del servicio Orbot para el modo anónimo
 */
enum class OrbotStatus {
    /** Orbot no está instalado en el dispositivo */
    NOT_INSTALLED,
    
    /** Orbot está instalado pero no está corriendo (sin proxy disponible) */
    INSTALLED_INACTIVE,
    
    /** Orbot está corriendo y conectado a la red Tor */
    ACTIVE_CONNECTED,
    
    /** Orbot está corriendo pero hay un error de conexión */
    ERROR
}

/**
 * Información de estado de Orbot para mostrar en la UI
 */
data class OrbotStatusInfo(
    val status: OrbotStatus,
    val message: String,
    val actionLabel: String?,
    val actionIntent: (() -> Unit)?
)
