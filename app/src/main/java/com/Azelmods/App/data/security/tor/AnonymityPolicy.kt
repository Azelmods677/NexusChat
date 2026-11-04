package com.Azelmods.App.data.security.tor

/**
 * Qué actividad deja de publicarse.
 *
 * Ir por Tor oculta la IP, pero no sirve de mucho si la app sigue publicando cuándo estás
 * conectado, cuándo escribes y cuándo lees. Mientras [hidesActivity] sea cierto:
 *  - No se publica presencia ni última conexión.
 *  - No se envía el indicador de "escribiendo".
 *  - No se envían confirmaciones de lectura.
 *
 * Lo activa el modo anónimo desde que se enciende, aunque Tor aún esté conectando: esos datos
 * no deben salir "mientras tanto". El modo fantasma hace lo mismo sin pasar por Tor.
 */
object AnonymityPolicy {
    /** Modo anónimo: todo por Tor. */
    @Volatile
    var active: Boolean = false

    /** Modo fantasma: red normal, sin publicar actividad. */
    @Volatile
    var ghost: Boolean = false

    val hidesActivity: Boolean get() = active || ghost
}
