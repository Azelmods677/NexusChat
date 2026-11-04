package com.Azelmods.App.data.security.tor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Qué motor pone Tor. */
enum class TorEngine(val label: String) {
    /** Dentro de la propia app: no hace falta instalar nada. */
    EMBEDDED("Integrado"),

    /** La app Orbot, si el usuario prefiere usarla (comparte Tor con otras apps). */
    ORBOT("Orbot")
}

/**
 * Modo anónimo: conecta la app a Tor, enruta su tráfico y vigila la conexión.
 *
 * Funciona con Tor integrado ([EmbeddedTor], sin dependencias externas) o a través de Orbot.
 *
 * Es de cierre seguro desde el primer instante: el enrutado se activa ANTES de que Tor esté
 * listo, apuntando a un puerto que aún no responde. Mientras conecta, las conexiones fallan;
 * nada sale por la red normal "mientras tanto". Y si Tor cae, sigue igual hasta que vuelva
 * o el usuario apague el modo.
 */
@Singleton
class TorService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val torDnsResolver: TorDnsResolver,
    private val firebaseProxyConfigurator: FirebaseProxyConfigurator,
    private val embeddedTor: EmbeddedTor,
    private val preferences: TorPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _torState = MutableStateFlow<TorState>(TorState.Disconnected)
    val torState: StateFlow<TorState> = _torState.asStateFlow()

    private var connectJob: Job? = null
    private var watchdogJob: Job? = null

    private val torProxySelector = TorProxySelector()

    /** Motor en uso (o el que se usará al activar el modo). */
    val engine: TorEngine get() = preferences.getEngine()

    /** Versión de Tor integrada, para mostrarla en la interfaz. */
    val embeddedVersion: String get() = embeddedTor.version

    companion object {
        private const val TAG = "TorService"
        const val SOCKS_PORT = OrbotDetector.DEFAULT_SOCKS_PORT
        const val HTTP_PROXY_PORT = OrbotDetector.DEFAULT_HTTP_PORT

        /** Puerto cerrado para el cierre seguro mientras Tor aún no ha abierto el suyo. */
        const val FAIL_CLOSED_PORT = 19050
        private const val EMBEDDED_TIMEOUT_MS = 180_000L
        private const val ORBOT_TIMEOUT_S = 60
        private const val WATCHDOG_INTERVAL_MS = 15_000L

        // Medido: acepta Firebase alrededor de 1 salida de cada 5.
        private const val EXIT_PROBES_PER_ROUND = 6
        private const val EXIT_SEARCH_ROUNDS = 8

        /** Cada cuántas vueltas del vigilante se comprueba la salida (20 × 15 s = 5 min). */
        private const val EXIT_RECHECK_TICKS = 20
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        OrbotIntents.register(context)
    }

    private val isBusy: Boolean
        get() = _torState.value is TorState.Connected || _torState.value is TorState.Connecting

    /** Activa el modo anónimo. Idempotente. */
    fun startTor() {
        if (isBusy) return
        AnonymityPolicy.active = true
        routeTrafficTo(FAIL_CLOSED_PORT)
        torDnsResolver.isTorEnabled = true

        _torState.value = TorState.Connecting(1, "Preparando la conexión")
        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                when (preferences.getEngine()) {
                    TorEngine.EMBEDDED -> connectEmbedded()
                    TorEngine.ORBOT -> connectOrbot()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error conectando a Tor", e)
                _torState.value = TorState.Error("No se pudo conectar: ${e.message}", e)
            }
        }
    }

    /** Sigue el arranque real de Tor por su puerto de control. */
    private suspend fun connectEmbedded() {
        embeddedTor.start()
        val deadline = System.currentTimeMillis() + EMBEDDED_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val progress = embeddedTor.bootstrap()
            if (progress != null) {
                if (progress.percent >= 100) {
                    val port = embeddedTor.socksPort()
                    if (port != null) {
                        onTorReady(port)
                        return
                    }
                }
                _torState.value = TorState.Connecting(progress.percent.coerceAtMost(99), progress.summary)
            }
            delay(400)
        }
        _torState.value = TorState.Error(
            "Tor no terminó de conectar. Puede que tu red bloquee Tor. Mientras el modo esté " +
                "activo, la app no sale a internet sin él: apágalo para volver a la conexión normal."
        )
        startWatchdog(startDown = true)
    }

    /** Pide a Orbot que arranque y espera a que su SOCKS responda. */
    private suspend fun connectOrbot() {
        val installed = OrbotDetector.isOrbotInstalled(context)
        if (installed) OrbotIntents.requestStart(context)
        if (!installed && !isPortOpen(OrbotDetector.socksPort, 1_000)) {
            _torState.value = TorState.Error(
                "Orbot no está instalado. Instálalo o cambia al motor integrado, que no necesita ninguna otra app."
            )
            return
        }
        for (second in 1..ORBOT_TIMEOUT_S) {
            val orbot = OrbotIntents.status.value
            orbot.socksPort?.let { OrbotDetector.socksPort = it }
            if (isPortOpen(OrbotDetector.socksPort, 1_000)) {
                onTorReady(OrbotDetector.socksPort)
                return
            }
            _torState.value = TorState.Connecting(
                progress = (5 + second * 85 / ORBOT_TIMEOUT_S).coerceAtMost(90),
                message = when (orbot.status) {
                    OrbotIntents.STATUS_STARTS_DISABLED -> "Orbot no deja que otras apps lo enciendan: ábrelo y pulsa Iniciar"
                    OrbotIntents.STATUS_STARTING -> "Orbot está conectando con la red Tor"
                    OrbotIntents.STATUS_ON -> "Orbot conectado, abriendo el proxy"
                    else -> "Esperando a Orbot"
                }
            )
            delay(1_000)
        }
        _torState.value = TorState.Error(
            "Orbot no respondió. Ábrelo y pulsa Iniciar, o cambia al motor integrado."
        )
        startWatchdog(startDown = true)
    }

    private suspend fun onTorReady(port: Int) {
        // El tráfico sigue bloqueado hasta tener una salida que Firebase acepte: si no,
        // la app abre sesión contra una salida rechazada y parece vacía.
        val reachable = when (preferences.getEngine()) {
            TorEngine.EMBEDDED -> findFirebaseExit(port) { attempt ->
                _torState.value = TorState.Connecting(99, "Buscando una salida que Firebase acepte ($attempt)")
            }
            // Orbot no deja elegir la salida desde aquí: solo se puede avisar.
            TorEngine.ORBOT -> FirebaseReachability.isReachable(port, newIsolation(), FirebaseReachability.databaseHost())
        }
        routeTrafficTo(port)
        // Firebase reabre su conexión para que pase ya por Tor.
        firebaseProxyConfigurator.enableTorMode()
        Log.i(TAG, "Tráfico de la app enrutado por Tor (SOCKS $port), Firebase accesible: $reachable")
        _torState.value = TorState.Connected(
            baseInfo(port).copy(checkMessage = "Comprobando la salida por Tor", firebaseReachable = reachable)
        )
        verifyConnection()
        startWatchdog()
    }

    /**
     * Prueba varias salidas a la vez y fija la primera que Firebase acepte. Cada circuito de
     * prueba tarda segundos y la mayoría falla, así que en paralelo se tarda mucho menos.
     */
    private suspend fun findFirebaseExit(port: Int, onAttempt: (Int) -> Unit = {}): Boolean {
        val host = FirebaseReachability.databaseHost()
        embeddedTor.clearExit()
        for (round in 1..EXIT_SEARCH_ROUNDS) {
            onAttempt(round)
            val exit = coroutineScope {
                List(EXIT_PROBES_PER_ROUND) {
                    async {
                        val isolation = newIsolation()
                        if (FirebaseReachability.isReachable(port, isolation, host)) {
                            embeddedTor.exitOfIsolation(isolation)
                        } else {
                            null
                        }
                    }
                }.awaitAll().firstOrNull { it != null }
            }
            if (exit != null && embeddedTor.pinExit(exit)) {
                Log.i(TAG, "Salida compatible con Firebase encontrada en la ronda $round")
                return true
            }
        }
        Log.w(TAG, "Ninguna salida probada acepta Firebase")
        return false
    }

    private fun newIsolation(): String = "nexus-${UUID.randomUUID()}"

    private fun routeTrafficTo(port: Int) {
        OrbotDetector.socksPort = port
        torProxySelector.setSocksPort(port)
        torProxySelector.install(enableTor = true)
        TorSocketRouter.enable(port)
        // WebView tiene su propia pila de red: sin esto, cualquier página cargada dentro de
        // la app saldría con la IP real aunque el resto vaya por Tor.
        mainHandler.post { ProxyManager.applyProxyRule("socks5://127.0.0.1:$port") {} }
    }

    /**
     * Pregunta al Proyecto Tor si el tráfico sale por su red y con qué IP, y actualiza el
     * circuito y qué partes de la app están pasando realmente por Tor.
     */
    fun verifyConnection() {
        scope.launch {
            val current = _torState.value as? TorState.Connected ?: return@launch
            val check = TorConnectivityChecker.checkViaSocks(current.circuitInfo.socksPort)
            val message = when {
                check.isTor -> "Verificado por check.torproject.org"
                check.error != null -> "No se pudo verificar: ${check.error}"
                else -> "El proxy responde, pero la salida NO es Tor"
            }
            _torState.update { state ->
                if (state !is TorState.Connected) state
                else state.copy(circuitInfo = withCircuit(state.circuitInfo).copy(
                    exitIp = check.ip ?: state.circuitInfo.exitIp,
                    verified = check.isTor,
                    checkMessage = message
                ))
            }
        }
    }

    /** Busca otra salida compatible y circuitos nuevos: cambia la IP con la que se te ve. */
    fun newIdentity(): Boolean {
        if (preferences.getEngine() != TorEngine.EMBEDDED) return false
        val port = embeddedTor.socksPort() ?: return false
        scope.launch {
            val reachable = findFirebaseExit(port)
            markFirebaseReachable(reachable)
            verifyConnection()
        }
        return true
    }

    private fun markFirebaseReachable(reachable: Boolean) {
        _torState.update { state ->
            if (state is TorState.Connected) {
                state.copy(circuitInfo = state.circuitInfo.copy(firebaseReachable = reachable))
            } else state
        }
    }

    /** Vigila que Tor siga vivo. Si cae, el tráfico sigue bloqueado hasta que vuelva. */
    private fun startWatchdog(startDown: Boolean = false) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            var down = startDown
            var ticks = 0
            while (true) {
                delay(WATCHDOG_INTERVAL_MS)
                if (++ticks % EXIT_RECHECK_TICKS == 0 && !down) recheckFirebaseExit()
                val port = currentSocksPort()
                val alive = port != null && isPortOpen(port, 2_000)
                when {
                    !alive && !down -> {
                        down = true
                        Log.w(TAG, "Tor dejó de responder: tráfico bloqueado hasta que vuelva")
                        _torState.value = TorState.Error(
                            "Tor dejó de responder. Por seguridad la app no sale a internet sin él " +
                                "hasta que vuelva o apagues el modo anónimo."
                        )
                    }
                    alive && down -> {
                        down = false
                        routeTrafficTo(port!!)
                        firebaseProxyConfigurator.enableTorMode()
                        _torState.value = TorState.Connected(baseInfo(port))
                        verifyConnection()
                    }
                    alive -> _torState.update { state ->
                        if (state is TorState.Connected) {
                            state.copy(circuitInfo = withCircuit(state.circuitInfo))
                        } else state
                    }
                }
            }
        }
    }

    /** Google puede empezar a bloquear la salida fijada: si pasa, se busca otra sin avisar. */
    private suspend fun recheckFirebaseExit() {
        val port = currentSocksPort() ?: return
        val host = FirebaseReachability.databaseHost()
        var reachable = FirebaseReachability.isReachable(port, newIsolation(), host)
        if (!reachable && preferences.getEngine() == TorEngine.EMBEDDED) {
            Log.w(TAG, "Firebase dejó de aceptar la salida: buscando otra")
            reachable = findFirebaseExit(port)
        }
        markFirebaseReachable(reachable)
    }

    private fun currentSocksPort(): Int? = when (preferences.getEngine()) {
        TorEngine.EMBEDDED -> embeddedTor.socksPort()
        TorEngine.ORBOT -> OrbotIntents.status.value.socksPort ?: OrbotDetector.socksPort
    }

    private fun baseInfo(port: Int) = withCircuit(
        TorCircuitInfo(
            entryNode = "",
            middleNode = "",
            exitNode = "",
            circuitId = "socks5://127.0.0.1:$port",
            bandwidth = 0L,
            socksPort = port,
            engine = preferences.getEngine()
        )
    )

    /** Añade lo que solo se sabe en vivo: el circuito real y si Firebase ya va por Tor. */
    private fun withCircuit(info: TorCircuitInfo): TorCircuitInfo {
        val relays = if (info.engine == TorEngine.EMBEDDED) embeddedTor.currentCircuit() else emptyList()
        return info.copy(
            entryNode = relays.getOrNull(0)?.nickname.orEmpty(),
            middleNode = relays.getOrNull(1)?.nickname.orEmpty(),
            exitNode = relays.lastOrNull()?.nickname.orEmpty(),
            messagingViaTor = TorSocketRouter.messagingRouted
        )
    }

    /** Apaga el modo anónimo y restaura la conexión normal. */
    fun stopTor() {
        connectJob?.cancel()
        watchdogJob?.cancel()
        scope.launch {
            AnonymityPolicy.active = false
            torDnsResolver.isTorEnabled = false
            TorSocketRouter.disable()
            torProxySelector.restore()
            mainHandler.post { ProxyManager.clearProxy {} }
            OrbotDetector.socksPort = OrbotDetector.DEFAULT_SOCKS_PORT
            embeddedTor.stop()
            firebaseProxyConfigurator.disableTorMode()
            _torState.value = TorState.Disconnected
            Log.d(TAG, "Modo anónimo desactivado: tráfico directo")
        }
    }

    /** Cambia de motor. Si el modo estaba activo, reconecta con el nuevo. */
    fun setEngine(engine: TorEngine) {
        if (engine == preferences.getEngine()) return
        val wasActive = _torState.value !is TorState.Disconnected
        if (wasActive && preferences.getEngine() == TorEngine.EMBEDDED) embeddedTor.stop()
        preferences.setEngine(engine)
        if (wasActive) {
            connectJob?.cancel()
            watchdogJob?.cancel()
            _torState.value = TorState.Disconnected
            startTor()
        }
    }

    /** Restaura el modo anónimo al abrir la app, antes de que haga ninguna petición. */
    fun restoreIfEnabled(preferences: TorPreferences) {
        if (preferences.isAnonymousModeEnabled()) startTor()
    }

    fun getProxySelector(): TorProxySelector = torProxySelector

    fun getSocksPort(): Int = OrbotDetector.socksPort
    fun getHttpPort(): Int = OrbotDetector.httpPort

    private fun isPortOpen(port: Int, timeoutMs: Int): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), timeoutMs) }
        true
    }.getOrDefault(false)
}
