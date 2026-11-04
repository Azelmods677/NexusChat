package com.Azelmods.App.data.security.tor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.torproject.jni.TorService as NativeTorService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Progreso de arranque de Tor, tal como lo informa el propio Tor. */
data class TorBootstrap(val percent: Int, val summary: String)

/** Un nodo del circuito actual. */
data class TorRelay(val nickname: String, val fingerprint: String)

/**
 * Tor dentro de la propia app: el mismo motor que usa Orbot (`libtor.so` del Guardian
 * Project), sin necesitar ninguna otra aplicación instalada.
 *
 * Corre en el proceso de la app mientras el modo anónimo esté activo. Los datos que muestra
 * la interfaz (progreso, circuito) se le preguntan al propio Tor por su puerto de control;
 * no hay ningún valor inventado.
 */
@Singleton
class EmbeddedTor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var service: NativeTorService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as? NativeTorService.LocalBinder)?.service
            Log.i(TAG, "Tor integrado en marcha (${NativeTorService.VERSION_NAME})")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    val isRunning: Boolean get() = service != null

    val version: String get() = NativeTorService.VERSION_NAME

    fun start() {
        if (bound) return
        removeStaleControlSocket()
        bound = context.bindService(
            Intent(context, NativeTorService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        if (!bound) Log.e(TAG, "No se pudo arrancar el servicio de Tor")
    }

    /**
     * Si Android mató la app, queda el socket de control de la ejecución anterior. La librería
     * lo da por bueno, se conecta antes de que Tor escuche, recibe "Connection refused" y no
     * vuelve a intentarlo: Tor se queda sin control hasta reiniciar la app.
     */
    private fun removeStaleControlSocket() {
        runCatching {
            File(context.getDir("TorService", Context.MODE_PRIVATE), "data/ControlSocket").delete()
        }.onFailure { Log.w(TAG, "No se pudo borrar el socket anterior: ${it.message}") }
    }

    fun stop() {
        if (!bound) return
        runCatching { context.unbindService(connection) }
        runCatching { context.stopService(Intent(context, NativeTorService::class.java)) }
        bound = false
        service = null
    }

    /** Puerto SOCKS real, o null si Tor aún no lo ha abierto. */
    fun socksPort(): Int? = runCatching { service?.socksPort }.getOrNull()?.takeIf { it > 0 }

    /** Progreso actual. Null mientras el puerto de control aún no responde. */
    fun bootstrap(): TorBootstrap? {
        val phase = info("status/bootstrap-phase") ?: return null
        return parseBootstrap(phase)
    }

    /** Sin conexión de control la librería registra un error por cada consulta: se evita. */
    private fun info(key: String): String? {
        val tor = service ?: return null
        if (tor.torControlConnection == null) return null
        return runCatching { tor.getInfo(key) }.getOrNull()
    }

    /** Obliga a salir por este nodo. No se guarda: al reiniciar Tor vuelve a elegirse. */
    fun pinExit(fingerprint: String): Boolean = runCatching {
        val control = service?.torControlConnection ?: return false
        control.setConf("ExitNodes", "\$$fingerprint")
        control.signal("NEWNYM")
        true
    }.onFailure { Log.w(TAG, "No se pudo fijar la salida: ${it.message}") }.getOrDefault(false)

    /** Vuelve a dejar que Tor elija la salida. */
    fun clearExit() {
        runCatching { service?.torControlConnection?.resetConf(listOf("ExitNodes")) }
            .onFailure { Log.w(TAG, "No se pudo liberar la salida: ${it.message}") }
    }

    /** Huella del nodo de salida del circuito abierto para este usuario SOCKS. */
    fun exitOfIsolation(isolation: String): String? {
        val status = info("circuit-status") ?: return null
        return parseExitFor(status, isolation)
    }

    /** Nodos del circuito de uso general más reciente. */
    fun currentCircuit(): List<TorRelay> {
        val status = info("circuit-status") ?: return emptyList()
        return parseCircuit(status)
    }

    companion object {
        private const val TAG = "EmbeddedTor"
        private val PROGRESS = Regex("PROGRESS=(\\d+)")
        private val TAG_NAME = Regex("TAG=(\\w+)")
        private val RELAY = Regex("\\$([0-9A-Fa-f]{40})~([^,\\s]+)")

        /** "NOTICE BOOTSTRAP PROGRESS=45 TAG=requesting_descriptors SUMMARY=..." */
        fun parseBootstrap(phase: String): TorBootstrap? {
            val percent = PROGRESS.find(phase)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            val tag = TAG_NAME.find(phase)?.groupValues?.get(1).orEmpty()
            return TorBootstrap(percent, describe(tag, percent))
        }

        private fun describe(tag: String, percent: Int): String = when {
            percent >= 100 -> "Conectado a la red Tor"
            tag.startsWith("conn") -> "Conectando con un nodo de entrada"
            tag.contains("handshake") -> "Negociando el cifrado con el nodo"
            tag.contains("consensus") -> "Descargando el directorio de la red"
            tag.contains("descriptors") -> "Descargando datos de los nodos"
            tag.contains("circuit") -> "Construyendo el circuito"
            else -> "Conectando con la red Tor"
        }

        /** `12 BUILT $FP~a,$FP~b,$FP~salida PURPOSE=GENERAL SOCKS_USERNAME="x"`. */
        fun parseExitFor(status: String, isolation: String): String? {
            val line = status.lines().lastOrNull { " BUILT " in it && "SOCKS_USERNAME=\"$isolation\"" in it }
                ?: return null
            return RELAY.findAll(line).lastOrNull()?.groupValues?.get(1)
        }

        /** Última línea BUILT de propósito general: "12 BUILT $FP~nick,$FP~nick,... PURPOSE=GENERAL". */
        fun parseCircuit(status: String): List<TorRelay> {
            val line = status.lines().lastOrNull { " BUILT " in it && "PURPOSE=GENERAL" in it }
                ?: return emptyList()
            return RELAY.findAll(line).map {
                TorRelay(nickname = it.groupValues[2], fingerprint = it.groupValues[1])
            }.toList()
        }
    }
}
