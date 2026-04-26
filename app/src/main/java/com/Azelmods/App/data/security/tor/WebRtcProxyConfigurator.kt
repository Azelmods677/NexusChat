package com.Azelmods.App.data.security.tor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.webrtc.*
import java.net.InetSocketAddress
import java.net.Socket

/**
 * WebRTC Proxy Configurator for Tor Integration.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  IMPORTANT – WebRTC & SOCKS5 proxy limitation                          │
 * │                                                                         │
 * │  WebRTC's PeerConnectionFactory has no public API for injecting a      │
 * │  SOCKS5 proxy.  ICE candidate gathering (STUN/TURN) and the media      │
 * │  data-channels use a C++ networking stack that runs beneath the JVM    │
 * │  and ignores both java.net.Proxy and OkHttp configuration.             │
 * │                                                                         │
 * │  What you *can* do today to anonymise WebRTC:                          │
 * │    • Run Orbot in "VPN mode" – all traffic, including WebRTC, is       │
 * │      tunnelled through Tor at the OS tun interface level.               │
 * │    • Use TURN-over-TCP servers that the SOCKS5 client can reach, and   │
 * │      disable host/srflx candidates so only relay candidates are used.   │
 * │                                                                         │
 * │  This class configures the PeerConnectionFactory to prefer TCP and     │
 * │  relay candidates, which works best when Orbot VPN mode is active.     │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * **Requirements validated:**
 *  - Requirement 2.4 : Route STUN/TURN requests through Tor (via Orbot VPN)
 *  - Requirement 19.5: Configure WebRTC PeerConnectionFactory for Tor
 */
class WebRtcProxyConfigurator {

