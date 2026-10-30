package com.Azelmods.App.data.security.tor

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔍 TorService — conecta la app a Tor a través de Orbot y vigila la conexión.
 *
 * Flujo de [startTor]:
 * 1. Pide a Orbot que arranque Tor por su API de intents ([OrbotIntents]): el
 *    usuario ya no tiene que salir a Orbot y pulsar "Iniciar".
 * 2. Espera a que el SOCKS de Orbot responda en el puerto REAL que anuncia.
 * 3. Instala el [TorProxySelector] global (HTTP/HTTPS/WebSocket de la app por
 *    Tor), activa el [TorDnsResolver] sin fugas y reconecta Firebase.
 * 4. Verifica con check.torproject.org que el tráfico sale por Tor y con qué IP,
 *    y detecta si Orbot está en modo VPN (toda la app por Tor).
 * 5. Vigila: si Orbot deja de responder, el estado pasa a error pero el proxy NO
 *    se retira. Las conexiones fallan en vez de salir por la red normal (cierre
 *    seguro): con el modo anónimo activo, nada debe salir sin Tor por sorpresa.
 */
@Singleton
class TorService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val torDnsResolver: TorDnsResolver,
    private val firebaseProxyConfigurator: FirebaseProxyConfigurator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _torState = MutableStateFlow<TorState>(TorState.Disconnected)
    val torState: StateFlow<TorState> = _torState.asStateFlow()

    @Volatile
    private var isMonitoring = false
    private var connectJob: Job? = null
    private var watchdogJob: Job? = null

    /** ProxySelector global para enrutar tráfico por Tor */
    private val torProxySelector = TorProxySelector()

    companion object {
        private const val TAG = "TorService"
        const val SOCKS_PORT = OrbotDetector.DEFAULT_SOCKS_PORT
        const val HTTP_PROXY_PORT = OrbotDetector.DEFAULT_HTTP_PORT

        private const val CONNECT_TIMEOUT_S = 60
        private const val WATCHDOG_INTERVAL_MS = 15_000L
    }

    init {
        OrbotIntents.register(context)
    }

    /**
     * Conecta a Tor vía Orbot. Idempotente: si ya está conectando o conectado,
     * no hace nada.
     */
    fun startTor() {
        if (isMonitoring || _torState.value is TorState.Connected) {
            Log.d(TAG, "Tor ya está activo o conectándose")
            return
        }
        isMonitoring = true
        connectJob = scope.launch {
            try {
                _torState.value = TorState.Connecting(progress = 3, message = "Pidiendo a Orbot que se conecte…")

                val installed = OrbotDetector.isOrbotInstalled(context)
                if (installed) OrbotIntents.requestStart(context)

                if (!installed && !OrbotDetector.isSocksProxyAvailable(1_000)) {
                    _torState.value = TorState.Error(
                        message = "Orbot no está instalado. Instálalo desde Google Play o F-Droid " +
                            "(org.torproject.android) y vuelve a activar el modo anónimo."
                    )
                    return@launch
                }

                // Cierre seguro desde el primer momento: el proxy se instala YA, antes
                // de que Orbot esté listo. Mientras arranca, las conexiones esperan o
                // fallan, pero ninguna sale por la red normal "mientras tanto".
                torProxySelector.setSocksPort(OrbotDetector.socksPort)
                torProxySelector.install(enableTor = true)
                torDnsResolver.isTorEnabled = true

                // ── Esperar a Orbot ──
                var connected = false
                for (second in 1..CONNECT_TIMEOUT_S) {
                    val orbot = OrbotIntents.status.value
                    orbot.socksPort?.let { OrbotDetector.socksPort = it }

                    if (orbot.status == OrbotIntents.STATUS_STARTS_DISABLED) {
                        _torState.value = TorState.Connecting(
                            progress = 10,
                            message = "Orbot no deja que otras apps lo enciendan: ábrelo y pulsa «Iniciar»."
                        )
                    }
                    if (OrbotDetector.isSocksProxyAvailable(1_000)) {
                        connected = true
                        break
                    }
                    _torState.value = TorState.Connecting(
                        progress = (5 + second * 85 / CONNECT_TIMEOUT_S).coerceAtMost(90),
                        message = when (orbot.status) {
                            OrbotIntents.STATUS_STARTING -> "Orbot está conectando con la red Tor…"
                            OrbotIntents.STATUS_ON -> "Orbot conectado, abriendo el proxy…"
                            else -> "Esperando a Orbot… ($second s)"
                        }
                    )
                    delay(1_000)
                }

                if (!connected) {
                    _torState.value = TorState.Error(
                        message = "Orbot no respondió. Ábrelo y pulsa «Iniciar». Mientras tanto la app no sale " +
                            "a internet sin Tor; desactiva el modo anónimo si quieres navegar sin él."
                    )
                    // Se sigue vigilando: en cuanto Orbot arranque, se conecta solo.
                    startWatchdog(startDown = true)
                    return@launch
                }

                // ── Enrutar la app por Tor ──
                val port = OrbotDetector.socksPort
                torProxySelector.setSocksPort(port)
                firebaseProxyConfigurator.enableTorMode()
                Log.i(TAG, "✓ Tráfico de la app enrutado por Tor (SOCKS $port)")

                _torState.value = TorState.Connected(
                    circuitInfo = baseInfo(port).copy(checkMessage = "Comprobando la salida por Tor…")
                )
                verifyConnection()
                startWatchdog()
            } catch (e: Exception) {
                Log.e(TAG, "Error conectando a Tor", e)
                _torState.value = TorState.Error(message = "Error: ${e.message}", exception = e)
            } finally {
                isMonitoring = false
            }
        }
    }

    /**
     * Comprueba con check.torproject.org que el tráfico sale por Tor y actualiza
     * la IP de salida. La pantalla lo llama con "Comprobar".
     */
    fun verifyConnection() {
        scope.launch {
            val current = _torState.value as? TorState.Connected ?: return@launch
            val port = current.circuitInfo.socksPort
            val viaSocks = TorConnectivityChecker.checkViaSocks(port)
            // Modo VPN de Orbot: solo se comprueba sin proxy si hay una VPN activa,
            // para no revelar la IP real a nadie innecesariamente.
            val vpnMode = TorConnectivityChecker.isVpnActive(context) &&
                TorConnectivityChecker.checkDirect().isTor

            val message = when {
                viaSocks.isTor && vpnMode -> "Verificado: toda la app sale por Tor (Orbot en modo VPN)."
                viaSocks.isTor -> "Verificado: el navegador, la IA y las descargas salen por Tor."
                viaSocks.error != null -> "No se pudo verificar (${viaSocks.error}). Tor puede seguir arrancando."
                else -> "El proxy responde pero la salida NO es Tor. Revisa la configuración de Orbot."
            }
            _torState.update { state ->
                if (state !is TorState.Connected) state
                else state.copy(
                    circuitInfo = state.circuitInfo.copy(
                        exitIp = viaSocks.ip ?: state.circuitInfo.exitIp,
                        exitNode = viaSocks.ip?.let { "Salida $it" } ?: state.circuitInfo.exitNode,
                        verified = viaSocks.isTor,
                        vpnMode = vpnMode,
                        checkMessage = message
                    )
                )
            }
        }
    }

    /**
     * Vigila que Orbot siga vivo. Si cae, el estado pasa a error pero el proxy
     * SIGUE instalado (las conexiones fallan en lugar de salir sin Tor). Si vuelve,
     * se recupera solo.
     */
    private fun startWatchdog(startDown: Boolean = false) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            var down = startDown
            while (true) {
                delay(WATCHDOG_INTERVAL_MS)
                OrbotIntents.status.value.socksPort?.let { newPort ->
                    if (newPort != OrbotDetector.socksPort) {
                        OrbotDetector.socksPort = newPort
                        torProxySelector.setSocksPort(newPort)
                    }
                }
                val alive = OrbotDetector.isSocksProxyAvailable(2_000)
                if (!alive && !down) {
                    down = true
                    Log.w(TAG, "Orbot dejó de responder: tráfico bloqueado hasta que vuelva")
                    _torState.value = TorState.Error(
                        message = "Orbot dejó de responder. Por seguridad la app no sale a internet sin Tor " +
                            "hasta que Orbot vuelva o desactives el modo anónimo."
                    )
                } else if (alive && down) {
                    down = false
                    Log.i(TAG, "Orbot vuelve a responder")
                    torProxySelector.setSocksPort(OrbotDetector.socksPort)
                    firebaseProxyConfigurator.enableTorMode()
                    _torState.value = TorState.Connected(baseInfo(OrbotDetector.socksPort))
                    verifyConnection()
                }
            }
        }
    }

    private fun baseInfo(port: Int) = TorCircuitInfo(
        entryNode = "Orbot",
        middleNode = "Red Tor",
        exitNode = "Comprobando…",
        circuitId = "socks5://127.0.0.1:$port",
        bandwidth = 0L,
        socksPort = port
    )

    /**
     * Detiene el modo anónimo y restaura la conectividad normal.
     */
    fun stopTor() {
        connectJob?.cancel()
        watchdogJob?.cancel()
        isMonitoring = false
        scope.launch {
            torDnsResolver.isTorEnabled = false
            firebaseProxyConfigurator.disableTorMode()
            torProxySelector.restore()
            _torState.value = TorState.Disconnected
            Log.d(TAG, "Modo anónimo desactivado: tráfico directo")
        }
    }

    /**
     * Restaura el modo anónimo al abrir la app si el usuario lo dejó activado.
     * Antes solo se restauraba al entrar en la pantalla de seguridad: hasta
     * entonces, la app navegaba sin Tor aunque el usuario lo hubiera activado.
     */
    fun restoreIfEnabled(preferences: TorPreferences) {
        if (preferences.isAnonymousModeEnabled()) startTor()
    }

    /** Retorna el TorProxySelector para consultar/ajustar estado */
    fun getProxySelector(): TorProxySelector = torProxySelector

    fun getSocksPort(): Int = OrbotDetector.socksPort
    fun getHttpPort(): Int = OrbotDetector.httpPort
}
