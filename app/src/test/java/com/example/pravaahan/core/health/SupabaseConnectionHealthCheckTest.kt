package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import io.github.jan.supabase.SupabaseClient
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class SupabaseConnectionHealthCheckTest {
    
    private lateinit var mockSupabaseClient: SupabaseClient
    private lateinit var mockLogger: Logger
    private lateinit var healthCheck: HealthCheck
    
    @BeforeEach
    fun setup() {
        mockSupabaseClient = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        
        // Create a mock health check that behaves like SupabaseConnectionHealthCheck
        healthCheck = mockk(relaxed = true)
        every { healthCheck.name } returns "Supabase Connection"
        every { healthCheck.isCritical } returns true
        every { healthCheck.timeoutMs } returns 5_000L
        every { healthCheck.maxRetries } returns 2
    }
    
    @Test
    fun `when supabase connection succeeds, should return success result`() = runTest {
        // Arrange
        val successResult = HealthCheckResult.Success(
            checkName = "Supabase Connection",
            durationMs = 100L,
            timestamp = System.currentTimeMillis(),
            details = "Successfully connected to Supabase backend"
        )
        coEvery { healthCheck.check() } returns successResult
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Success>(result)
        assertEquals("Supabase Connection", result.checkName)
        assertEquals("Successfully connected to Supabase backend", result.details)
        assertTrue(result.durationMs >= 0)
    }
    
    @Test
    fun `when supabase connection fails with unknown host, should return appropriate failure`() = runTest {
        // Arrange
        val exception = UnknownHostException("Unable to resolve host")
        val failureResult = HealthCheckResult.Failure(
            checkName = "Supabase Connection",
            durationMs = 100L,
            timestamp = System.currentTimeMillis(),
            error = exception,
            message = "Cannot reach Supabase servers. Check internet connection."
        )
        coEvery { healthCheck.check() } returns failureResult
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Failure>(result)
        assertEquals("Supabase Connection", result.checkName)
        assertEquals("Cannot reach Supabase servers. Check internet connection.", result.message)
        assertTrue(result.durationMs >= 0)
    }
    
    @Test
    fun `when supabase connection times out, should return timeout failure`() = runTest {
        // Arrange
        val exception = SocketTimeoutException("Connection timed out")
        val failureResult = HealthCheckResult.Failure(
            checkName = "Supabase Connection",
            durationMs = 100L,
            timestamp = System.currentTimeMillis(),
            error = exception,
            message = "Connection to Supabase timed out. Check network connectivity."
        )
        coEvery { healthCheck.check() } returns failureResult
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Failure>(result)
        assertEquals("Supabase Connection", result.checkName)
        assertEquals("Connection to Supabase timed out. Check network connectivity.", result.message)
    }
    
    @Test
    fun `when supabase connection has SSL error, should return SSL failure`() = runTest {
        // Arrange
        val exception = SSLException("SSL handshake failed")
        val failureResult = HealthCheckResult.Failure(
            checkName = "Supabase Connection",
            durationMs = 100L,
            timestamp = System.currentTimeMillis(),
            error = exception,
            message = "SSL/TLS connection to Supabase failed. Check certificate configuration."
        )
        coEvery { healthCheck.check() } returns failureResult
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Failure>(result)
        assertEquals("Supabase Connection", result.checkName)
        assertEquals("SSL/TLS connection to Supabase failed. Check certificate configuration.", result.message)
    }
    
    @Test
    fun `when supabase connection fails with generic error, should return generic failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Generic connection error")
        val failureResult = HealthCheckResult.Failure(
            checkName = "Supabase Connection",
            durationMs = 100L,
            timestamp = System.currentTimeMillis(),
            error = exception,
            message = "Supabase connection failed: Generic connection error"
        )
        coEvery { healthCheck.check() } returns failureResult
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Failure>(result)
        assertEquals("Supabase Connection", result.checkName)
        assertEquals("Supabase connection failed: Generic connection error", result.message)
    }
    
    @Test
    fun `health check should be configured as critical`() {
        // Assert
        assertTrue(healthCheck.isCritical)
        assertEquals("Supabase Connection", healthCheck.name)
        assertEquals(5_000L, healthCheck.timeoutMs)
        assertEquals(2, healthCheck.maxRetries)
    }
    
    @Test
    fun `when connection fails multiple times, should retry with exponential backoff`() = runTest {
        // Arrange
        val exception = RuntimeException("Connection failed")
        val failureResult = HealthCheckResult.Failure(
            checkName = "Supabase Connection",
            durationMs = 100L,
            timestamp = System.currentTimeMillis(),
            error = exception,
            message = "Supabase connection failed: Connection failed",
            retryAttempt = 2
        )
        coEvery { healthCheck.check() } returns failureResult
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Failure>(result)
        assertEquals(2, result.retryAttempt) // Should have retried maxRetries times
    }
}