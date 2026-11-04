package com.Azelmods.App.data.security.tor

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketImpl
import java.net.SocketImplFactory
import java.net.SocketOptions
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * Enruta por Tor las conexiones de la app que no consultan ningún proxy.
 *
 * El [TorProxySelector] solo cubre lo que pregunta al ProxySelector (OkHttp,
 * HttpURLConnection). Firebase Realtime Database abre su WebSocket con un socket directo y
 * Android ignora el ProxySelector en esos casos, de modo que **los mensajes en tiempo real
 * seguían saliendo con la IP real** salvo que Orbot estuviera en modo VPN. Android tampoco
 * ofrece una forma de configurar un proxy en ese SDK.
 *
 * Aquí se intercepta en el único punto común: la fábrica de sockets de Java. Con el modo
 * anónimo activo, toda conexión TCP que no vaya al propio dispositivo pasa por el SOCKS de
 * Tor, y el nombre del servidor lo resuelve el nodo de salida.
 *
 * Límites, que la interfaz también explica:
 *  - Conscrypt resuelve el nombre antes de conectar, así que el DNS del sistema ve qué host
 *    se pide (el de Firebase, no el contenido). Solo una VPN puede evitarlo.
 *  - Lo que abre Google Play Services (notificaciones push, inicio de sesión con Google)
 *    vive en otro proceso y aquí no se puede tocar.
 *  - El código nativo no pasa por aquí: WebRTC tiene su propia política (solo relay).
 *
 * La fábrica solo puede fijarse una vez por proceso, así que no se instala hasta la primera
 * activación del modo anónimo: quien no lo usa nunca no pasa por este código. Después, con
 * el modo apagado, las conexiones van directas sin más.
 */
object TorSocketRouter {

    private const val TAG = "TorSocketRouter"

    @Volatile
    private var socks: InetSocketAddress? = null

    @Volatile
    var installed: Boolean = false
        private set

    /** Hosts que han salido por Tor en esta sesión: la prueba real de lo que está cubierto. */
    private val routedHosts = ConcurrentHashMap.newKeySet<String>()

    /** `true` si Firebase ya abrió su conexión de mensajes a través de Tor. */
    val messagingRouted: Boolean
        get() = routedHosts.any { it.contains("firebaseio") || it.contains("firebasedatabase") }

    fun enable(port: Int): Boolean {
        routedHosts.clear()
        socks = InetSocketAddress("127.0.0.1", port)
        if (!installed) {
            try {
                Socket.setSocketImplFactory(SocketImplFactory { RoutedSocketImpl() })
                installed = true
            } catch (e: IOException) {
                // Alguien fijó la fábrica antes; no debería ocurrir en esta app.
                Log.e(TAG, "No se pudo instalar el enrutado de sockets", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "No se pudo instalar el enrutado de sockets", e)
            }
        }
        return installed
    }

    fun disable() {
        socks = null
        routedHosts.clear()
    }

    private fun isLocal(address: InetSocketAddress): Boolean {
        val resolved = address.address
        if (resolved != null && (resolved.isLoopbackAddress || resolved.isAnyLocalAddress)) return true
        return address.hostString.equals("localhost", ignoreCase = true)
    }

    private class RoutedSocketImpl : SocketImpl() {
        private var delegate: Socket? = null
        private var ownedFd: ParcelFileDescriptor? = null
        private val pendingOptions = HashMap<Int, Any>()

        override fun create(stream: Boolean) = Unit

        override fun connect(host: String, port: Int) =
            connect(InetSocketAddress(host, port), 0)

        override fun connect(address: InetAddress, port: Int) =
            connect(InetSocketAddress(address, port), 0)

