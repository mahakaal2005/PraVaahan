package com.example.pravaahan.core.resilience

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class CircuitBreakerBasicTest {
    
    private lateinit var circuitBreaker: RealTimeCircuitBreaker
    
    @BeforeEach
    fun setUp() {
        circuitBreaker = RealTimeCircuitBreaker()
    }
    
    @Test
    fun `circuit breaker starts in closed state`() = runTest {
        val metrics = circuitBreaker.metrics.value
        assertEquals(CircuitBreakerState.CLOSED, metrics.state)
        assertEquals(0, metrics.failureCount)
        assertTrue(circuitBreaker.canExecute())
    }
    
    @Test
    fun `successful operations work correctly`() = runTest {
        val result = circuitBreaker.execute { "success" }
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
    }
}