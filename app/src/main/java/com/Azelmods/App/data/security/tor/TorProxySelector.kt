package com.Azelmods.App.data.security.tor

import android.util.Log
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 🌐 TorProxySelector — ProxySelector global que enruta todo el tráfico HTTP/S
 * a través del proxy SOCKS5 de Orbot cuando Tor está activo.
 *
 * Cómo funciona:
 * - Se instala vía [ProxySelector.setDefault] cuando Tor se conecta.
 * - Mientras [isTorEnabled] sea `true`, todas las conexiones HTTP/HTTPS
 *   y WebSocket se enrutan a través de 127.0.0.1:9050 (SOCKS5 de Orbot).
 * - Orbot internamente traduce SOCKS5 → Tor, dando anonimato a TODAS
 *   las conexiones de red de la app: APIs, Firebase, Coil, etc.
 * - Cuando Tor se desactiva, [restore] restaura el ProxySelector original.
 *
 * @param orbotHost Dirección del proxy SOCKS de Orbot (default: 127.0.0.1)
 * @param orbotPort Puerto SOCKS de Orbot (default: 9050)
 */
class TorProxySelector(
    private val orbotHost: String = "127.0.0.1",
    private val orbotPort: Int = 9050
) : ProxySelector() {

    companion object {
        private const val TAG = "TorProxySelector"

        /** Esquemas URI que se enrutan por Tor */
        private val PROXY_SCHEMES = setOf("http", "https", "ws", "wss")
    }

    /** Dirección SOCKS5 de Orbot — inicializada lazy con los parámetros del constructor */
    private val socksAddress: InetSocketAddress by lazy {
        InetSocketAddress(orbotHost, orbotPort)
    }

    /** Proxy SOCKS5 listo para usar */
    private val socksProxy: Proxy by lazy {
        Proxy(Proxy.Type.SOCKS, socksAddress)
    }

    /** Indica si el enrutamiento Tor está activo */
    @Volatile
    var isTorEnabled: Boolean = false

    /** ProxySelector original para restaurar al desactivar Tor */
    private val originalSelector = AtomicReference<ProxySelector?>(null)

    /**
     * Instala este ProxySelector como el predeterminado del sistema.
     *
     * A partir de este momento, todas las conexiones HTTP/HTTPS/WebSocket
     * de la app se enrutarán a través de Orbot → Tor.
     *
     * @param enableTor si `true`, activa el enrutamiento inmediatamente
     */
    fun install(enableTor: Boolean = true) {
        val current = ProxySelector.getDefault()
        // Guardamos el selector original solo si no somos nosotros mismos
        if (current !== this) {
            originalSelector.set(current)
        }
        isTorEnabled = enableTor
        ProxySelector.setDefault(this)
        Log.d(TAG, "✓ TorProxySelector instalado (torEnabled=$enableTor)")
    }

    /**
     * Restaura el ProxySelector original.
     *
     * Después de llamar a este método, las conexiones HTTP volverán
     * a usar la configuración de red normal del dispositivo.
     */
    fun restore() {
        isTorEnabled = false
        val original = originalSelector.getAndSet(null)
        if (original != null && original !== this) {
            ProxySelector.setDefault(original)
            Log.d(TAG, "✓ ProxySelector original restaurado")
        } else {
            // Si no hay original, usar el predeterminado del sistema
            ProxySelector.setDefault(null)
            Log.d(TAG, "✓ ProxySelector default del sistema restaurado")
        }
    }

    /**
     * Activa o desactiva el enrutamiento Tor sin cambiar el selector instalado.
     */
    fun setEnabled(enabled: Boolean) {
        isTorEnabled = enabled
        Log.d(TAG, "TorProxySelector.enabled = $enabled")
    }

    // ── ProxySelector overrides ─────────────────────────────────

    /**
     * Retorna el proxy a usar para una URI dada.
     *
     * - Si Tor está activo → retorna el proxy SOCKS5 de Orbot
     * - Si Tor está inactivo → delega al selector original o retorna DIRECT
     */
    override fun select(uri: URI?): List<Proxy> {
        if (uri == null) return listOf(Proxy.NO_PROXY)

        if (isTorEnabled) {
            val scheme = uri.scheme?.lowercase() ?: ""
            // Solo enrutamos http, https y ws/wss a través de Tor
            if (scheme in PROXY_SCHEMES) {
                return listOf(socksProxy)
            }
        }

        // Si Tor no está activo, delegar al selector original
        val original = originalSelector.get()
        if (original != null && original !== this) {
            return original.select(uri)
        }

        return listOf(Proxy.NO_PROXY)
    }

    /**
     * Callback cuando una conexión a través del proxy falla.
     * Delega al selector original si existe.
     */
    override fun connectFailed(uri: URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {
        Log.w(TAG, "connectFailed: uri=$uri sa=$sa error=${ioe?.message}")

        if (isTorEnabled) {
            Log.e(TAG, "✗ Conexión Tor falló para $uri — ¿Orbot activo?")
        }

        // Delegar al selector original
        val original = originalSelector.get()
        if (original != null && original !== this) {
            original.connectFailed(uri, sa, ioe)
        }
    }
}
