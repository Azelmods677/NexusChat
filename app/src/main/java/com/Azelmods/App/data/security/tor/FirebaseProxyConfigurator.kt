package com.Azelmods.App.data.security.tor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * FirebaseProxyConfigurator – Tor/SOCKS5 proxy utilities for Firebase-adjacent HTTP calls.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  IMPORTANT – Why Firebase SDK cannot be proxied via SOCKS5             │
 * │                                                                         │
 * │  Firebase Realtime Database, Firestore, Authentication and Storage all  │
 * │  use Google's internal gRPC / Firebase-Connection transport, or their   │
 * │  own OkHttp instance that is constructed deep inside native C++ code.   │
 * │  None of these transports expose a public API for injecting a custom    │
 * │  OkHttpClient or a java.net.Proxy.                                      │
 * │                                                                         │
 * │  Previous attempts to reach those transports via reflection             │
 * │  (getDeclaredField("okHttpClient") etc.) always fail silently because   │
 * │  the field names do not exist in any released version of the SDK.       │
 * │                                                                         │
 * │  What DOES work for anonymising Firebase traffic:                       │
 * │    • Run Orbot in "VPN mode" – this tunnels ALL device traffic,         │
 * │      including Firebase, through Tor at the OS level.                   │
 * │    • Replace Firebase calls with your own REST/gRPC client built on     │
 * │      the OkHttpClient returned by [createTorEnabledOkHttpClient].       │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * What this class *does* provide:
 *  - [createTorEnabledOkHttpClient] – an OkHttpClient pre-configured with a
 *    SOCKS5 proxy and [TorDnsResolver]; suitable for manual REST calls.
 *  - [verifyProxyConnectivity]      – checks whether the SOCKS5 proxy can reach
 *    the public internet and whether the exit IP is recognised as a Tor exit node.
 *  - [configureFirebaseProxy]       – logs the limitation clearly, calls
 *    [verifyProxyConnectivity], and returns its result so callers know whether
 *    Tor connectivity is available.
 *  - [removeFirebaseProxy]          – no-op placeholder; kept for API compatibility.
 *
 * **Requirements validated:** 2.1, 2.2, 2.3, 19.2, 19.3, 19.4, 19.6
 */
class FirebaseProxyConfigurator {

    companion object {
        private const val TAG = "FirebaseProxyConfig"

        /** URL used to verify Tor exit-node connectivity. */
        private const val TOR_CHECK_URL = "https://check.torproject.org/api/ip"

        /** Connection / read timeout for all verification requests (ms). */
        private const val VERIFY_TIMEOUT_MS = 15_000
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns an [OkHttpClient] configured to route all requests through the Tor
     * SOCKS5 proxy described by [config].
     *
     * The client also uses [TorDnsResolver] to prevent DNS leaks: every hostname
     * is resolved inside the Tor network rather than by the device's system resolver.
     *
     * Use this client for any manual HTTP/REST calls you want to anonymise.
     * Firebase SDK calls are **not** affected (see class-level documentation).
     *
     * @param config  Active Tor proxy configuration.
     * @return A ready-to-use [OkHttpClient] that tunnels through Tor.
     */
    fun createTorEnabledOkHttpClient(config: TorProxyConfig): OkHttpClient {
        val socks5Proxy = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress(config.socksHost, config.socksPort)
        )

        return OkHttpClient.Builder()
            .proxy(socks5Proxy)
            .dns(TorDnsResolver(config))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
            .also {
                Log.d(TAG, "Created Tor-enabled OkHttpClient (SOCKS5 ${config.socksHost}:${config.socksPort})")
            }
    }

    /**
     * Verifies that the SOCKS5 proxy described by [config] is reachable and that
     * outbound traffic exits through the Tor network.
     *
     * The check connects to [TOR_CHECK_URL] through the SOCKS5 proxy using a plain
     * [HttpURLConnection]; if the response JSON contains `"IsTor":true` the method
     * returns `true`.
     *
     * This approach works reliably because [HttpURLConnection] honours the
     * [java.net.Proxy] passed to [URL.openConnection], unlike the Firebase SDK.
     *
     * @param config  Active Tor proxy configuration.
     * @return `true` if traffic is confirmed to exit through Tor; `false` otherwise.
     */
    suspend fun verifyProxyConnectivity(config: TorProxyConfig): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val socks5Proxy = Proxy(
                    Proxy.Type.SOCKS,
                    InetSocketAddress(config.socksHost, config.socksPort)
                )

