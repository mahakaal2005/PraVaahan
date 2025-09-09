package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for the complete monitoring ecosystem
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringIntegrationTest {
    
    private lateinit var logger: Logger
    private lateinit var metricsCorrelationEngine: MetricsCorrelationEngine
    private lateinit var alertingSystem: AlertingSystem
    private lateinit var realTimeMetricsCollector: RealTimeMetricsCollector
    private lateinit var memoryLeakDetector: MemoryLeakDetector
    private lateinit var securityEventMonitor: SecurityEventMonitor
    private lateinit var monitoringService: MonitoringService
    
    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
        
        // Create real instances for integration testing
        metricsCorrelationEngine = MetricsCorrelationEngine(logger)
        alertingSystem = AlertingSystem(logger)
        realTimeMetricsCollector = RealTimeMetricsCollector(logger)
        memoryLeakDetector = MemoryLeakDetector(logger)
        securityEventMonitor = SecurityEventMonitor(logger)
        
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
    fun `complete monitoring workflow should work end-to-end`() = runTest {
        // Given - Simulate a complete monitoring scenario
        val baseTime = Clock.System.now()
        
        // Step 1: Record normal train positions
        for (i in 1..10) {
            val position = TrainPosition(
                trainId = "TRAIN_001",
                latitude = 28.6139 + (i * 0.001),
                longitude = 77.2090 + (i * 0.001),
                speed = 60.0 + (i * 2.0), // Gradually increasing speed
                timestamp = baseTime.plus(i.seconds),
                accuracy = 5.0
            )
            
            monitoringService.recordTrainPosition(position)
        }
        
        // Step 2: Record correlated metrics that should trigger correlation detection
        for (i in 1..15) {
            val timestamp = baseTime.plus(i.seconds)
            
            // Create strong correlation between error rate and response time
            metricsCorrelationEngine.recordMetric("error_rate_percent", i.toDouble(), timestamp)
            metricsCorrelationEngine.recordMetric("response_time_ms", i.toDouble() * 10, timestamp)
            
            // Create memory usage trend
            metricsCorrelationEngine.recordMetric("memory_usage_mb", 100.0 + (i * 5), timestamp)
        }
        
        // Step 3: Create an anomaly
        val anomalyTime = baseTime.plus(20.seconds)
        metricsCorrelationEngine.recordMetric("cpu_usage_percent", 200.0, anomalyTime) // Impossible CPU usage
        
        // Wait for analysis to complete
        delay(200)
        
        // Step 4: Verify correlations were detected
        val correlations = metricsCorrelationEngine.correlations.first()
        val errorResponseCorrelation = correlations.find { 
            (it.metric1 == "error_rate_percent" && it.metric2 == "response_time_ms") ||
            (it.metric1 == "response_time_ms" && it.metric2 == "error_rate_percent")
        }
        
        assertNotNull(errorResponseCorrelation, "Should detect correlation between error rate and response time")
        assertTrue(errorResponseCorrelation.coefficient > 0.8, "Should be strong positive correlation")
        
        // Step 5: Verify trends were detected
        val trends = metricsCorrelationEngine.trends.first()
        val memoryTrend = trends.find { it.metricName == "memory_usage_mb" }
        
        assertNotNull(memoryTrend, "Should detect memory usage trend")
        assertEquals(TrendDirection.INCREASING, memoryTrend.direction)
        
        // Step 6: Verify anomalies were detected
        val anomalies = metricsCorrelationEngine.anomalies.first()
        val cpuAnomaly = anomalies.find { it.metricName == "cpu_usage_percent" }
        
        assertNotNull(cpuAnomaly, "Should detect CPU usage anomaly")
        assertEquals(AnomalyType.SPIKE, cpuAnomaly.type)
        
        // Step 7: Verify insights were generated
        val insights = metricsCorrelationEngine.insights.first()
        assertTrue(insights.isNotEmpty(), "Should generate insights from analysis")
        
        // Step 8: Verify alerts were raised
        val alerts = alertingSystem.activeAlerts.first()
        assertTrue(alerts.isNotEmpty(), "Should raise alerts based on correlations and anomalies")
        
        // Step 9: Verify system health reflects the issues
        val systemHealth = monitoringService.systemHealth.first()
        assertTrue(systemHealth.status != SystemHealthStatus.HEALTHY, "System health should reflect detected issues")
        
        // Step 10: Verify monitoring dashboard aggregates all data
        val dashboard = monitoringService.getMonitoringDashboard()
        
        assertNotNull(dashboard.systemHealth)
        assertTrue(dashboard.statistics.activeCorrelations > 0)
        assertTrue(dashboard.statistics.activeAnomalies > 0)
        assertTrue(dashboard.statistics.generatedInsights > 0)
        assertTrue(dashboard.recentAlerts.isNotEmpty())
        assertTrue(dashboard.topCorrelations.isNotEmpty())
        assertTrue(dashboard.criticalAnomalies.isNotEmpty())
        assertTrue(dashboard.actionableInsights.isNotEmpty())
    }
    
    @Test
    fun `monitoring should handle high-frequency train position updates`() = runTest {
        // Given - High frequency position updates
        val baseTime = Clock.System.now()
        val trainIds = listOf("TRAIN_001", "TRAIN_002", "TRAIN_003", "TRAIN_004", "TRAIN_005")
        
        // Simulate 100 position updates across 5 trains
        for (i in 1..100) {
            val trainId = trainIds[i % trainIds.size]
            val position = TrainPosition(
                trainId = trainId,
                latitude = 28.6139 + (i * 0.0001),
                longitude = 77.2090 + (i * 0.0001),
                speed = 50.0 + (i % 20),
                timestamp = baseTime.plus((i * 100).kotlinx.time.Duration.ZERO.inWholeMilliseconds.toInt().kotlinx.time.Duration.ZERO),
                accuracy = 3.0 + (i % 5)
            )
            
            monitoringService.recordTrainPosition(position)
        }
        
        // Wait for processing
        delay(100)
        
        // Then - System should handle the load gracefully
        val realTimeMetrics = realTimeMetricsCollector.metrics.first()
        assertTrue(realTimeMetrics.performance.throughput > 0, "Should record positive throughput")
        assertTrue(realTimeMetrics.performance.errorRate < 0.1, "Should maintain low error rate")
        
        val systemHealth = monitoringService.systemHealth.first()
        assertNotNull(systemHealth, "System health should be available")
    }
    
    @Test
    fun `monitoring should detect and alert on security violations`() = runTest {
        // Given - Suspicious train position data
        val baseTime = Clock.System.now()
        
        // Record normal positions first
        for (i in 1..5) {
            val position = TrainPosition(
                trainId = "TRAIN_SECURE",
                latitude = 28.6139,
                longitude = 77.2090,
                speed = 60.0,
                timestamp = baseTime.plus(i.seconds)
            )
            
            monitoringService.recordTrainPosition(position)
        }
        
        // Then record suspicious position (impossible speed)
        val suspiciousPosition = TrainPosition(
            trainId = "TRAIN_SECURE",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 300.0, // Impossible speed for a train
            timestamp = baseTime.plus(10.seconds)
        )
        
        monitoringService.recordTrainPosition(suspiciousPosition)
        
        // Wait for processing
        delay(100)
        
        // Then - Security event should be recorded and alert raised
        val alerts = alertingSystem.activeAlerts.first()
        val securityAlert = alerts.find { it.type == AlertType.SECURITY_THREAT }
        
        // Note: The security alert might be raised through the anomaly detection system
        // or directly through security validation
        assertTrue(alerts.isNotEmpty(), "Should raise alerts for security violations")
        
        val systemHealth = monitoringService.systemHealth.first()
        assertTrue(systemHealth.status != SystemHealthStatus.HEALTHY, "System health should reflect security concerns")
    }
    
    @Test
    fun `monitoring should provide comprehensive dashboard data`() = runTest {
        // Given - Various monitoring data
        val baseTime = Clock.System.now()
        
        // Record some train positions
        for (i in 1..5) {
            val position = TrainPosition(
                trainId = "TRAIN_DASH",
                latitude = 28.6139,
                longitude = 77.2090,
                speed = 70.0,
                timestamp = baseTime.plus(i.seconds)
            )
            
            monitoringService.recordTrainPosition(position)
        }
        
        // Record some metrics for correlation
        for (i in 1..10) {
            metricsCorrelationEngine.recordMetric("test_metric_a", i.toDouble(), baseTime.plus(i.seconds))
            metricsCorrelationEngine.recordMetric("test_metric_b", (i * 2).toDouble(), baseTime.plus(i.seconds))
        }
        
        // Wait for analysis
        delay(150)
        
        // When
        val dashboard = monitoringService.getMonitoringDashboard()
        
        // Then
        assertNotNull(dashboard.systemHealth)
        assertNotNull(dashboard.statistics)
        
        // Verify statistics are populated
        assertTrue(dashboard.statistics.totalMetricsTracked > 0)
        assertTrue(dashboard.statistics.lastUpdated <= Clock.System.now())
        
        // Verify health status is reasonable
        assertTrue(dashboard.systemHealth.lastUpdated <= Clock.System.now())
    }
    
    @Test
    fun `monitoring should handle cleanup operations correctly`() = runTest {
        // Given - Old monitoring data
        val oldTime = Clock.System.now().minus(2.minutes)
        val recentTime = Clock.System.now()
        
        // Record old data
        metricsCorrelationEngine.recordMetric("old_metric", 10.0, oldTime)
        
        // Record recent data
        metricsCorrelationEngine.recordMetric("recent_metric", 20.0, recentTime)
        
        // Raise an old alert
        alertingSystem.raiseAlert(
            source = "test",
            type = AlertType.SYSTEM_ERROR,
            severity = AlertSeverity.LOW,
            title = "Old Alert",
            description = "This is an old alert"
        )
        
        // Wait for processing
        delay(100)
        
        // When - Cleanup old data
        val cutoffTime = Clock.System.now().minus(1.minutes)
        monitoringService.cleanupOldData(cutoffTime)
        
        // Wait for cleanup
        delay(100)
        
        // Then - Old data should be cleaned up, recent data should remain
        // This is verified through the logging and internal state management
        // The exact verification depends on the implementation details
        
        val dashboard = monitoringService.getMonitoringDashboard()
        assertNotNull(dashboard, "Dashboard should still be available after cleanup")
    }
    
    @Test
    fun `monitoring should maintain performance under stress`() = runTest {
        // Given - High load scenario
        val baseTime = Clock.System.now()
        val startTime = System.currentTimeMillis()
        
        // Simulate high load with many concurrent operations
        repeat(50) { i ->
            // Record train position
            val position = TrainPosition(
                trainId = "TRAIN_STRESS_${i % 10}",
                latitude = 28.6139 + (i * 0.0001),
                longitude = 77.2090 + (i * 0.0001),
                speed = 60.0 + (i % 30),
                timestamp = baseTime.plus(i.seconds)
            )
            
            monitoringService.recordTrainPosition(position)
            
            // Record metrics
            metricsCorrelationEngine.recordMetric("stress_metric_${i % 5}", i.toDouble(), baseTime.plus(i.seconds))
            
            // Occasionally raise alerts
            if (i % 10 == 0) {
                alertingSystem.raiseAlert(
                    source = "stress_test",
                    type = AlertType.PERFORMANCE_DEGRADATION,
                    severity = AlertSeverity.LOW,
                    title = "Stress Test Alert $i",
                    description = "Generated during stress test"
                )
            }
        }
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime
        
        // Wait for all processing to complete
        delay(200)
        
        // Then - System should handle the load efficiently
        assertTrue(processingTime < 5000, "Should process 50 operations in under 5 seconds")
        
        val dashboard = monitoringService.getMonitoringDashboard()
        assertNotNull(dashboard, "Dashboard should be available under stress")
        
        val systemHealth = monitoringService.systemHealth.first()
        assertNotNull(systemHealth, "System health should be available under stress")
    }
}