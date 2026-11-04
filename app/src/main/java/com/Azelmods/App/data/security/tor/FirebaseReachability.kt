package com.Azelmods.App.data.security.tor

import com.google.firebase.FirebaseApp
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Comprueba si los servidores de Firebase aceptan una salida concreta de Tor.
 *
 * Google rechaza la mayoría de nodos de salida: medido en esta app, 8 de cada 10 responden
 * 403 a `securetoken.googleapis.com` o cortan la conexión con Realtime Database. Sin renovar
 * ese token la sesión deja de valer y la app parece vacía, aunque Tor funcione.
 *
 * Cada usuario SOCKS distinto obliga a Tor a usar un circuito distinto, y eso permite probar
 * varias salidas a la vez. Las peticiones no llevan credenciales ni datos de la cuenta: solo
 * importa el código de respuesta.
 */
object FirebaseReachability {

    private const val TOKEN_HOST = "securetoken.googleapis.com"
    private const val TOKEN_PATH = "/v1/token"
    private const val DATABASE_PATH = "/.json?shallow=true"
    private const val TIMEOUT_MS = 25_000
    private const val BLOCKED = 403

    fun databaseHost(): String? = runCatching {
        URI(FirebaseApp.getInstance().options.databaseUrl.orEmpty()).host
    }.getOrNull()

    /** `true` si desde este circuito se llega tanto al token de sesión como a la base de datos. */
    fun isReachable(socksPort: Int, isolation: String, databaseHost: String?): Boolean {
        val token = httpStatus(socksPort, isolation, TOKEN_HOST, TOKEN_PATH) ?: return false
        if (token == BLOCKED) return false
        if (databaseHost == null) return true
        val database = httpStatus(socksPort, isolation, databaseHost, DATABASE_PATH) ?: return false
        return database != BLOCKED
    }

    /** Código HTTP de una petición por el circuito indicado, o null si no se pudo completar. */
    private fun httpStatus(socksPort: Int, isolation: String, host: String, path: String): Int? = try {
        val raw = Socket()
        try {
            raw.soTimeout = TIMEOUT_MS
            raw.connect(InetSocketAddress("127.0.0.1", socksPort), TIMEOUT_MS)
            openSocksTunnel(raw, isolation, host)
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            (factory.createSocket(raw, host, 443, true) as SSLSocket).use { tls ->
                tls.startHandshake()
                if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, tls.session)) {
                    throw IOException("Certificado que no es de $host")
                }
                tls.outputStream.write(
                    "GET $path HTTP/1.1\r\nHost: $host\r\nUser-Agent: Mozilla/5.0\r\nConnection: close\r\n\r\n"
                        .toByteArray()
                )
                tls.outputStream.flush()
                // "HTTP/1.1 404 Not Found"
                tls.inputStream.bufferedReader().readLine()?.split(' ')?.getOrNull(1)?.toIntOrNull()
            }
        } finally {
            runCatching { raw.close() }
        }
    } catch (_: Exception) {
        null
    }

    /** Protocolo SOCKS5 con usuario y contraseña (RFC 1928 y 1929). El nombre lo resuelve Tor. */
    private fun openSocksTunnel(socket: Socket, isolation: String, host: String) {
        val input = DataInputStream(socket.getInputStream())
        val output = socket.getOutputStream()

        output.write(byteArrayOf(5, 1, 2))
        output.flush()
        if (input.readUnsignedByte() != 5 || input.readUnsignedByte() != 2) throw IOException("SOCKS sin autenticación")

        val user = isolation.toByteArray()
        output.write(byteArrayOf(1, user.size.toByte()) + user + byteArrayOf(1, 'x'.code.toByte()))
        output.flush()
        input.readUnsignedByte()
        if (input.readUnsignedByte() != 0) throw IOException("SOCKS rechazó las credenciales")

        val name = host.toByteArray()
        output.write(byteArrayOf(5, 1, 0, 3, name.size.toByte()) + name + byteArrayOf(1, 0xBB.toByte()))
        output.flush()
        input.readUnsignedByte()
        val reply = input.readUnsignedByte()
        input.readUnsignedByte()
        when (input.readUnsignedByte()) {
            1 -> input.skipBytes(4)
            3 -> input.skipBytes(input.readUnsignedByte())
            4 -> input.skipBytes(16)
        }
        input.skipBytes(2)
        if (reply != 0) throw IOException("La salida no abrió la conexión ($reply)")
    }
}
