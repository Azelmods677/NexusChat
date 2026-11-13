package com.Azelmods.App.utils

import com.Azelmods.App.i18n.tr

/**
 * Convierte un fallo de Firebase en una frase clara para quien usa la app.
 *
 * El texto crudo ("Permission denied", "Client is offline") no dice qué hacer. Aquí se
 * distingue sesión caducada, permiso denegado y falta de red, que tienen arreglos distintos.
 */
fun friendlyFirebaseError(e: Throwable?): String {
    val raw = e?.message.orEmpty()
    return when {
        raw.contains("permission", true) -> tr("El servidor no permitió esta acción. Cierra y abre la app; si sigue, actualízala.")
        raw.contains("offline", true) || raw.contains("network", true) || e is java.io.IOException ->
            tr("Sin conexión. Revisa tu internet y vuelve a intentarlo.")
        raw.contains("auth", true) || raw.contains("token", true) -> tr("Tu sesión caducó. Vuelve a iniciar sesión.")
        else -> tr("Algo salió mal. Vuelve a intentarlo.")
    }
}
