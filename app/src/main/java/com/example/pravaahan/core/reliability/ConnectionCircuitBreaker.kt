package com.example.pravaahan.core.reliability

import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Circuit breaker pattern implementation for connection reliability
 * in railway real-time systems.
 */
@Singleton
class ConnectionCircuitBreaker @Inject constructor(
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "ConnectionCircuitBreaker"
        private const val FAILURE_THRESHOLD = 5
        private const val SUCCESS_THRESHOLD = 3
        private const val TIMEOUT_SECONDS = 60L
        private const val HALF_OPEN_MAX_CALLS = 3
    }
    
    private val mutex = Mutex()
    
    private var state = CircuitBreakerState.CLOSED
    private var failureCount = 0
    private var successCount = 0
    private var lastFailureTime = Instant.DISTANT_PAST
    private var halfOpenCallCount = 0
    
    /**
     * Checks if connection attempts are allowed
     */
    suspend fun canAttemptConnection(): Boolean = mutex.withLock {
        when (state) {
            CircuitBreakerState.CLOSED -> true
            CircuitBreakerState.OPEN -> {
                if (shouldTransitionToHalfOpen()) {
                    transitionToHalfOpen()
                    true
                } else {
                    false
                }
            }
            CircuitBreakerState.HALF_OPEN -> halfOpenCallCount < HALF_OPEN_MAX_CALLS
        }
    }
    
    /**
     * Records a successful connection
     */
    suspend fun recordSuccess() = mutex.withLock {
        when (state) {
            CircuitBreakerState.CLOSED -> {
                failureCount = 0
            }
            CircuitBreakerState.HALF_OPEN -> {
                successCount++
                if (successCount >= SUCCESS_THRESHOLD) {
                    transitionToClosed()
                }
            }
            CircuitBreakerState.OPEN -> {
                // Should not happen, but reset if it does
                logger.warn(TAG, "Unexpected success in OPEN state")
                transitionToClosed()
            }
        }
        
        logger.debug(TAG, "Recorded success. State: $state, failures: $failureCount, successes: $successCount")
    }
    
    /**
     * Records a connection failure
     */
    suspend fun recordFailure() = mutex.withLock {
        failureCount++
        lastFailureTime = Clock.System.now()
        
        when (state) {
            CircuitBreakerState.CLOSED -> {
                if (failureCount >= FAILURE_THRESHOLD) {
                    transitionToOpen()
                }
            }
            CircuitBreakerState.HALF_OPEN -> {
                transitionToOpen()
            }
            CircuitBreakerState.OPEN -> {
                // Already open, just update failure time
            }
        }
        
        logger.debug(TAG, "Recorded failure. State: $state, failures: $failureCount")
    }
    
    /**
     * Checks if circuit breaker should transition from OPEN to HALF_OPEN
     */
    private fun shouldTransitionToHalfOpen(): Boolean {
        val now = Clock.System.now()
        val timeSinceLastFailure = now - lastFailureTime
        return timeSinceLastFailure.inWholeSeconds >= TIMEOUT_SECONDS
    }
    
    /**
     * Transitions to CLOSED state
     */
    private fun transitionToClosed() {
        state = CircuitBreakerState.CLOSED
        failureCount = 0
        successCount = 0
        halfOpenCallCount = 0
        logger.info(TAG, "Circuit breaker transitioned to CLOSED")
    }
    
    /**
     * Transitions to OPEN state
     */
    private fun transitionToOpen() {
        state = CircuitBreakerState.OPEN
        successCount = 0
        halfOpenCallCount = 0
        logger.warn(TAG, "Circuit breaker transitioned to OPEN after $failureCount failures")
    }
    
    /**
     * Transitions to HALF_OPEN state
     */
    private fun transitionToHalfOpen() {
        state = CircuitBreakerState.HALF_OPEN
        halfOpenCallCount = 0
        successCount = 0
        logger.info(TAG, "Circuit breaker transitioned to HALF_OPEN")
    }
    
    /**
     * Increments half-open call count
     */
    suspend fun incrementHalfOpenCalls() = mutex.withLock {
        if (state == CircuitBreakerState.HALF_OPEN) {
            halfOpenCallCount++
        }
    }
    
    /**
     * Gets current circuit breaker state
     */
    suspend fun getCurrentState(): CircuitBreakerState = mutex.withLock {
        state
    }
    
    /**
     * Gets circuit breaker statistics
     */
    suspend fun getStats(): CircuitBreakerStats = mutex.withLock {
        CircuitBreakerStats(
            state = state,
            failureCount = failureCount,
            successCount = successCount,
            lastFailureTime = lastFailureTime,
            halfOpenCallCount = halfOpenCallCount,
            failureThreshold = FAILURE_THRESHOLD,
            successThreshold = SUCCESS_THRESHOLD,
            timeoutSeconds = TIMEOUT_SECONDS
        )
    }
    
    /**
     * Resets circuit breaker to CLOSED state
     */
    suspend fun reset() = mutex.withLock {
        transitionToClosed()
        logger.info(TAG, "Circuit breaker manually reset")
    }
    
    /**
     * Forces circuit breaker to OPEN state
     */
    suspend fun forceOpen() = mutex.withLock {
        transitionToOpen()
        logger.warn(TAG, "Circuit breaker manually forced to OPEN")
    }
}

/**
 * Circuit breaker states
 */
enum class CircuitBreakerState {
    CLOSED,     // Normal operation, requests allowed
    OPEN,       // Failure threshold exceeded, requests blocked
    HALF_OPEN   // Testing if service has recovered
}

/**
 * Circuit breaker statistics
 */
data class CircuitBreakerStats(
    val state: CircuitBreakerState,
    val failureCount: Int,
    val successCount: Int,
    val lastFailureTime: Instant,
    val halfOpenCallCount: Int,
    val failureThreshold: Int,
    val successThreshold: Int,
    val timeoutSeconds: Long
)