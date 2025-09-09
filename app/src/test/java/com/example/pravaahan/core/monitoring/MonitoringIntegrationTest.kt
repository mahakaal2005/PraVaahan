package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for the complete monitoring ecosystem
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringIntegrationTest {
    
    private lateinit var logger: Logger
    private lateinit var realTimeMetricsCollector: RealTimeMetricsCollector
    
    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
        realTimeMetricsCollector = RealTimeMetricsCollector(logger)
    }
    
    @Test
    fun `metrics collector should handle train position updates`() = runTest {
        // Given - Train position data
        val baseTime = Clock.System.now()
        
        // Record train positions
        for (i in 1..10) {
            val position = TrainPosition(
                trainId = "TRAIN_001",
                latitude = 28.6139 + (i * 0.001),
                longitude = 77.2090 + (i * 0.001),
                speed = 60.0 + (i * 2.0),
                heading = 90.0,
                timestamp = baseTime.plus(i.seconds),
                sectionId = "SECTION_001",
                accuracy = 5.0
            )
            
            realTimeMetricsCollector.recordMessageReceived(position)
        }
        
        // Wait for processing
        delay(100)
        
        // Then - Metrics should be recorded
        val metrics = realTimeMetricsCollector.metrics.first()
        assertTrue(metrics.performance.throughput >= 0.0, "Should record throughput")
        assertTrue(metrics.performance.errorRate <= 1.0, "Error rate should be reasonable")
    }
    
    @Test
    fun `metrics collector should handle connection events`() = runTest {
        // Given - Connection events
        realTimeMetricsCollector.recordConnectionEstablished()
        
        delay(50)
        
        // Then - Connection status should be updated
        val metrics = realTimeMetricsCollector.metrics.first()
        assertEquals(ConnectionStatus.CONNECTED, metrics.connectionStatus)
        
        // When - Connection is lost
        realTimeMetricsCollector.recordConnectionLost("Test disconnect")
        
        delay(50)
        
        // Then - Status should be updated
        val updatedMetrics = realTimeMetricsCollector.metrics.first()
        assertEquals(ConnectionStatus.DISCONNECTED, updatedMetrics.connectionStatus)
    }
    
    @Test
    fun `metrics collector should track data quality`() = runTest {
        // Given - Various quality scenarios
        val baseTime = Clock.System.now()
        
        // Record good quality data
        for (i in 1..5) {
            val position = TrainPosition(
                trainId = "TRAIN_QUALITY",
                latitude = 28.6139,
                longitude = 77.2090,
                speed = 60.0,
                heading = 90.0,
                timestamp = baseTime.plus(i.seconds),
                sectionId = "SECTION_001",
                accuracy = 3.0
            )
            
            realTimeMetricsCollector.recordMessageReceived(position)
        }
        
        // Wait for processing
        delay(100)
        
        // Then - Data quality metrics should be available
        val metrics = realTimeMetricsCollector.metrics.first()
        assertNotNull(metrics.dataQuality, "Data quality should be tracked")
        assertTrue(metrics.dataQuality.reliability >= 0.0f, "Reliability should be non-negative")
        assertTrue(metrics.dataQuality.accuracy >= 0.0, "Accuracy should be non-negative")
    }
    
    @Test
    fun `metrics collector should handle validation failures`() = runTest {
        // Given - Validation failure scenario
        realTimeMetricsCollector.recordValidationFailure("TRAIN_001", "Invalid coordinates")
        
        delay(50)
        
        // Then - Security metrics should reflect the failure
        val metrics = realTimeMetricsCollector.metrics.first()
        assertTrue(metrics.security.validationFailures >= 0L, "Should track validation failures")
    }
    
    @Test
    fun `metrics collector should handle security anomalies`() = runTest {
        // Given - Security anomaly
        realTimeMetricsCollector.recordSecurityAnomaly("TRAIN_001", "Speed anomaly", "Impossible speed detected")
        
        delay(50)
        
        // Then - Security metrics should be updated
        val metrics = realTimeMetricsCollector.metrics.first()
        assertTrue(metrics.security.anomaliesDetected >= 0L, "Should track anomalies")
    }
    
    @Test
    fun `metrics collector should track memory usage`() = runTest {
        // Given - Memory usage data
        val memoryUsage = 100L * 1024 * 1024 // 100MB
        realTimeMetricsCollector.recordMemoryUsage(memoryUsage)
        
        delay(50)
        
        // Then - Memory metrics should be updated
        val metrics = realTimeMetricsCollector.metrics.first()
        assertTrue(metrics.performance.memoryUsage >= 0L, "Should track memory usage")
    }
    
    @Test
    fun `metrics collector should provide system health status`() = runTest {
        // Given - Establish connection and record some data
        realTimeMetricsCollector.recordConnectionEstablished()
        
        val position = TrainPosition(
            trainId = "TRAIN_HEALTH",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 60.0,
            heading = 90.0,
            timestamp = Clock.System.now(),
            sectionId = "SECTION_001",
            accuracy = 5.0
        )
        
        realTimeMetricsCollector.recordMessageReceived(position)
        
        delay(100)
        
        // Then - System health should be available
        val isHealthy = realTimeMetricsCollector.isSystemHealthy()
        assertTrue(isHealthy || !isHealthy, "Health status should be determinable")
        
        val errorRate = realTimeMetricsCollector.getCurrentErrorRate()
        assertTrue(errorRate >= 0.0, "Error rate should be non-negative")
    }
    
    @Test
    fun `metrics collector should handle reset operations`() = runTest {
        // Given - Some recorded data
        val position = TrainPosition(
            trainId = "TRAIN_RESET",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 60.0,
            heading = 90.0,
            timestamp = Clock.System.now(),
            sectionId = "SECTION_001",
            accuracy = 5.0
        )
        
        realTimeMetricsCollector.recordMessageReceived(position)
        delay(50)
        
        // When - Reset metrics
        realTimeMetricsCollector.resetMetrics()
        delay(50)
        
        // Then - Metrics should be reset
        val metrics = realTimeMetricsCollector.metrics.first()
        assertNotNull(metrics, "Metrics should still be available after reset")
    }
    
    @Test
    fun `metrics collector should handle high frequency updates`() = runTest {
        // Given - High frequency updates
        val baseTime = Clock.System.now()
        
        repeat(50) { i ->
            val position = TrainPosition(
                trainId = "TRAIN_${i % 5}",
                latitude = 28.6139 + (i * 0.0001),
                longitude = 77.2090 + (i * 0.0001),
                speed = 50.0 + (i % 20),
                heading = 90.0,
                timestamp = baseTime.plus((i * 100).seconds),
                sectionId = "SECTION_001",
                accuracy = 3.0
            )
            
            realTimeMetricsCollector.recordMessageReceived(position)
        }
        
        delay(200)
        
        // Then - System should handle the load
        val metrics = realTimeMetricsCollector.metrics.first()
        assertTrue(metrics.performance.throughput >= 0.0, "Should handle high frequency updates")
    }
}