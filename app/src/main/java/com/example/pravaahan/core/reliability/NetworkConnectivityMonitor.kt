package com.example.pravaahan.core.reliability

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.pravaahan.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity and quality for adaptive behavior
 * in railway real-time systems.
 */
@Singleton
class NetworkConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "NetworkConnectivityMonitor"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    private val _networkQuality = MutableStateFlow(NetworkQuality.UNAVAILABLE)
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()
    
    private val _networkType = MutableStateFlow(NetworkType.UNKNOWN)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    init {
        startMonitoring()
    }
    
    /**
     * Starts network monitoring
     */
    private fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                logger.info(TAG, "Network available: $network")
                updateNetworkStatus(true)
                updateNetworkCapabilities(network)
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                logger.warn(TAG, "Network lost: $network")
                updateNetworkStatus(false)
                _networkQuality.value = NetworkQuality.UNAVAILABLE
                _networkType.value = NetworkType.UNKNOWN
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                updateNetworkCapabilities(network, networkCapabilities)
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        
        // Initial status check
        checkInitialNetworkStatus()
    }
    
    /**
     * Checks initial network status
     */
    private fun checkInitialNetworkStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = activeNetwork?.let { 
            connectivityManager.getNetworkCapabilities(it) 
        }
        
        val isConnected = activeNetwork != null && 
                         networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        
        updateNetworkStatus(isConnected)
        
        if (isConnected && activeNetwork != null) {
            updateNetworkCapabilities(activeNetwork, networkCapabilities)
        }
    }
    
    /**
     * Updates network availability status
     */
    private fun updateNetworkStatus(isAvailable: Boolean) {
        _isNetworkAvailable.value = isAvailable
        logger.debug(TAG, "Network availability: $isAvailable")
    }
    
    /**
     * Updates network capabilities and quality assessment
     */
    private fun updateNetworkCapabilities(network: Network, capabilities: NetworkCapabilities? = null) {
        val networkCapabilities = capabilities ?: connectivityManager.getNetworkCapabilities(network)
        
        if (networkCapabilities != null) {
            // Determine network type
            val type = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }
            _networkType.value = type
            
            // Assess network quality
            val quality = assessNetworkQuality(networkCapabilities)
            _networkQuality.value = quality
            
            logger.debug(TAG, "Network type: $type, quality: $quality")
        }
    }
    
    /**
     * Assesses network quality based on capabilities
     */
    private fun assessNetworkQuality(capabilities: NetworkCapabilities): NetworkQuality {
        return when {
            // High-speed connections
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && 
             capabilities.linkDownstreamBandwidthKbps > 50000) -> NetworkQuality.EXCELLENT
            
            // Good WiFi or fast cellular
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
             capabilities.linkDownstreamBandwidthKbps > 10000) -> NetworkQuality.GOOD
            
            // Moderate cellular
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            capabilities.linkDownstreamBandwidthKbps > 1000 -> NetworkQuality.FAIR
            
            // Slow or unknown connection
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkQuality.POOR
            
            else -> NetworkQuality.UNAVAILABLE
        }
    }
    
    /**
     * Checks if network is available synchronously
     */
    fun isNetworkAvailable(): Boolean {
        return _isNetworkAvailable.value
    }
    
    /**
     * Gets current network quality
     */
    fun getCurrentNetworkQuality(): NetworkQuality {
        return _networkQuality.value
    }
    
    /**
     * Gets current network type
     */
    fun getCurrentNetworkType(): NetworkType {
        return _networkType.value
    }
    
    /**
     * Checks if network is metered (for data usage optimization)
     */
    fun isNetworkMetered(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { 
            connectivityManager.getNetworkCapabilities(it) 
        }
        
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
    }
    
    /**
     * Gets estimated bandwidth in Kbps
     */
    fun getEstimatedBandwidth(): Int {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { 
            connectivityManager.getNetworkCapabilities(it) 
        }
        
        return capabilities?.linkDownstreamBandwidthKbps ?: 0
    }
    
    /**
     * Gets network statistics for monitoring
     */
    fun getNetworkStats(): NetworkStats {
        return NetworkStats(
            isAvailable = _isNetworkAvailable.value,
            quality = _networkQuality.value,
            type = _networkType.value,
            isMetered = isNetworkMetered(),
            estimatedBandwidthKbps = getEstimatedBandwidth()
        )
    }
    
    /**
     * Stops network monitoring
     */
    fun stopMonitoring() {
        networkCallback?.let { callback ->
            connectivityManager.unregisterNetworkCallback(callback)
            networkCallback = null
        }
        logger.info(TAG, "Stopped network monitoring")
    }
}

/**
 * Network quality levels
 */
enum class NetworkQuality {
    UNAVAILABLE,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT
}

/**
 * Network types
 */
enum class NetworkType {
    UNKNOWN,
    WIFI,
    CELLULAR,
    ETHERNET
}

/**
 * Network statistics
 */
data class NetworkStats(
    val isAvailable: Boolean,
    val quality: NetworkQuality,
    val type: NetworkType,
    val isMetered: Boolean,
    val estimatedBandwidthKbps: Int
)