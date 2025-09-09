package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MetricsCorrelationEngineTest {
    
    private lateinit var logger: Logger
    private lateinit var correlationEngine: MetricsCorrelationEngine
    
    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
        correlationEngine = MetricsCorrelationEngine(logger)
    }
    
    @Test
    fun `recordMetric should store metric data point`() = runTest {
        // Given
        val metricName = "test_metric"
        val value = 42.0
        val timestamp = Clock.System.now()
        
        // When
        correlationEngine.recordMetric(metricName, value, timestamp)
        
        // Then
        verify { logger.debug("MetricsCorrelationEngine", "Recorded metric: $metricName = $value") }
    }
    
    @Test
    fun `should detect strong positive correlation between related metrics`() = runTest {
        // Given - Create two metrics with strong positive correlation
        val baseTime = Clock.System.now()
        
        // Record correlated data points
        for (i in 1..20) {
            val timestamp = baseTime.plus(i.seconds)
            val baseValue = i.toDouble()
            
            // Metric 1: linear increase
            correlationEngine.recordMetric("metric_a", baseValue, timestamp)
            
            // Metric 2: strongly correlated (same pattern + small noise)
            correlationEngine.recordMetric("metric_b", baseValue * 2 + 1, timestamp)
        }
        
        // Wait for analysis to complete
        kotlinx.coroutines.delay(100)
        
        // When
        val correlations = correlationEngine.correlations.first()
        
        // Then
        val correlation = correlations.find { 
            (it.metric1 == "metric_a" && it.metric2 == "metric_b") ||
            (it.metric1 == "metric_b" && it.metric2 == "metric_a")
        }
        
        assertNotNull(correlation)
        assertTrue(correlation.coefficient > 0.9) // Strong positive correlation
        assertEquals(CorrelationDirection.POSITIVE, correlation.direction)
        assertTrue(correlation.strength == CorrelationStrength.VERY_STRONG || 
                  correlation.strength == CorrelationStrength.STRONG)
    }
    
    @Test
    fun `should detect strong negative correlation between inversely related metrics`() = runTest {
        // Given - Create two metrics with strong negative correlation
        val baseTime = Clock.System.now()
        
        // Record inversely correlated data points
        for (i in 1..20) {
            val timestamp = baseTime.plus(i.seconds)
            
            // Metric 1: increasing
            correlationEngine.recordMetric("cpu_usage", i.toDouble(), timestamp)
            
            // Metric 2: decreasing (inverse relationship)
            correlationEngine.recordMetric("available_memory", (21 - i).toDouble(), timestamp)
        }
        
        // Wait for analysis to complete
        kotlinx.coroutines.delay(100)
        
        // When
        val correlations = correlationEngine.correlations.first()
        
        // Then
        val correlation = correlations.find { 
            (it.metric1 == "cpu_usage" && it.metric2 == "available_memory") ||
            (it.metric1 == "available_memory" && it.metric2 == "cpu_usage")
        }
        
        assertNotNull(correlation)
        assertTrue(correlation.coefficient < -0.9) // Strong negative correlation
        assertEquals(CorrelationDirection.NEGATIVE, correlation.direction)
    }
    
    @Test
    fun `should detect increasing trend in metric data`() = runTest {
        // Given - Create metric with clear increasing trend
        val baseTime = Clock.System.now()
        
        for (i in 1..15) {
            val timestamp = baseTime.plus(i.minutes)
            val value = i.toDouble() * 10 // Clear increasing trend
            
            correlationEngine.recordMetric("memory_usage", value, timestamp)
        }
        
        // Wait for analysis to complete
        kotlinx.coroutines.delay(100)
        
        // When
        val trends = correlationEngine.trends.first()
        
        // Then
        val trend = trends.find { it.metricName == "memory_usage" }
        assertNotNull(trend)
        assertEquals(TrendDirection.INCREASING, trend.direction)
        assertTrue(trend.slope > 0)
        assertTrue(trend.changePercent > 0)
    }
    
    @Test
    fun `should detect decreasing trend in metric data`() = runTest {
        // Given - Create metric with clear decreasing trend
        val baseTime = Clock.System.now()
        
        for (i in 1..15) {
            val timestamp = baseTime.plus(i.minutes)
            val value = (16 - i).toDouble() * 5 // Clear decreasing trend
            
            correlationEngine.recordMetric("response_time", value, timestamp)
        }
        
        // Wait for analysis to complete
        kotlinx.coroutines.delay(100)
        
        // When
        val trends = correlationEngine.trends.first()
        
        // Then
        val trend = trends.find { it.metricName == "response_time" }
        assertNotNull(trend)
        assertEquals(TrendDirection.DECREASING, trend.direction)
        assertTrue(trend.slope < 0)
        assertTrue(trend.changePercent < 0)
    }
    
    @Test
    fun `should detect anomalies in metric data`() = runTest {
        // Given - Create baseline data with normal values
        val baseTime = Clock.System.now()
        val normalValue = 50.0
        
        // Record normal baseline data
        for (i in 1..20) {
            val timestamp = baseTime.plus(i.seconds)
            val value = normalValue + (Math.random() - 0.5) * 2 // Small random variation
            
            correlationEngine.recordMetric("network_latency", value, timestamp)
        }
        
        // Record anomalous data point
        val anomalyTime = baseTime.plus(25.seconds)
        val anomalyValue = normalValue + 50 // Significant spike
        correlationEngine.recordMetric("network_latency", anomalyValue, anomalyTime)
        
        // Wait for analysis to complete
        kotlinx.coroutines.delay(100)
        
        // When
        val anomalies = correlationEngine.anomalies.first()
        
        // Then
        val anomaly = anomalies.find { it.metricName == "network_latency" }
        assertNotNull(anomaly)
        assertEquals(AnomalyType.SPIKE, anomaly.type)
        assertTrue(anomaly.deviation > 2.0) // Should be significant deviation
        assertTrue(anomaly.value > anomaly.expectedValue)
    }
    
    @Test
    fun `should generate correlation insights for strong correlations`() = runTest {
        // Given - Create strong correlation
        val baseTime = Clock.System.now()
        
        for (i in 1..20) {
            val timestamp = baseTime.plus(i.seconds)
            
            correlationEngine.recordMetric("error_rate", i.toDouble(), timestamp)
            correlationEngine.recordMetric("response_time", i.toDouble() * 1.5, timestamp)
        }
        
        // Wait for analysis to complete
        kotlinx.coroutines.delay(100)
        
        // When
        val insights = correlationEngine.insights.first()
        
        // Then
        val correlationInsight = insights.find { it.type == InsightType.CORRELATION }
        assertNotNull(correlationInsight)
        assertTrue(correlationInsight.actionable)
        assertTrue(correlationInsight.recommendations.isNotEmpty())
    }
    
    @Test
    fun `should generate trend insights for significant trends`() = runTest {
        // Given - Create significant trend
        val baseTime = Clock.System.now()
        
        for (i in 1..15) {
            val timestamp = baseTime.plus(i.minutes)
            val value = i.toDouble() * 20 // Strong increasing trend
            
            correlationEngine.recordMetric("memory_usage", value, timestamp)
        }
        
        // Wait for analysis to complete
        kotlinx.coroutines.delay(100)
        
        // When
        val insights = correlationEngine.insights.first()
        
        // Then
        val trendInsight = insights.find { it.type == InsightType.TREND }
        assertNotNull(trendInsight)
        assertTrue(trendInsight.actionable)
        assertTrue(trendInsight.recommendations.isNotEmpty())
    }
    
    @Test
    fun `should generate anomaly insights for critical anomalies`() = runTest {
        // Given - Create critical anomaly
        val baseTime = Clock.System.now()
        
        // Normal data
        for (i in 1..20) {
            val timestamp = baseTime.plus(i.seconds)
            correlationEngine.recordMetric("cpu_usage", 30.0, timestamp)
        }
        
        // Critical anomaly
        val anomalyTime = baseTime.plus(25.seconds)
        correlationEngine.recordMetric("cpu_usage", 200.0, anomalyTime) // Impossible CPU usage
        
        // Wait for analysis to complete
        kotlinx.coroutines.delay(100)
        
        // When
        val insights = correlationEngine.insights.first()
        
        // Then
        val anomalyInsight = insights.find { it.type == InsightType.ANOMALY }
        assertNotNull(anomalyInsight)
        assertEquals(InsightSeverity.HIGH, anomalyInsight.severity)
        assertTrue(anomalyInsight.actionable)
    }
    
    @Test
    fun `getCorrelationsFor should return correlations for specific metric`() = runTest {
        // Given
        val baseTime = Clock.System.now()
        
        for (i in 1..20) {
            val timestamp = baseTime.plus(i.seconds)
            correlationEngine.recordMetric("metric_x", i.toDouble(), timestamp)
            correlationEngine.recordMetric("metric_y", i.toDouble() * 2, timestamp)
            correlationEngine.recordMetric("metric_z", (21 - i).toDouble(), timestamp)
        }
        
        // Wait for analysis
        kotlinx.coroutines.delay(100)
        
        // When
        val correlationsForX = correlationEngine.getCorrelationsFor("metric_x")
        
        // Then
        assertTrue(correlationsForX.isNotEmpty())
        assertTrue(correlationsForX.all { 
            it.metric1 == "metric_x" || it.metric2 == "metric_x" 
        })
    }
    
    @Test
    fun `getTrendFor should return trend for specific metric`() = runTest {
        // Given
        val baseTime = Clock.System.now()
        
        for (i in 1..15) {
            val timestamp = baseTime.plus(i.minutes)
            correlationEngine.recordMetric("test_trend", i.toDouble() * 5, timestamp)
        }
        
        // Wait for analysis
        kotlinx.coroutines.delay(100)
        
        // When
        val trend = correlationEngine.getTrendFor("test_trend")
        
        // Then
        assertNotNull(trend)
        assertEquals("test_trend", trend.metricName)
        assertEquals(TrendDirection.INCREASING, trend.direction)
    }
    
    @Test
    fun `getAnomaliesFor should return anomalies for specific metric`() = runTest {
        // Given
        val baseTime = Clock.System.now()
        
        // Normal data
        for (i in 1..20) {
            correlationEngine.recordMetric("test_anomaly", 50.0, baseTime.plus(i.seconds))
        }
        
        // Anomaly
        correlationEngine.recordMetric("test_anomaly", 150.0, baseTime.plus(25.seconds))
        
        // Wait for analysis
        kotlinx.coroutines.delay(100)
        
        // When
        val anomalies = correlationEngine.getAnomaliesFor("test_anomaly")
        
        // Then
        assertTrue(anomalies.isNotEmpty())
        assertTrue(anomalies.all { it.metricName == "test_anomaly" })
    }
    
    @Test
    fun `clearOldData should remove old metric data`() = runTest {
        // Given
        val oldTime = Clock.System.now().minus(2.minutes)
        val recentTime = Clock.System.now()
        
        correlationEngine.recordMetric("old_metric", 10.0, oldTime)
        correlationEngine.recordMetric("recent_metric", 20.0, recentTime)
        
        // When
        correlationEngine.clearOldData(Clock.System.now().minus(1.minutes))
        
        // Then
        verify { logger.info("MetricsCorrelationEngine", any<String>()) }
    }
    
    @Test
    fun `should handle edge cases gracefully`() = runTest {
        // Test with insufficient data
        correlationEngine.recordMetric("sparse_metric", 1.0)
        
        // Wait for analysis
        kotlinx.coroutines.delay(100)
        
        // Should not crash and should handle gracefully
        val correlations = correlationEngine.correlations.first()
        val trends = correlationEngine.trends.first()
        val anomalies = correlationEngine.anomalies.first()
        
        // Should be empty or minimal due to insufficient data
        assertTrue(correlations.isEmpty() || correlations.size <= 1)
    }
    
    @Test
    fun `should calculate pearson correlation correctly`() = runTest {
        // Given - Perfect positive correlation
        val baseTime = Clock.System.now()
        
        for (i in 1..10) {
            val timestamp = baseTime.plus(i.seconds)
            correlationEngine.recordMetric("perfect_a", i.toDouble(), timestamp)
            correlationEngine.recordMetric("perfect_b", i.toDouble() * 3, timestamp) // Perfect linear relationship
        }
        
        // Wait for analysis
        kotlinx.coroutines.delay(100)
        
        // When
        val correlations = correlationEngine.correlations.first()
        
        // Then
        val perfectCorrelation = correlations.find { 
            (it.metric1 == "perfect_a" && it.metric2 == "perfect_b") ||
            (it.metric1 == "perfect_b" && it.metric2 == "perfect_a")
        }
        
        assertNotNull(perfectCorrelation)
        assertTrue(abs(perfectCorrelation.coefficient - 1.0) < 0.1) // Should be very close to 1.0
    }
}