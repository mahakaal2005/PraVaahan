package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector
import com.example.pravaahan.core.resilience.RealTimeCircuitBreaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Performance alert levels for real-time monitoring
 */
enum class PerformanceAlertLevel {
    NORMAL,     // All systems operating normally
    WARNING,    // Performance degradation detected
    CRITICAL,   // Critical performance issues
    EMERGENCY   // System failure imminent
}

/**
 * Performance alert information
 */
data class PerformanceAlert(
    val level: PerformanceAlertLevel,
    val message: String,
    val details: String,
    val timestamp: Instant,
    val metricType: String,
    val currentValue: String,
    val threshold: String,
    val recommendedAction: String? = null
)

/**
 * Performance trend analysis
 */
data class PerformanceTrend(
    val metricName: String,
    val direction: TrendDirection,
    val changePercentage: Double,
    val timeWindow: Duration,
    val isSignificant: Boolean
)

enum class TrendDirection {
    IMPROVING, STABLE, DEGRADING
}

/**
 * System health dashboard data
 */
data class HealthDashboard(
    val overallStatus: PerformanceAlertLevel,
    val activeAlerts: List<PerformanceAlert>,
    val performanceTrends: List<PerformanceTrend>,
    val systemUptime: Duration,
    val lastUpdated: Instant,
    val keyMetrics: Map<String, String>
)

/**
 * Advanced performance monitor for real-time train position system.
 * 
 * Provides continuous monitoring, trend analysis, predictive alerting,
 * and comprehensive health dashboards for railway operations.
 */
