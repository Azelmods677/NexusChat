package com.Azelmods.App.data.security.tor

/**
 * Represents the current state of the Tor service.
 *
 * States:
 *  - [Disconnected] – Tor is not running or has been cleanly stopped.
 *  - [Connecting]   – Tor is bootstrapping / connecting to the network.
 *  - [Connected]    – Tor is fully connected and ready to route traffic.
 *  - [Error]        – A fatal or recoverable error occurred.
 */
sealed class TorState {

    /**
     * Tor is disconnected and not running.
     * This is the initial state before [Connecting] is requested.
     */
    object Disconnected : TorState()

    /**
     * Tor is actively bootstrapping and connecting to the network.
     *
     * @param progress Bootstrap progress percentage in the range [0, 100].
     * @param message  Human-readable description of the current bootstrap phase.
     */
    data class Connecting(
        val progress: Int,
        val message: String
    ) : TorState()

    /**
     * Tor is actively bootstrapping and connecting to the network.
     * Alias for Connecting to maintain compatibility with TorBrowserScreenNew.
     *
     * @param progress Bootstrap progress percentage in the range [0, 100].
     * @param message  Human-readable description of the current bootstrap phase.
     */
    data class Bootstrapping(
        val progress: Int,
        val message: String = ""
    ) : TorState()

    /**
     * Tor is fully connected and routing traffic through the Tor network.
     *
     * @param circuitInfo Details about the active Tor circuit.
     */
    data class Connected(
        val circuitInfo: TorCircuitInfo
    ) : TorState()

    /**
     * Tor encountered an error that prevented a successful connection or caused a drop.
     *
     * @param message   Human-readable description of what went wrong.
     * @param exception Optional underlying exception for debugging purposes.
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : TorState()
}

/**
 * Describes the three-hop Tor circuit currently in use.
 *
 * @param entryNode  Nickname or fingerprint prefix of the guard (entry) relay.
 * @param middleNode Nickname or fingerprint prefix of the middle relay.
 * @param exitNode   Nickname or fingerprint prefix of the exit relay.
 * @param circuitId  Tor control-port circuit identifier.
 * @param bandwidth  Estimated current throughput in bytes per second (0 if unknown).
 */
data class TorCircuitInfo(
    val entryNode: String,
    val middleNode: String,
    val exitNode: String,
    val circuitId: String,
    val bandwidth: Long,
    /** IP con la que te ve internet (la del nodo de salida), si ya se comprobó. */
    val exitIp: String? = null,
    /** true si check.torproject.org confirmó que el tráfico sale por Tor. */
    val verified: Boolean = false,
    /** true si Orbot está en modo VPN (toda la app, Firebase incluido, va por Tor). */
    val vpnMode: Boolean = false,
    /** Puerto SOCKS real de Orbot. */
    val socksPort: Int = 9050,
    /** Texto de la última comprobación (o por qué no se pudo verificar). */
    val checkMessage: String = ""
)
