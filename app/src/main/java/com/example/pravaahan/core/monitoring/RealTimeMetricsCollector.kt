package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Connection status for real-time monitoring
 */
enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * Data quality metrics for real-time position updates
 */
data class DataQuality(
    val latency: Duration,
    val accuracy: Double,
    val freshness: Duration,
    val reliability: Float,
    val outOfOrderCount: Int = 0,
    val duplicateCount: Int = 0,
    val invalidDataCount: Int = 0
) {
    companion object {
        fun default() = DataQuality(
            latency = 0.milliseconds,
            accuracy = 1.0,
            freshness = 0.milliseconds,
            reliability = 1.0f
        )
    }
}

/**
 * Performance metrics for real-time operations
 */
data class PerformanceMetrics(
    val averageLatency: Duration,
    val maxLatency: Duration,
    val minLatency: Duration,
    val throughput: Double, // messages per second
    val errorRate: Double,  // percentage
    val uptime: Duration,
    val memoryUsage: Long,  // bytes
    val connectionCount: Int
)

/**
 * Security metrics for real-time operations
 */
data class SecurityMetrics(
    val validationFailures: Long,
    val anomaliesDetected: Long,
    val suspiciousPatterns: Long,
    val lastSecurityEvent: Instant?
)

/**
 * Comprehensive real-time metrics
 */
data class RealTimeMetrics(
    val connectionStatus: ConnectionStatus,
    val dataQuality: DataQuality,
    val performance: PerformanceMetrics,
    val security: SecurityMetrics,
    val lastUpdated: Instant
)

/**
 * Comprehensive metrics collector for real-time train position monitoring
 * 
 * Collects and analyzes performance, security, and data quality metrics
 * for real-time operations with railway-specific considerations.
 */