        override fun connect(endpoint: SocketAddress, timeout: Int) {
            val target = endpoint as? InetSocketAddress
                ?: throw IOException("Dirección no admitida: $endpoint")
            val proxy = socks
            val direct = proxy == null || isLocal(target)

            val socket = if (direct) {
                // Un Socket normal volvería a esta fábrica; el del canal no pasa por ella.
                SocketChannel.open().socket()
            } else {
                Socket(Proxy(Proxy.Type.SOCKS, proxy))
            }
            pendingOptions.forEach { (id, value) -> applyOption(socket, id, value) }
            try {
                val destination = when {
                    !direct -> InetSocketAddress.createUnresolved(target.hostString, target.port)
                    target.isUnresolved -> InetSocketAddress(target.hostString, target.port)
                    else -> target
                }
                socket.connect(destination, timeout)
            } catch (e: IOException) {
                runCatching { socket.close() }
                throw e
            }
            if (!direct) routedHosts.add(target.hostString)

            delegate = socket
            address = target.address ?: InetAddress.getByAddress(target.hostString, byteArrayOf(0, 0, 0, 0))
            port = target.port
            localport = socket.localPort
            // Conscrypt puede trabajar directamente sobre el descriptor del socket.
            ownedFd = runCatching { ParcelFileDescriptor.fromSocket(socket) }.getOrNull()
            fd = ownedFd?.fileDescriptor
        }

        override fun bind(host: InetAddress, port: Int) = Unit

        override fun listen(backlog: Int) = throw IOException("No admitido")

        override fun accept(s: SocketImpl) = throw IOException("No admitido")

        // Un socket devuelto por ServerSocket.accept() trae descriptor pero no delegado.
        override fun getInputStream(): InputStream =
            delegate?.getInputStream() ?: FileInputStream(requireFd())

        override fun getOutputStream(): OutputStream =
            delegate?.getOutputStream() ?: FileOutputStream(requireFd())

        private fun requireFd(): FileDescriptor = fd ?: throw IOException("Socket sin conectar")

        override fun available(): Int = delegate?.getInputStream()?.available() ?: 0

        override fun close() {
            runCatching { ownedFd?.close() }
            ownedFd = null
            delegate?.close()
        }

        override fun shutdownInput() {
            delegate?.shutdownInput()
        }

        override fun shutdownOutput() {
            delegate?.shutdownOutput()
        }

        override fun sendUrgentData(data: Int) {
            delegate?.sendUrgentData(data)
        }

        override fun setOption(optID: Int, value: Any) {
            val socket = delegate
            if (socket != null) applyOption(socket, optID, value) else pendingOptions[optID] = value
        }

        override fun getOption(optID: Int): Any {
            val socket = delegate
            return when (optID) {
                SocketOptions.SO_BINDADDR -> socket?.localAddress ?: InetAddress.getLoopbackAddress()
                SocketOptions.TCP_NODELAY -> socket?.tcpNoDelay ?: (pendingOptions[optID] ?: false)
                SocketOptions.SO_KEEPALIVE -> socket?.keepAlive ?: (pendingOptions[optID] ?: false)
                SocketOptions.SO_TIMEOUT -> socket?.soTimeout ?: (pendingOptions[optID] ?: 0)
                SocketOptions.SO_LINGER -> socket?.soLinger ?: -1
                SocketOptions.SO_RCVBUF -> socket?.receiveBufferSize ?: (pendingOptions[optID] ?: 65536)
                SocketOptions.SO_SNDBUF -> socket?.sendBufferSize ?: (pendingOptions[optID] ?: 65536)
                SocketOptions.SO_REUSEADDR -> socket?.reuseAddress ?: false
                SocketOptions.IP_TOS -> socket?.trafficClass ?: 0
                SocketOptions.SO_OOBINLINE -> socket?.oobInline ?: false
                else -> pendingOptions[optID] ?: throw java.net.SocketException("Opción desconocida: $optID")
            }
        }

        private fun applyOption(socket: Socket, optID: Int, value: Any) {
            runCatching {
                when (optID) {
                    SocketOptions.TCP_NODELAY -> socket.tcpNoDelay = value as Boolean
                    SocketOptions.SO_KEEPALIVE -> socket.keepAlive = value as Boolean
                    SocketOptions.SO_TIMEOUT -> socket.soTimeout = value as Int
                    SocketOptions.SO_LINGER -> when (value) {
                        is Boolean -> socket.setSoLinger(value, 0)
                        is Int -> socket.setSoLinger(true, value)
                    }
                    SocketOptions.SO_RCVBUF -> socket.receiveBufferSize = value as Int
                    SocketOptions.SO_SNDBUF -> socket.sendBufferSize = value as Int
                    SocketOptions.SO_REUSEADDR -> socket.reuseAddress = value as Boolean
                    SocketOptions.IP_TOS -> socket.trafficClass = value as Int
                    SocketOptions.SO_OOBINLINE -> socket.oobInline = value as Boolean
                }
            }
        }
    }
}
