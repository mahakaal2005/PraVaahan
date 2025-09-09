package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector
import com.example.pravaahan.domain.service.RealTimePositionService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PositionDataQualityHealthCheckTest {
    
    private lateinit var realTimePositionService: RealTimePositionService
    private lateinit var metricsCollector: RealTimeMetricsCollector
    private lateinit var logger: Logger
    private lateinit var healthCheck: PositionDataQualityHealthCheck
    
    @Before
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
            reliability = 0.98,
            outOfOrderCount = 2,
            duplicateCount = 1,
            invalidDataCount = 0
        )
        
        every { metricsCollector.metrics } returns flowOf(goodMetrics)
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertTrue("Health check should pass with good metrics", result is HealthCheckResult.Success)
        assertEquals("Position Data Quality", result.checkName)
    }
    
    @Test
    fun `health check should fail with low reliability`() = runTest {
        // Arrange
        val badMetrics = createMockMetrics(
            reliability = 0.80, // Below 90% threshold
            outOfOrderCount = 2,
            duplicateCount = 1,
            invalidDataCount = 0
        )
        
        every { metricsCollector.metrics } returns flowOf(badMetrics)
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertTrue("Health check should fail with low reliability", result is HealthCheckResult.Failure)
        val failure = result as HealthCheckResult.Failure
        assertTrue("Error message should mention reliability", failure.message.contains("reliability"))
    }
    
    @Test
    fun `health check should fail with high invalid data count`() = runTest {
        // Arrange
        val badMetrics = createMockMetrics(
            reliability = 0.95,
            outOfOrderCount = 2,
            duplicateCount = 1,
            invalidDataCount = 10 // Above threshold
        )
        
        every { metricsCollector.metrics } returns flowOf(badMetrics)
        
        // Act
        val result = healthCheck.check()
        
        // Assert
        assertTrue("Health check should fail with high invalid data count", result is HealthCheckResult.Failure)
        val failure = result as HealthCheckResult.Failure
        assertTrue("Error message should mention invalid data", failure.message.contains("Invalid data count"))
    }
    
    @Test
    fun `health check properties should be configured correctly`() {
        // Assert
        assertEquals("Position Data Quality", healthCheck.name)
        assertTrue("Should be critical for railway safety", healthCheck.isCritical)
        assertEquals(10_000L, healthCheck.timeoutMs)
        assertEquals(1, healthCheck.maxRetries)
    }
    
    private fun createMockMetrics(
        reliability: Double,
        outOfOrderCount: Int,
        duplicateCount: Int,
        invalidDataCount: Int
    ): com.example.pravaahan.core.monitoring.RealTimeMetrics {
        val dataQuality = mockk<com.example.pravaahan.core.monitoring.DataQualityMetrics>()
        every { dataQuality.reliability } returns reliability
        every { dataQuality.outOfOrderCount } returns outOfOrderCount
        every { dataQuality.duplicateCount } returns duplicateCount
        every { dataQuality.invalidDataCount } returns invalidDataCount
        every { dataQuality.accuracy } returns 0.95
        
        val performance = mockk<com.example.pravaahan.core.monitoring.PerformanceMetrics>()
        every { performance.averageLatency } returns kotlin.time.Duration.parse("100ms")
        every { performance.errorRate } returns 0.01
        every { performance.throughput } returns 100.0
        every { performance.memoryUsage } returns 50L * 1024 * 1024 // 50MB
        every { performance.activeConnections } returns 10
        
        val security = mockk<com.example.pravaahan.core.monitoring.SecurityMetrics>()
        every { security.anomaliesDetected } returns 0
        every { security.validationFailures } returns 0
        
        val connectionStatus = com.example.pravaahan.core.monitoring.ConnectionStatus.CONNECTED
        
        val metrics = mockk<com.example.pravaahan.core.monitoring.RealTimeMetrics>()
        every { metrics.dataQuality } returns dataQuality
        every { metrics.performance } returns performance
        every { metrics.security } returns security
        every { metrics.connectionStatus } returns connectionStatus
        
        return metrics
    }
}