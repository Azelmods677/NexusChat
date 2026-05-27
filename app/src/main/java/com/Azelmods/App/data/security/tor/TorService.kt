package com.Azelmods.App.data.security.tor

import android.content.Context
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔍 TorService — Detecta Orbot y gestiona el ProxySelector global
 *
 * Responsabilidades:
 * 1. Detectar si Orbot está instalado y funcionando
 * 2. Instalar/restaurar [TorProxySelector] para enrutar todo el
 *    tráfico HTTP de la app a través de Orbot → Tor cuando
 *    el modo anónimo está activo
 * 3. Exponer el estado de Tor para la UI
 * 4. Gestionar [TorDnsResolver] para evitar DNS leaks
 * 5. Configurar [FirebaseProxyConfigurator] para Firebase sobre Tor
 *
 * Cuando Tor se conecta, [TorProxySelector] se instala globalmente
 * via [java.net.ProxySelector.setDefault], enrutando todas las
 * conexiones HTTP/HTTPS/WebSocket a través de SOCKS5 de Orbot.
 * Adicionalmente, [TorDnsResolver] y [FirebaseProxyConfigurator]
 * se activan para cubrir DNS y Firebase respectivamente.
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

    private var isMonitoring = false

    /** ProxySelector global para enrutar tráfico por Tor */
    private val torProxySelector = TorProxySelector()

    companion object {
        private const val TAG = "TorService"
        const val SOCKS_PORT = 9050
        const val HTTP_PROXY_PORT = 8118
    }

    /**
     * Inicia la detección de Orbot.
     *
     * 1. Escanea los puertos proxy de Orbot cada segundo
     * 2. Cuando Orbot responde, instala [TorProxySelector] globalmente
     * 3. Todo el tráfico HTTP/HTTPS de la app se enruta por Tor
     */
    fun startTor() {
        if (isMonitoring) {
            Log.d(TAG, "Ya monitoreando Orbot")
            return
        }

        isMonitoring = true
        scope.launch {
            try {
                Log.d(TAG, "Buscando Orbot...")
                _torState.value = TorState.Connecting(progress = 0, message = "Buscando Orbot...")

                // Verificar si Orbot está instalado
                if (!OrbotDetector.isOrbotInstalled(context)) {
                    _torState.value = TorState.Error(
                        message = "Orbot no está instalado. Descárgalo desde Play Store o F-Droid (org.torproject.android)",
                        exception = null
                    )
                    isMonitoring = false
                    return@launch
                }

                // Monitorear hasta que Orbot esté activo (máx 30 segundos)
                var attempts = 0
                val maxAttempts = 30

                while (attempts < maxAttempts) {
                    attempts++
                    val progress = ((attempts * 100) / maxAttempts).coerceAtMost(95)
                    _torState.value = TorState.Connecting(
                        progress = progress,
                        message = "Esperando que Orbot se conecte a Tor... ($attempts/$maxAttempts)"
                    )

                    if (OrbotDetector.isTorAvailable()) {
                        Log.d(TAG, "✓ Orbot detectado y conectado!")

                        // ── 1. Instalar ProxySelector global ──
                        // A partir de aquí, TODAS las conexiones HTTP/HTTPS
                        // de la app (OkHttp, Firebase, Coil, etc.) pasan
                        // por Orbot → SOCKS5 → Tor
                        torProxySelector.install(enableTor = true)
                        Log.i(TAG, "✓ TorProxySelector instalado — Todo el tráfico enrutado por Tor")

                        // ── 2. Activar TorDnsResolver (evita DNS leaks) ──
                        torDnsResolver.isTorEnabled = true
                        Log.i(TAG, "✓ TorDnsResolver activado — DNS por Tor sin leaks")

                        // ── 3. Activar FirebaseProxyConfigurator ──
                        firebaseProxyConfigurator.enableTorMode()
                        Log.i(TAG, "✓ FirebaseProxyConfigurator activado — Firebase por Tor")

                        _torState.value = TorState.Connected(
                            circuitInfo = TorCircuitInfo(
                                entryNode = "Orbot",
                                middleNode = "Red Tor",
                                exitNode = "Nodo de salida",
                                circuitId = "orbot_${System.currentTimeMillis()}",
                                bandwidth = 0L
                            )
                        )
                        isMonitoring = false
                        return@launch
                    }

                    delay(1000)
                }

                // Timeout
                _torState.value = TorState.Error(
                    message = "Orbot no responde. Abre Orbot y presiona 'Iniciar' para conectar a Tor.",
                    exception = null
                )
                isMonitoring = false

            } catch (e: Exception) {
                Log.e(TAG, "Error monitoreando Orbot", e)
                _torState.value = TorState.Error(
                    message = "Error: ${e.message}",
                    exception = e
                )
                isMonitoring = false
            }
        }
    }

    /**
     * Detiene Tor y restaura la conectividad normal.
     *
     * - Restaura el ProxySelector original del sistema
     * - Las conexiones HTTP vuelven a la configuración normal
     */
    fun stopTor() {
        scope.launch {
            // ── 1. Desactivar TorDnsResolver ──
            torDnsResolver.isTorEnabled = false
            Log.i(TAG, "✓ TorDnsResolver desactivado — DNS del sistema")

            // ── 2. Desactivar FirebaseProxyConfigurator ──
            firebaseProxyConfigurator.disableTorMode()
            Log.i(TAG, "✓ FirebaseProxyConfigurator desactivado — Firebase directo")

            // ── 3. Restaurar ProxySelector original ──
            torProxySelector.restore()
            Log.i(TAG, "✓ ProxySelector restaurado — Tráfico directo")

            _torState.value = TorState.Disconnected
            isMonitoring = false
            Log.d(TAG, "Monitoreo de Orbot detenido")
        }
    }

    /**
     * Retorna el TorProxySelector para consultar/ajustar estado
     */
    fun getProxySelector(): TorProxySelector = torProxySelector

    fun getSocksPort(): Int = SOCKS_PORT
    fun getHttpPort(): Int = HTTP_PROXY_PORT
}
