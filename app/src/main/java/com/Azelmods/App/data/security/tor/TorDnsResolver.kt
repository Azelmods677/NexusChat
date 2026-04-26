package com.Azelmods.App.data.security.tor

import android.util.Log
import okhttp3.Dns
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Custom DNS resolver that routes all DNS queries through the Tor network using
 * the SOCKS5 RESOLVE command (CMD = 0xF0), a Tor-specific extension that performs
 * DNS resolution server-side without establishing any TCP connection to the target host.
 *
 * ## Why RESOLVE instead of connecting to port 80?
 *
 * The naïve approach of opening a SOCKS5 connection to `hostname:80` causes a real
 * TCP connection to be established at the exit node, which:
 *  - Wastes a Tor circuit for every DNS lookup.
 *  - Triggers SYN packets to the target server even when only the IP is needed.
 *  - Exposes the caller to unexpected side-effects (server logs, rate limits, etc.).
 *
 * The RESOLVE command (CMD = 0xF0) asks the Tor SOCKS5 server to resolve the
 * hostname and return the resulting IP address in the reply, with **no connection**
 * made to the destination.
 *
 * ## DNS leak prevention
 *
 * [lookup] does **not** fall back to the system DNS resolver.  If the Tor proxy is
 * unreachable or the resolution fails, an [IOException] is thrown immediately.
 * Callers that catch [Dns] failures should surface the error rather than silently
 * retrying through the system resolver.
 *
 * @param config Tor proxy configuration supplying [TorProxyConfig.socksHost] and
 *               [TorProxyConfig.socksPort].
 */
class TorDnsResolver(private val config: TorProxyConfig) : Dns {

    companion object {
        private const val TAG = "TorDnsResolver"

        /** Timeout in milliseconds for socket connect and individual read operations. */
        private const val DNS_TIMEOUT_MS = 10_000

        // SOCKS5 constants
        private const val SOCKS_VERSION: Byte = 0x05
        private const val AUTH_NO_AUTH: Byte = 0x00
        private const val CMD_RESOLVE: Byte = 0xF0.toByte()   // Tor extension
        private const val RSV: Byte = 0x00
        private const val ATYP_DOMAIN: Byte = 0x03
        private const val ATYP_IPV4: Byte = 0x01
        private const val ATYP_IPV6: Byte = 0x04
        private const val REP_SUCCEEDED: Byte = 0x00
    }

