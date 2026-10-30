package com.Azelmods.App.data.security.tor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * API pública de Orbot por intents (la misma que usa NetCipher/OrbotHelper).
 *
 * Con ella NexusChat puede:
 * - pedir a Orbot que arranque Tor SIN que el usuario cambie de app
 *   ([requestStart]); antes había que abrir Orbot y pulsar "Iniciar" a mano;
 * - saber el estado real (ON/STARTING/OFF…) y los PUERTOS reales que usa Orbot
 *   ([status]). Orbot cambia de puerto si el 9050 está ocupado; con los puertos
 *   fijos de antes, en ese caso el modo anónimo "no detectaba" Orbot.
 */
object OrbotIntents {

    private const val TAG = "OrbotIntents"

    const val ORBOT_PACKAGE = "org.torproject.android"
    const val ACTION_START = "org.torproject.android.intent.action.START"
    const val ACTION_STATUS = "org.torproject.android.intent.action.STATUS"
    const val EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS"
    const val EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME"
    const val EXTRA_SOCKS_PROXY_PORT = "org.torproject.android.intent.extra.SOCKS_PROXY_PORT"
    const val EXTRA_HTTP_PROXY_PORT = "org.torproject.android.intent.extra.HTTP_PROXY_PORT"

    const val STATUS_ON = "ON"
    const val STATUS_OFF = "OFF"
    const val STATUS_STARTING = "STARTING"
    const val STATUS_STOPPING = "STOPPING"
    /** El usuario desactivó en Orbot "permitir que otras apps lo inicien". */
    const val STATUS_STARTS_DISABLED = "STARTS_DISABLED"

    data class Status(
        val status: String? = null,
        val socksPort: Int? = null,
        val httpPort: Int? = null,
        val receivedAt: Long = 0L
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_STATUS) return
            val extras = intent.extras
            val status = extras?.getString(EXTRA_STATUS)
            val socks = readPort(intent, EXTRA_SOCKS_PROXY_PORT)
            val http = readPort(intent, EXTRA_HTTP_PROXY_PORT)
            Log.d(TAG, "Orbot: status=$status socks=$socks http=$http")
            _status.value = Status(
                status = status,
                socksPort = socks ?: _status.value.socksPort,
                httpPort = http ?: _status.value.httpPort,
                receivedAt = System.currentTimeMillis()
            )
            socks?.let { OrbotDetector.socksPort = it }
            http?.let { OrbotDetector.httpPort = it }
        }
    }

    /** Empieza a escuchar el estado de Orbot (idempotente). */
    fun register(context: Context) {
        if (registered) return
        synchronized(this) {
            if (registered) return
            runCatching {
                // EXPORTED: el emisor es otra app (Orbot).
                ContextCompat.registerReceiver(
                    context.applicationContext,
                    receiver,
                    IntentFilter(ACTION_STATUS),
                    ContextCompat.RECEIVER_EXPORTED
                )
                registered = true
            }.onFailure { Log.w(TAG, "No se pudo escuchar a Orbot: ${it.message}") }
        }
    }

    /**
     * Pide a Orbot que arranque Tor. Orbot responde con un broadcast de estado
     * (y con STARTS_DISABLED si el usuario no lo permite). Devuelve false si no se
     * pudo enviar (Orbot no instalado).
     */
    fun requestStart(context: Context): Boolean = runCatching {
        val intent = Intent(ACTION_START).apply {
            setPackage(ORBOT_PACKAGE)
            putExtra(EXTRA_PACKAGE_NAME, context.packageName)
        }
        context.sendBroadcast(intent)
        true
    }.getOrElse {
        Log.w(TAG, "No se pudo pedir a Orbot que arranque: ${it.message}")
        false
    }

    private fun readPort(intent: Intent, key: String): Int? {
        val extras = intent.extras ?: return null
        if (!extras.containsKey(key)) return null
        // Según la versión de Orbot llega como Int o como String.
        val asInt = extras.getInt(key, -1)
        if (asInt in 1..65535) return asInt
        return extras.getString(key)?.substringAfterLast(':')?.trim()?.toIntOrNull()?.takeIf { it in 1..65535 }
    }
}
