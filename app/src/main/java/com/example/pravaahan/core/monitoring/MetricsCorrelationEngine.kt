package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.logging.logPerformanceMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Metrics correlation engine for analyzing relationships between different system metrics.
 * Identifies patterns, trends, and correlations to provide insights for system optimization.
 */
@Singleton
class MetricsCorrelationEngine @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "MetricsCorrelationEngine"
        private const val ANALYSIS_INTERVAL_MS = 60_000L // 1 minute
        private const val CORRELATION_THRESHOLD = 0.7 // Strong correlation threshold
        private const val TREND_ANALYSIS_WINDOW_MINUTES = 15L
        private const val MAX_METRICS_HISTORY = 1000
        private const val ANOMALY_DETECTION_SENSITIVITY = 2.0 // Standard deviations
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _correlations = MutableStateFlow<List<MetricCorrelation>>(emptyList())
    val correlations: StateFlow<List<MetricCorrelation>> = _correlations.asStateFlow()
    
    private val _trends = MutableStateFlow<List<MetricTrend>>(emptyList())
    val trends: StateFlow<List<MetricTrend>> = _trends.asStateFlow()
    
    private val _anomalies = MutableStateFlow<List<MetricAnomaly>>(emptyList())
    val anomalies: StateFlow<List<MetricAnomaly>> = _anomalies.asStateFlow()
    
    private val _insights = MutableStateFlow<List<SystemInsight>>(emptyList())
    val insights: StateFlow<List<SystemInsight>> = _insights.asStateFlow()
    
    // Metrics storage for analysis
    private val metricsHistory = mutableMapOf<String, MutableList<MetricDataPoint>>()
    
    init {
        logger.info(TAG, "MetricsCorrelationEngine initialized")
        startPeriodicAnalysis()
    }

    /**
     * Record a metric data point for analysis
     */
    fun recordMetric(
        metricName: String,
        value: Double,
        timestamp: Instant = Clock.System.now(),
        metadata: Map<String, String> = emptyMap()
    ) {
        val dataPoint = MetricDataPoint(
            metricName = metricName,
            value = value,
            timestamp = timestamp,
            metadata = metadata
        )
        
        val history = metricsHistory.getOrPut(metricName) { mutableListOf() }
        history.add(dataPoint)
        
        // Keep only recent data
        if (history.size > MAX_METRICS_HISTORY) {
            history.removeAt(0)
        }
        
        logger.debug(TAG, "Recorded metric: $metricName = $value")
    }
    
    /**
     * Start periodic analysis of metrics
     */
    private fun startPeriodicAnalysis() {
        scope.launch {
            while (true) {
                try {
                    performAnalysis()
                    delay(ANALYSIS_INTERVAL_MS)
                } catch (e: Exception) {
                    logger.error(TAG, "Error during periodic analysis", e)
                    delay(ANALYSIS_INTERVAL_MS)
                }
            }
        }
    }
    
    /**
     * Perform comprehensive metrics analysis
     */
    private fun performAnalysis() {
        logger.debug(TAG, "Starting metrics correlation analysis")
        
        // Analyze correlations between metrics
        analyzeCorrelations()
        
        // Analyze trends in individual metrics
        analyzeTrends()
        
        // Detect anomalies
        detectAnomalies()
        
        // Generate system insights
        generateInsights()
        
        logger.logPerformanceMetric(TAG, "analysis_completed", metricsHistory.size.toFloat(), "metrics")
    }
    
    /**
     * Analyze correlations between different metrics
     */
    private fun analyzeCorrelations() {
        val metricNames = metricsHistory.keys.toList()
        val correlations = mutableListOf<MetricCorrelation>()
        
        // Compare each pair of metrics
        for (i in metricNames.indices) {
            for (j in i + 1 until metricNames.size) {
                val metric1 = metricNames[i]
                val metric2 = metricNames[j]
                
                val correlation = calculateCorrelation(metric1, metric2)
                if (correlation != null && abs(correlation.coefficient) >= CORRELATION_THRESHOLD) {
                    correlations.add(correlation)
                }
            }
        }
        
        _correlations.value = correlations
        
        if (correlations.isNotEmpty()) {
            logger.info(TAG, "Found ${correlations.size} significant correlations")
        }
    }
    
    /**
     * Calculate correlation coefficient between two metrics
     */
    private fun calculateCorrelation(metric1Name: String, metric2Name: String): MetricCorrelation? {
        val data1 = metricsHistory[metric1Name] ?: return null
        val data2 = metricsHistory[metric2Name] ?: return null
        
        if (data1.size < 10 || data2.size < 10) return null // Need sufficient data
        
        // Align data points by timestamp (take last N points)
        val alignedData = alignMetricData(data1, data2, 50)
        if (alignedData.first.size < 10) return null
        
        val values1 = alignedData.first
        val values2 = alignedData.second
        
        val coefficient = pearsonCorrelation(values1, values2)
        
        return MetricCorrelation(
            metric1 = metric1Name,
            metric2 = metric2Name,
            coefficient = coefficient,
            strength = when {
                abs(coefficient) >= 0.9 -> CorrelationStrength.VERY_STRONG
                abs(coefficient) >= 0.7 -> CorrelationStrength.STRONG
                abs(coefficient) >= 0.5 -> CorrelationStrength.MODERATE
                abs(coefficient) >= 0.3 -> CorrelationStrength.WEAK
                else -> CorrelationStrength.VERY_WEAK
            },
            direction = if (coefficient > 0) CorrelationDirection.POSITIVE else CorrelationDirection.NEGATIVE,
            timestamp = Clock.System.now(),
            sampleSize = values1.size
        )
    }
    
    /**
     * Align two metric datasets by timestamp
     */
    private fun alignMetricData(
        data1: List<MetricDataPoint>, 
        data2: List<MetricDataPoint>,
        maxPoints: Int
    ): Pair<List<Double>, List<Double>> {
        val aligned1 = mutableListOf<Double>()
        val aligned2 = mutableListOf<Double>()
        
        // Take recent data points
        val recent1 = data1.takeLast(maxPoints)
        val recent2 = data2.takeLast(maxPoints)
        
        // Simple alignment - match by closest timestamps
        for (point1 in recent1) {
            val closestPoint2 = recent2.minByOrNull { 
                abs((it.timestamp - point1.timestamp).inWholeMilliseconds) 
            }
            
            if (closestPoint2 != null) {
                val timeDiff = abs((closestPoint2.timestamp - point1.timestamp).inWholeMilliseconds)
                if (timeDiff < 60_000) { // Within 1 minute
                    aligned1.add(point1.value)
                    aligned2.add(closestPoint2.value)
                }
            }
        }
        
        return Pair(aligned1, aligned2)
    }
    
    /**
     * Calculate Pearson correlation coefficient
     */
    private fun pearsonCorrelation(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.isEmpty()) return 0.0
        
        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { a, b -> a * b }.sum()
        val sumX2 = x.map { it * it }.sum()
        val sumY2 = y.map { it * it }.sum()
        
        val numerator = n * sumXY - sumX * sumY
        val denominator = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    /**
     * Analyze trends in individual metrics
     */
    private fun analyzeTrends() {
        val trends = mutableListOf<MetricTrend>()
        val cutoffTime = Clock.System.now().minus(TREND_ANALYSIS_WINDOW_MINUTES.minutes)
        
        metricsHistory.forEach { (metricName, data) ->
            val recentData = data.filter { it.timestamp > cutoffTime }
            if (recentData.size >= 5) {
                val trend = calculateTrend(metricName, recentData)
                if (trend != null) {
                    trends.add(trend)
                }
            }
        }
        
        _trends.value = trends
        
        if (trends.isNotEmpty()) {
            logger.info(TAG, "Analyzed trends for ${trends.size} metrics")
        }
    }
    
    /**
     * Calculate trend for a metric
     */
    private fun calculateTrend(metricName: String, data: List<MetricDataPoint>): MetricTrend? {
        if (data.size < 5) return null
        
        // Calculate linear regression slope
        val n = data.size
        val x = (0 until n).map { it.toDouble() }
        val y = data.map { it.value }
        
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { a, b -> a * b }.sum()
        val sumX2 = x.map { it * it }.sum()
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n
        
        // Calculate R-squared
        val yMean = y.average()
        val ssRes = y.zip(x) { yi, xi -> (yi - (slope * xi + intercept)).let { it * it } }.sum()
        val ssTot = y.map { (it - yMean).let { diff -> diff * diff } }.sum()
        val rSquared = if (ssTot != 0.0) 1 - (ssRes / ssTot) else 0.0
        
        val direction = when {
            slope > 0.1 -> TrendDirection.INCREASING
            slope < -0.1 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
        
        val strength = when {
            rSquared >= 0.8 -> TrendStrength.STRONG
            rSquared >= 0.6 -> TrendStrength.MODERATE
            rSquared >= 0.4 -> TrendStrength.WEAK
            else -> TrendStrength.VERY_WEAK
        }
        
        return MetricTrend(
            metricName = metricName,
            direction = direction,
            strength = strength,
            slope = slope,
            rSquared = rSquared,
            startValue = data.first().value,
            endValue = data.last().value,
            changePercent = if (data.first().value != 0.0) {
                ((data.last().value - data.first().value) / data.first().value) * 100
            } else 0.0,
            timestamp = Clock.System.now(),
            sampleSize = data.size
        )
    }
    
    /**
     * Detect anomalies in metrics
     */
    private fun detectAnomalies() {
        val anomalies = mutableListOf<MetricAnomaly>()
        val cutoffTime = Clock.System.now().minus(5.minutes)
        
        metricsHistory.forEach { (metricName, data) ->
            val recentData = data.filter { it.timestamp > cutoffTime }
            if (recentData.size >= 10) {
                val detectedAnomalies = detectMetricAnomalies(metricName, data, recentData)
                anomalies.addAll(detectedAnomalies)
            }
        }
        
        _anomalies.value = anomalies
        
        if (anomalies.isNotEmpty()) {
            logger.warn(TAG, "Detected ${anomalies.size} metric anomalies")
        }
    }
    
    /**
     * Detect anomalies in a specific metric
     */
    private fun detectMetricAnomalies(
        metricName: String,
        historicalData: List<MetricDataPoint>,
        recentData: List<MetricDataPoint>
    ): List<MetricAnomaly> {
        val anomalies = mutableListOf<MetricAnomaly>()
        
        // Calculate baseline statistics from historical data
        val historicalValues = historicalData.map { it.value }
        val mean = historicalValues.average()
        val stdDev = calculateStandardDeviation(historicalValues, mean)
        
        // Check recent data points for anomalies
        recentData.forEach { dataPoint ->
            val zScore = if (stdDev > 0) abs(dataPoint.value - mean) / stdDev else 0.0
            
            if (zScore > ANOMALY_DETECTION_SENSITIVITY) {
                val severity = when {
                    zScore > 4.0 -> AnomalySeverity.CRITICAL
                    zScore > 3.0 -> AnomalySeverity.HIGH
                    zScore > 2.5 -> AnomalySeverity.MEDIUM
                    else -> AnomalySeverity.LOW
                }
                
                val type = if (dataPoint.value > mean) {
                    AnomalyType.SPIKE
                } else {
                    AnomalyType.DIP
                }
                
                anomalies.add(
                    MetricAnomaly(
                        metricName = metricName,
                        type = type,
                        severity = severity,
                        value = dataPoint.value,
                        expectedValue = mean,
                        deviation = zScore,
                        timestamp = dataPoint.timestamp,
                        description = "Metric $metricName showed ${type.name.lowercase()} " +
                                "with value ${dataPoint.value} (expected ~${"%.2f".format(mean)}, " +
                                "deviation: ${"%.2f".format(zScore)}σ)"
                    )
                )
            }
        }
        
        return anomalies
    }
    
    /**
     * Calculate standard deviation
     */
    private fun calculateStandardDeviation(values: List<Double>, mean: Double): Double {
        if (values.size < 2) return 0.0
        
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
    
    /**
     * Generate system insights based on analysis results
     */
    private fun generateInsights() {
        val insights = mutableListOf<SystemInsight>()
        
        // Analyze correlations for insights
        _correlations.value.forEach { correlation ->
            if (correlation.strength == CorrelationStrength.STRONG || 
                correlation.strength == CorrelationStrength.VERY_STRONG) {
                
                val insight = SystemInsight(
                    type = InsightType.CORRELATION,
                    title = "Strong Correlation Detected",
                    description = "Metrics ${correlation.metric1} and ${correlation.metric2} " +
                            "show ${correlation.strength.name.lowercase()} ${correlation.direction.name.lowercase()} " +
                            "correlation (${String.format("%.3f", correlation.coefficient)})",
                    severity = InsightSeverity.INFO,
                    actionable = true,
                    recommendations = listOf(
                        "Monitor both metrics together for system optimization",
                        "Consider ${correlation.metric1} as a leading indicator for ${correlation.metric2}",
                        "Investigate root cause of correlation for potential system improvements"
                    ),
                    timestamp = Clock.System.now(),
                    metadata = mapOf(
                        "metric1" to correlation.metric1,
                        "metric2" to correlation.metric2,
                        "coefficient" to correlation.coefficient.toString()
                    )
                )
                insights.add(insight)
            }
        }
        
        // Analyze trends for insights
        _trends.value.forEach { trend ->
            if (trend.strength == TrendStrength.STRONG && 
                trend.direction != TrendDirection.STABLE) {
                
                val severity = when {
                    abs(trend.changePercent) > 50 -> InsightSeverity.HIGH
                    abs(trend.changePercent) > 25 -> InsightSeverity.MEDIUM
                    else -> InsightSeverity.LOW
                }
                
                val insight = SystemInsight(
                    type = InsightType.TREND,
                    title = "Significant Trend Detected",
                    description = "Metric ${trend.metricName} shows ${trend.direction.name.lowercase()} " +
                            "trend with ${String.format("%.1f", abs(trend.changePercent))}% change " +
                            "(R² = ${String.format("%.3f", trend.rSquared)})",
                    severity = severity,
                    actionable = true,
                    recommendations = generateTrendRecommendations(trend),
                    timestamp = Clock.System.now(),
                    metadata = mapOf(
                        "metric" to trend.metricName,
                        "direction" to trend.direction.name,
                        "changePercent" to trend.changePercent.toString(),
                        "rSquared" to trend.rSquared.toString()
                    )
                )
                insights.add(insight)
            }
        }
        
        // Analyze anomalies for insights
        val criticalAnomalies = _anomalies.value.filter { 
            it.severity == AnomalySeverity.CRITICAL || it.severity == AnomalySeverity.HIGH 
        }
        
        if (criticalAnomalies.isNotEmpty()) {
            val insight = SystemInsight(
                type = InsightType.ANOMALY,
                title = "Critical Anomalies Detected",
                description = "Detected ${criticalAnomalies.size} critical anomalies across " +
                        "${criticalAnomalies.map { it.metricName }.distinct().size} metrics",
                severity = InsightSeverity.HIGH,
                actionable = true,
                recommendations = listOf(
                    "Investigate root cause of anomalies immediately",
                    "Check system resources and external dependencies",
                    "Review recent configuration changes",
                    "Consider scaling resources if performance-related"
                ),
                timestamp = Clock.System.now(),
                metadata = mapOf(
                    "anomalyCount" to criticalAnomalies.size.toString(),
                    "affectedMetrics" to criticalAnomalies.map { it.metricName }.distinct().joinToString(",")
                )
            )
            insights.add(insight)
        }
        
        _insights.value = insights
        
        if (insights.isNotEmpty()) {
            logger.info(TAG, "Generated ${insights.size} system insights")
        }
    }
    
    /**
     * Generate recommendations based on trend analysis
     */
    private fun generateTrendRecommendations(trend: MetricTrend): List<String> {
        return when (trend.metricName.lowercase()) {
            "memory_usage", "heap_usage" -> when (trend.direction) {
                TrendDirection.INCREASING -> listOf(
                    "Monitor memory usage closely to prevent OutOfMemoryError",
                    "Consider optimizing memory allocation patterns",
                    "Review object lifecycle management",
                    "Enable memory profiling to identify leaks"
                )
                TrendDirection.DECREASING -> listOf(
                    "Memory usage optimization appears successful",
                    "Continue monitoring to ensure stability"
                )
                TrendDirection.STABLE -> listOf("Memory usage is stable")
            }
            
            "cpu_usage", "processor_usage" -> when (trend.direction) {
                TrendDirection.INCREASING -> listOf(
                    "High CPU usage trend detected",
                    "Consider optimizing computational algorithms",
                    "Review background task scheduling",
                    "Monitor for CPU-intensive operations"
                )
                TrendDirection.DECREASING -> listOf(
                    "CPU usage optimization showing positive results",
                    "Continue current optimization strategies"
                )
                TrendDirection.STABLE -> listOf("CPU usage is stable")
            }
            
            "network_latency", "response_time" -> when (trend.direction) {
                TrendDirection.INCREASING -> listOf(
                    "Network performance degradation detected",
                    "Check network connectivity and bandwidth",
                    "Review API endpoint performance",
                    "Consider implementing request caching"
                )
                TrendDirection.DECREASING -> listOf(
                    "Network performance improvements detected",
                    "Continue current optimization efforts"
                )
                TrendDirection.STABLE -> listOf("Network performance is stable")
            }
            
            else -> listOf(
                "Monitor ${trend.metricName} trend closely",
                "Investigate potential causes of ${trend.direction.name.lowercase()} trend",
                "Consider setting up alerts for this metric"
            )
        }
    }
    
    /**
     * Get correlation analysis for specific metrics
     */
    fun getCorrelationsFor(metricName: String): List<MetricCorrelation> {
        return _correlations.value.filter { 
            it.metric1 == metricName || it.metric2 == metricName 
        }
    }
    
    /**
     * Get trend analysis for specific metric
     */
    fun getTrendFor(metricName: String): MetricTrend? {
        return _trends.value.find { it.metricName == metricName }
    }
    
    /**
     * Get anomalies for specific metric
     */
    fun getAnomaliesFor(metricName: String): List<MetricAnomaly> {
        return _anomalies.value.filter { it.metricName == metricName }
    }
    
    /**
     * Clear old metrics data
     */
    fun clearOldData(olderThan: Instant) {
        metricsHistory.forEach { (_, data) ->
            data.removeAll { it.timestamp < olderThan }
        }
        logger.info(TAG, "Cleared metrics data older than $olderThan")
    }
}

// Data Classes and Enums

/**
 * Represents a single metric data point
 */
data class MetricDataPoint(
    val metricName: String,
    val value: Double,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Represents correlation between two metrics
 */
data class MetricCorrelation(
    val metric1: String,
    val metric2: String,
    val coefficient: Double,
    val strength: CorrelationStrength,
    val direction: CorrelationDirection,
    val timestamp: Instant,
    val sampleSize: Int
)

/**
 * Represents trend analysis of a metric
 */
data class MetricTrend(
    val metricName: String,
    val direction: TrendDirection,
    val strength: TrendStrength,
    val slope: Double,
    val rSquared: Double,
    val startValue: Double,
    val endValue: Double,
    val changePercent: Double,
    val timestamp: Instant,
    val sampleSize: Int
)

/**
 * Represents a detected anomaly in metrics
 */
data class MetricAnomaly(
    val metricName: String,
    val type: AnomalyType,
    val severity: AnomalySeverity,
    val value: Double,
    val expectedValue: Double,
    val deviation: Double,
    val timestamp: Instant,
    val description: String
)

/**
 * Represents a system insight generated from analysis
 */
data class SystemInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val severity: InsightSeverity,
    val actionable: Boolean,
    val recommendations: List<String>,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

// Enums

enum class CorrelationStrength {
    VERY_WEAK, WEAK, MODERATE, STRONG, VERY_STRONG
}

enum class CorrelationDirection {
    POSITIVE, NEGATIVE
}

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

enum class TrendStrength {
    VERY_WEAK, WEAK, MODERATE, STRONG
}

enum class AnomalyType {
    SPIKE, DIP, PATTERN_BREAK, SUSTAINED_DEVIATION
}

enum class AnomalySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class InsightType {
    CORRELATION, TREND, ANOMALY, PERFORMANCE, OPTIMIZATION
}

enum class InsightSeverity {
    INFO, LOW, MEDIUM, HIGH, CRITICAL
}