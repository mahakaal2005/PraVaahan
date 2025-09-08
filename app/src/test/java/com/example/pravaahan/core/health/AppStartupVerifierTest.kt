package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppStartupVerifierTest {
    
    private lateinit var mockLogger: Logger
    private lateinit var mockSupabaseHealthCheck: SupabaseConnectionHealthCheck
    private lateinit var mockDatabaseHealthCheck: DatabaseAccessHealthCheck
    private lateinit var mockRealtimeHealthCheck: RealtimeConnectionHealthCheck
    private lateinit var mockNavigationHealthCheck: NavigationHealthCheck
    private lateinit var mockDependencyInjectionHealthCheck: DependencyInjectionHealthCheck
    private lateinit var appStartupVerifier: AppStartupVerifier
    
    @BeforeEach
    fun setup() {
        mockLogger = mockk(relaxed = true)
        mockSupabaseHealthCheck = mockk()
        mockDatabaseHealthCheck = mockk()
        mockRealtimeHealthCheck = mockk()
        mockNavigationHealthCheck = mockk()
        mockDependencyInjectionHealthCheck = mockk()
        
        // Setup mock properties
        every { mockSupabaseHealthCheck.name } returns "Supabase Connection"
        every { mockSupabaseHealthCheck.isCritical } returns true
        every { mockDatabaseHealthCheck.name } returns "Database Access"
        every { mockDatabaseHealthCheck.isCritical } returns true
        every { mockRealtimeHealthCheck.name } returns "Realtime Connection"
        every { mockRealtimeHealthCheck.isCritical } returns true
        every { mockNavigationHealthCheck.name } returns "Navigation Setup"
        every { mockNavigationHealthCheck.isCritical } returns false
        every { mockDependencyInjectionHealthCheck.name } returns "Dependency Injection"
        every { mockDependencyInjectionHealthCheck.isCritical } returns true
        
        appStartupVerifier = AppStartupVerifier(
            mockSupabaseHealthCheck,
            mockDatabaseHealthCheck,
            mockRealtimeHealthCheck,
            mockNavigationHealthCheck,
            mockDependencyInjectionHealthCheck,
            mockLogger
        )
    }
    
    @Test
    fun `when all health checks pass, should return healthy status`() = runTest {
        // Arrange
        val successResults = listOf(
            HealthCheckResult.Success("Dependency Injection", 100, System.currentTimeMillis()),
            HealthCheckResult.Success("Supabase Connection", 200, System.currentTimeMillis()),
            HealthCheckResult.Success("Database Access", 150, System.currentTimeMillis()),
            HealthCheckResult.Success("Realtime Connection", 300, System.currentTimeMillis()),
            HealthCheckResult.Success("Navigation Setup", 50, System.currentTimeMillis())
        )
        
        coEvery { mockDependencyInjectionHealthCheck.check() } returns successResults[0]
        coEvery { mockSupabaseHealthCheck.check() } returns successResults[1]
        coEvery { mockDatabaseHealthCheck.check() } returns successResults[2]
        coEvery { mockRealtimeHealthCheck.check() } returns successResults[3]
        coEvery { mockNavigationHealthCheck.check() } returns successResults[4]
        
        // Act
        val result = appStartupVerifier.verifyAppStartup()
        
        // Assert
        assertIs<AppHealthStatus.Healthy>(result)
        assertEquals(5, result.checkResults.size)
        assertTrue(result.totalDurationMs > 0)
        verify { mockLogger.info("AppStartupVerifier", any()) }
    }
    
    @Test
    fun `when critical health check fails, should return unhealthy status`() = runTest {
        // Arrange
        val criticalFailure = HealthCheckResult.Failure(
            "Supabase Connection",
            500,
            System.currentTimeMillis(),
            Exception("Connection failed"),
            "Failed to connect to Supabase"
        )
        
        coEvery { mockDependencyInjectionHealthCheck.check() } returns 
            HealthCheckResult.Success("Dependency Injection", 100, System.currentTimeMillis())
        coEvery { mockSupabaseHealthCheck.check() } returns criticalFailure
        coEvery { mockDatabaseHealthCheck.check() } returns 
            HealthCheckResult.Success("Database Access", 150, System.currentTimeMillis())
        coEvery { mockRealtimeHealthCheck.check() } returns 
            HealthCheckResult.Success("Realtime Connection", 300, System.currentTimeMillis())
        coEvery { mockNavigationHealthCheck.check() } returns 
            HealthCheckResult.Success("Navigation Setup", 50, System.currentTimeMillis())
        
        // Act
        val result = appStartupVerifier.verifyAppStartup()
        
        // Assert
        assertIs<AppHealthStatus.Unhealthy>(result)
        assertEquals(1, result.criticalFailures.size)
        assertEquals("Supabase Connection", result.criticalFailures[0].checkName)
        assertEquals(0, result.nonCriticalFailures.size)
        verify { mockLogger.error("AppStartupVerifier", any(), any()) }
    }
    
    @Test
    fun `when non-critical health check fails, should return degraded status`() = runTest {
        // Arrange
        val nonCriticalFailure = HealthCheckResult.Failure(
            "Navigation Setup",
            100,
            System.currentTimeMillis(),
            Exception("Navigation validation failed"),
            "Invalid route configuration"
        )
        
        coEvery { mockDependencyInjectionHealthCheck.check() } returns 
            HealthCheckResult.Success("Dependency Injection", 100, System.currentTimeMillis())
        coEvery { mockSupabaseHealthCheck.check() } returns 
            HealthCheckResult.Success("Supabase Connection", 200, System.currentTimeMillis())
        coEvery { mockDatabaseHealthCheck.check() } returns 
            HealthCheckResult.Success("Database Access", 150, System.currentTimeMillis())
        coEvery { mockRealtimeHealthCheck.check() } returns 
            HealthCheckResult.Success("Realtime Connection", 300, System.currentTimeMillis())
        coEvery { mockNavigationHealthCheck.check() } returns nonCriticalFailure
        
        // Act
        val result = appStartupVerifier.verifyAppStartup()
        
        // Assert
        assertIs<AppHealthStatus.Degraded>(result)
        assertEquals(0, result.warnings.size)
        assertEquals(5, result.checkResults.size)
        verify { mockLogger.warn("AppStartupVerifier", any()) }
    }
    
    @Test
    fun `when health check has warnings, should return degraded status`() = runTest {
        // Arrange
        val warning = HealthCheckResult.Warning(
            "Database Access",
            200,
            System.currentTimeMillis(),
            "Slow response time detected",
            "Query took longer than expected"
        )
        
        coEvery { mockDependencyInjectionHealthCheck.check() } returns 
            HealthCheckResult.Success("Dependency Injection", 100, System.currentTimeMillis())
        coEvery { mockSupabaseHealthCheck.check() } returns 
            HealthCheckResult.Success("Supabase Connection", 200, System.currentTimeMillis())
        coEvery { mockDatabaseHealthCheck.check() } returns warning
        coEvery { mockRealtimeHealthCheck.check() } returns 
            HealthCheckResult.Success("Realtime Connection", 300, System.currentTimeMillis())
        coEvery { mockNavigationHealthCheck.check() } returns 
            HealthCheckResult.Success("Navigation Setup", 50, System.currentTimeMillis())
        
        // Act
        val result = appStartupVerifier.verifyAppStartup()
        
        // Assert
        assertIs<AppHealthStatus.Degraded>(result)
        assertEquals(1, result.warnings.size)
        assertEquals("Database Access", result.warnings[0].checkName)
        verify { mockLogger.warn("AppStartupVerifier", any()) }
    }
    
    @Test
    fun `quick health check should only run critical checks`() = runTest {
        // Arrange
        coEvery { mockDependencyInjectionHealthCheck.check() } returns 
            HealthCheckResult.Success("Dependency Injection", 100, System.currentTimeMillis())
        coEvery { mockSupabaseHealthCheck.check() } returns 
            HealthCheckResult.Success("Supabase Connection", 200, System.currentTimeMillis())
        coEvery { mockDatabaseHealthCheck.check() } returns 
            HealthCheckResult.Success("Database Access", 150, System.currentTimeMillis())
        coEvery { mockRealtimeHealthCheck.check() } returns 
            HealthCheckResult.Success("Realtime Connection", 300, System.currentTimeMillis())
        
        // Act
        val result = appStartupVerifier.quickHealthCheck()
        
        // Assert
        assertIs<AppHealthStatus.Healthy>(result)
        // Should have 4 critical checks (DI, Supabase, Database, Realtime) but not Navigation
        assertEquals(4, result.checkResults.size)
        assertTrue(result.checkResults.none { it.checkName == "Navigation Setup" })
    }
    
    @Test
    fun `when global timeout is exceeded, should handle gracefully`() = runTest {
        // Arrange
        val config = HealthCheckConfig(globalTimeoutMs = 100L) // Very short timeout
        
        coEvery { mockDependencyInjectionHealthCheck.check() } coAnswers {
            kotlinx.coroutines.delay(200) // Longer than timeout
            HealthCheckResult.Success("Dependency Injection", 200, System.currentTimeMillis())
        }
        
        // Act
        val result = appStartupVerifier.verifyAppStartup(config)
        
        // Assert
        assertIs<AppHealthStatus.Unhealthy>(result)
        verify { mockLogger.error("AppStartupVerifier", any(), any()) }
    }
}