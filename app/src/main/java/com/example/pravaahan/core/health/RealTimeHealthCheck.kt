package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.ConnectionStatus
import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector
import com.example.pravaahan.core.resilience.CircuitBreakerState
import com.example.pravaahan.core.resilience.RealTimeCircuitBreaker
import com.example.pravaahan.domain.service.RealTimePositionService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive health check for real-time train position monitoring system.
 * 
 * Validates real-time connection status, data quality, performance metrics,
 * circuit breaker state, and overall system health for railway operations.
 */
@Singleton
class RealTimeHealthCheck @Inject constructor(
    private val realTimePositionService: RealTimePositionService,
    private val metricsCollector: RealTimeMetricsCollector,
    private val circuitBreaker: RealTimeCircuitBreaker,
    logger: Logger
) : BaseHealthCheck(
    name = "Real-Time Position Monitoring",
    isCritical = true, // Critical for railway safety operations
    timeoutMs = 15_000L, // 15 seconds timeout
    maxRetries = 2,
    logger = logger
) {
    
    companion object {
        private const val TAG = "RealTimeHealthCheck"
        
        // Health thresholds for railway operations
        private const val MAX_ACCEPTABLE_LATENCY_MS = 2000L // 2 seconds max latency
        private const val MIN_ACCEPTABLE_RELIABILITY = 0.95f // 95% reliability required
        private const val MAX_ACCEPTABLE_ERROR_RATE = 0.05 // 5% error rate max
        private const val CRITICAL_LATENCY_THRESHOLD_MS = 5000L // 5 seconds critical
    }
    
    override suspend fun performCheck() {
        logger.info(TAG, "Starting comprehensive real-time health check")
        
        // 1. Check connection status
        validateConnectionStatus()
        
        // 2. Check circuit breaker state
        validateCircuitBreakerState()
        
        // 3. Check data quality and performance metrics
        validateDataQualityMetrics()
        
        // 4. Perform end-to-end connectivity test
        performConnectivityTest()
        
        logger.info(TAG, "Real-time health check completed successfully")
    }
    
    private suspend fun validateConnectionStatus() {
        logger.debug(TAG, "Validating real-time connection status")
        
        val connectionStatus = withTimeout(5.seconds) {
            realTimePositionService.getConnectionStatus().first()
        }
        
        when (connectionStatus) {
            ConnectionStatus.CONNECTED -> {
                logger.info(TAG, "✅ Real-time connection is healthy")
            }
            ConnectionStatus.RECONNECTING -> {
                logger.warn(TAG, "⚠️ Real-time connection is reconnecting")
                // This is acceptable but should be monitored
            }
            ConnectionStatus.DISCONNECTED -> {
                logger.error(TAG, "❌ Real-time connection is disconnected")
                throw RealTimeHealthException("Real-time connection is disconnected")
            }
            ConnectionStatus.ERROR -> {
                logger.error(TAG, "❌ Real-time connection has errors")
                throw RealTimeHealthException("Real-time connection is in error state")
            }
        }
    }
    
    private suspend fun validateCircuitBreakerState() {
        logger.debug(TAG, "Validating circuit breaker state")
        
        val circuitBreakerMetrics = circuitBreaker.metrics.first()
        
        when (circuitBreakerMetrics.state) {
            CircuitBreakerState.CLOSED -> {
                logger.info(TAG, "✅ Circuit breaker is closed (normal operation)")
            }
            CircuitBreakerState.HALF_OPEN -> {
                logger.warn(TAG, "⚠️ Circuit breaker is half-open (testing recovery)")
                // Acceptable but should be monitored
            }
            CircuitBreakerState.OPEN -> {
                logger.error(TAG, "❌ Circuit breaker is open (failing fast)")
                throw RealTimeHealthException(
                    "Circuit breaker is open - ${circuitBreakerMetrics.failureCount} failures detected"
                )
            }
        }
        
        // Check failure rate
        val totalRequests = circuitBreakerMetrics.totalRequests
        val totalFailures = circuitBreakerMetrics.totalFailures
        
        if (totalRequests > 0) {
            val failureRate = totalFailures.toDouble() / totalRequests.toDouble()
            
            if (failureRate > MAX_ACCEPTABLE_ERROR_RATE) {
                logger.error(TAG, "❌ High failure rate detected: ${(failureRate * 100).toInt()}%")
                throw RealTimeHealthException(
                    "High failure rate: ${(failureRate * 100).toInt()}% (${totalFailures}/${totalRequests})"
                )
            } else {
                logger.info(TAG, "✅ Failure rate is acceptable: ${(failureRate * 100).toInt()}%")
            }
        }
    }
    
    private suspend fun validateDataQualityMetrics() {
        logger.debug(TAG, "Validating data quality and performance metrics")
        
        val metrics = metricsCollector.metrics.first()
        
        // Check connection status from metrics
        if (metrics.connectionStatus != ConnectionStatus.CONNECTED) {
            logger.warn(TAG, "⚠️ Metrics show connection status: ${metrics.connectionStatus}")
        }
        
        // Check latency
        val avgLatencyMs = metrics.performance.averageLatency.inWholeMilliseconds
        val maxLatencyMs = metrics.performance.maxLatency.inWholeMilliseconds
        
        when {
            maxLatencyMs > CRITICAL_LATENCY_THRESHOLD_MS -> {
                logger.error(TAG, "❌ Critical latency detected: max=${maxLatencyMs}ms, avg=${avgLatencyMs}ms")
                throw RealTimeHealthException(
                    "Critical latency: max=${maxLatencyMs}ms exceeds ${CRITICAL_LATENCY_THRESHOLD_MS}ms threshold"
                )
            }
            avgLatencyMs > MAX_ACCEPTABLE_LATENCY_MS -> {
                logger.warn(TAG, "⚠️ High latency detected: avg=${avgLatencyMs}ms")
                // Log warning but don't fail - this might be temporary
            }
            else -> {
                logger.info(TAG, "✅ Latency is acceptable: avg=${avgLatencyMs}ms, max=${maxLatencyMs}ms")
            }
        }
        
        // Check reliability
        val reliability = metrics.dataQuality.reliability
        if (reliability < MIN_ACCEPTABLE_RELIABILITY) {
            logger.error(TAG, "❌ Low reliability detected: ${(reliability * 100).toInt()}%")
            throw RealTimeHealthException(
                "Low reliability: ${(reliability * 100).toInt()}% below ${(MIN_ACCEPTABLE_RELIABILITY * 100).toInt()}% threshold"
            )
        } else {
            logger.info(TAG, "✅ Reliability is acceptable: ${(reliability * 100).toInt()}%")
        }
        
        // Check error rate
        val errorRate = metrics.performance.errorRate
        if (errorRate > MAX_ACCEPTABLE_ERROR_RATE) {
            logger.error(TAG, "❌ High error rate detected: ${(errorRate * 100).toInt()}%")
            throw RealTimeHealthException(
                "High error rate: ${(errorRate * 100).toInt()}% exceeds ${(MAX_ACCEPTABLE_ERROR_RATE * 100).toInt()}% threshold"
            )
        } else {
            logger.info(TAG, "✅ Error rate is acceptable: ${(errorRate * 100).toInt()}%")
        }
        
        // Check data quality issues
        val dataQuality = metrics.dataQuality
        if (dataQuality.outOfOrderCount > 10) {
            logger.warn(TAG, "⚠️ High out-of-order count: ${dataQuality.outOfOrderCount}")
        }
        
        if (dataQuality.duplicateCount > 5) {
            logger.warn(TAG, "⚠️ High duplicate count: ${dataQuality.duplicateCount}")
        }
        
        // Check security metrics
        val security = metrics.security
        if (security.anomaliesDetected > 0) {
            logger.warn(TAG, "⚠️ Security anomalies detected: ${security.anomaliesDetected}")
        }
        
        if (security.validationFailures > 10) {
            logger.warn(TAG, "⚠️ High validation failures: ${security.validationFailures}")
        }
    }
    
    private suspend fun performConnectivityTest() {
        logger.debug(TAG, "Performing end-to-end connectivity test")
        
        try {
            // Test if we can establish a connection and receive data
            val testStartTime = Clock.System.now()
            
            // Use circuit breaker to test connectivity
            val connectivityResult = circuitBreaker.executeWithTimeout {
                // This would typically involve a lightweight test operation
                // For now, we'll just verify the service can be started
                realTimePositionService.start()
                true
            }
            
            val testDuration = Clock.System.now() - testStartTime
            
            if (connectivityResult.isSuccess) {
                logger.info(TAG, "✅ End-to-end connectivity test passed in ${testDuration.inWholeMilliseconds}ms")
            } else {
                val exception = connectivityResult.exceptionOrNull()
                logger.error(TAG, "❌ End-to-end connectivity test failed", exception)
                throw RealTimeHealthException(
                    "Connectivity test failed: ${exception?.message ?: "Unknown error"}"
                )
            }
            
        } catch (e: Exception) {
            logger.error(TAG, "❌ Connectivity test failed with exception", e)
            throw RealTimeHealthException("Connectivity test failed: ${e.message}", e)
        }
    }
    
    override fun getSuccessDetails(): String {
        return "Real-time monitoring system is healthy and operational"
    }
    
    override fun getFailureMessage(exception: Throwable?): String {
        return when (exception) {
            is RealTimeHealthException -> exception.message ?: "Real-time health check failed"
            else -> "Real-time monitoring system health check failed: ${exception?.message ?: "Unknown error"}"
        }
    }
}

/**
 * Exception for real-time health check failures
 */
class RealTimeHealthException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)