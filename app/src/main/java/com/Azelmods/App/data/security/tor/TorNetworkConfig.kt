package com.Azelmods.App.data.security.tor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Utility class for configuring network clients to use Tor SOCKS5 proxy
 * with DNS leak prevention.
 *
 * This class provides helper methods to configure OkHttp clients and other
 * network components to route all traffic through Tor, including DNS queries.
 */
object TorNetworkConfig {

    /**
     * Configures an OkHttpClient to route all traffic through Tor SOCKS5 proxy
     * with DNS leak prevention.
     *
     * **Example Usage:**
     * ```kotlin
     * val torConfig = TorProxyConfig(
     *     dataDirectory = File(context.filesDir, "tor_data"),
     *     geoipFile     = File(context.filesDir, "geoip"),
     *     geoip6File    = File(context.filesDir, "geoip6"),
     *     torrcFile     = File(context.filesDir, "torrc")
     * )
     *
     * val okHttpClient = TorNetworkConfig.createTorEnabledOkHttpClient(torConfig)
     *
     * // Use this client for all network requests
     * val request = Request.Builder()
     *     .url("https://check.torproject.org/api/ip")
     *     .build()
     *
     * val response = okHttpClient.newCall(request).execute()
     * ```
     *
     * @param config               Tor proxy configuration.
     * @param connectTimeoutSeconds Connection timeout in seconds (default: 30).
     * @param readTimeoutSeconds    Read timeout in seconds (default: 30).
     * @param writeTimeoutSeconds   Write timeout in seconds (default: 30).
     * @return Configured [OkHttpClient] that routes all traffic through Tor.
     */
    fun createTorEnabledOkHttpClient(
        config: TorProxyConfig,
        connectTimeoutSeconds: Long = 30,
        readTimeoutSeconds: Long = 30,
        writeTimeoutSeconds: Long = 30
    ): OkHttpClient {
        // SOCKS5 proxy pointing at the Tor / Orbot port
        val socksProxy = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress(config.socksHost, config.socksPort)
        )

        // TorDnsResolver routes every DNS lookup through the SOCKS5 RESOLVE
        // command (CMD = 0xF0) instead of the system resolver, preventing DNS leaks.
        val torDnsResolver = TorDnsResolver(config)

        return OkHttpClient.Builder()
            .proxy(socksProxy)
            .dns(torDnsResolver)
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Verifies that [client] is actually routing traffic through the Tor network
     * by querying the Tor Project's IP-check endpoint.
     *
     * The [OkHttpClient.newCall] `execute()` call is blocking I/O; this function
     * is therefore a `suspend fun` and the blocking work is dispatched onto
     * [Dispatchers.IO] via [withContext] to avoid blocking the calling coroutine's
     * thread (which may be the main thread or a limited coroutine dispatcher).
     *
     * @param client The [OkHttpClient] to verify.
     * @return `true` if the response from check.torproject.org confirms that the
     *         connection is going through Tor (`"IsTor": true`), `false` otherwise.
     */
    suspend fun verifyTorConnection(client: OkHttpClient): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://check.torproject.org/api/ip")
                .build()

            // execute() is blocking – must not be called on a coroutine dispatcher
            // that restricts blocking (e.g. Dispatchers.Main).
            val responseBody = withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                response.use { it.body?.string() ?: "" }
            }

            // The JSON response looks like: {"IsTor": true, "IP": "185.x.x.x"}
            responseBody.contains("\"IsTor\":true", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
