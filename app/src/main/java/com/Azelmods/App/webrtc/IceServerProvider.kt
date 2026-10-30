package com.Azelmods.App.webrtc

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.PeerConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servidores STUN/TURN de las llamadas.
 *
 * Antes estaban fijados en el código, con el TURN público y compartido de
 * openrelay.metered.ca. Un TURN gratuito no garantiza nada: cuando se satura o
 * cambia sus credenciales, las llamadas entre redes móviles (CG-NAT) dejan de
 * conectar y la única forma de arreglarlo era publicar otra versión de la app.
 *
 * Ahora se leen de la base de datos, en `config/webrtc/iceServers`, con este
 * formato (lo escribe el administrador desde la consola de Firebase; las reglas
 * no dejan escribirlo a los clientes):
 *
 * ```json
 * "config": { "webrtc": { "iceServers": [
 *   { "urls": "stun:stun.l.google.com:19302" },
 *   { "urls": "turn:turn.tudominio.com:3478", "username": "u", "credential": "p" },
 *   { "urls": "turns:turn.tudominio.com:5349", "username": "u", "credential": "p" }
 * ] } }
 * ```
 *
 * `urls` admite una cadena o una lista. Si el nodo no existe, no se puede leer o
 * tarda más de [READ_TIMEOUT_MS], se usan [DEFAULT_ICE_SERVERS]: la llamada sale
 * igual, sin quedarse esperando a la configuración.
 */
@Singleton
class IceServerProvider @Inject constructor() {

    companion object {
        private const val TAG = "IceServerProvider"
        private const val CONFIG_PATH = "config/webrtc/iceServers"
        private const val READ_TIMEOUT_MS = 3_000L

        /** STUN de Google + el TURN público como último recurso. */
        val DEFAULT_ICE_SERVERS: List<PeerConnection.IceServer> = listOf(
            PeerConnection.IceServer.builder(
                listOf(
                    "stun:stun.l.google.com:19302",
                    "stun:stun1.l.google.com:19302",
                    "stun:stun2.l.google.com:19302"
                )
            ).createIceServer(),
            PeerConnection.IceServer.builder(
                listOf(
                    "turn:openrelay.metered.ca:80",
                    "turn:openrelay.metered.ca:443",
                    "turn:openrelay.metered.ca:443?transport=tcp"
                )
            )
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer()
        )
    }

    @Volatile
    private var cached: List<PeerConnection.IceServer>? = null

    /** `true` si la lista activa incluye algún TURN (necesario para `relayOnly`). */
    fun hasTurn(servers: List<PeerConnection.IceServer>): Boolean =
        servers.any { server -> server.urls.any { it.startsWith("turn:") || it.startsWith("turns:") } }

    suspend fun getIceServers(): List<PeerConnection.IceServer> {
        cached?.let { return it }

        val remote = withTimeoutOrNull(READ_TIMEOUT_MS) {
            runCatching {
                val snapshot = FirebaseDatabase.getInstance().getReference(CONFIG_PATH).get().await()
                parse(snapshot.value)
            }.onFailure {
                Log.w(TAG, "No se pudo leer $CONFIG_PATH: ${it.message}")
            }.getOrNull()
        }

        val servers = if (remote.isNullOrEmpty()) DEFAULT_ICE_SERVERS else remote
        Log.d(TAG, "ICE servers: ${servers.size} (${if (remote.isNullOrEmpty()) "por defecto" else "remotos"})")
        if (!remote.isNullOrEmpty()) cached = servers
        return servers
    }

    private fun parse(raw: Any?): List<PeerConnection.IceServer> {
        val entries: List<Any?> = when (raw) {
            is List<*> -> raw
            // Firebase devuelve un Map si las claves no son índices consecutivos.
            is Map<*, *> -> raw.values.toList()
            else -> return emptyList()
        }
        return entries.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val urls = when (val u = map["urls"] ?: map["url"]) {
                is String -> listOf(u)
                is List<*> -> u.filterIsInstance<String>()
                is Map<*, *> -> u.values.filterIsInstance<String>()
                else -> emptyList()
            }.map { it.trim() }.filter { it.isNotEmpty() }
            if (urls.isEmpty()) return@mapNotNull null

            val builder = PeerConnection.IceServer.builder(urls)
            (map["username"] as? String)?.let { builder.setUsername(it) }
            (map["credential"] as? String)?.let { builder.setPassword(it) }
            builder.createIceServer()
        }
    }
}
