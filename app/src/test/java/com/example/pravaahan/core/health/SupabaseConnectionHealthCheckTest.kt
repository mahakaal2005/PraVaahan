package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SupabaseConnectionHealthCheckTest {
    
    private lateinit var mockSupabaseClient: SupabaseClient
    private lateinit var mockPostgrest: Postgrest
    private lateinit var mockLogger: Logger
    private lateinit var healthCheck: SupabaseConnectionHealthCheck
    
    @BeforeEach
    fun setup() {
        mockSupabaseClient = mockk()
        mockPostgrest = mockk()
        mockLogger = mockk(relaxed = true)
        
        every { mockSupabaseClient.postgrest } returns mockPostgrest
        
        healthCheck = SupabaseConnectionHealthCheck(mockSupabaseClient, mockLogger)
    }
    
    @Test
    fun `when supabase connection succeeds, should return success result`() = runTest {
        // Arrange
        coEvery { mockPostgrest.rpc(any(), any()) } returns mockk()
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Success>(result)
        assertEquals("Supabase Connection", result.checkName)
        assertEquals("Successfully connected to Supabase backend", result.details)
        assertTrue(result.durationMs >= 0)
        verify { mockLogger.debug("SupabaseConnectionHealthCheck", "Supabase connection test successful") }
    }
    
    @Test
    fun `when supabase connection fails with unknown host, should return appropriate failure`() = runTest {
        // Arrange
        val exception = UnknownHostException("Unable to resolve host")
        coEvery { mockPostgrest.rpc(any(), any()) } throws exception
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Failure>(result)
        assertEquals("Supabase Connection", result.checkName)
        assertEquals("Cannot reach Supabase servers. Check internet connection.", result.message)
        assertTrue(result.durationMs >= 0)
        verify { mockLogger.error("SupabaseConnectionHealthCheck", "Supabase connection test failed", any()) }
    }
    
    @Test
    fun `when supabase connection times out, should return timeout failure`() = runTest {
        // Arrange
        val exception = SocketTimeoutException("Connection timed out")
        coEvery { mockPostgrest.rpc(any(), any()) } throws exception
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Failure>(result)
        assertEquals("Supabase Connection", result.checkName)
        assertEquals("Connection to Supabase timed out. Check network connectivity.", result.message)
        verify { mockLogger.error("SupabaseConnectionHealthCheck", "Supabase connection test failed", any()) }
    }
    
    @Test
    fun `when supabase connection has SSL error, should return SSL failure`() = runTest {
        // Arrange
        val exception = SSLException("SSL handshake failed")
        coEvery { mockPostgrest.rpc(any(), any()) } throws exception
        
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
        coEvery { mockPostgrest.rpc(any(), any()) } throws exception
        
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
        coEvery { mockPostgrest.rpc(any(), any()) } throws exception
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertIs<HealthCheckResult.Failure>(result)
        assertEquals(2, result.retryAttempt) // Should have retried maxRetries times
        
        // Verify multiple attempts were logged
        verify(atLeast = 3) { mockLogger.warn("BaseHealthCheck", any(), any()) }
    }
}