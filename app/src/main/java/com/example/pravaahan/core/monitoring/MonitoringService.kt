package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.logging.logPerformanceMetric
import com.example.pravaahan.domain.model.TrainPosition
import com.example.pravaahan.core.monitoring.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Central monitoring service that coordinates all monitoring components
 * and provides unified monitoring capabilities for the PraVaahan system.
 */
@Singleton
class MonitoringService @Inject constructor(
    private val metricsCorrelationEngine: MetricsCorrelationEngine,
    private val alertingSystem: AlertingSystem,
    private val realTimeMetricsCollector: RealTimeMetricsCollector,
    private val memoryLeakDetector: MemoryLeakDetector,
    private val securityEventMonitor: SecurityEventMonitor,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "MonitoringService"
        private const val HEALTH_CHECK_INTERVAL_MS = 60_000L // 1 minute
        private const val METRICS_SYNC_INTERVAL_MS = 30_000L // 30 seconds
        private const val CORRELATION_ALERT_THRESHOLD = 0.8 // Strong correlation threshold for alerts
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _systemHealth = MutableStateFlow(SystemHealth())
    val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()
    
    private val _monitoringStatistics = MutableStateFlow(MonitoringStatistics())
    val monitoringStatistics: StateFlow<MonitoringStatistics> = _monitoringStatistics.asStateFlow()
    
    private var isInitialized = false
    
    init {
        logger.info(TAG, "MonitoringService initializing comprehensive monitoring")
        initializeMonitoring()
    }

    /**
     * Initialize the monitoring system
     */
    private fun initializeMonitoring() {
        if (isInitialized) return
        
        scope.launch {
            try {
                // Start health monitoring
                startHealthMonitoring()
                
                // Start metrics synchronization
                startMetricsSynchronization()
                
                // Start correlation-based alerting
                startCorrelationAlerting()
                
                // Start automated insights generation
                startInsightsGeneration()
                
                isInitialized = true
                logger.info(TAG, "MonitoringService fully initialized")
                
            } catch (e: Exception) {
                logger.error(TAG, "Error initializing monitoring service", e)
            }
        }
    }
    
    /**
     * Start continuous system health monitoring
     */
    private fun startHealthMonitoring() {
        scope.launch {
            while (true) {
                try {
                    updateSystemHealth()
                    delay(HEALTH_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    logger.error(TAG, "Error in health monitoring", e)
                    delay(HEALTH_CHECK_INTERVAL_MS)
                }
            }
        }
    }
    
    /**
     * Start metrics synchronization between components
     */
    private fun startMetricsSynchronization() {
        scope.launch {
            while (true) {
                try {
                    synchronizeMetrics()
                    delay(METRICS_SYNC_INTERVAL_MS)
                } catch (e: Exception) {
                    logger.error(TAG, "Error in metrics synchronization", e)
                    delay(METRICS_SYNC_INTERVAL_MS)
                }
            }
        }
    }
    
    /**
     * Start correlation-based alerting
     */
    private fun startCorrelationAlerting() {
        scope.launch {
            metricsCorrelationEngine.correlations.collect { correlations ->
                try {
                    processCorrelationAlerts(correlations)
                } catch (e: Exception) {
                    logger.error(TAG, "Error processing correlation alerts", e)
                }
            }
        }
        
        scope.launch {
            metricsCorrelationEngine.anomalies.collect { anomalies ->
                try {
                    processAnomalyAlerts(anomalies)
                } catch (e: Exception) {
                    logger.error(TAG, "Error processing anomaly alerts", e)
                }
            }
        }
    }
    
    /**
     * Start automated insights generation
     */
    private fun startInsightsGeneration() {
        scope.launch {
            metricsCorrelationEngine.insights.collect { insights ->
                try {
                    processSystemInsights(insights)
                } catch (e: Exception) {
                    logger.error(TAG, "Error processing system insights", e)
                }
            }
        }
    }
    
    /**
     * Record a train position update and trigger monitoring
     */
    suspend fun recordTrainPosition(position: TrainPosition) {
        try {
            // Record in real-time metrics collector
            realTimeMetricsCollector.recordMessageReceived(position)
            
            // Record metrics for correlation analysis
            recordPositionMetrics(position)
            
            // Security validation
            validatePositionSecurity(position)
            
        } catch (e: Exception) {
            logger.error(TAG, "Error recording train position", e)
            realTimeMetricsCollector.recordConnectionError(e)
        }
    }
    
    /**
     * Record performance metrics for correlation analysis
     */
    private fun recordPositionMetrics(position: TrainPosition) {
        val now = Clock.System.now()
        
        // Record latency metric
        val latency = (now - position.timestamp).inWholeMilliseconds.toDouble()
        metricsCorrelationEngine.recordMetric("position_latency_ms", latency, now)
        
        // Record speed metric
        metricsCorrelationEngine.recordMetric("train_speed_kmh", position.speed, now, 
            mapOf("train_id" to position.trainId))
        
        // Record accuracy metric if available
        position.accuracy?.let { accuracy ->
            metricsCorrelationEngine.recordMetric("position_accuracy_m", accuracy, now,
                mapOf("train_id" to position.trainId))
        }
    }
    
    /**
     * Validate position data for security issues
     */
    private suspend fun validatePositionSecurity(position: TrainPosition) {
        // Check for impossible speed values
        if (position.speed > 200.0) { // 200 km/h is very high for trains
            // Use security event monitor to analyze position
            securityEventMonitor.analyzePosition(position)
            
            realTimeMetricsCollector.recordValidationFailure(
                position.trainId, 
                "Impossible speed: ${position.speed} km/h"
            )
        }
        
        // Check for stale data
        val dataAge = Clock.System.now() - position.timestamp
        if (dataAge > 5.minutes) {
            realTimeMetricsCollector.recordValidationFailure(
                position.trainId,
                "Stale position data: ${dataAge.inWholeMinutes} minutes old"
            )
        }
    }
    
    /**
     * Process correlation alerts
     */
    private fun processCorrelationAlerts(correlations: List<MetricCorrelation>) {
        correlations.forEach { correlation ->
            if (correlation.strength == CorrelationStrength.VERY_STRONG && 
                kotlin.math.abs(correlation.coefficient) >= CORRELATION_ALERT_THRESHOLD) {
                
                val severity = when {
                    correlation.metric1.contains("error") || correlation.metric2.contains("error") -> AlertSeverity.HIGH
                    correlation.metric1.contains("latency") || correlation.metric2.contains("latency") -> AlertSeverity.MEDIUM
                    else -> AlertSeverity.LOW
                }
                
                alertingSystem.raiseAlert(
                    source = "correlation_engine",
                    type = AlertType.PERFORMANCE_DEGRADATION,
                    severity = severity,
                    title = "Strong Metric Correlation Detected",
                    description = "Strong ${correlation.direction.name.lowercase()} correlation " +
                            "(${String.format("%.3f", correlation.coefficient)}) detected between " +
                            "${correlation.metric1} and ${correlation.metric2}",
                    metadata = mapOf(
                        "metric1" to correlation.metric1,
                        "metric2" to correlation.metric2,
                        "coefficient" to correlation.coefficient.toString(),
                        "strength" to correlation.strength.name,
                        "sample_size" to correlation.sampleSize.toString()
                    )
                )
            }
        }
    }
    
    /**
     * Process anomaly alerts
     */
    private fun processAnomalyAlerts(anomalies: List<MetricAnomaly>) {
        anomalies.forEach { anomaly ->
            val alertSeverity = when (anomaly.severity) {
                AnomalySeverity.CRITICAL -> AlertSeverity.CRITICAL
                AnomalySeverity.HIGH -> AlertSeverity.HIGH
                AnomalySeverity.MEDIUM -> AlertSeverity.MEDIUM
                AnomalySeverity.LOW -> AlertSeverity.LOW
            }
            
            val alertType = when {
                anomaly.metricName.contains("security") -> AlertType.SECURITY_THREAT
                anomaly.metricName.contains("memory") -> AlertType.MEMORY_USAGE
                anomaly.metricName.contains("network") || anomaly.metricName.contains("latency") -> AlertType.NETWORK_ISSUE
                anomaly.metricName.contains("train") -> AlertType.TRAIN_ANOMALY
                else -> AlertType.SYSTEM_ERROR
            }
            
            alertingSystem.raiseAlert(
                source = "anomaly_detector",
                type = alertType,
                severity = alertSeverity,
                title = "Metric Anomaly Detected",
                description = anomaly.description,
                metadata = mapOf(
                    "metric" to anomaly.metricName,
                    "anomaly_type" to anomaly.type.name,
                    "value" to anomaly.value.toString(),
                    "expected_value" to anomaly.expectedValue.toString(),
                    "deviation" to anomaly.deviation.toString()
                )
            )
        }
    }
    
    /**
     * Process system insights
     */
    private fun processSystemInsights(insights: List<SystemInsight>) {
        insights.forEach { insight ->
            if (insight.actionable && insight.severity != InsightSeverity.INFO) {
                val alertSeverity = when (insight.severity) {
                    InsightSeverity.CRITICAL -> AlertSeverity.CRITICAL
                    InsightSeverity.HIGH -> AlertSeverity.HIGH
                    InsightSeverity.MEDIUM -> AlertSeverity.MEDIUM
                    InsightSeverity.LOW -> AlertSeverity.LOW
                    InsightSeverity.INFO -> AlertSeverity.INFO
                }
                
                alertingSystem.raiseAlert(
                    source = "insights_engine",
                    type = AlertType.SYSTEM_ERROR,
                    severity = alertSeverity,
                    title = insight.title,
                    description = "${insight.description}\n\nRecommendations:\n${insight.recommendations.joinToString("\n• ", "• ")}",
                    metadata = insight.metadata + mapOf(
                        "insight_type" to insight.type.name,
                        "actionable" to insight.actionable.toString()
                    )
                )
            }
        }
    }
    
    /**
     * Synchronize metrics between monitoring components
     */
    private suspend fun synchronizeMetrics() {
        try {
            // Get current metrics from all components
            val realTimeMetrics = realTimeMetricsCollector.metrics.value
            val memoryStats = memoryLeakDetector.getMemoryStatistics()
            
            // Record in correlation engine
            metricsCorrelationEngine.recordMetric("connection_status", 
                if (realTimeMetrics.connectionStatus == ConnectionStatus.CONNECTED) 1.0 else 0.0)
            
            metricsCorrelationEngine.recordMetric("average_latency_ms", 
                realTimeMetrics.performance.averageLatency.inWholeMilliseconds.toDouble())
            
            metricsCorrelationEngine.recordMetric("error_rate_percent", 
                realTimeMetrics.performance.errorRate * 100)
            
            metricsCorrelationEngine.recordMetric("memory_usage_mb", 
                memoryStats.currentUsageMB.toDouble())
            
            metricsCorrelationEngine.recordMetric("heap_usage_percent", 
                memoryStats.heapUsagePercent.toDouble())
            
            // Record memory usage in real-time collector
            realTimeMetricsCollector.recordMemoryUsage(memoryStats.currentUsageMB * 1024 * 1024)
            
            logger.logPerformanceMetric(TAG, "metrics_synchronized", 1.0f, "sync")
            
        } catch (e: Exception) {
            logger.error(TAG, "Error synchronizing metrics", e)
        }
    }
    
    /**
     * Update overall system health
     */
    private fun updateSystemHealth() {
        try {
            val realTimeMetrics = realTimeMetricsCollector.metrics.value
            val memoryStats = memoryLeakDetector.getMemoryStatistics()
            val alertStats = alertingSystem.alertStatistics.value
            
            val isRealTimeHealthy = realTimeMetricsCollector.isSystemHealthy()
            val isMemoryHealthy = memoryStats.isHealthy
            val hasCriticalAlerts = alertStats.criticalAlerts > 0
            
            val overallHealth = when {
                hasCriticalAlerts -> SystemHealthStatus.CRITICAL
                !isRealTimeHealthy || !isMemoryHealthy -> SystemHealthStatus.DEGRADED
                alertStats.highSeverityAlerts > 5 -> SystemHealthStatus.WARNING
                else -> SystemHealthStatus.HEALTHY
            }
            
            val health = SystemHealth(
                status = overallHealth,
                realTimeConnectionHealthy = isRealTimeHealthy,
                memoryHealthy = isMemoryHealthy,
                criticalAlerts = alertStats.criticalAlerts,
                highSeverityAlerts = alertStats.highSeverityAlerts,
                averageLatencyMs = realTimeMetrics.performance.averageLatency.inWholeMilliseconds,
                errorRatePercent = (realTimeMetrics.performance.errorRate * 100).toInt(),
                memoryUsageMB = memoryStats.currentUsageMB,
                heapUsagePercent = memoryStats.heapUsagePercent,
                lastUpdated = Clock.System.now()
            )
            
            _systemHealth.value = health
            
            // Update monitoring statistics
            updateMonitoringStatistics(realTimeMetrics, memoryStats, alertStats)
            
        } catch (e: Exception) {
            logger.error(TAG, "Error updating system health", e)
        }
    }
    
    /**
     * Update monitoring statistics
     */
    private fun updateMonitoringStatistics(
        realTimeMetrics: RealTimeMetrics,
        memoryStats: MemoryStatistics,
        alertStats: AlertStatistics
    ) {
        val correlations = metricsCorrelationEngine.correlations.value
        val trends = metricsCorrelationEngine.trends.value
        val anomalies = metricsCorrelationEngine.anomalies.value
        val insights = metricsCorrelationEngine.insights.value
        
        val stats = MonitoringStatistics(
            totalMetricsTracked = 10, // Approximate number of core metrics
            activeCorrelations = correlations.size,
            detectedTrends = trends.size,
            activeAnomalies = anomalies.size,
            generatedInsights = insights.size,
            totalAlerts = alertStats.totalActiveAlerts,
            resolvedAlerts = alertStats.resolvedAlerts,
            systemUptimeMinutes = realTimeMetrics.performance.uptime.inWholeMinutes,
            averageResponseTimeMs = realTimeMetrics.performance.averageLatency.inWholeMilliseconds,
            dataQualityScore = (realTimeMetrics.dataQuality.reliability * 100).toInt(),
            lastUpdated = Clock.System.now()
        )
        
        _monitoringStatistics.value = stats
    }
    
    /**
     * Get comprehensive monitoring dashboard data
     */
    fun getMonitoringDashboard(): MonitoringDashboard {
        return MonitoringDashboard(
            systemHealth = _systemHealth.value,
            statistics = _monitoringStatistics.value,
            recentAlerts = alertingSystem.activeAlerts.value.take(10),
            topCorrelations = metricsCorrelationEngine.correlations.value
                .sortedByDescending { kotlin.math.abs(it.coefficient) }.take(5),
            criticalAnomalies = metricsCorrelationEngine.anomalies.value
                .filter { it.severity == AnomalySeverity.CRITICAL || it.severity == AnomalySeverity.HIGH },
            actionableInsights = metricsCorrelationEngine.insights.value
                .filter { it.actionable }.take(5)
        )
    }
    
    /**
     * Cleanup old monitoring data
     */
    fun cleanupOldData(olderThan: Instant = Clock.System.now().minus(24.minutes)) {
        scope.launch {
            try {
                metricsCorrelationEngine.clearOldData(olderThan)
                alertingSystem.cleanupOldData(olderThan)
                memoryLeakDetector.clearOldData(olderThan)
                
                logger.info(TAG, "Cleaned up monitoring data older than $olderThan")
            } catch (e: Exception) {
                logger.error(TAG, "Error cleaning up old data", e)
            }
        }
    }
}

/**
 * System health status
 */
enum class SystemHealthStatus {
    HEALTHY,
    WARNING,
    DEGRADED,
    CRITICAL
}

/**
 * Overall system health information
 */
data class SystemHealth(
    val status: SystemHealthStatus = SystemHealthStatus.HEALTHY,
    val realTimeConnectionHealthy: Boolean = true,
    val memoryHealthy: Boolean = true,
    val criticalAlerts: Int = 0,
    val highSeverityAlerts: Int = 0,
    val averageLatencyMs: Long = 0,
    val errorRatePercent: Int = 0,
    val memoryUsageMB: Long = 0,
    val heapUsagePercent: Int = 0,
    val lastUpdated: Instant = Clock.System.now()
)

/**
 * Monitoring statistics
 */
data class MonitoringStatistics(
    val totalMetricsTracked: Int = 0,
    val activeCorrelations: Int = 0,
    val detectedTrends: Int = 0,
    val activeAnomalies: Int = 0,
    val generatedInsights: Int = 0,
    val totalAlerts: Int = 0,
    val resolvedAlerts: Int = 0,
    val systemUptimeMinutes: Long = 0,
    val averageResponseTimeMs: Long = 0,
    val dataQualityScore: Int = 100,
    val lastUpdated: Instant = Clock.System.now()
)

/**
 * Comprehensive monitoring dashboard data
 */
data class MonitoringDashboard(
    val systemHealth: SystemHealth,
    val statistics: MonitoringStatistics,
    val recentAlerts: List<Alert>,
    val topCorrelations: List<MetricCorrelation>,
    val criticalAnomalies: List<MetricAnomaly>,
    val actionableInsights: List<SystemInsight>
)