                val connection = URL(TOR_CHECK_URL)
                    .openConnection(socks5Proxy) as HttpURLConnection

                connection.connectTimeout = VERIFY_TIMEOUT_MS
                connection.readTimeout    = VERIFY_TIMEOUT_MS
                connection.connect()

                val responseCode = connection.responseCode
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                if (responseCode != 200) {
                    Log.w(TAG, "Tor-check returned HTTP $responseCode")
                    return@withContext false
                }

                val isTor = body.contains("\"IsTor\":true", ignoreCase = true)

                if (isTor) {
                    Log.d(TAG, "Proxy verification OK – traffic exits through Tor")
                } else {
                    Log.w(TAG, "Proxy verification FAILED – traffic is NOT exiting through Tor. Body: $body")
                }

                isTor
            } catch (e: Exception) {
                Log.e(TAG, "Proxy verification exception: ${e.message}", e)
                false
            }
        }

    /**
     * Attempts to "configure Firebase" to use the Tor proxy.
     *
     * **Honest behaviour:** The Firebase SDK does not expose a public API for
     * SOCKS5 proxy injection, and all known reflection-based approaches silently
     * fail because the internal field names do not exist.  To route Firebase
     * traffic through Tor, instruct the user to enable Orbot's VPN mode.
     *
     * This method therefore:
     *  1. Logs the limitation clearly.
     *  2. Calls [verifyProxyConnectivity] to confirm that Tor is reachable.
     *  3. Returns the connectivity result so callers can update the UI.
     *
     * @param config  Active Tor proxy configuration.
     * @return `true` if Tor connectivity is available; `false` otherwise.
     */
    suspend fun configureFirebaseProxy(config: TorProxyConfig): Boolean {
        Log.i(
            TAG,
            "NOTE: Firebase SDK (RTDB / Auth / Storage / Firestore) does NOT support " +
            "SOCKS5 proxy injection at the application level. Its internal gRPC / " +
            "Firebase-Connection transport bypasses OkHttp configuration entirely. " +
            "To route Firebase traffic through Tor, enable Orbot's VPN mode so that " +
            "all device traffic is tunnelled at the OS network layer."
        )
        Log.d(TAG, "Checking whether Tor SOCKS5 proxy is reachable at ${config.socksHost}:${config.socksPort}…")

        val connected = verifyProxyConnectivity(config)

        if (connected) {
            Log.d(
                TAG,
                "Tor proxy is reachable. Manual REST calls via createTorEnabledOkHttpClient() " +
                "will be anonymised. Firebase SDK calls require Orbot VPN mode."
            )
        } else {
            Log.w(
                TAG,
                "Tor proxy is NOT reachable. Anonymous mode may not function correctly. " +
                "Ensure Orbot is installed and running."
            )
        }

        return connected
    }

    /**
     * Cleans up any Tor-related configuration applied by this class.
     *
     * Because this class no longer modifies Firebase internals there is nothing
     * to undo.  The method is kept for API compatibility with callers that may
     * invoke it on disconnect.
     */
    fun removeFirebaseProxy() {
        Log.d(TAG, "removeFirebaseProxy() called – no Firebase internals were modified, nothing to clean up.")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience helper (kept for callers that need a quick connectivity check
    // without the OkHttp wrapper)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Checks if the Tor SOCKS5 port is accepting TCP connections.
     *
     * This is a lightweight pre-check that does not make an outbound HTTP request.
     * A successful result means the proxy is listening; it does not guarantee
     * that Tor is fully bootstrapped or that traffic exits through Tor.
     *
     * @param config  Active Tor proxy configuration.
     * @return `true` if the SOCKS5 port accepts a TCP connection within 5 seconds.
     */
    suspend fun isSocksPortOpen(config: TorProxyConfig): Boolean =
        withContext(Dispatchers.IO) {
            try {
                java.net.Socket().use { socket ->
                    socket.connect(
                        InetSocketAddress(config.socksHost, config.socksPort),
                        5_000
                    )
                    true
                }
            } catch (e: Exception) {
                Log.d(TAG, "SOCKS5 port ${config.socksPort} is not open: ${e.message}")
                false
            }
        }
}
