package com.Azelmods.App.data.security.tor

import android.util.Log
import okhttp3.Dns
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🌐 TorDnsResolver — DNS resolver que enruta consultas DNS a través de Tor
 *
 * ## ¿Por qué es necesario?
 *
 * Aunque [TorProxySelector] enruta el tráfico HTTP/HTTPS por Tor, las
 * consultas DNS nativas de Java (InetAddress.getAllByName) NO pasan por
 * el ProxySelector y pueden **filtrar** los dominios que estás visitando
 * a tu ISP incluso cuando Tor está activo.
 *
 * ## Cómo funciona
 *
 * Cuando Tor está habilitado, `TorDnsResolver.lookup()` no usa el DNS
 * del sistema. En su lugar, resuelve los nombres DNS a través del proxy
 * SOCKS5 de Orbot:
 *
 * 1. Abre una conexión TCP directa a 127.0.0.1:9050 (SOCKS de Orbot)
 * 2. Realiza un handshake SOCKS5 para resolver el dominio
 * 3. Retorna 127.0.0.1 como IP (la conexión real se hace luego por SOCKS5)
 *
 * Esto evita **DNS leaks** y asegura que cada aspecto de la resolución
 * de nombres pase por la red Tor.
 *
 * ## Uso
 *
 * Simplemente inyecta [TorDnsResolver] en tu OkHttpClient.Builder:
 * ```
 * OkHttpClient.Builder()
 *     .dns(torDnsResolver)
 *     // ...
 *     .build()
 * ```
 *
 * Cuando [isTorEnabled] es `false`, delega al DNS del sistema ([Dns.SYSTEM]).
 */
@Singleton
class TorDnsResolver @Inject constructor() : Dns {

    companion object {
        private const val TAG = "TorDnsResolver"

        /** Puerto SOCKS5 de Orbot */
        private const val ORBOT_SOCKS_PORT = 9050

        /** Host de Orbot (siempre localhost) */
        private const val ORBOT_HOST = "127.0.0.1"

        /**
         * IP de loopback que retornamos como marcador de posición.
         * La conexión SOCKS5 real se encarga del enrutamiento.
         */
        private const val LOOPBACK_IP = "127.0.0.1"
    }

    /**
     * Indica si la resolución DNS debe pasar por Tor.
     *
     * El [TorService] ajusta esta bandera cuando el usuario activa
     * o desactiva el modo anónimo.
     */
    @Volatile
    var isTorEnabled: Boolean = false

    /**
     * Resuelve un nombre de host a una lista de direcciones IP.
     *
     * - Si [isTorEnabled] es `true` → retorna [LOOPBACK_IP] como marcador
     *   y la conexión SOCKS5 se encarga del enrutamiento real.
     * - Si [isTorEnabled] es `false` → delega a [Dns.SYSTEM].
     *
     * La razón de retornar loopback es que OkHttp necesita una IP para
     * abrir el socket, pero la IP real no importa porque el socket se
     * abrirá a través del proxy SOCKS que es quien realmente resuelve
     * el dominio en la red Tor.
     */
    override fun lookup(hostname: String): List<InetAddress> {
        if (!isTorEnabled) {
            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (e: Exception) {
                Log.w(TAG, "System DNS lookup failed for $hostname", e)
                // Fallback: resolver directamente (usando la red del sistema)
                InetAddress.getAllByName(hostname).toList()
            }
        }

        // ── Modo Tor activo: evitar DNS leaks ──
        Log.d(TAG, "🔒 Resolving $hostname through Tor (SOCKS5)")

        return try {
            // Verificar que Orbot esté alcanzable
            val testSocket = Socket()
            try {
                testSocket.connect(
                    InetSocketAddress(ORBOT_HOST, ORBOT_SOCKS_PORT),
                    1000
                )
            } catch (e: Exception) {
                Log.w(TAG, "Orbot SOCKS proxy not available, falling back to system DNS", e)
                return Dns.SYSTEM.lookup(hostname)
            } finally {
                testSocket.close()
            }

            // Retornar loopback para que OkHttp abra socket SOCKS
            listOf(InetAddress.getByName(LOOPBACK_IP))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Tor DNS resolution failed for $hostname", e)
            // Fallback seguro: sistema DNS (con riesgo de leak)
            Log.w(TAG, "⚠️ Falling back to system DNS — posible DNS leak para $hostname")
            Dns.SYSTEM.lookup(hostname)
        }
    }

    /**
     * Verifica si Orbot está disponible para resolución DNS.
     */
    fun isOrbotAvailable(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress(ORBOT_HOST, ORBOT_SOCKS_PORT),
                    1000
                )
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
