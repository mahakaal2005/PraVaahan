package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringServiceTest {
    
    private lateinit var metricsCorrelationEngine: MetricsCorrelationEngine
    private lateinit var alertingSystem: AlertingSystem
    private lateinit var realTimeMetricsCollector: RealTimeMetricsCollector
    private lateinit var memoryLeakDetector: MemoryLeakDetector
    private lateinit var securityEventMonitor: SecurityEventMonitor
    private lateinit var logger: Logger
    private lateinit var monitoringService: MonitoringService
    
    @BeforeEach
    fun setup() {
        metricsCorrelationEngine = mockk(relaxed = true)
        alertingSystem = mockk(relaxed = true)
        realTimeMetricsCollector = mockk(relaxed = true)
        memoryLeakDetector = mockk(relaxed = true)
        securityEventMonitor = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        
        // Setup default flows
        every { metricsCorrelationEngine.correlations } returns MutableStateFlow(emptyList())
        every { metricsCorrelationEngine.anomalies } returns MutableStateFlow(emptyList())
        every { metricsCorrelationEngine.insights } returns MutableStateFlow(emptyList())
        every { alertingSystem.alertStatistics } returns MutableStateFlow(AlertStatistics())
        every { realTimeMetricsCollector.metrics } returns MutableStateFlow(createDefaultRealTimeMetrics())
        every { memoryLeakDetector.getMemoryStatistics() } returns createDefaultMemoryStatistics()
        every { realTimeMetricsCollector.isSystemHealthy() } returns true
        
        monitoringService = MonitoringService(
            metricsCorrelationEngine = metricsCorrelationEngine,
            alertingSystem = alertingSystem,
            realTimeMetricsCollector = realTimeMetricsCollector,
            memoryLeakDetector = memoryLeakDetector,
            securityEventMonitor = securityEventMonitor,
            logger = logger
        )
    }
    
    @Test
    fun `recordTrainPosition should record metrics and validate security`() = runTest {
        // Given
        val position = TrainPosition(
            trainId = "TRAIN_001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 80.0,
            heading = 45.0,
            timestamp = Clock.System.now(),
            sectionId = "SEC_001",
            accuracy = 5.0
        )
        
        // When
        monitoringService.recordTrainPosition(position)
        
        // Then
        coVerify { realTimeMetricsCollector.recordMessageReceived(position) }
        verify { metricsCorrelationEngine.recordMetric("position_latency_ms", any(), any()) }
        verify { metricsCorrelationEngine.recordMetric("train_speed_kmh", position.speed, any(), any()) }
        verify { metricsCorrelationEngine.recordMetric("position_accuracy_m", position.accuracy!!, any(), any()) }
    }
    
    @Test
    fun `recordTrainPosition should detect impossible speed and raise security alert`() = runTest {
        // Given
        val position = TrainPosition(
            trainId = "TRAIN_002",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 250.0, // Impossible speed
            heading = 90.0,
            timestamp = Clock.System.now(),
            sectionId = "SEC_002",
            accuracy = 5.0
        )
        
        // When
        monitoringService.recordTrainPosition(position)
        
        // Then
        coVerify { 
            realTimeMetricsCollector.recordValidationFailure(
                position.trainId, 
                "Impossible speed: ${position.speed} km/h"
            )
        }
    }
    
    @Test
    fun `recordTrainPosition should detect stale data and record validation failure`() = runTest {
        // Given
        val stalePosition = TrainPosition(
            trainId = "TRAIN_003",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 60.0,
            heading = 180.0,
            timestamp = Clock.System.now().minus(600000.milliseconds), // 10 minutes old
            sectionId = "SEC_003",
            accuracy = 5.0
        )
        
        // When
        monitoringService.recordTrainPosition(stalePosition)
        
        // Then
        coVerify { 
            realTimeMetricsCollector.recordValidationFailure(
                stalePosition.trainId,
                match { it.contains("Stale position data") }
            )
        }
    }
    
    @Test
    fun `should process strong correlations and raise alerts`() = runTest {
        // Given
        val strongCorrelation = MetricCorrelation(
            metric1 = "error_rate",
            metric2 = "response_time",
            coefficient = 0.95,
            strength = CorrelationStrength.VERY_STRONG,
            direction = CorrelationDirection.POSITIVE,
            timestamp = Clock.System.now(),
            sampleSize = 50
        )
        
        val correlationsFlow = MutableStateFlow(listOf(strongCorrelation))
        every { metricsCorrelationEngine.correlations } returns correlationsFlow
        
        // When - trigger correlation processing by updating the flow
        correlationsFlow.value = listOf(strongCorrelation)
        
        // Give some time for processing
        kotlinx.coroutines.delay(100.milliseconds)
        
        // Then
        verify { 
            alertingSystem.raiseAlert(
                source = "correlation_engine",
                type = AlertType.PERFORMANCE_DEGRADATION,
                severity = any(),
                title = "Strong Metric Correlation Detected",
                description = match { it.contains("Strong positive correlation") },
                metadata = any()
            )
        }
    }
    
    @Test
    fun `should process critical anomalies and raise alerts`() = runTest {
        // Given
        val criticalAnomaly = MetricAnomaly(
            metricName = "memory_usage",
            type = AnomalyType.SPIKE,
            severity = AnomalySeverity.CRITICAL,
            value = 500.0,
            expectedValue = 100.0,
            deviation = 5.0,
            timestamp = Clock.System.now(),
            description = "Critical memory spike detected"
        )
        
        val anomaliesFlow = MutableStateFlow(listOf(criticalAnomaly))
        every { metricsCorrelationEngine.anomalies } returns anomaliesFlow
        
        // When
        anomaliesFlow.value = listOf(criticalAnomaly)
        kotlinx.coroutines.delay(100.milliseconds)
        
        // Then
        verify { 
            alertingSystem.raiseAlert(
                source = "anomaly_detector",
                type = AlertType.MEMORY_USAGE,
                severity = AlertSeverity.CRITICAL,
                title = "Metric Anomaly Detected",
                description = criticalAnomaly.description,
                metadata = any()
            )
        }
    }
    
    @Test
    fun `should process actionable insights and raise alerts`() = runTest {
        // Given
        val actionableInsight = SystemInsight(
            type = InsightType.PERFORMANCE,
            title = "Performance Degradation Detected",
            description = "System performance has degraded significantly",
            severity = InsightSeverity.HIGH,
            actionable = true,
            recommendations = listOf("Increase memory allocation", "Optimize queries"),
            timestamp = Clock.System.now(),
            metadata = mapOf("component" to "database")
        )
        
        val insightsFlow = MutableStateFlow(listOf(actionableInsight))
        every { metricsCorrelationEngine.insights } returns insightsFlow
        
        // When
        insightsFlow.value = listOf(actionableInsight)
        kotlinx.coroutines.delay(100.milliseconds)
        
        // Then
        verify { 
            alertingSystem.raiseAlert(
                source = "insights_engine",
                type = AlertType.SYSTEM_ERROR,
                severity = AlertSeverity.HIGH,
                title = actionableInsight.title,
                description = match { it.contains("Recommendations:") },
                metadata = any()
            )
        }
    }
    
    @Test
    fun `systemHealth should reflect critical alerts`() = runTest {
        // Given
        val criticalAlertStats = AlertStatistics(
            criticalAlerts = 2,
            highSeverityAlerts = 1
        )
        every { alertingSystem.alertStatistics } returns MutableStateFlow(criticalAlertStats)
        
        // Wait for health update
        kotlinx.coroutines.delay(100)
        
        // When
        val systemHealth = monitoringService.systemHealth.first()
        
        // Then
        assertEquals(SystemHealthStatus.CRITICAL, systemHealth.status)
        assertEquals(2, systemHealth.criticalAlerts)
        assertEquals(1, systemHealth.highSeverityAlerts)
    }
    
    @Test
    fun `systemHealth should reflect degraded performance`() = runTest {
        // Given
        every { realTimeMetricsCollector.isSystemHealthy() } returns false
        every { memoryLeakDetector.getMemoryStatistics() } returns MemoryStatistics(
            currentUsageMB = 200,
            baselineUsageMB = 50,
            heapUsagePercent = 85,
            totalLeaksDetected = 2,
            recentLeaks = 1,
            totalAlerts = 5,
            recentAlerts = 2,
            memoryGrowthMB = 150,
            isHealthy = false
        )
        
        // Wait for health update
        kotlinx.coroutines.delay(100)
        
        // When
        val systemHealth = monitoringService.systemHealth.first()
        
        // Then
        assertEquals(SystemHealthStatus.DEGRADED, systemHealth.status)
    }
    
    @Test
    fun `getMonitoringDashboard should return comprehensive dashboard data`() = runTest {
        // Given
        val mockAlerts = listOf(
            Alert(
                id = "ALERT_001",
                source = "test",
                type = AlertType.PERFORMANCE_DEGRADATION,
                severity = AlertSeverity.HIGH,
                title = "Test Alert",
                description = "Test Description",
                metadata = emptyMap(),
                timestamp = Clock.System.now()
            )
        )
        
        val mockCorrelations = listOf(
            MetricCorrelation(
                metric1 = "cpu",
                metric2 = "memory",
                coefficient = 0.8,
                strength = CorrelationStrength.STRONG,
                direction = CorrelationDirection.POSITIVE,
                timestamp = Clock.System.now(),
                sampleSize = 100
            )
        )
        
        every { alertingSystem.activeAlerts } returns MutableStateFlow(mockAlerts)
        every { metricsCorrelationEngine.correlations } returns MutableStateFlow(mockCorrelations)
        
        // When
        val dashboard = monitoringService.getMonitoringDashboard()
        
        // Then
        assertNotNull(dashboard.systemHealth)
        assertNotNull(dashboard.statistics)
        assertTrue(dashboard.recentAlerts.isNotEmpty())
        assertTrue(dashboard.topCorrelations.isNotEmpty())
    }
    
    @Test
    fun `cleanupOldData should call cleanup on all components`() = runTest {
        // Given
        val cutoffTime = Clock.System.now().minus(3600000.milliseconds)
        
        // When
        monitoringService.cleanupOldData(cutoffTime)
        
        // Give time for async cleanup
        kotlinx.coroutines.delay(100)
        
        // Then
        verify { metricsCorrelationEngine.clearOldData(cutoffTime) }
        verify { alertingSystem.cleanupOldData(cutoffTime) }
        verify { memoryLeakDetector.clearOldData(cutoffTime) }
    }
    
    @Test
    fun `should handle errors gracefully during train position recording`() = runTest {
        // Given
        val position = TrainPosition(
            trainId = "TRAIN_ERROR",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 80.0,
            heading = 0.0,
            timestamp = Clock.System.now(),
            sectionId = "SEC_ERROR"
        )
        
        coEvery { realTimeMetricsCollector.recordMessageReceived(any()) } throws RuntimeException("Test error")
        
        // When
        monitoringService.recordTrainPosition(position)
        
        // Then
        coVerify { realTimeMetricsCollector.recordConnectionError(any()) }
        verify { logger.error("MonitoringService", "Error recording train position", any()) }
    }
    
    private fun createDefaultRealTimeMetrics(): RealTimeMetrics {
        return RealTimeMetrics(
            connectionStatus = ConnectionStatus.CONNECTED,
            dataQuality = DataQuality.default(),
            performance = PerformanceMetrics(
                averageLatency = 100.milliseconds,
                maxLatency = 500.milliseconds,
                minLatency = 50.milliseconds,
                throughput = 10.0,
                errorRate = 0.01,
                uptime = 3600000.milliseconds,
                memoryUsage = 100 * 1024 * 1024,
                connectionCount = 1
            ),
            security = SecurityMetrics(
                validationFailures = 0,
                anomaliesDetected = 0,
                suspiciousPatterns = 0,
                lastSecurityEvent = null
            ),
            lastUpdated = Clock.System.now()
        )
    }
    
    private fun createDefaultMemoryStatistics(): MemoryStatistics {
        return MemoryStatistics(
            currentUsageMB = 100,
            baselineUsageMB = 80,
            heapUsagePercent = 60,
            totalLeaksDetected = 0,
            recentLeaks = 0,
            totalAlerts = 0,
            recentAlerts = 0,
            memoryGrowthMB = 20,
            isHealthy = true
        )
    }
}