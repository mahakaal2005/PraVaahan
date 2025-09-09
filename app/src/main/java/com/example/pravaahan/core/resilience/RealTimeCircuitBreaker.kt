package com.example.pravaahan.core.resilience

import kotlinx.coroutines.delay
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
import kotlin.time.Duration.Companion.seconds

/**
 * Circuit breaker states for real-time connections
 */
enum class CircuitBreakerState {
    CLOSED,     // Normal operation
    OPEN,       // Failing fast, not allowing requests
    HALF_OPEN   // Testing if service has recovered
}

/**
 * Circuit breaker configuration for real-time connections
 */
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,              // Number of failures before opening
    val recoveryTimeout: Duration = 30.seconds, // Time to wait before trying again
    val successThreshold: Int = 3,              // Successes needed to close from half-open
    val timeout: Duration = 10.seconds          // Request timeout
)

/**
 * Circuit breaker metrics for monitoring
 */
data class CircuitBreakerMetrics(
    val state: CircuitBreakerState,
    val failureCount: Int,
    val successCount: Int,
    val lastFailureTime: Instant?,
    val lastSuccessTime: Instant?,
    val totalRequests: Long,
    val totalFailures: Long,
    val totalSuccesses: Long
)

/**
 * Circuit breaker for real-time connections with resilience patterns
 * 
 * Implements the circuit breaker pattern to prevent cascading failures
 * and provide graceful degradation for real-time connections.
 */
@Singleton
class RealTimeCircuitBreaker @Inject constructor() {
    
    companion object {
        private const val TAG = "RealTimeCircuitBreaker"
    }
    
    private val config = CircuitBreakerConfig()
    private val mutex = Mutex()
    
    private var state = CircuitBreakerState.CLOSED
    private var failureCount = 0
    private var successCount = 0
    private var lastFailureTime: Instant? = null
    private var lastSuccessTime: Instant? = null
    private var totalRequests = 0L
    private var totalFailures = 0L
    private var totalSuccesses = 0L
    
    private val _metrics = MutableStateFlow(getCurrentMetrics())
    val metrics: StateFlow<CircuitBreakerMetrics> = _metrics.asStateFlow()
    
    private val _stateFlow = MutableStateFlow(CircuitBreakerState.CLOSED)
    val stateFlow: StateFlow<CircuitBreakerState> = _stateFlow.asStateFlow()
    
    /**
     * Execute a suspending operation with circuit breaker protection
     */
    suspend fun <T> execute(operation: suspend () -> T): Result<T> {
        return mutex.withLock {
            totalRequests++
            
            when (state) {
                CircuitBreakerState.OPEN -> {
                    if (shouldAttemptReset()) {
                        transitionToHalfOpen()
                    } else {
                        return@withLock Result.failure(CircuitBreakerOpenException("Circuit breaker is OPEN"))
                    }
                }
                CircuitBreakerState.HALF_OPEN -> {
                    // Allow limited requests in half-open state
                }
                CircuitBreakerState.CLOSED -> {
                    // Normal operation
                }
            }
            
            try {
                val result = operation()
                onSuccess()
                Result.success(result)
            } catch (exception: Exception) {
                onFailure(exception)
                Result.failure(exception)
            }
        }
    }
    
    /**
     * Execute operation with timeout protection
     */
    suspend fun <T> executeWithTimeout(operation: suspend () -> T): Result<T> {
        return execute {
            kotlinx.coroutines.withTimeout(config.timeout) {
                operation()
            }
        }
    }
    
    /**
     * Check if circuit breaker allows requests
     */
    fun canExecute(): Boolean {
        return when (state) {
            CircuitBreakerState.CLOSED -> true
            CircuitBreakerState.HALF_OPEN -> true
            CircuitBreakerState.OPEN -> shouldAttemptReset()
        }
    }
    
    /**
     * Force circuit breaker to open (for testing or manual intervention)
     */
    suspend fun forceOpen() {
        mutex.withLock {
            transitionToOpen()
        }
    }
    
    /**
     * Force circuit breaker to close (for testing or manual intervention)
     */
    suspend fun forceClose() {
        mutex.withLock {
            transitionToClosed()
        }
    }
    
    /**
     * Force circuit breaker to half-open (for testing)
     */
    suspend fun forceHalfOpen() {
        mutex.withLock {
            transitionToHalfOpen()
        }
    }
    
    /**
     * Reset circuit breaker metrics
     */
    suspend fun reset() {
        mutex.withLock {
            state = CircuitBreakerState.CLOSED
            failureCount = 0
            successCount = 0
            lastFailureTime = null
            lastSuccessTime = null
            totalRequests = 0L
            totalFailures = 0L
            totalSuccesses = 0L
            updateMetrics()
        }
    }
    
    private fun onSuccess() {
        val now = Clock.System.now()
        lastSuccessTime = now
        totalSuccesses++
        
        when (state) {
            CircuitBreakerState.HALF_OPEN -> {
                successCount++
                if (successCount >= config.successThreshold) {
                    transitionToClosed()
                }
            }
            CircuitBreakerState.CLOSED -> {
                // Reset failure count on success
                failureCount = 0
            }
            CircuitBreakerState.OPEN -> {
                // Should not happen, but reset if it does
                transitionToClosed()
            }
        }
        
        updateMetrics()
    }
    
    private fun onFailure(exception: Exception) {
        val now = Clock.System.now()
        lastFailureTime = now
        totalFailures++
        
        when (state) {
            CircuitBreakerState.CLOSED -> {
                failureCount++
                if (failureCount >= config.failureThreshold) {
                    transitionToOpen()
                }
            }
            CircuitBreakerState.HALF_OPEN -> {
                // Any failure in half-open state should open the circuit
                transitionToOpen()
            }
            CircuitBreakerState.OPEN -> {
                // Already open, just update metrics
            }
        }
        
        updateMetrics()
    }
    
    private fun shouldAttemptReset(): Boolean {
        val lastFailure = lastFailureTime ?: return true
        val now = Clock.System.now()
        return (now - lastFailure) >= config.recoveryTimeout
    }
    
    private fun transitionToClosed() {
        state = CircuitBreakerState.CLOSED
        failureCount = 0
        successCount = 0
        _stateFlow.value = state
        updateMetrics()
    }
    
    private fun transitionToOpen() {
        state = CircuitBreakerState.OPEN
        successCount = 0
        lastFailureTime = Clock.System.now()
        _stateFlow.value = state
        updateMetrics()
    }
    
    private fun transitionToHalfOpen() {
        state = CircuitBreakerState.HALF_OPEN
        successCount = 0
        _stateFlow.value = state
        updateMetrics()
    }
    
    private fun updateMetrics() {
        _metrics.value = getCurrentMetrics()
    }
    
    private fun getCurrentMetrics(): CircuitBreakerMetrics {
        return CircuitBreakerMetrics(
            state = state,
            failureCount = failureCount,
            successCount = successCount,
            lastFailureTime = lastFailureTime,
            lastSuccessTime = lastSuccessTime,
            totalRequests = totalRequests,
            totalFailures = totalFailures,
            totalSuccesses = totalSuccesses
        )
    }
}

/**
 * Exception thrown when circuit breaker is open
 */
class CircuitBreakerOpenException(message: String) : Exception(message)

/**
 * Exception thrown when operation times out
 */
class CircuitBreakerTimeoutException(message: String) : Exception(message)