@Singleton
class RealTimeMetricsCollector @Inject constructor(
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "RealTimeMetricsCollector"
        private const val METRICS_WINDOW_SIZE = 100
        private const val HIGH_LATENCY_THRESHOLD_MS = 1000L
        private const val CRITICAL_LATENCY_THRESHOLD_MS = 5000L
        private const val MAX_ACCEPTABLE_ERROR_RATE = 0.05 // 5%
    }
    
    private val mutex = Mutex()
    
    // Connection tracking
    private var connectionStatus = ConnectionStatus.DISCONNECTED
    private var connectionStartTime: Instant? = null
    private var lastConnectionTime: Instant? = null
    
    // Performance tracking
    private val latencyHistory = mutableListOf<Duration>()
    private var totalMessages = 0L
    private var totalErrors = 0L
    private var windowStartTime = Clock.System.now()
    
    // Data quality tracking
    private var outOfOrderCount = 0
    private var duplicateCount = 0
    private val positionHistory = mutableMapOf<String, TrainPosition>() // trainId -> last position
    
    // Security tracking
    private var validationFailures = 0L
    private var anomaliesDetected = 0L
    private var suspiciousPatterns = 0L
    private var lastSecurityEvent: Instant? = null
    
    // Memory tracking
    private var peakMemoryUsage = 0L
    private var currentMemoryUsage = 0L
    
    private val _metrics = MutableStateFlow(getCurrentMetrics())
    val metrics: StateFlow<RealTimeMetrics> = _metrics.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatusFlow: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    /**
     * Record connection established
     */
    suspend fun recordConnectionEstablished() {
        mutex.withLock {
            val now = Clock.System.now()
            connectionStatus = ConnectionStatus.CONNECTED
            connectionStartTime = now
            lastConnectionTime = now
            
            logger.info(TAG, "Real-time connection established")
            updateMetrics()
        }
    }
    
    /**
     * Record connection lost
     */
    suspend fun recordConnectionLost(reason: String) {
        mutex.withLock {
            connectionStatus = ConnectionStatus.DISCONNECTED
            
            logger.warn(TAG, "Real-time connection lost: $reason")
            updateMetrics()
        }
    }
    
    /**
     * Record reconnection attempt
     */
    suspend fun recordReconnectionAttempt() {
        mutex.withLock {
            connectionStatus = ConnectionStatus.RECONNECTING
            
            logger.info(TAG, "Attempting real-time reconnection")
            updateMetrics()
        }
    }
    
    /**
     * Record connection error
     */
    suspend fun recordConnectionError(error: Throwable) {
        mutex.withLock {
            connectionStatus = ConnectionStatus.ERROR
            totalErrors++
            
            logger.error(TAG, "Real-time connection error", error)
            updateMetrics()
        }
    }
    
    /**
     * Record successful message received
     */
    suspend fun recordMessageReceived(position: TrainPosition, receiveTime: Instant = Clock.System.now()) {
        mutex.withLock {
            totalMessages++
            
            // Calculate latency
            val latency = receiveTime - position.timestamp
            recordLatency(latency)
            
            // Check for data quality issues
            checkDataQuality(position)
            
            // Log performance metrics if needed
            if (latency.inWholeMilliseconds > HIGH_LATENCY_THRESHOLD_MS) {
                logger.warn(TAG, "High latency detected: ${latency.inWholeMilliseconds}ms for train ${position.trainId}")
            }
            
            if (latency.inWholeMilliseconds > CRITICAL_LATENCY_THRESHOLD_MS) {
                logger.error(TAG, "Critical latency detected: ${latency.inWholeMilliseconds}ms for train ${position.trainId}")
            }
            
            updateMetrics()
        }
    }
    
    /**
     * Record validation failure
     */
    suspend fun recordValidationFailure(trainId: String, reason: String) {
        mutex.withLock {
            validationFailures++
            lastSecurityEvent = Clock.System.now()
            
            logger.warn(TAG, "Validation failure for train $trainId: $reason")
            updateMetrics()
        }
    }
    
    /**
     * Record security anomaly
     */
    suspend fun recordSecurityAnomaly(trainId: String, anomalyType: String, details: String) {
        mutex.withLock {
            anomaliesDetected++
            lastSecurityEvent = Clock.System.now()
            
            logger.error(TAG, "Security anomaly detected for train $trainId: $anomalyType - $details")
            updateMetrics()
        }
    }
    
    /**
     * Record suspicious pattern
     */
    suspend fun recordSuspiciousPattern(pattern: String, details: String) {
        mutex.withLock {
            suspiciousPatterns++
            lastSecurityEvent = Clock.System.now()
            
            logger.warn(TAG, "Suspicious pattern detected: $pattern - $details")
            updateMetrics()
        }
    }
    
    /**
     * Update memory usage metrics
     */
    suspend fun recordMemoryUsage(currentUsage: Long) {
        mutex.withLock {
            currentMemoryUsage = currentUsage
            if (currentUsage > peakMemoryUsage) {
                peakMemoryUsage = currentUsage
            }
            
            // Log memory warnings
            val memoryMB = currentUsage / (1024 * 1024)
            if (memoryMB > 100) { // More than 100MB
                logger.warn(TAG, "High memory usage detected: ${memoryMB}MB")
            }
            
            updateMetrics()
        }
    }
    
    /**
     * Get current error rate
     */
    fun getCurrentErrorRate(): Double {
        return if (totalMessages > 0) {
            totalErrors.toDouble() / totalMessages.toDouble()
        } else {
            0.0
        }
    }
    
    /**
     * Check if system is healthy based on metrics
     */
    fun isSystemHealthy(): Boolean {
        val errorRate = getCurrentErrorRate()
        val avgLatency = getAverageLatency()
        
        return connectionStatus == ConnectionStatus.CONNECTED &&
                errorRate <= MAX_ACCEPTABLE_ERROR_RATE &&
                avgLatency.inWholeMilliseconds <= HIGH_LATENCY_THRESHOLD_MS
    }
    
    /**
     * Reset metrics (for testing or maintenance)
     */
    suspend fun resetMetrics() {
        mutex.withLock {
            latencyHistory.clear()
            totalMessages = 0L
            totalErrors = 0L
            outOfOrderCount = 0
            duplicateCount = 0
            positionHistory.clear()
            validationFailures = 0L
            anomaliesDetected = 0L
            suspiciousPatterns = 0L
            lastSecurityEvent = null
            peakMemoryUsage = 0L
            currentMemoryUsage = 0L
            windowStartTime = Clock.System.now()
            
            logger.info(TAG, "Metrics reset")
            updateMetrics()
        }
    }
    
    private fun recordLatency(latency: Duration) {
        latencyHistory.add(latency)
        
        // Keep only recent latency measurements
        if (latencyHistory.size > METRICS_WINDOW_SIZE) {
            latencyHistory.removeAt(0)
        }
    }
    
    private fun checkDataQuality(position: TrainPosition) {
        val trainId = position.trainId
        val lastPosition = positionHistory[trainId]
        
        if (lastPosition != null) {
            // Check for out-of-order updates
            if (position.timestamp < lastPosition.timestamp) {
                outOfOrderCount++
                logger.debug(TAG, "Out-of-order update detected for train $trainId")
            }
            
            // Check for duplicate updates
            if (position.timestamp == lastPosition.timestamp &&
                position.latitude == lastPosition.latitude &&
                position.longitude == lastPosition.longitude) {
                duplicateCount++
                logger.debug(TAG, "Duplicate update detected for train $trainId")
            }
        }
        
        positionHistory[trainId] = position
    }
    
    private fun getAverageLatency(): Duration {
        return if (latencyHistory.isNotEmpty()) {
            val totalMs = latencyHistory.sumOf { it.inWholeMilliseconds }
            (totalMs / latencyHistory.size).milliseconds
        } else {
            0.milliseconds
        }
    }
    
    private fun getMaxLatency(): Duration {
        return latencyHistory.maxOfOrNull { it } ?: 0.milliseconds
    }
    
    private fun getMinLatency(): Duration {
        return latencyHistory.minOfOrNull { it } ?: 0.milliseconds
    }
    
    private fun getThroughput(): Double {
        val now = Clock.System.now()
        val windowDuration = now - windowStartTime
        
        return if (windowDuration.inWholeSeconds > 0) {
            totalMessages.toDouble() / windowDuration.inWholeSeconds.toDouble()
        } else {
            0.0
        }
    }
    
    private fun getUptime(): Duration {
        val startTime = connectionStartTime
        return if (startTime != null && connectionStatus == ConnectionStatus.CONNECTED) {
            Clock.System.now() - startTime
        } else {
            0.seconds
        }
    }
    
    private fun updateMetrics() {
        _connectionStatus.value = connectionStatus
        _metrics.value = getCurrentMetrics()
    }
    
    private fun getCurrentMetrics(): RealTimeMetrics {
        val avgLatency = getAverageLatency()
        
        return RealTimeMetrics(
            connectionStatus = connectionStatus,
            dataQuality = DataQuality(
                latency = avgLatency,
                accuracy = 1.0, // Could be calculated based on GPS accuracy
                freshness = avgLatency,
                reliability = if (totalMessages > 0) {
                    1.0f - (totalErrors.toFloat() / totalMessages.toFloat())
                } else 1.0f,
                outOfOrderCount = outOfOrderCount,
                duplicateCount = duplicateCount
            ),
            performance = PerformanceMetrics(
                averageLatency = avgLatency,
                maxLatency = getMaxLatency(),
                minLatency = getMinLatency(),
                throughput = getThroughput(),
                errorRate = getCurrentErrorRate(),
                uptime = getUptime(),
                memoryUsage = currentMemoryUsage,
                connectionCount = if (connectionStatus == ConnectionStatus.CONNECTED) 1 else 0
            ),
            security = SecurityMetrics(
                validationFailures = validationFailures,
                anomaliesDetected = anomaliesDetected,
                suspiciousPatterns = suspiciousPatterns,
                lastSecurityEvent = lastSecurityEvent
            ),
            lastUpdated = Clock.System.now()
        )
    }
}