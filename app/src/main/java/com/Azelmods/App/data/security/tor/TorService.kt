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
 * Tor Service for Orbot integration.
 *
 * Manages Tor connection via Orbot and monitors bootstrap progress through the
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
        private const val ORBOT_PACKAGE = "org.torproject.android"
        private const val CONTROL_PORT = 9051
        private const val SOCKS_PORT = 9050
    }

    // ─── Orbot helpers ───────────────────────────────────────────────────────

    /**
     * Returns `true` if the Orbot app is installed on this device.
     */
    fun isOrbotInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(ORBOT_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Opens the Play Store (or browser fallback) to the Orbot listing.
     */
    fun installOrbot() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$ORBOT_PACKAGE")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$ORBOT_PACKAGE")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launches the Orbot app so the user can start it manually.
     */
    fun startOrbot() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(ORBOT_PACKAGE)
            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Orbot", e)
        }
    }

    // ─── Connection lifecycle ─────────────────────────────────────────────────

    /**
     * Starts monitoring the Tor / Orbot connection.
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

        if (!isOrbotInstalled()) {
            _torState.value = TorState.Error(
                message = "Orbot not installed. Please install Orbot from Play Store."
            )
            return
        }

        isStarting = true
        scope.launch {
            try {
                Log.d(TAG, "Starting Tor connection monitoring…")
                _torState.value = TorState.Connecting(progress = 0, message = "Bootstrapping...")

                monitorBootstrapProgress()

            } catch (e: Exception) {
                Log.e(TAG, "Error starting Tor: ${e.message}", e)
                _torState.value = TorState.Error(
                    message = e.message ?: "Unknown error starting Tor",
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

                    // Authenticate (no password – Orbot default)
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
                                        message = "Bootstrapping..."
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
                        message = "Bootstrapping..."
                    )
                }

                if (progress >= 100) {
                    _torState.value = TorState.Connected(
                        circuitInfo = TorCircuitInfo(
                            entryNode = "Via Orbot",
                            middleNode = "External",
                            exitNode = "Unknown",
                            circuitId = "orbot",
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
                    "Tor bootstrap timeout after $maxAttempts seconds. Make sure Orbot is running."
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

    /** Returns the SOCKS5 proxy port (Orbot default: 9050). */
    fun getSocksPort(): Int = SOCKS_PORT

    /** Returns the HTTP proxy port (Orbot default: 8118). */
    fun getHttpPort(): Int = 8118
}