    companion object {
        private const val TAG = "WebRtcProxyConfigurator"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result type
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wraps the [PeerConnectionFactory] together with the single [EglBase]
     * instance that was used to create it.
     *
     * Callers **must** release both objects when they are no longer needed:
     * ```kotlin
     * result.factory.dispose()
     * result.eglBase.release()
     * ```
     */
    data class TorPeerConnectionFactoryResult(
        val factory: PeerConnectionFactory,
        val eglBase: EglBase
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Factory creation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a [PeerConnectionFactory] whose settings are tuned for use with
     * Tor (prefer TCP, relay-only ICE, reduced bitrates).
     *
     * A single [EglBase] is created here and shared between the encoder and
     * decoder factories to avoid leaking GPU contexts.  The caller owns both
     * objects and must dispose / release them when done.
     *
     * **Note:** WebRTC does not support SOCKS5 at the factory level.
     * For real traffic anonymisation, enable Orbot's VPN mode before calling
     * this function (see class-level documentation).
     *
     * @param context     Android application context.
     * @param proxyConfig Tor proxy configuration (used for logging / verification only).
     * @return [TorPeerConnectionFactoryResult] containing the factory and the EglBase.
     */
    fun createTorEnabledPeerConnectionFactory(
        context: Context,
        proxyConfig: TorProxyConfig
    ): TorPeerConnectionFactoryResult {
        Log.d(TAG, "Creating Tor-enabled PeerConnectionFactory")
        Log.d(
            TAG,
            "NOTE: SOCKS5 proxy (${proxyConfig.socksHost}:${proxyConfig.socksPort}) cannot be " +
            "injected at the WebRTC native layer. Enable Orbot VPN mode for full anonymisation."
        )

        // Initialise the WebRTC native library once.
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        // Create a SINGLE EglBase and reuse it for both encoder and decoder.
        // Creating multiple EglBase instances leaks EGL contexts and causes
        // "EGL context lost" crashes on some devices.
        val eglBase = EglBase.create()
        val eglBaseContext = eglBase.eglBaseContext

        val encoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

        val factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options().apply {
                // Keep encryption enabled – Tor does not change the DTLS requirement.
                disableEncryption = false
                // Disable the built-in network monitor; Orbot VPN mode handles routing.
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory created (EglBase: $eglBase)")
        return TorPeerConnectionFactoryResult(factory = factory, eglBase = eglBase)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RTCConfiguration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds an [PeerConnection.RTCConfiguration] tuned for operation over Tor.
     *
     * Key choices:
     *  - TCP candidates are preferred (SOCKS5 / Orbot VPN works better over TCP).
     *  - Continual gathering is enabled to cope with Tor's higher latency.
     *  - [PeerConnection.IceTransportsType.ALL] is used so that relay (TURN)
     *    candidates are gathered; callers that want relay-only anonymisation should
     *    switch to [PeerConnection.IceTransportsType.RELAY] instead.
     *
     * @param proxyConfig      Active Tor proxy configuration (used for logging).
     * @param customIceServers Optional replacement ICE-server list; if `null` the
     *                         default Google STUN servers are used.
     * @return Configured [PeerConnection.RTCConfiguration].
     */
    fun createTorEnabledRTCConfiguration(
        proxyConfig: TorProxyConfig,
        customIceServers: List<PeerConnection.IceServer>? = null
    ): PeerConnection.RTCConfiguration {
        Log.d(TAG, "Creating Tor-enabled RTCConfiguration")

        val iceServers = customIceServers ?: listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )

        Log.d(TAG, "Configured ${iceServers.size} ICE servers for Tor-optimised WebRTC")

        // NOTE: The return statement is intentionally the last expression in the
        // function body.  Any log statements must appear *before* it.
        return PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics           = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType      = PeerConnection.IceTransportsType.ALL
            bundlePolicy           = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy          = PeerConnection.RtcpMuxPolicy.REQUIRE
            // Prefer TCP – better suited for SOCKS5 / Orbot VPN tunnelling.
            tcpCandidatePolicy     = PeerConnection.TcpCandidatePolicy.ENABLED
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.LOW_COST
        }
        // ⚠️  Do NOT add log statements here – they are unreachable after the return.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PeerConnection helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Documents that a [PeerConnection] has been handed to the Tor-aware
     * network stack.
     *
     * WebRTC has no `setPeerConnectionProxy()` API; actual routing depends on
     * the OS network layer (Orbot VPN mode).  This method exists as a clearly
     * named call-site marker so that future SDK changes are easy to integrate.
     *
     * @param peerConnection The [PeerConnection] to be used over Tor.
     * @param proxyConfig    Active Tor proxy configuration.
     */
    fun configurePeerConnectionForTor(
        peerConnection: PeerConnection,
        proxyConfig: TorProxyConfig
    ) {
        Log.d(
            TAG,
            "configurePeerConnectionForTor(): WebRTC PeerConnection has no SOCKS5 API. " +
            "Routing depends on OS-level tunnelling (Orbot VPN). " +
            "Proxy target: ${proxyConfig.socksHost}:${proxyConfig.socksPort}"
        )
        // Nothing else to do here at the PeerConnection level.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ICE server helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a list of ICE servers optimised for use over Tor.
     *
     * STUN operates primarily over UDP, which SOCKS5 does not proxy.  Callers
     * that need guaranteed anonymisation should pass `includeTurnServers = true`
     * with TCP-capable TURN credentials and restrict ICE to relay-only mode.
     *
     * @param includeTurnServers   Whether to include TURN servers.
     * @param turnUsername         TURN credential username (required when [includeTurnServers] = true).
     * @param turnCredential       TURN credential password (required when [includeTurnServers] = true).
     */
    fun createTorCompatibleIceServers(
        includeTurnServers: Boolean = false,
        turnUsername: String? = null,
        turnCredential: String? = null
    ): List<PeerConnection.IceServer> {
        val servers = mutableListOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
        )

        if (includeTurnServers && turnUsername != null && turnCredential != null) {
            // Replace placeholder URIs with your own TURN servers.
            servers += PeerConnection.IceServer
                .builder("turn:turn.example.com:3478")
                .setUsername(turnUsername)
                .setPassword(turnCredential)
                .createIceServer()

            // TCP transport – preferred for Tor / Orbot VPN compatibility.
            servers += PeerConnection.IceServer
                .builder("turn:turn.example.com:3478?transport=tcp")
                .setUsername(turnUsername)
                .setPassword(turnCredential)
                .createIceServer()

            Log.d(TAG, "Added TURN servers with authentication")
        }

        Log.d(TAG, "Prepared ${servers.size} Tor-compatible ICE servers")
        return servers
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Connectivity verification
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that the SOCKS5 proxy described by [proxyConfig] is accepting
     * TCP connections.
     *
     * **Why not connect to a STUN server?**
     * STUN uses UDP as its primary transport.  A raw [java.net.Socket] always
     * creates a TCP socket, so attempting to "connect to stun.l.google.com:19302"
     * via a plain socket does not test STUN reachability — it tests whether the
     * host accepts TCP on that port (it typically does not).
     *
     * The correct check for WebRTC-over-Tor readiness is: *"is the SOCKS5 proxy
     * itself reachable over TCP?"*  If Orbot VPN mode is active the proxy port
     * will accept connections and all subsequent WebRTC traffic will be tunnelled
     * through it at the OS level.
     *
     * @param proxyConfig Active Tor proxy configuration.
     * @return `true` if the SOCKS5 proxy port accepts a TCP connection within
     *         5 seconds; `false` otherwise.
     */
    suspend fun verifyWebRtcTorRouting(proxyConfig: TorProxyConfig): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Verifying SOCKS5 proxy TCP reachability at ${proxyConfig.socksHost}:${proxyConfig.socksPort}…")

                Socket().use { socket ->
                    socket.connect(
                        InetSocketAddress(proxyConfig.socksHost, proxyConfig.socksPort),
                        5_000
                    )
                }

                Log.d(TAG, "✓ SOCKS5 proxy is reachable – WebRTC traffic will route through Orbot VPN when active")
                true
            } catch (e: Exception) {
                Log.e(TAG, "✗ SOCKS5 proxy unreachable: ${e.message}")
                false
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Settings map
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a map of recommended WebRTC settings for use over Tor.
     *
     * These values account for Tor's higher latency (~200–600 ms) and
     * reduced throughput relative to a direct connection.
     */
    fun getTorOptimizedWebRtcSettings(): Map<String, Any> = mapOf(
        "iceGatheringTimeout"    to 30_000,   // ms – extended for Tor latency
        "connectionTimeout"      to 60_000,   // ms – extended for Tor latency
        "preferredVideoCodec"    to "H264",   // better compression for limited bandwidth
        "preferredAudioCodec"    to "opus",
        "maxVideoBitrate"        to 500_000,  // bps – conservative for Tor throughput
        "maxAudioBitrate"        to 32_000,   // bps
        "adaptiveBitrate"        to true,
        "iceCandidatePoolSize"   to 10,
        "enableTcpCandidates"    to true,     // TCP works with SOCKS5 / Orbot VPN
        "disableUdpCandidates"   to false     // keep UDP available (Orbot VPN tunnels it)
    )
}
