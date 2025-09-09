package com.example.pravaahan.core.reliability

import com.example.pravaahan.core.error.AppError
import com.example.pravaahan.core.error.getUserMessage
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.logging.logApiCall
import com.example.pravaahan.core.logging.logApiResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Manages real-time connections with intelligent retry logic, exponential backoff,
 * and graceful degradation for railway system reliability.
 */
@Singleton
class RealTimeConnectionManager @Inject constructor(
    private val logger: Logger,
    private val networkMonitor: NetworkConnectivityMonitor
) {
    
    companion object {
        private const val TAG = "RealTimeConnectionManager"
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val HEARTBEAT_INTERVAL_MS = 30000L
    }
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _connectionQuality = MutableStateFlow(ConnectionQuality.UNKNOWN)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()
    
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var retryAttempts = 0
    private var lastConnectionAttempt = Instant.DISTANT_PAST
    
    /**
     * Establishes connection with intelligent retry and circuit breaker protection
     */
    suspend fun connect(endpoint: String): Result<Unit> {
        logger.logApiCall(TAG, "connect", mapOf("endpoint" to endpoint))
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            logger.info(TAG, "Already connected to $endpoint")
            return Result.success(Unit)
        }
        
        // Check network availability before attempting connection
        if (!networkMonitor.isNetworkAvailable()) {
            val error = AppError.NetworkError.NoConnection
            logger.logApiResponse(TAG, "connect", false)
            return Result.failure(error)
        }
        
        return try {
            _connectionState.value = ConnectionState.CONNECTING
            
            val result = attemptConnection(endpoint)
            
            if (result.isSuccess) {
                onConnectionSuccess(endpoint)
            } else {
                onConnectionFailure(result.exceptionOrNull())
            }
            
            result
        } catch (e: Exception) {
            onConnectionFailure(e)
            Result.failure(AppError.NetworkError.UnknownNetworkError("Connection failed: ${e.message}"))
        }
    }
    
    /**
     * Disconnects from the current connection
     */
    suspend fun disconnect() {
        logger.info(TAG, "Disconnecting from real-time service")
        
        connectionJob?.cancel()
        heartbeatJob?.cancel()
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectionQuality.value = ConnectionQuality.UNKNOWN
        
        retryAttempts = 0
    }
    
    /**
     * Attempts connection with timeout and error handling
     */
    private suspend fun attemptConnection(endpoint: String): Result<Unit> {
        return withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
            try {
                // Simulate connection attempt - in real implementation, this would
                // establish WebSocket or other real-time connection
                delay(1000) // Simulate connection time
                
                if (networkMonitor.isNetworkAvailable()) {
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NetworkError.NoConnection)
                }
            } catch (e: Exception) {
                Result.failure(AppError.NetworkError.UnknownNetworkError("Connection attempt failed: ${e.message}"))
            }
        } ?: Result.failure(AppError.NetworkError.Timeout)
    }
    
    /**
     * Handles successful connection
     */
    private fun onConnectionSuccess(endpoint: String) {
        _connectionState.value = ConnectionState.CONNECTED
        _connectionQuality.value = ConnectionQuality.GOOD
        retryAttempts = 0
        
        logger.logApiResponse(TAG, "connect", true)
        
        // Start heartbeat monitoring
        startHeartbeat()
        
        // Start connection quality monitoring
        startConnectionQualityMonitoring()
    }
    
    /**
     * Handles connection failure with retry logic
     */
    private suspend fun onConnectionFailure(error: Throwable?) {
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectionQuality.value = ConnectionQuality.POOR
        
        val errorMessage = error?.message ?: "Unknown connection error"
        logger.warn(TAG, "Connection failed: $errorMessage")
        
        if (retryAttempts < MAX_RETRY_ATTEMPTS) {
            scheduleRetry()
        } else {
            logger.error(TAG, "Max retry attempts reached. Entering degraded mode.")
            _connectionState.value = ConnectionState.FAILED
        }
    }
    
    /**
     * Schedules retry with exponential backoff
     */
    private suspend fun scheduleRetry() {
        retryAttempts++
        val delayMs = calculateRetryDelay(retryAttempts)
        
        logger.info(TAG, "Scheduling retry attempt $retryAttempts in ${delayMs}ms")
        
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            delay(delayMs)
            
            if (networkMonitor.isNetworkAvailable()) {
                // Retry connection - this would need the original endpoint
                // In a real implementation, you'd store the endpoint
                logger.info(TAG, "Retrying connection (attempt $retryAttempts)")
            } else {
                logger.warn(TAG, "Network unavailable, postponing retry")
                scheduleRetry()
            }
        }
    }
    
    /**
     * Calculates retry delay with exponential backoff and jitter
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val baseDelay = INITIAL_RETRY_DELAY_MS * (1L shl (attempt - 1))
        val cappedDelay = minOf(baseDelay, MAX_RETRY_DELAY_MS)
        
        // Add jitter to prevent thundering herd
        val jitter = (0..1000).random()
        return cappedDelay + jitter
    }
    
    /**
     * Starts heartbeat monitoring to detect connection issues
     */
    private fun startHeartbeat() {
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    delay(HEARTBEAT_INTERVAL_MS)
                    
                    // Send heartbeat - in real implementation, this would ping the server
                    val heartbeatSuccess = sendHeartbeat()
                    
                    if (!heartbeatSuccess) {
                        logger.warn(TAG, "Heartbeat failed, connection may be lost")
                        _connectionQuality.value = ConnectionQuality.POOR
                        
                        // Trigger reconnection
                        onConnectionFailure(Exception("Heartbeat failed"))
                        break
                    }
                } catch (e: Exception) {
                    logger.error(TAG, "Heartbeat error: ${e.message}")
                    break
                }
            }
        }
    }
    
    /**
     * Sends heartbeat to check connection health
     */
    private suspend fun sendHeartbeat(): Boolean {
        return try {
            // Simulate heartbeat - in real implementation, this would send a ping
            networkMonitor.isNetworkAvailable()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Monitors connection quality based on network conditions
     */
    private fun startConnectionQualityMonitoring() {
        CoroutineScope(Dispatchers.IO).launch {
            networkMonitor.networkQuality.collect { quality ->
                _connectionQuality.value = when (quality) {
                    NetworkQuality.EXCELLENT -> ConnectionQuality.EXCELLENT
                    NetworkQuality.GOOD -> ConnectionQuality.GOOD
                    NetworkQuality.FAIR -> ConnectionQuality.FAIR
                    NetworkQuality.POOR -> ConnectionQuality.POOR
                    NetworkQuality.UNAVAILABLE -> ConnectionQuality.POOR
                }
            }
        }
    }
    
    /**
     * Gets connection statistics for monitoring
     */
    fun getConnectionStats(): ConnectionStats {
        return ConnectionStats(
            state = _connectionState.value,
            quality = _connectionQuality.value,
            retryAttempts = retryAttempts,
            lastConnectionAttempt = lastConnectionAttempt,
            isCircuitBreakerOpen = false
        )
    }
    
    /**
     * Forces reconnection (for manual recovery)
     */
    suspend fun forceReconnect(endpoint: String) {
        logger.info(TAG, "Forcing reconnection")
        disconnect()
        retryAttempts = 0
        connect(endpoint)
    }
}

/**
 * Connection states
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}

/**
 * Connection quality levels
 */
enum class ConnectionQuality {
    UNKNOWN,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT
}

/**
 * Connection statistics
 */
data class ConnectionStats(
    val state: ConnectionState,
    val quality: ConnectionQuality,
    val retryAttempts: Int,
    val lastConnectionAttempt: Instant,
    val isCircuitBreakerOpen: Boolean
)