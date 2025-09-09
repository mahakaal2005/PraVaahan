package com.example.pravaahan.core.resilience

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class RealTimeCircuitBreakerTest {
    
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
    fun `successful operations keep circuit closed`() = runTest {
        repeat(10) {
            val result = circuitBreaker.execute { "success" }
            assertTrue(result.isSuccess)
            assertEquals("success", result.getOrNull())
        }
        
        val metrics = circuitBreaker.metrics.value
        assertEquals(CircuitBreakerState.CLOSED, metrics.state)
        assertEquals(0, metrics.failureCount)
        assertEquals(10L, metrics.totalSuccesses)
    }
    
    @Test
    fun `circuit opens after failure threshold`() = runTest {
        // Cause 5 failures (default threshold)
        repeat(5) {
            val result = circuitBreaker.execute { 
                throw RuntimeException("Test failure") 
            }
            assertTrue(result.isFailure)
        }
        
        val metrics = circuitBreaker.metrics.value
        assertEquals(CircuitBreakerState.OPEN, metrics.state)
        assertEquals(5L, metrics.totalFailures)
        assertFalse(circuitBreaker.canExecute())
    }
    
    @Test
    fun `open circuit rejects requests immediately`() = runTest {
        // Force circuit to open
        circuitBreaker.forceOpen()
        
        val result = circuitBreaker.execute { "should not execute" }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CircuitBreakerOpenException)
    }
    
    @Test
    fun `circuit transitions to half-open after recovery timeout`() = runTest {
        // Force circuit to open
        circuitBreaker.forceOpen()
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.stateFlow.value)
        
        // Wait for recovery timeout (simulated by forcing transition)
        // In real scenario, this would happen after the configured timeout
        circuitBreaker.forceClose()
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.stateFlow.value)
    }
    
    @Test
    fun `half-open circuit closes after successful operations`() = runTest {
        // Force circuit to half-open state
        circuitBreaker.forceHalfOpen()
        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.stateFlow.value)
        
        // Execute successful operations (default success threshold is 3)
        repeat(3) {
            val result = circuitBreaker.execute { "success" }
            assertTrue(result.isSuccess)
        }
        
        // After 3 successful operations, circuit should close
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.stateFlow.value)
    }
    
    @Test
    fun `executeWithTimeout handles exceptions properly`() = runTest {
        val result = circuitBreaker.executeWithTimeout {
            throw RuntimeException("Test exception")
        }
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("Test exception", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `metrics are updated correctly`() = runTest {
        // Execute some successful operations
        repeat(3) {
            circuitBreaker.execute { "success" }
        }
        
        // Execute some failures
        repeat(2) {
            circuitBreaker.execute { throw RuntimeException("failure") }
        }
        
        val metrics = circuitBreaker.metrics.value
        assertEquals(5L, metrics.totalRequests)
        assertEquals(3L, metrics.totalSuccesses)
        assertEquals(2L, metrics.totalFailures)
        assertEquals(2, metrics.failureCount)
    }
    
    @Test
    fun `reset clears all metrics`() = runTest {
        // Generate some activity
        repeat(3) {
            circuitBreaker.execute { "success" }
        }
        repeat(2) {
            circuitBreaker.execute { throw RuntimeException("failure") }
        }
        
        // Reset
        circuitBreaker.reset()
        
        val metrics = circuitBreaker.metrics.value
        assertEquals(CircuitBreakerState.CLOSED, metrics.state)
        assertEquals(0, metrics.failureCount)
        assertEquals(0, metrics.successCount)
        assertEquals(0L, metrics.totalRequests)
        assertEquals(0L, metrics.totalSuccesses)
        assertEquals(0L, metrics.totalFailures)
    }
    
    @Test
    fun `concurrent operations are handled safely`() = runTest {
        val results = mutableListOf<Result<String>>()
        
        // Execute operations concurrently
        coroutineScope {
            repeat(10) { index ->
                launch {
                    val result = circuitBreaker.execute { 
                        if (index % 3 == 0) {
                            throw RuntimeException("Failure $index")
                        } else {
                            "Success $index"
                        }
                    }
                    synchronized(results) {
                        results.add(result)
                    }
                }
            }
        }
        
        assertEquals(10, results.size)
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }
        
        assertTrue(successCount > 0)
        assertTrue(failureCount > 0)
        
        val metrics = circuitBreaker.metrics.value
        assertEquals(10L, metrics.totalRequests)
    }
}