@Singleton
class RealTimePerformanceMonitor @Inject constructor(
    private val metricsCollector: RealTimeMetricsCollector,
    private val circuitBreaker: RealTimeCircuitBreaker,
    private val alertThresholdConfig: AlertThresholdConfig,
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "RealTimePerformanceMonitor"
        
        // Performance thresholds
        private const val WARNING_LATENCY_MS = 1000L
        private const val CRITICAL_LATENCY_MS = 3000L
        private const val EMERGENCY_LATENCY_MS = 5000L
        
        private const val WARNING_ERROR_RATE = 0.02 // 2%
        private const val CRITICAL_ERROR_RATE = 0.05 // 5%
        private const val EMERGENCY_ERROR_RATE = 0.10 // 10%
        
        private const val WARNING_MEMORY_MB = 100L
        private const val CRITICAL_MEMORY_MB = 200L
        private const val EMERGENCY_MEMORY_MB = 300L
        
        // Monitoring intervals
        private val MONITORING_INTERVAL = 30.seconds
        private val TREND_ANALYSIS_WINDOW = 5.minutes
        private val ALERT_COOLDOWN = 2.minutes
    }
    
    private val monitoringScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    
    // State management
    private val _healthDashboard = MutableStateFlow(createInitialDashboard())
    val healthDashboard: StateFlow<HealthDashboard> = _healthDashboard.asStateFlow()
    
    private val _activeAlerts = MutableStateFlow<List<PerformanceAlert>>(emptyList())
    val activeAlerts: StateFlow<List<PerformanceAlert>> = _activeAlerts.asStateFlow()
    
    // Historical data for trend analysis
    private val latencyHistory = mutableListOf<Pair<Instant, Long>>()
    private val errorRateHistory = mutableListOf<Pair<Instant, Double>>()
    private val memoryUsageHistory = mutableListOf<Pair<Instant, Long>>()
    private val throughputHistory = mutableListOf<Pair<Instant, Double>>()
    
    // Alert management
    private val alertCooldowns = mutableMapOf<String, Instant>()
    private val systemStartTime = Clock.System.now()
    
    /**
     * Start continuous performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) {
            logger.warn(TAG, "Performance monitoring is already running")
            return
        }
        
        logger.info(TAG, "Starting real-time performance monitoring")
        isMonitoring = true
        
        monitoringJob = monitoringScope.launch {
            while (isMonitoring) {
                try {
                    performMonitoringCycle()
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    logger.error(TAG, "Error in monitoring cycle", e)
                    delay(MONITORING_INTERVAL)
                }
            }
        }
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        logger.info(TAG, "Stopping real-time performance monitoring")
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Get current system health status
     */
    fun getCurrentHealthStatus(): PerformanceAlertLevel {
        val currentAlerts = _activeAlerts.value
        
        return when {
            currentAlerts.any { it.level == PerformanceAlertLevel.EMERGENCY } -> PerformanceAlertLevel.EMERGENCY
            currentAlerts.any { it.level == PerformanceAlertLevel.CRITICAL } -> PerformanceAlertLevel.CRITICAL
            currentAlerts.any { it.level == PerformanceAlertLevel.WARNING } -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Force a health check and update dashboard
     */
    suspend fun forceHealthCheck() {
        logger.info(TAG, "Performing forced health check")
        performMonitoringCycle()
    }
    
    /**
     * Clear all alerts (for testing or manual intervention)
     */
    fun clearAllAlerts() {
        logger.info(TAG, "Clearing all performance alerts")
        _activeAlerts.value = emptyList()
        alertCooldowns.clear()
        updateDashboard()
    }
    
    /**
     * Get performance recommendations based on current metrics
     */
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val currentAlerts = _activeAlerts.value
        
        currentAlerts.forEach { alert ->
            alert.recommendedAction?.let { action ->
                recommendations.add(action)
            }
        }
        
        // Add general recommendations based on trends
        val trends = analyzePerformanceTrends()
        trends.forEach { trend ->
            if (trend.direction == TrendDirection.DEGRADING && trend.isSignificant) {
                when (trend.metricName) {
                    "latency" -> recommendations.add("Consider optimizing network configuration or reducing data payload size")
                    "error_rate" -> recommendations.add("Investigate error patterns and implement additional retry logic")
                    "memory_usage" -> recommendations.add("Review memory usage patterns and implement garbage collection optimization")
                    "throughput" -> recommendations.add("Consider scaling real-time processing capacity")
                }
            }
        }
        
        return recommendations.distinct()
    }
    
    private suspend fun performMonitoringCycle() {
        val now = Clock.System.now()
        
        // Collect current metrics
        val metrics = metricsCollector.metrics.value
        val circuitBreakerMetrics = circuitBreaker.metrics.value
        
        // Update historical data
        updateHistoricalData(metrics, now)
        
        // Analyze performance and generate alerts
        val newAlerts = analyzePerformanceMetrics(metrics, now)
        
        // Update active alerts
        updateActiveAlerts(newAlerts, now)
        
        // Update dashboard
        updateDashboard()
        
        logger.debug(TAG, "Monitoring cycle completed - ${_activeAlerts.value.size} active alerts")
    }
    
    private fun updateHistoricalData(metrics: com.example.pravaahan.core.monitoring.RealTimeMetrics, timestamp: Instant) {
        // Add current metrics to history
        latencyHistory.add(timestamp to metrics.performance.averageLatency.inWholeMilliseconds)
        errorRateHistory.add(timestamp to metrics.performance.errorRate)
        memoryUsageHistory.add(timestamp to (metrics.performance.memoryUsage / (1024 * 1024))) // Convert to MB
        throughputHistory.add(timestamp to metrics.performance.throughput)
        
        // Keep only recent history (last hour)
        val cutoffTime = timestamp - 60.minutes
        latencyHistory.removeAll { it.first < cutoffTime }
        errorRateHistory.removeAll { it.first < cutoffTime }
        memoryUsageHistory.removeAll { it.first < cutoffTime }
        throughputHistory.removeAll { it.first < cutoffTime }
    }
    
    private fun analyzePerformanceMetrics(
        metrics: com.example.pravaahan.core.monitoring.RealTimeMetrics, 
        timestamp: Instant
    ): List<PerformanceAlert> {
        val alerts = mutableListOf<PerformanceAlert>()
        
        // Analyze latency using configurable thresholds
        val avgLatencyMs = metrics.performance.averageLatency.inWholeMilliseconds
        val latencyLevel = alertThresholdConfig.getLatencyAlertLevel(avgLatencyMs)
        val thresholds = alertThresholdConfig.getCurrentThresholds()
        
        val latencyAlert = when (latencyLevel) {
            PerformanceAlertLevel.EMERGENCY -> createAlert(
                PerformanceAlertLevel.EMERGENCY,
                "Critical Latency Emergency",
                "Average latency is ${avgLatencyMs}ms, exceeding emergency threshold",
                timestamp,
                "latency",
                "${avgLatencyMs}ms",
                "${thresholds.emergencyLatencyMs}ms",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.EMERGENCY, "latency")
            )
            PerformanceAlertLevel.CRITICAL -> createAlert(
                PerformanceAlertLevel.CRITICAL,
                "High Latency Critical",
                "Average latency is ${avgLatencyMs}ms, exceeding critical threshold",
                timestamp,
                "latency",
                "${avgLatencyMs}ms",
                "${thresholds.criticalLatencyMs}ms",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.CRITICAL, "latency")
            )
            PerformanceAlertLevel.WARNING -> createAlert(
                PerformanceAlertLevel.WARNING,
                "Elevated Latency Warning",
                "Average latency is ${avgLatencyMs}ms, above normal levels",
                timestamp,
                "latency",
                "${avgLatencyMs}ms",
                "${thresholds.warningLatencyMs}ms",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.WARNING, "latency")
            )
            PerformanceAlertLevel.NORMAL -> null
        }
        latencyAlert?.let { alerts.add(it) }
        
        // Analyze error rate using configurable thresholds
        val errorRate = metrics.performance.errorRate
        val errorRatePercent = (errorRate * 100).toInt()
        val errorRateLevel = alertThresholdConfig.getErrorRateAlertLevel(errorRate)
        
        val errorAlert = when (errorRateLevel) {
            PerformanceAlertLevel.EMERGENCY -> createAlert(
                PerformanceAlertLevel.EMERGENCY,
                "Critical Error Rate Emergency",
                "Error rate is ${errorRatePercent}%, system stability compromised",
                timestamp,
                "error_rate",
                "${errorRatePercent}%",
                "${(thresholds.emergencyErrorRate * 100).toInt()}%",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.EMERGENCY, "error_rate")
            )
            PerformanceAlertLevel.CRITICAL -> createAlert(
                PerformanceAlertLevel.CRITICAL,
                "High Error Rate Critical",
                "Error rate is ${errorRatePercent}%, exceeding acceptable limits",
                timestamp,
                "error_rate",
                "${errorRatePercent}%",
                "${(thresholds.criticalErrorRate * 100).toInt()}%",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.CRITICAL, "error_rate")
            )
            PerformanceAlertLevel.WARNING -> createAlert(
                PerformanceAlertLevel.WARNING,
                "Elevated Error Rate Warning",
                "Error rate is ${errorRatePercent}%, above normal levels",
                timestamp,
                "error_rate",
                "${errorRatePercent}%",
                "${(thresholds.warningErrorRate * 100).toInt()}%",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.WARNING, "error_rate")
            )
            PerformanceAlertLevel.NORMAL -> null
        }
        errorAlert?.let { alerts.add(it) }
        
        // Analyze memory usage using configurable thresholds
        val memoryMB = metrics.performance.memoryUsage / (1024 * 1024)
        val memoryLevel = alertThresholdConfig.getMemoryAlertLevel(memoryMB)
        
        val memoryAlert = when (memoryLevel) {
            PerformanceAlertLevel.EMERGENCY -> createAlert(
                PerformanceAlertLevel.EMERGENCY,
                "Critical Memory Usage Emergency",
                "Memory usage is ${memoryMB}MB, system may become unstable",
                timestamp,
                "memory_usage",
                "${memoryMB}MB",
                "${thresholds.emergencyMemoryMB}MB",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.EMERGENCY, "memory_usage")
            )
            PerformanceAlertLevel.CRITICAL -> createAlert(
                PerformanceAlertLevel.CRITICAL,
                "High Memory Usage Critical",
                "Memory usage is ${memoryMB}MB, approaching system limits",
                timestamp,
                "memory_usage",
                "${memoryMB}MB",
                "${thresholds.criticalMemoryMB}MB",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.CRITICAL, "memory_usage")
            )
            PerformanceAlertLevel.WARNING -> createAlert(
                PerformanceAlertLevel.WARNING,
                "Elevated Memory Usage Warning",
                "Memory usage is ${memoryMB}MB, above normal levels",
                timestamp,
                "memory_usage",
                "${memoryMB}MB",
                "${thresholds.warningMemoryMB}MB",
                alertThresholdConfig.getRecommendedAction(PerformanceAlertLevel.WARNING, "memory_usage")
            )
            PerformanceAlertLevel.NORMAL -> null
        }
        memoryAlert?.let { alerts.add(it) }
        
        return alerts
    }
    
    private fun createAlert(
        level: PerformanceAlertLevel,
        message: String,
        details: String,
        timestamp: Instant,
        metricType: String,
        currentValue: String,
        threshold: String,
        recommendedAction: String? = null
    ): PerformanceAlert? {
        
        // Check cooldown to prevent alert spam
        val alertKey = "${level}_${metricType}"
        val lastAlert = alertCooldowns[alertKey]
        
        if (lastAlert != null && (timestamp - lastAlert) < ALERT_COOLDOWN) {
            return null // Still in cooldown period
        }
        
        alertCooldowns[alertKey] = timestamp
        
        return PerformanceAlert(
            level = level,
            message = message,
            details = details,
            timestamp = timestamp,
            metricType = metricType,
            currentValue = currentValue,
            threshold = threshold,
            recommendedAction = recommendedAction
        )
    }
    
    private fun updateActiveAlerts(newAlerts: List<PerformanceAlert>, timestamp: Instant) {
        val currentAlerts = _activeAlerts.value.toMutableList()
        
        // Remove old alerts (older than 10 minutes)
        val cutoffTime = timestamp - 10.minutes
        currentAlerts.removeAll { it.timestamp < cutoffTime }
        
        // Add new alerts
        currentAlerts.addAll(newAlerts)
        
        // Sort by severity and timestamp
        currentAlerts.sortWith(compareBy<PerformanceAlert> { 
            when (it.level) {
                PerformanceAlertLevel.EMERGENCY -> 0
                PerformanceAlertLevel.CRITICAL -> 1
                PerformanceAlertLevel.WARNING -> 2
                PerformanceAlertLevel.NORMAL -> 3
            }
        }.thenByDescending { it.timestamp })
        
        _activeAlerts.value = currentAlerts
        
        // Log new alerts
        newAlerts.forEach { alert ->
            when (alert.level) {
                PerformanceAlertLevel.EMERGENCY -> logger.error(TAG, "🚨 EMERGENCY: ${alert.message} - ${alert.details}")
                PerformanceAlertLevel.CRITICAL -> logger.error(TAG, "❌ CRITICAL: ${alert.message} - ${alert.details}")
                PerformanceAlertLevel.WARNING -> logger.warn(TAG, "⚠️ WARNING: ${alert.message} - ${alert.details}")
                PerformanceAlertLevel.NORMAL -> logger.info(TAG, "ℹ️ INFO: ${alert.message} - ${alert.details}")
            }
        }
    }
    
    private fun analyzePerformanceTrends(): List<PerformanceTrend> {
        val trends = mutableListOf<PerformanceTrend>()
        val now = Clock.System.now()
        val windowStart = now - TREND_ANALYSIS_WINDOW
        
        // Analyze latency trend
        val recentLatency = latencyHistory.filter { it.first >= windowStart }
        if (recentLatency.size >= 2) {
            val latencyTrend = calculateTrend(recentLatency.map { it.second.toDouble() })
            trends.add(PerformanceTrend(
                metricName = "latency",
                direction = latencyTrend.direction,
                changePercentage = latencyTrend.changePercentage,
                timeWindow = TREND_ANALYSIS_WINDOW,
                isSignificant = latencyTrend.isSignificant
            ))
        }
        
        // Analyze error rate trend
        val recentErrorRate = errorRateHistory.filter { it.first >= windowStart }
        if (recentErrorRate.size >= 2) {
            val errorTrend = calculateTrend(recentErrorRate.map { it.second })
            trends.add(PerformanceTrend(
                metricName = "error_rate",
                direction = errorTrend.direction,
                changePercentage = errorTrend.changePercentage,
                timeWindow = TREND_ANALYSIS_WINDOW,
                isSignificant = errorTrend.isSignificant
            ))
        }
        
        return trends
    }
    
    private fun calculateTrend(values: List<Double>): TrendAnalysis {
        if (values.size < 2) return TrendAnalysis(TrendDirection.STABLE, 0.0, false)
        
        val firstHalf = values.take(values.size / 2)
        val secondHalf = values.drop(values.size / 2)
        
        val firstAvg = firstHalf.average()
        val secondAvg = secondHalf.average()
        
        val changePercentage = if (firstAvg != 0.0) {
            ((secondAvg - firstAvg) / firstAvg) * 100
        } else {
            0.0
        }
        
        val direction = when {
            changePercentage > 5.0 -> TrendDirection.DEGRADING
            changePercentage < -5.0 -> TrendDirection.IMPROVING
            else -> TrendDirection.STABLE
        }
        
        val isSignificant = kotlin.math.abs(changePercentage) > 10.0
        
        return TrendAnalysis(direction, changePercentage, isSignificant)
    }
    
    private fun updateDashboard() {
        val now = Clock.System.now()
        val uptime = now - systemStartTime
        val currentAlerts = _activeAlerts.value
        val trends = analyzePerformanceTrends()
        val overallStatus = getCurrentHealthStatus()
        
        val keyMetrics = mutableMapOf<String, String>()
        
        // Add key metrics from latest data
        if (latencyHistory.isNotEmpty()) {
            keyMetrics["Average Latency"] = "${latencyHistory.last().second}ms"
        }
        if (errorRateHistory.isNotEmpty()) {
            keyMetrics["Error Rate"] = "${(errorRateHistory.last().second * 100).toInt()}%"
        }
        if (memoryUsageHistory.isNotEmpty()) {
            keyMetrics["Memory Usage"] = "${memoryUsageHistory.last().second}MB"
        }
        if (throughputHistory.isNotEmpty()) {
            keyMetrics["Throughput"] = "${throughputHistory.last().second.toInt()} msg/sec"
        }
        
        val dashboard = HealthDashboard(
            overallStatus = overallStatus,
            activeAlerts = currentAlerts,
            performanceTrends = trends,
            systemUptime = uptime,
            lastUpdated = now,
            keyMetrics = keyMetrics
        )
        
        _healthDashboard.value = dashboard
    }
    
    private fun createInitialDashboard(): HealthDashboard {
        return HealthDashboard(
            overallStatus = PerformanceAlertLevel.NORMAL,
            activeAlerts = emptyList(),
            performanceTrends = emptyList(),
            systemUptime = Duration.ZERO,
            lastUpdated = Clock.System.now(),
            keyMetrics = emptyMap()
        )
    }
    
    private data class TrendAnalysis(
        val direction: TrendDirection,
        val changePercentage: Double,
        val isSignificant: Boolean
    )
}