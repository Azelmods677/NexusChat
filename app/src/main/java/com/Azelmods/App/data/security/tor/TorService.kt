package com.Azelmods.App.data.security.tor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tor Service for direct Tor integration.
 *
 * Manages Tor connection and monitors bootstrap progress through the
 * Tor control port. Emits top-level [TorState] values; the former inner
 * `TorState` class has been removed to avoid name conflicts.
 */
@Singleton
class TorService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Publicly observable Tor connection state (top-level [TorState]). */
    private val _torState = MutableStateFlow<TorState>(TorState.Disconnected)
    val torState: StateFlow<TorState> = _torState.asStateFlow()

    private var isStarting = false

    companion object {
        private const val TAG = "TorService"
        private const val CONTROL_PORT = 9051
        private const val SOCKS_PORT = 9050
    }

    // ─── Connection lifecycle ─────────────────────────────────────────────────

    /**
     * Starts monitoring the Tor connection directly via embedded Tor binary.
     *
     * Transitions through [TorState.Connecting] while bootstrapping and
     * ultimately emits [TorState.Connected] on success or [TorState.Error] on
     * failure. No-ops when already starting or connected.
     */
    fun startTor() {
        if (isStarting
            || _torState.value is TorState.Connected
            || _torState.value is TorState.Connecting
        ) {
            Log.d(TAG, "Tor already starting or connected – skipping")
            return
        }

        isStarting = true
        scope.launch {
            try {
                Log.d(TAG, "Starting Tor connection monitoring…")
                _torState.value = TorState.Connecting(progress = 0, message = "Iniciando Tor...")

                monitorBootstrapProgress()

            } catch (e: Exception) {
                Log.e(TAG, "Error starting Tor: ${e.message}", e)
                _torState.value = TorState.Error(
                    message = e.message ?: "No se pudo conectar a Tor",
                    exception = e
                )
                isStarting = false
            }
        }
    }

    /**
     * Stops Tor monitoring and resets state to [TorState.Disconnected].
     */
    fun stopTor() {
        scope.launch {
            try {
                Log.d(TAG, "Stopping Tor monitoring…")
                _torState.value = TorState.Disconnected
                isStarting = false
                Log.d(TAG, "Tor monitoring stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Tor: ${e.message}", e)
            }
        }
    }

    // ─── Bootstrap monitoring ─────────────────────────────────────────────────

    /**
     * Polls the Tor control port once per second to read bootstrap progress.
     *
     * Falls back to a time-based progress estimate when the control port is
     * not yet reachable. Times out after 60 seconds.
     */
    private suspend fun monitorBootstrapProgress() {
        try {
            var progress = 0
            var attempts = 0
            val maxAttempts = 60

            while (progress < 100 && attempts < maxAttempts) {
                delay(1000)
                attempts++

                try {
                    val socket = withContext(Dispatchers.IO) {
                        Socket("127.0.0.1", CONTROL_PORT)
                    }

                    val reader: BufferedReader = socket.getInputStream().bufferedReader()
                    val writer: BufferedWriter = socket.getOutputStream().bufferedWriter()

                    // Authenticate (no password – default)
                    writer.write("AUTHENTICATE \"\"\r\n")
                    writer.flush()

                    val authResponse = reader.readLine()
                    Log.d(TAG, "Auth response: $authResponse")

                    if (authResponse?.contains("250 OK") == true) {
                        writer.write("GETINFO status/bootstrap-phase\r\n")
                        writer.flush()

                        val statusLine = reader.readLine()
                        Log.d(TAG, "Bootstrap status: $statusLine")

                        // e.g. "250-status/bootstrap-phase=NOTICE BOOTSTRAP PROGRESS=80 TAG=…"
                        if (statusLine?.contains("PROGRESS=") == true) {
                            Regex("PROGRESS=(\\d+)").find(statusLine)
                                ?.groupValues?.get(1)
                                ?.toIntOrNull()
                                ?.let { parsed ->
                                    progress = parsed
                                    _torState.value = TorState.Connecting(
                                        progress = progress,
                                        message = "Conectando a Tor..."
                                    )
                                    Log.d(TAG, "Tor bootstrap progress: $progress%")
                                }
                        }
                    }

                    writer.write("QUIT\r\n")
                    writer.flush()
                    socket.close()

                } catch (e: Exception) {
                    // Control port not ready yet – use time-based estimate
                    Log.d(TAG, "Control port not ready (attempt $attempts): ${e.message}")
                    progress = minOf(95, (attempts * 100) / 30)
                    _torState.value = TorState.Connecting(
                        progress = progress,
                        message = "Conectando a Tor..."
                    )
                }

                if (progress >= 100) {
                    _torState.value = TorState.Connected(
                        circuitInfo = TorCircuitInfo(
                            entryNode = "Proxy Tor",
                            middleNode = "Externo",
                            exitNode = "Desconocido",
                            circuitId = "tor",
                            bandwidth = 0L
                        )
                    )
                    Log.d(TAG, "Tor connected successfully!")
                    isStarting = false
                    return
                }
            }

            if (progress < 100) {
                throw Exception(
                    "No se pudo conectar a Tor después de $maxAttempts segundos. Intenta de nuevo."
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error monitoring bootstrap: ${e.message}", e)
            _torState.value = TorState.Error(
                message = e.message ?: "Bootstrap failed",
                exception = e
            )
            isStarting = false
        }
    }

    // ─── Port accessors ───────────────────────────────────────────────────────

    /** Returns the SOCKS5 proxy port (default: 9050). */
    fun getSocksPort(): Int = SOCKS_PORT

    /** Returns the HTTP proxy port (default: 8118). */
    fun getHttpPort(): Int = 8118
}