    /**
     * Resolves [hostname] to a list of [InetAddress] objects by routing the DNS
     * query through the configured Tor SOCKS5 proxy.
     *
     * **No fallback** to the system DNS resolver is performed.  Any failure throws
     * an [IOException] so that the caller is aware the resolution did not succeed
     * through Tor, preventing unintentional DNS leaks.
     *
     * @param hostname The hostname to resolve (e.g. `"example.com"`).
     * @return A list containing the resolved [InetAddress].
     * @throws IOException if the Tor proxy is unreachable, authentication fails,
     *         the RESOLVE command is rejected, or an unexpected response is received.
     */
    override fun lookup(hostname: String): List<InetAddress> {
        Log.d(TAG, "Resolving '$hostname' through Tor RESOLVE command")
        return try {
            lookupThroughTor(hostname)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "DNS resolution through Tor RESOLVE failed for '$hostname': ${e.message}. " +
                    "NOT falling back to system DNS to prevent DNS leak.",
                e
            )
            // Re-throw so OkHttp / callers know the lookup truly failed.
            throw IOException("Tor DNS RESOLVE failed for '$hostname': ${e.message}", e)
        }
    }

    /**
     * Performs the actual SOCKS5 RESOLVE handshake.
     *
     * ### Protocol flow
     * ```
     * Client → Proxy : [VER=5, NMETHODS=1, METHOD=NO_AUTH]
     * Proxy  → Client: [VER=5, METHOD=NO_AUTH]
     * Client → Proxy : [VER=5, CMD=0xF0, RSV=0, ATYP=DOMAIN, LEN, hostname, PORT_HI, PORT_LO]
     * Proxy  → Client: [VER=5, REP, RSV, ATYP, BND.ADDR (4 or 16 bytes), BND.PORT (2 bytes)]
     * ```
     *
     * @param hostname The hostname whose IP address is to be resolved via Tor.
     * @return A single-element list containing the resolved [InetAddress].
     * @throws IOException on any protocol or network error.
     */
    private fun lookupThroughTor(hostname: String): List<InetAddress> {
        val socket = Socket()
        socket.connect(
            InetSocketAddress(config.socksHost, config.socksPort),
            DNS_TIMEOUT_MS
        )
        socket.soTimeout = DNS_TIMEOUT_MS

        return try {
            val out = socket.getOutputStream()
            val din = DataInputStream(socket.getInputStream())

            // ── Step 1: SOCKS5 greeting ───────────────────────────────────────
            // [VER=5 | NMETHODS=1 | METHOD=0x00 (NO AUTH)]
            out.write(byteArrayOf(SOCKS_VERSION, 0x01, AUTH_NO_AUTH))
            out.flush()

            // ── Step 2: Verify server selected NO_AUTH ────────────────────────
            val serverVer = din.readByte()
            val selectedMethod = din.readByte()
            if (serverVer != SOCKS_VERSION || selectedMethod != AUTH_NO_AUTH) {
                throw IOException(
                    "SOCKS5 method negotiation failed: " +
                        "VER=0x${serverVer.toUByte().toString(16).uppercase()}, " +
                        "METHOD=0x${selectedMethod.toUByte().toString(16).uppercase()}"
                )
            }

            // ── Step 3: Send RESOLVE request ──────────────────────────────────
            // [VER=5 | CMD=0xF0 | RSV=0 | ATYP=3(DOMAIN) | LEN | hostname | PORT(2 bytes=0)]
            val hostnameBytes = hostname.toByteArray(Charsets.US_ASCII)
            if (hostnameBytes.size > 255) {
                throw IOException("Hostname too long for SOCKS5 RESOLVE: ${hostnameBytes.size} bytes")
            }

            val requestBuf = ByteArrayOutputStream(7 + hostnameBytes.size)
            requestBuf.write(byteArrayOf(
                SOCKS_VERSION,
                CMD_RESOLVE,
                RSV,
                ATYP_DOMAIN,
                hostnameBytes.size.toByte()
            ))
            requestBuf.write(hostnameBytes)
            // DST.PORT – not meaningful for RESOLVE but required by the framing
            requestBuf.write(byteArrayOf(0x00, 0x00))
            out.write(requestBuf.toByteArray())
            out.flush()

            // ── Step 4: Read RESOLVE response header ──────────────────────────
            // [VER | REP | RSV | ATYP]
            val rVer = din.readByte()
            val rRep = din.readByte()
            @Suppress("UNUSED_VARIABLE")
            val rRsv = din.readByte()   // reserved – ignore
            val rAtyp = din.readByte()

            if (rVer != SOCKS_VERSION) {
                throw IOException(
                    "Unexpected SOCKS version in RESOLVE response: " +
                        "0x${rVer.toUByte().toString(16).uppercase()}"
                )
            }
            if (rRep != REP_SUCCEEDED) {
                throw IOException(
                    "SOCKS5 RESOLVE command rejected by Tor proxy: " +
                        "REP=0x${rRep.toUByte().toString(16).uppercase()}"
                )
            }

            // ── Step 5: Parse bound address ───────────────────────────────────
            val resolvedAddress: InetAddress = when (rAtyp) {
                ATYP_IPV4 -> {
                    // 4-byte IPv4 address followed by 2-byte port
                    val ipBytes = ByteArray(4)
                    din.readFully(ipBytes)
                    din.readShort() // BND.PORT – discard
                    InetAddress.getByAddress(hostname, ipBytes)
                }
                ATYP_IPV6 -> {
                    // 16-byte IPv6 address followed by 2-byte port
                    val ipBytes = ByteArray(16)
                    din.readFully(ipBytes)
                    din.readShort() // BND.PORT – discard
                    InetAddress.getByAddress(hostname, ipBytes)
                }
                else -> throw IOException(
                    "Unexpected ATYP in RESOLVE response: " +
                        "0x${rAtyp.toUByte().toString(16).uppercase()}"
                )
            }

            Log.d(TAG, "Tor RESOLVE: '$hostname' → ${resolvedAddress.hostAddress}")
            listOf(resolvedAddress)

        } finally {
            try {
                socket.close()
            } catch (closeEx: Exception) {
                Log.w(TAG, "Error closing RESOLVE socket: ${closeEx.message}")
            }
        }
    }
}
