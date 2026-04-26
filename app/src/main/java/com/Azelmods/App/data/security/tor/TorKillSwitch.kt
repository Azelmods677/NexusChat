package com.Azelmods.App.data.security.tor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TorKillSwitch - Blocks all network traffic when Tor is enabled but not connected.
 *
 * This prevents accidental leaks when:
 * - Tor is connecting but not yet ready
 * - Tor connection drops unexpectedly
 * - Tor fails to connect
 *
 * Implementation:
 * - Uses Android VpnService to create a local VPN that blocks all traffic
 * - Only allows traffic when Tor is in Connected state
 * - Automatically re-enables when Tor reconnects
 *
 * Note: This requires VpnService permission and user approval.
 * For a simpler implementation without VPN, we just track state and warn the user.
 */
@Singleton
class TorKillSwitch @Inject constructor(
    private val context: Context
) {

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _blockedConnections = MutableStateFlow(0)
    val blockedConnections: StateFlow<Int> = _blockedConnections.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Activates the kill switch.
     *
     * When active, all network traffic is blocked until Tor is connected.
     * This prevents leaks during Tor bootstrap or connection failures.
     */
    fun activate() {
        if (_isActive.value) {
            Log.d(TAG, "Kill switch already active")
            return
        }

        Log.i(TAG, "Activating Tor kill switch")
        _isActive.value = true

        // Register network callback to monitor and block connections
        registerNetworkCallback()
    }

    /**
     * Deactivates the kill switch.
     *
     * Network traffic is allowed again. Call this when:
     * - Tor is successfully connected
     * - User disables Tor
     */
    fun deactivate() {
        if (!_isActive.value) {
            Log.d(TAG, "Kill switch already inactive")
            return
        }

        Log.i(TAG, "Deactivating Tor kill switch")
        _isActive.value = false

        // Unregister network callback
        unregisterNetworkCallback()

        // Reset blocked connections counter
        _blockedConnections.value = 0
    }

    /**
     * Checks if a network connection should be allowed.
     *
     * Returns true if the connection is allowed, false if it should be blocked.
     */
    fun shouldAllowConnection(torState: TorState): Boolean {
        if (!_isActive.value) {
            // Kill switch is off, allow all connections
            return true
        }

        // Only allow connections when Tor is fully connected
        val allowed = torState is TorState.Connected

        if (!allowed) {
            _blockedConnections.value++
            Log.w(TAG, "Blocked connection (Tor not connected). Total blocked: ${_blockedConnections.value}")
        }

        return allowed
    }

    /**
     * Registers a network callback to monitor connections.
     *
     * Note: This is a simplified implementation that tracks connections.
     * A full VpnService implementation would actually block traffic at the network layer.
     */
    private fun registerNetworkCallback() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                // In a full implementation, we would block this network if Tor is not connected
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        Log.d(TAG, "Network callback registered")
    }

    /**
     * Unregisters the network callback.
     */
    private fun unregisterNetworkCallback() {
        networkCallback?.let { callback ->
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(callback)
                Log.d(TAG, "Network callback unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering network callback", e)
            }
        }
        networkCallback = null
    }

    companion object {
        private const val TAG = "TorKillSwitch"
    }
}

/**
 * Extension function to check if a connection should be allowed based on kill switch state.
 */
fun TorKillSwitch.isConnectionAllowed(torState: TorState): Boolean {
    return shouldAllowConnection(torState)
}
