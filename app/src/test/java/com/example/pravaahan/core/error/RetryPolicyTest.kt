package com.example.pravaahan.core.error

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for the RetryPolicy class
 */
class RetryPolicyTest {

    @Test
    fun `default retry policy has correct configuration`() {
        val policy = RetryPolicy()
        
        assertEquals(3, policy.maxAttempts)
        assertEquals(1000L, policy.initialDelayMs)
        assertEquals(30000L, policy.maxDelayMs)
        assertEquals(2.0, policy.backoffMultiplier)
        assertEquals(0.1, policy.jitterFactor)
        assertTrue(policy.retryableErrors.isNotEmpty())
    }

    @Test
    fun `critical operations policy has faster retry`() {
        val policy = RetryPolicy.CRITICAL_OPERATIONS
        
        assertEquals(5, policy.maxAttempts)
        assertEquals(500L, policy.initialDelayMs)
        assertEquals(10000L, policy.maxDelayMs)
        assertEquals(1.5, policy.backoffMultiplier)
    }

    @Test
    fun `no retry policy prevents retries`() {
        val policy = RetryPolicy.NO_RETRY
        
        assertEquals(1, policy.maxAttempts)
        assertTrue(policy.retryableErrors.isEmpty())
    }

    @Test
    fun `isRetryable returns true for network errors`() {
        val policy = RetryPolicy()
        val error = AppError.NetworkError.NoConnection
        
        assertTrue(policy.isRetryable(error))
    }

    @Test
    fun `isRetryable returns false for validation errors`() {
        val policy = RetryPolicy()
        val error = AppError.ValidationError.InvalidInput("field", "message")
        
        assertFalse(policy.isRetryable(error))
    }

    @Test
    fun `calculateDelay increases with attempt number`() {
        val policy = RetryPolicy(
            initialDelayMs = 1000L,
            backoffMultiplier = 2.0,
            jitterFactor = 0.0 // No jitter for predictable testing
        )
        
        val delay1 = policy.calculateDelay(1)
        val delay2 = policy.calculateDelay(2)
        val delay3 = policy.calculateDelay(3)
        
        assertEquals(1000L, delay1)
        assertEquals(2000L, delay2)
        assertEquals(4000L, delay3)
    }

    @Test
    fun `calculateDelay respects max delay limit`() {
        val policy = RetryPolicy(
            initialDelayMs = 10000L,
            maxDelayMs = 15000L,
            backoffMultiplier = 3.0,
            jitterFactor = 0.0
        )
        
        val delay = policy.calculateDelay(5) // Would be 10000 * 3^4 = 810000 without limit
        
        assertEquals(15000L, delay)
    }

    @Test
    fun `calculateDelay with jitter produces variable results`() {
        val policy = RetryPolicy(
            initialDelayMs = 1000L,
            backoffMultiplier = 1.0, // No exponential increase
            jitterFactor = 0.5
        )
        
        val delays = (1..10).map { policy.calculateDelay(1) }
        
        // With jitter, delays should vary
        assertTrue(delays.distinct().size > 1)
        // All delays should be within reasonable bounds
        assertTrue(delays.all { it in 500L..1500L })
    }
}