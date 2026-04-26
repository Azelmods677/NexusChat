package com.Azelmods.App.data.security.tor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Tor circuits via the Tor control port (default: 9051).
 *
 * This class provides a high-level API over the raw Tor control protocol:
 *  - Requesting a new circuit / identity via `SIGNAL NEWNYM`.
 *  - Querying the active circuit path (entry / middle / exit nodes).
 *  - Measuring approximate traffic throughput.
 *  - Checking whether the control port is reachable.
 *  - Maintaining a rolling history of the last [MAX_CIRCUIT_HISTORY] circuits.
 *
 * ## Control-port authentication
 * Every public method that communicates with Tor opens its **own** short-lived
 * TCP connection, authenticates with `AUTHENTICATE ""` (no cookie / password –
 * Orbot's default), issues the command, reads the response, and then sends
 * `QUIT` before closing.  This avoids shared mutable state from a persistent
 * socket while keeping the implementation straightforward.
 *
 * ## Availability
 * Orbot does not always expose the control port.  All methods that require it
 * handle `ConnectException` / `IOException` gracefully and return safe default
 * values (`false`, `null`, `0L`) rather than propagating the exception to the
 * caller.  Use [isControlPortAvailable] to probe before calling the others.
 *
 * @param config             Tor proxy configuration – used for [TorProxyConfig.controlPort]
 *                           and [TorProxyConfig.socksHost].
 * @param torServiceManager  Application-scoped manager whose [TorServiceManager.getTorState]
 *                           is checked where a connected state is a prerequisite.
 */
@Singleton
class TorCircuitManager @Inject constructor(
    private val config: TorProxyConfig,
    private val torServiceManager: TorServiceManager
) {

    companion object {
        private const val TAG = "TorCircuitManager"

        /** Maximum number of circuit snapshots kept in [circuitHistory]. */
        private const val MAX_CIRCUIT_HISTORY = 10

        /** Milliseconds to wait for a single control-port I/O operation. */
        private const val IO_TIMEOUT_MS = 5_000L

        /** Milliseconds allowed for the TCP connect to the control port. */
        private const val CONNECT_TIMEOUT_MS = 3_000

        /** The control address is always localhost regardless of socksHost. */
        private const val CONTROL_HOST = "127.0.0.1"
    }

    // ── Circuit history ────────────────────────────────────────────────────────

    /**
     * Bounded deque holding the most recently observed [TorCircuitInfo] objects.
     * Access is always from a coroutine running on [Dispatchers.IO], so no
     * additional synchronisation is required beyond the single-threaded nature
     * of each suspend call site.
     */
    private val circuitHistory = ArrayDeque<TorCircuitInfo>(MAX_CIRCUIT_HISTORY + 1)

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Sends an arbitrary command to the Tor control port and returns the raw
     * multi-line response as a single trimmed [String].
     *
     * The function:
     * 1. Opens a TCP socket to `127.0.0.1:[config.controlPort]`.
     * 2. Authenticates with `AUTHENTICATE ""`.
     * 3. Writes [command] followed by CRLF.
     * 4. Reads response lines until a final `250 …` or error line is received.
     * 5. Sends `QUIT` and closes the socket.
     *
     * @param command The raw Tor control-protocol command string (e.g.
     *                `"SIGNAL NEWNYM"` or `"GETINFO circuit-status"`).
     * @return The server response lines joined with `\n`.
     * @throws IOException if the socket cannot connect, authentication fails,
     *         or the server returns a `4xx` / `5xx` error code.
     */
    suspend fun sendControlCommand(command: String): String = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(
                InetSocketAddress(CONTROL_HOST, config.controlPort),
                CONNECT_TIMEOUT_MS
            )
            socket.soTimeout = IO_TIMEOUT_MS.toInt()

            val writer = PrintWriter(socket.getOutputStream(), /* autoFlush= */ true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // ── Authenticate ──────────────────────────────────────────────────
            writer.println("AUTHENTICATE \"\"")
            val authLine = withTimeoutOrNull(IO_TIMEOUT_MS) { reader.readLine() }
                ?: throw IOException("Timeout waiting for AUTHENTICATE response")

            if (!authLine.startsWith("250")) {
                throw IOException("Control port authentication failed: $authLine")
            }

            // ── Send command ──────────────────────────────────────────────────
            writer.println(command)

            // ── Collect response lines ────────────────────────────────────────
            // The Tor control protocol terminates multi-line replies with a line
            // that starts "250 " (note the space, not a dash).  Single-line
            // replies are also "250 …".  Error replies start with 4xx or 5xx.
            val responseBuffer = StringBuilder()
            while (true) {
                val line = withTimeoutOrNull(IO_TIMEOUT_MS) { reader.readLine() }
                    ?: break   // timeout or EOF – stop reading

                responseBuffer.appendLine(line)

                when {
                    // Definitive OK – end of reply
                    line == "250 OK" || (line.startsWith("250 ") && !line.startsWith("250-")) -> break
                    // Error from Tor
                    line.length >= 3 && (line[0] == '4' || line[0] == '5') && line[3] == ' ' -> {
                        throw IOException("Tor control error for command '$command': $line")
                    }
                }
            }

            // ── Close gracefully ──────────────────────────────────────────────
            writer.println("QUIT")

            responseBuffer.toString().trimEnd()

        } finally {
            runCatching { socket?.close() }
        }
    }

    /**
     * Requests a new Tor identity by sending `SIGNAL NEWNYM` to the control port.
     *
     * After `NEWNYM` Tor will use different circuits for subsequent connections.
     * Note that Tor enforces a 10-second rate limit between `NEWNYM` signals;
     * if called too quickly the control port will return a `451` error, which
     * this function converts into a `false` return value rather than an exception.
     *
     * @return `true` if the signal was accepted (`250 OK`), `false` if the
     *         control port is unavailable, returned an error, or the rate limit
     *         was hit.
     */
    suspend fun requestNewCircuit(): Boolean {
        return try {
            val response = sendControlCommand("SIGNAL NEWNYM")
            val accepted = response.contains("250")
            if (accepted) {
                Log.d(TAG, "NEWNYM accepted – Tor will use new circuits going forward.")
            } else {
                Log.w(TAG, "NEWNYM response did not contain '250': $response")
            }
            accepted
        } catch (ex: Exception) {
            Log.w(
                TAG,
                "requestNewCircuit() failed – control port may not be available: ${ex.message}"
            )
            false
        }
    }

    /**
     * Queries the active Tor circuit via `GETINFO circuit-status` and returns
     * the first `BUILT` circuit found in the response.
     *
     * The circuit is also appended to [circuitHistory] (capped at
     * [MAX_CIRCUIT_HISTORY] entries).
     *
     * @return A [TorCircuitInfo] for the first built circuit, or `null` if no
     *         built circuit exists or the control port is unreachable.
     */
    suspend fun getCurrentCircuit(): TorCircuitInfo? {
        return try {
            val response = sendControlCommand("GETINFO circuit-status")
            Log.d(TAG, "circuit-status response:\n$response")
            val circuit = parseCircuitStatusResponse(response)
            if (circuit != null) {
                addToHistory(circuit)
                Log.d(
                    TAG,
                    "Active circuit #${circuit.circuitId}: " +
                        "${circuit.entryNode} → ${circuit.middleNode} → ${circuit.exitNode}"
                )
            } else {
                Log.d(TAG, "No BUILT circuit found in circuit-status response.")
            }
            circuit
        } catch (ex: Exception) {
            Log.w(TAG, "getCurrentCircuit() failed: ${ex.message}")
            null
        }
    }

    /**
     * Estimates the cumulative traffic throughput by querying
     * `GETINFO traffic/read` and `GETINFO traffic/written`.
     *
     * The two values returned by Tor represent the **total** bytes transferred
     * since the Tor process started, not a per-second rate.  Callers that need
     * a true bandwidth figure should call this method twice with a known
     * interval between calls and compute the delta themselves.
     *
     * @return The sum of bytes read and written by Tor since startup, or `0`
     *         if the control port is unavailable or returns unexpected data.
     */
    suspend fun getBandwidth(): Long {
        return try {
            val readResponse    = sendControlCommand("GETINFO traffic/read")
            val writtenResponse = sendControlCommand("GETINFO traffic/written")

            val readBytes    = parseTrafficValue(readResponse,    "traffic/read")
            val writtenBytes = parseTrafficValue(writtenResponse, "traffic/written")

            val total = readBytes + writtenBytes
            Log.d(TAG, "Cumulative traffic – read: $readBytes B, written: $writtenBytes B, total: $total B")
            total
        } catch (ex: Exception) {
            Log.w(TAG, "getBandwidth() failed: ${ex.message}")
            0L
        }
    }

    /**
     * Attempts to open a TCP connection to the control port and authenticate.
     *
     * This is a lightweight probe that does **not** send any actual command –
     * it just verifies that the port is open and responds to `AUTHENTICATE ""`.
     *
     * @return `true` if the control port is reachable and authentication
     *         succeeds within [CONNECT_TIMEOUT_MS] + [IO_TIMEOUT_MS], `false`
     *         otherwise.
     */
    suspend fun isControlPortAvailable(): Boolean = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(
                InetSocketAddress(CONTROL_HOST, config.controlPort),
                CONNECT_TIMEOUT_MS
            )
            socket.soTimeout = IO_TIMEOUT_MS.toInt()

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.println("AUTHENTICATE \"\"")
            val response = withTimeoutOrNull(IO_TIMEOUT_MS) { reader.readLine() }

            writer.println("QUIT")

            val available = response?.startsWith("250") == true
            Log.d(TAG, "Control port probe: available=$available (response='$response')")
            available

        } catch (ex: Exception) {
            Log.d(TAG, "Control port probe failed: ${ex.message}")
            false
        } finally {
            runCatching { socket?.close() }
        }
    }

    /**
     * Returns a snapshot of the most recently observed Tor circuits, oldest
     * first, capped at [MAX_CIRCUIT_HISTORY] entries.
     *
     * The list is a defensive copy – mutations to the returned list do not
     * affect the internal history deque.
     */
    fun getCircuitHistory(): List<TorCircuitInfo> = circuitHistory.toList()

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Appends [circuit] to [circuitHistory], evicting the oldest entry first
     * when the deque is at capacity.
     */
    private fun addToHistory(circuit: TorCircuitInfo) {
        if (circuitHistory.size >= MAX_CIRCUIT_HISTORY) {
            circuitHistory.removeFirst()
        }
        circuitHistory.addLast(circuit)
    }

    /**
     * Parses the raw `GETINFO circuit-status` response and returns the first
     * circuit whose status is `BUILT`.
     *
     * ### Response format (abbreviated)
     * ```
     * 250-circuit-status=
     * 250-1 BUILT $AA~GuardNode,$BB~MiddleNode,$CC~ExitNode BUILD_FLAGS=...
     * 250-2 EXTENDING $DD~GuardNode,...
     * 250 OK
     * ```
     * Each circuit line (after the header) contains:
     *  - Circuit ID (integer)
     *  - Status (`BUILT`, `EXTENDING`, `FAILED`, …)
     *  - Path (comma-separated `$fingerprint~nickname` tokens)
     *  - Optional key=value attributes
     *
     * @param response The raw multi-line string returned by [sendControlCommand].
     * @return The first [TorCircuitInfo] for a `BUILT` circuit, or `null`.
     */
    private fun parseCircuitStatusResponse(response: String): TorCircuitInfo? {
        for (rawLine in response.lines()) {
            // Strip the leading "250-" or "250 " reply code and optional whitespace.
            val line = rawLine
                .removePrefix("250-")
                .removePrefix("250 ")
                .trim()

            // Skip the "circuit-status=" header line and blank lines.
            if (line.isEmpty() || line == "circuit-status=" || line == "OK") continue

            // A data line looks like: "1 BUILT $fp1~name1,$fp2~name2,$fp3~name3 key=val …"
            val parts = line.split(" ")
            if (parts.size < 3) continue

            val circuitId = parts[0]
            val status    = parts[1]

            if (status != "BUILT") continue   // only care about established circuits

            val pathToken = parts[2]           // "$fp1~name1,$fp2~name2,$fp3~name3"
            val nodes     = pathToken.split(",")

            // Extract a human-readable label per node:
            //   "$fingerprint~nickname" → "nickname"
            //   "$fingerprint"          → first 8 hex chars + "…"
            val nodeLabels = nodes.map { node ->
                when {
                    node.contains('~') -> node.substringAfter('~')
                    node.startsWith('$') -> node.drop(1).take(8) + "…"
                    else -> node.take(12)
                }
            }

            return TorCircuitInfo(
                circuitId  = circuitId,
                entryNode  = nodeLabels.getOrElse(0) { "Unknown" },
                middleNode = nodeLabels.getOrElse(1) { "Unknown" },
                exitNode   = nodeLabels.getOrElse(2) { "Unknown" },
                bandwidth  = 0L   // populated separately via getBandwidth()
            )
        }
        return null
    }

    /**
     * Extracts a long integer value from a `GETINFO traffic/read` or
     * `GETINFO traffic/written` response.
     *
     * Expected formats:
     * ```
     * 250-traffic/read=123456
     * 250 OK
     * ```
     * or the single-line variant:
     * ```
     * 250 traffic/read=123456
     * ```
     *
     * @param response The raw response string from [sendControlCommand].
     * @param key      The GETINFO key being parsed (e.g. `"traffic/read"`).
     * @return The parsed value, or `0` if the key is not found or the value
     *         cannot be converted to [Long].
     */
    private fun parseTrafficValue(response: String, key: String): Long {
        for (line in response.lines()) {
            // The key appears in lines like "250-traffic/read=12345" or "250 traffic/read=12345"
            val marker = "$key="
            if (line.contains(marker)) {
                val raw = line.substringAfter(marker).trim()
                return raw.toLongOrNull() ?: run {
                    Log.w(TAG, "Could not parse traffic value from line: '$line'")
                    0L
                }
            }
        }
        Log.w(TAG, "Key '$key' not found in GETINFO response: $response")
        return 0L
    }
}
