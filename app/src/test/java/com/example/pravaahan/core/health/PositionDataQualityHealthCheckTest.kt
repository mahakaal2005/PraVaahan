package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector
import com.example.pravaahan.domain.service.RealTimePositionService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class PositionDataQualityHealthCheckTest {
    
    private lateinit var realTimePositionService: RealTimePositionService
    private lateinit var metricsCollector: RealTimeMetricsCollector
    private lateinit var logger: Logger
    private lateinit var healthCheck: PositionDataQualityHealthCheck
    
    @BeforeEach
    fun setup() {
        realTimePositionService = mockk()
        metricsCollector = mockk()
        logger = mockk(relaxed = true)
        
        healthCheck = PositionDataQualityHealthCheck(
            realTimePositionService,
            metricsCollector,
            logger
        )
    }
    
    @Test
    fun `health check should pass with good data quality metrics`() = runTest {
        // Arrange
        val goodMetrics = createMockMetrics(
            reliability = 0.98f,
            outOfOrderCount = 2,
            duplicateCount = 1,
            invalidDataCount = 0
        )
        
        every { metricsCollector.metrics } returns MutableStateFlow(goodMetrics)
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertTrue(result is HealthCheckResult.Success, "Health check should pass with good metrics")
        assertEquals("Position Data Quality", result.checkName)
    }
    
    @Test
    fun `health check should fail with low reliability`() = runTest {
        // Arrange
        val badMetrics = createMockMetrics(
            reliability = 0.80f, // Below 90% threshold
            outOfOrderCount = 2,
            duplicateCount = 1,
            invalidDataCount = 0
        )
        
        every { metricsCollector.metrics } returns MutableStateFlow(badMetrics)
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertTrue(result is HealthCheckResult.Failure, "Health check should fail with low reliability")
        val failure = result as HealthCheckResult.Failure
        assertTrue(failure.message.contains("reliability"), "Error message should mention reliability")
    }
    
    @Test
    fun `health check should fail with high invalid data count`() = runTest {
        // Arrange
        val badMetrics = createMockMetrics(
            reliability = 0.95f,
            outOfOrderCount = 2,
            duplicateCount = 1,
            invalidDataCount = 10 // Above threshold
        )
        
        every { metricsCollector.metrics } returns MutableStateFlow(badMetrics)
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertTrue(result is HealthCheckResult.Failure, "Health check should fail with high invalid data count")
        val failure = result as HealthCheckResult.Failure
        assertTrue(failure.message.contains("Invalid data count"), "Error message should mention invalid data")
    }
    
    @Test
    fun `health check properties should be configured correctly`() {
        // Assert
        assertEquals("Position Data Quality", healthCheck.name)
        assertTrue(healthCheck.isCritical, "Should be critical for railway safety")
        assertEquals(10_000L, healthCheck.timeoutMs)
        assertEquals(1, healthCheck.maxRetries)
    }
    
    private fun createMockMetrics(
        reliability: Float,
        outOfOrderCount: Int,
        duplicateCount: Int,
        invalidDataCount: Int
    ): com.example.pravaahan.core.monitoring.RealTimeMetrics {
        val dataQuality = com.example.pravaahan.core.monitoring.DataQuality(
            latency = 100.milliseconds,
            accuracy = 0.95,
            freshness = 50.milliseconds,
            reliability = reliability,
            outOfOrderCount = outOfOrderCount,
            duplicateCount = duplicateCount,
            invalidDataCount = invalidDataCount
        )
        
        val performance = com.example.pravaahan.core.monitoring.PerformanceMetrics(
            averageLatency = 100.milliseconds,
            maxLatency = 200.milliseconds,
            minLatency = 50.milliseconds,
            throughput = 100.0,
            errorRate = 0.01,
            uptime = 3600.milliseconds,
            memoryUsage = 50L * 1024 * 1024, // 50MB
            connectionCount = 10
        )
        
        val security = com.example.pravaahan.core.monitoring.SecurityMetrics(
            validationFailures = 0L,
            anomaliesDetected = 0L,
            suspiciousPatterns = 0L,
            lastSecurityEvent = null
        )
        
        val connectionStatus = com.example.pravaahan.core.monitoring.ConnectionStatus.CONNECTED
        
        return com.example.pravaahan.core.monitoring.RealTimeMetrics(
            connectionStatus = connectionStatus,
            dataQuality = dataQuality,
            performance = performance,
            security = security,
            lastUpdated = kotlinx.datetime.Clock.System.now()
        )
    }
}