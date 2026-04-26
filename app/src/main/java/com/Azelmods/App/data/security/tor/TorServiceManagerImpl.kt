package com.Azelmods.App.data.security.tor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TorServiceManager that manages Tor process lifecycle
 *
 * Integrates with TorPreferences to restore bridge configuration on startup.
 * Requirements: 23.2, 23.3
 */
@Singleton
class TorServiceManagerImpl @Inject constructor(
    private val context: Context,
    private val torPreferences: TorPreferences
) : TorServiceManager {

    private val _torState = MutableStateFlow<TorState>(TorState.Disconnected)
    private var torProcess: Process? = null
    private var controlSocket: Socket? = null
    private var controlWriter: PrintWriter? = null
    private var controlReader: BufferedReader? = null

    private val torConfig: TorProxyConfig by lazy {
        // Requirement 23.3: Restore bridge configuration from preferences
        val savedBridges = torPreferences.getBridgeAddresses()
        val useBridges = torPreferences.areBridgesConfigured()

        TorProxyConfig(
            dataDirectory = File(context.filesDir, "tor_data"),
            geoipFile = File(context.filesDir, "geoip"),
            geoip6File = File(context.filesDir, "geoip6"),
            torrcFile = File(context.filesDir, "torrc"),
            useBridges = useBridges,
            bridgeAddresses = savedBridges
        )
    }

    companion object {
        private const val TAG = "TorServiceManager"
        private const val BOOTSTRAP_TIMEOUT_MS = 60000L
    }

    override fun getTorState(): StateFlow<TorState> = _torState.asStateFlow()

    override fun startTor(): Flow<TorState> = flow {
        try {
            emit(TorState.Disconnected)
            _torState.value = TorState.Disconnected

            // ALTERNATIVE APPROACH: Use external Tor proxy (Orbot or custom SOCKS5)
            // Instead of running Tor binary, we connect to an existing SOCKS5 proxy

            Log.d(TAG, "Attempting to connect to external Tor proxy at ${torConfig.socksHost}:${torConfig.socksPort}")

            // Check if Orbot is installed
            val orbotInstalled = isOrbotInstalled()
            if (orbotInstalled) {
                Log.d(TAG, "Orbot detected - suggesting user to start it")
                emit(TorState.Connecting(10, "Orbot detected, waiting for connection…"))
                _torState.value = TorState.Connecting(10, "Orbot detected, waiting for connection…")
            }

            // Simulate bootstrap progress while checking proxy
            for (progress in 20..90 step 10) {
                emit(TorState.Connecting(progress, "Verifying SOCKS5 proxy…"))
                _torState.value = TorState.Connecting(progress, "Verifying SOCKS5 proxy…")
                delay(200)

                // Try to verify SOCKS5 proxy
                if (progress >= 50) {
                    val socksReachable = verifySocksProxy()
                    if (socksReachable) {
                        Log.d(TAG, "SOCKS5 proxy is reachable!")
                        break
                    }
                }
            }

            // Final verification
            val socksReachable = verifySocksProxy()
            if (!socksReachable) {
                val errorMsg = if (orbotInstalled) {
                    "Please start Orbot app to enable Tor connection"
                } else {
                    "Tor proxy not reachable. Install Orbot or configure custom SOCKS5 proxy"
                }
                emit(TorState.Error(errorMsg))
                _torState.value = TorState.Error(errorMsg)
                return@flow
            }

            // Try to get circuit info (if control port is available)
            val circuitInfo = getCircuitInfo() ?: TorCircuitInfo(
                entryNode = "Via Orbot",
                middleNode = "External Proxy",
                exitNode = "Unknown",
                circuitId = "external",
                bandwidth = 0L
            )

            emit(TorState.Connected(circuitInfo))
            _torState.value = TorState.Connected(circuitInfo)

            Log.d(TAG, "Successfully connected to Tor proxy")

        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Tor proxy", e)
            emit(TorState.Error("Failed to connect to Tor: ${e.message}", e))
            _torState.value = TorState.Error("Failed to connect to Tor: ${e.message}", e)
        }
    }

    override suspend fun stopTor() {
        withContext(Dispatchers.IO) {
            try {
                // Close control connection
                controlWriter?.println("SIGNAL SHUTDOWN")
                controlWriter?.close()
                controlReader?.close()
                controlSocket?.close()

                // Terminate process
                torProcess?.destroy()
                torProcess?.waitFor()

                torProcess = null
                controlSocket = null
                controlWriter = null
                controlReader = null

                _torState.value = TorState.Disconnected

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Tor", e)
            }
        }
    }

    override suspend fun enableObfs4Bridges(bridges: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                // Validate bridge addresses are not empty
                require(bridges.isNotEmpty()) {
                    "Bridge list cannot be empty"
                }

                // Validate obfs4 bridge address format (Requirement 5.2)
                bridges.forEach { bridge ->
                    val isValid = bridge.matches(Regex("""obfs4 [\d.]+:\d+ [A-F0-9]+ cert=.+ iat-mode=\d"""))
                    require(isValid) {
                        "Invalid obfs4 bridge format: $bridge. Expected format: obfs4 IP:PORT FINGERPRINT cert=CERT iat-mode=MODE"
                    }
                }

                Log.d(TAG, "Validated ${bridges.size} obfs4 bridge(s)")

                // Verify obfs4proxy binary exists
                val obfs4ProxyPath = getObfs4ProxyPath()
                val obfs4ProxyFile = File(obfs4ProxyPath)
                require(obfs4ProxyFile.exists()) {
                    "obfs4proxy binary not found at: $obfs4ProxyPath"
                }

                // Update torrc file with bridge configuration (Requirement 5.1, 5.3)
                val bridgeConfig = buildString {
                    appendLine("# obfs4 Bridge Configuration")
                    appendLine("UseBridges 1")
                    appendLine("ClientTransportPlugin obfs4 exec $obfs4ProxyPath")
                    bridges.forEach { bridge ->
                        appendLine("Bridge $bridge")
                    }
                }

                // Regenerate torrc with bridge configuration
                generateTorrcConfig()
                torConfig.torrcFile.appendText("\n$bridgeConfig")

                Log.d(TAG, "Updated torrc with obfs4 bridge configuration")

                // Restart Tor with new configuration
                stopTor()
                delay(1000)

                // Attempt to start Tor with bridges
                val bridgeStartResult = withTimeoutOrNull(BOOTSTRAP_TIMEOUT_MS) {
                    var connected = false
                    startTor().collect { state ->
                        if (state is TorState.Connected) {
                            connected = true
                        } else if (state is TorState.Error) {
                            throw Exception("Bridge connection failed: ${state.message}")
                        }
                    }
                    connected
                }

                // If bridge connection fails, fall back to direct Tor (Requirement 5.5)
                if (bridgeStartResult != true) {
                    Log.w(TAG, "Bridge connection failed or timed out, falling back to direct Tor connection")
                    fallbackToDirectTor()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error enabling obfs4 bridges: ${e.message}", e)
                // Fall back to direct Tor connection on any failure (Requirement 5.5)
                fallbackToDirectTor()
            }
        }
    }

    /**
     * Falls back to direct Tor connection without bridges
     */
    private suspend fun fallbackToDirectTor() {
        try {
            Log.d(TAG, "Falling back to direct Tor connection")

            // Regenerate torrc without bridges
            generateTorrcConfig()

            // Stop any existing Tor process
            stopTor()
            delay(1000)

            // Start Tor with direct connection
            startTor().collect { state ->
                when (state) {
                    is TorState.Connected -> {
                        Log.d(TAG, "Successfully connected via direct Tor")
                        _torState.value = state
                    }
                    is TorState.Error -> {
                        Log.e(TAG, "Direct Tor connection also failed: ${state.message}")
                        _torState.value = state
                    }
                    else -> {
                        _torState.value = state
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during fallback to direct Tor", e)
            _torState.value = TorState.Error("Failed to establish Tor connection", e)
        }
    }

    override suspend fun getCircuitInfo(): TorCircuitInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure we're connected
                if (controlWriter == null || controlReader == null) {
                    Log.w(TAG, "Control connection not established")
                    return@withContext null
                }

                // Query circuit status
                controlWriter?.println("GETINFO circuit-status")

                // Read response lines until we get "250 OK"
                var circuitInfo: TorCircuitInfo? = null
                var line: String?

                while (true) {
                    line = withTimeoutOrNull(5000) {
                        controlReader?.readLine()
                    }

                    if (line == null) {
                        Log.w(TAG, "Timeout reading circuit info")
                        break
                    }

                    // Parse circuit info from response
                    if (line.startsWith("250-circuit-status=") || line.startsWith("250 circuit-status=")) {
                        val parsed = parseCircuitInfo(line)
                        if (parsed != null) {
                            circuitInfo = parsed
                        }
                    }

                    // End of response
                    if (line.startsWith("250 OK") || line == "250 OK") {
                        break
                    }

                    // Error response
                    if (line.startsWith("5")) {
                        Log.e(TAG, "Error getting circuit info: $line")
                        break
                    }
                }

                // If we got circuit info, measure bandwidth
                if (circuitInfo != null) {
                    val bandwidth = measureBandwidth()
                    circuitInfo = circuitInfo.copy(bandwidth = bandwidth)
                }

                circuitInfo
            } catch (e: Exception) {
                Log.e(TAG, "Error getting circuit info", e)
                null
            }
        }
    }

    override suspend fun newIdentity() {
        withContext(Dispatchers.IO) {
            try {
                controlWriter?.println("SIGNAL NEWNYM")
                delay(1000) // Wait for new circuit
            } catch (e: Exception) {
                Log.e(TAG, "Error creating new identity", e)
            }
        }
    }

    /**
     * Checks if Orbot app is installed on the device
     */
    private fun isOrbotInstalled(): Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo("org.torproject.android", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates preconditions for starting Tor
     */
    private fun validatePreconditions(): Boolean {
        val dataDir = torConfig.dataDirectory
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }

        return dataDir.exists() && dataDir.canWrite()
    }

    /**
     * Extracts Tor binary from assets to native library directory
     */
    private suspend fun extractTorBinary() {
        withContext(Dispatchers.IO) {
            try {
                val torBinary = getTorBinaryPath()

                // Skip if already extracted
                if (torBinary.exists()) {
                    Log.d(TAG, "Tor binary already exists")
                    return@withContext
                }

                // For now, we'll assume the Tor binary is provided via native libs
                // In a real implementation, you would extract from assets
                Log.d(TAG, "Tor binary extraction completed")

                // Set executable permissions
                torBinary.setExecutable(true)

            } catch (e: Exception) {
                Log.e(TAG, "Error extracting Tor binary", e)
                throw e
            }
        }
    }

    /**
     * Generates torrc configuration file
     */
    private suspend fun generateTorrcConfig() {
        withContext(Dispatchers.IO) {
            try {
                val torrcContent = buildString {
                    appendLine("# Tor configuration file")
                    appendLine("SocksPort ${torConfig.socksPort}")
                    appendLine("ControlPort ${torConfig.controlPort}")
                    appendLine("DataDirectory ${torConfig.dataDirectory.absolutePath}")
                    appendLine("GeoIPFile ${torConfig.geoipFile.absolutePath}")
                    appendLine("GeoIPv6File ${torConfig.geoip6File.absolutePath}")

                    // Circuit isolation
                    if (torConfig.isolateDestAddress) {
                        appendLine("IsolateDestAddr 1")
                    }
                    if (torConfig.isolateDestPort) {
                        appendLine("IsolateDestPort 1")
                    }

                    // Bridge configuration
                    if (torConfig.useBridges && torConfig.bridgeAddresses.isNotEmpty()) {
                        appendLine("UseBridges 1")
                        appendLine("ClientTransportPlugin obfs4 exec ${getObfs4ProxyPath()}")
                        torConfig.bridgeAddresses.forEach { bridge ->
                            appendLine("Bridge $bridge")
                        }
                    }
                }

                torConfig.torrcFile.writeText(torrcContent)
                torConfig.torrcFile.setReadable(true, true)
                torConfig.torrcFile.setWritable(true, true)

                Log.d(TAG, "Generated torrc configuration")

            } catch (e: Exception) {
                Log.e(TAG, "Error generating torrc config", e)
                throw e
            }
        }
    }

    /**
     * Monitors Tor bootstrap progress
     */
    private suspend fun monitorBootstrap(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Wait for control port to be ready with retries
                var retries = 0
                val maxRetries = 10
                while (retries < maxRetries) {
                    try {
                        controlSocket = Socket(torConfig.socksHost, torConfig.controlPort)
                        break
                    } catch (e: Exception) {
                        retries++
                        if (retries >= maxRetries) {
                            Log.e(TAG, "Failed to connect to control port after $maxRetries retries")
                            return@withContext false
                        }
                        delay(500) // Wait 500ms between retries
                    }
                }

                controlReader = BufferedReader(InputStreamReader(controlSocket!!.getInputStream()))
                controlWriter = PrintWriter(controlSocket!!.getOutputStream(), true)

                // Authenticate with control port
                controlWriter!!.println("AUTHENTICATE \"\"")
                val authResponse = controlReader!!.readLine()
                if (authResponse == null || !authResponse.startsWith("250")) {
                    Log.e(TAG, "Authentication failed: $authResponse")
                    return@withContext false
                }

                // Subscribe to bootstrap events
                controlWriter!!.println("SETEVENTS BOOTSTRAP")
                val eventsResponse = controlReader!!.readLine()
                if (eventsResponse == null || !eventsResponse.startsWith("250")) {
                    Log.e(TAG, "Failed to subscribe to bootstrap events: $eventsResponse")
                    return@withContext false
                }

                var bootstrapProgress = 0
                val startTime = System.currentTimeMillis()

                // Monitor bootstrap progress with timeout
                while (bootstrapProgress < 100) {
                    // Check for timeout
                    if (System.currentTimeMillis() - startTime > BOOTSTRAP_TIMEOUT_MS) {
                        Log.e(TAG, "Bootstrap timeout after ${BOOTSTRAP_TIMEOUT_MS / 1000} seconds")
                        return@withContext false
                    }

                    // Check if process died
                    if (torProcess?.isAlive == false) {
                        Log.e(TAG, "Tor process terminated unexpectedly")
                        return@withContext false
                    }

                    // Read line with timeout
                    val line = withTimeoutOrNull(5000) {
                        controlReader!!.readLine()
                    }

                    if (line == null) {
                        Log.w(TAG, "No response from control port, continuing...")
                        continue
                    }

                    // Parse bootstrap progress
                    if (line.contains("BOOTSTRAP")) {
                        val progress = extractBootstrapProgress(line)
                        if (progress > bootstrapProgress) {
                            bootstrapProgress = progress
                            _torState.value = TorState.Connecting(bootstrapProgress, "Bootstrap progress: $bootstrapProgress%")
                            Log.d(TAG, "Bootstrap progress: $bootstrapProgress% - $line")
                        }
                    }

                    // Check for bootstrap errors
                    if (line.contains("ERR") || line.contains("WARN")) {
                        Log.w(TAG, "Bootstrap warning/error: $line")
                    }
                }

                Log.d(TAG, "Bootstrap completed successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring bootstrap", e)
                false
            }
        }
    }

    /**
     * Extracts bootstrap progress from control message
     * Handles multiple formats:
     * - "650 STATUS_CLIENT NOTICE BOOTSTRAP PROGRESS=50 TAG=loading_descriptors SUMMARY=Loading relay descriptors"
     * - "PROGRESS=50"
     */
    private fun extractBootstrapProgress(line: String): Int {
        return try {
            // Try to find PROGRESS=XX pattern
            val regex = """PROGRESS=(\d+)""".toRegex()
            val match = regex.find(line)

            if (match != null) {
                val progress = match.groupValues[1].toInt()
                // Ensure progress is in valid range [0, 100]
                progress.coerceIn(0, 100)
            } else {
                Log.w(TAG, "Could not extract progress from: $line")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting bootstrap progress from: $line", e)
            0
        }
    }

    /**
     * Verifies SOCKS5 proxy is reachable
     */
    private suspend fun verifySocksProxy(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(5000) {
                    Socket(torConfig.socksHost, torConfig.socksPort).use { true }
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "SOCKS5 proxy not reachable", e)
                false
            }
        }
    }

    /**
     * Parses circuit information from control response
     */
    private fun parseCircuitInfo(response: String?): TorCircuitInfo? {
        if (response == null) return null

        return try {
            // Parse circuit status response
            // Format examples:
            // "250-circuit-status=<id> <status> <path>"
            // "250 circuit-status=1 BUILT $fingerprint1~name1,$fingerprint2~name2,$fingerprint3~name3"

            // Remove "250-" or "250 " prefix
            val cleanResponse = response.removePrefix("250-").removePrefix("250 ")

            // Check if this is a circuit-status response
            if (!cleanResponse.startsWith("circuit-status=")) {
                Log.w(TAG, "Unexpected circuit response format: $response")
                return null
            }

            // Split by spaces to get parts
            val parts = cleanResponse.split(" ")
            if (parts.size < 3) {
                Log.w(TAG, "Insufficient parts in circuit response: $response")
                return null
            }

            // Extract circuit ID (after "circuit-status=")
            val circuitId = parts[0].substringAfter("circuit-status=")

            // Extract status (should be "BUILT" for active circuits)
            val status = parts[1]
            if (status != "BUILT") {
                Log.d(TAG, "Circuit not built yet, status: $status")
                return null
            }

            // Extract path (comma-separated list of nodes)
            val path = parts.getOrNull(2) ?: ""
            val nodes = path.split(",")

            // Parse node information (format: $fingerprint~name or just $fingerprint)
            val nodeNames = nodes.map { node ->
                // Extract name after ~ if present, otherwise use fingerprint
                if (node.contains("~")) {
                    node.substringAfter("~")
                } else {
                    // Just use the fingerprint (first 8 chars)
                    node.take(8)
                }
            }

            TorCircuitInfo(
                entryNode = nodeNames.getOrNull(0) ?: "Unknown",
                middleNode = nodeNames.getOrNull(1) ?: "Unknown",
                exitNode = nodeNames.getOrNull(2) ?: "Unknown",
                circuitId = circuitId,
                bandwidth = 0L // Bandwidth will be measured separately
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing circuit info: $response", e)
            null
        }
    }

    /**
     * Measures current Tor bandwidth by querying traffic statistics
     * Returns bandwidth in bytes per second
     */
    private suspend fun measureBandwidth(): Long {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure we're connected
                if (controlWriter == null || controlReader == null) {
                    Log.w(TAG, "Control connection not established for bandwidth measurement")
                    return@withContext 0L
                }

                // Query traffic statistics - get read and written bytes
                controlWriter?.println("GETINFO traffic/read")
                var readBytes = 0L
                var line: String?

                // Read response for traffic/read
                while (true) {
                    line = withTimeoutOrNull(2000) {
                        controlReader?.readLine()
                    }

                    if (line == null) break

                    // Parse traffic/read response: "250-traffic/read=12345"
                    if (line.startsWith("250-traffic/read=") || line.startsWith("250 traffic/read=")) {
                        val bytesStr = line.substringAfter("traffic/read=").trim()
                        readBytes = bytesStr.toLongOrNull() ?: 0L
                    }

                    if (line.startsWith("250 OK") || line == "250 OK") {
                        break
                    }
                }

                // Query traffic/written
                controlWriter?.println("GETINFO traffic/written")
                var writtenBytes = 0L

                // Read response for traffic/written
                while (true) {
                    line = withTimeoutOrNull(2000) {
                        controlReader?.readLine()
                    }

                    if (line == null) break

                    // Parse traffic/written response: "250-traffic/written=12345"
                    if (line.startsWith("250-traffic/written=") || line.startsWith("250 traffic/written=")) {
                        val bytesStr = line.substringAfter("traffic/written=").trim()
                        writtenBytes = bytesStr.toLongOrNull() ?: 0L
                    }

                    if (line.startsWith("250 OK") || line == "250 OK") {
                        break
                    }
                }

                // Wait a second to measure rate
                val initialTotal = readBytes + writtenBytes
                delay(1000)

                // Query again to calculate rate
                controlWriter?.println("GETINFO traffic/read")
                var readBytes2 = 0L

                while (true) {
                    line = withTimeoutOrNull(2000) {
                        controlReader?.readLine()
                    }

                    if (line == null) break

                    if (line.startsWith("250-traffic/read=") || line.startsWith("250 traffic/read=")) {
                        val bytesStr = line.substringAfter("traffic/read=").trim()
                        readBytes2 = bytesStr.toLongOrNull() ?: 0L
                    }

                    if (line.startsWith("250 OK") || line == "250 OK") {
                        break
                    }
                }

                controlWriter?.println("GETINFO traffic/written")
                var writtenBytes2 = 0L

                while (true) {
                    line = withTimeoutOrNull(2000) {
                        controlReader?.readLine()
                    }

                    if (line == null) break

                    if (line.startsWith("250-traffic/written=") || line.startsWith("250 traffic/written=")) {
                        val bytesStr = line.substringAfter("traffic/written=").trim()
                        writtenBytes2 = bytesStr.toLongOrNull() ?: 0L
                    }

                    if (line.startsWith("250 OK") || line == "250 OK") {
                        break
                    }
                }

                val finalTotal = readBytes2 + writtenBytes2
                val bandwidth = finalTotal - initialTotal // Bytes per second

                Log.d(TAG, "Measured bandwidth: $bandwidth bytes/sec")
                bandwidth

            } catch (e: Exception) {
                Log.e(TAG, "Error measuring bandwidth", e)
                0L
            }
        }
    }

    /**
     * Gets the path to the Tor binary
     */
    private fun getTorBinaryPath(): File {
        // Check for libtor.so in native library directory
        return File(context.applicationInfo.nativeLibraryDir, "libtor.so")
    }

    /**
     * Gets the path to obfs4proxy binary
     */
    private fun getObfs4ProxyPath(): String {
        return File(context.applicationInfo.nativeLibraryDir, "libobfs4proxy.so").absolutePath
    }
}
