package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector
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
 * Comprehensive health dashboard data
 */
data class SystemHealthDashboard(
    val overallStatus: PerformanceAlertLevel,
    val lastUpdated: Instant,
    val systemUptime: Duration,
    
    // Health check results
    val healthCheckResults: List<HealthCheckResult>,
    val criticalHealthIssues: List<HealthCheckResult.Failure>,
    val healthCheckSummary: HealthCheckSummary,
    
    // Performance metrics
    val performanceMetrics: PerformanceMetricsSummary,
    val activeAlerts: List<PerformanceAlert>,
    val alertSummary: AlertSummary,
    
    // System recommendations
    val recommendations: List<SystemRecommendation>,
    val healthScore: Double, // 0.0 to 100.0
    
    // Operational status
    val operationalMode: AlertThresholdConfig.OperationalMode,
    val connectionStatus: String,
    val dataQualityStatus: String
)

/**
 * Health check summary
 */
data class HealthCheckSummary(
    val totalChecks: Int,
    val passedChecks: Int,
    val failedChecks: Int,
    val warningChecks: Int,
    val criticalFailures: Int,
    val lastCheckTime: Instant,
    val averageCheckDuration: Duration
)

/**
 * Performance metrics summary
 */
data class PerformanceMetricsSummary(
    val averageLatency: Duration,
    val errorRate: Double,
    val throughput: Double,
    val memoryUsage: Long, // MB
    val connectionCount: Int,
    val dataQuality: Double,
    val positionAccuracy: Double
)

/**
 * Alert summary
 */
data class AlertSummary(
    val totalAlerts: Int,
    val emergencyAlerts: Int,
    val criticalAlerts: Int,
    val warningAlerts: Int,
    val recentAlerts: List<PerformanceAlert>,
    val alertTrends: Map<String, TrendDirection>
)

/**
 * System recommendation
 */
data class SystemRecommendation(
    val priority: RecommendationPriority,
    val category: String,
    val title: String,
    val description: String,
    val action: String,
    val estimatedImpact: String,
    val timestamp: Instant
)

enum class RecommendationPriority {
    CRITICAL, HIGH, MEDIUM, LOW
}

/**
 * Comprehensive health dashboard service that aggregates all health monitoring data.
 * 
 * Provides unified health status, performance metrics, alerts, and recommendations
 * for the entire PraVaahan railway monitoring system.
 */
@Singleton
class HealthDashboardService @Inject constructor(
    private val appStartupVerifier: AppStartupVerifier,
    private val realTimePerformanceMonitor: RealTimePerformanceMonitor,
    private val metricsCollector: RealTimeMetricsCollector,
    private val alertThresholdConfig: AlertThresholdConfig,
    private val monitoringService: com.example.pravaahan.core.monitoring.MonitoringService,
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "HealthDashboardService"
        private val DASHBOARD_UPDATE_INTERVAL = 30.seconds
        private val RECOMMENDATION_UPDATE_INTERVAL = 5.minutes
    }
    
    private val dashboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null
    private var isRunning = false
    
    // Dashboard state
    private val _dashboard = MutableStateFlow(createInitialDashboard())
    val dashboard: StateFlow<SystemHealthDashboard> = _dashboard.asStateFlow()
    
    // System startup time
    private val systemStartTime = Clock.System.now()
    
    // Historical data for trend analysis
    private val healthScoreHistory = mutableListOf<Pair<Instant, Double>>()
    private val recommendationHistory = mutableListOf<SystemRecommendation>()
    
    /**
     * Start the health dashboard monitoring
     */
    fun startMonitoring() {
        if (isRunning) {
            logger.warn(TAG, "Health dashboard monitoring is already running")
            return
        }
        
        logger.info(TAG, "Starting health dashboard monitoring")
        isRunning = true
        
        updateJob = dashboardScope.launch {
            while (isRunning) {
                try {
                    updateDashboard()
                    delay(DASHBOARD_UPDATE_INTERVAL)
                } catch (e: Exception) {
                    logger.error(TAG, "Error updating health dashboard", e)
                    delay(DASHBOARD_UPDATE_INTERVAL)
                }
            }
        }
    }
    
    /**
     * Stop the health dashboard monitoring
     */
    fun stopMonitoring() {
        logger.info(TAG, "Stopping health dashboard monitoring")
        isRunning = false
        updateJob?.cancel()
        updateJob = null
    }
    
    /**
     * Force an immediate dashboard update
     */
    suspend fun forceUpdate() {
        logger.info(TAG, "Forcing health dashboard update")
        updateDashboard()
    }
    
    /**
     * Get current system health score (0-100)
     */
    fun getCurrentHealthScore(): Double {
        return _dashboard.value.healthScore
    }
    
    /**
     * Get system recommendations by priority
     */
    fun getRecommendationsByPriority(priority: RecommendationPriority): List<SystemRecommendation> {
        return _dashboard.value.recommendations.filter { it.priority == priority }
    }
    
    /**
     * Get health trend over time
     */
    fun getHealthTrend(timeWindow: Duration): List<Pair<Instant, Double>> {
        val cutoffTime = Clock.System.now() - timeWindow
        return healthScoreHistory.filter { it.first >= cutoffTime }
    }
    
    private suspend fun updateDashboard() {
        val now = Clock.System.now()
        val uptime = now - systemStartTime
        
        // Collect health check results
        val healthCheckResults = collectHealthCheckResults()
        val healthCheckSummary = createHealthCheckSummary(healthCheckResults, now)
        
        // Collect performance metrics
        val performanceMetrics = collectPerformanceMetrics()
        
        // Collect alerts
        val activeAlerts = realTimePerformanceMonitor.activeAlerts.value
        val alertSummary = createAlertSummary(activeAlerts)
        
        // Calculate overall status and health score
        val overallStatus = calculateOverallStatus(healthCheckResults, activeAlerts)
        val healthScore = calculateHealthScore(healthCheckResults, performanceMetrics, activeAlerts)
        
        // Get monitoring insights and correlations
        val monitoringDashboard = monitoringService.getMonitoringDashboard()
        
        // Generate recommendations (including monitoring insights)
        val recommendations = generateRecommendations(healthCheckResults, performanceMetrics, activeAlerts, monitoringDashboard)
        
        // Update historical data
        updateHistoricalData(now, healthScore)
        
        // Create dashboard
        val dashboard = SystemHealthDashboard(
            overallStatus = overallStatus,
            lastUpdated = now,
            systemUptime = uptime,
            healthCheckResults = healthCheckResults,
            criticalHealthIssues = healthCheckResults.filterIsInstance<HealthCheckResult.Failure>()
                .filter { isHealthCheckCritical(it.checkName) },
            healthCheckSummary = healthCheckSummary,
            performanceMetrics = performanceMetrics,
            activeAlerts = activeAlerts,
            alertSummary = alertSummary,
            recommendations = recommendations,
            healthScore = healthScore,
            operationalMode = alertThresholdConfig.currentMode.value,
            connectionStatus = getConnectionStatus(performanceMetrics),
            dataQualityStatus = getDataQualityStatus(performanceMetrics)
        )
        
        _dashboard.value = dashboard
        
        logger.debug(TAG, "Dashboard updated - Health Score: ${healthScore.toInt()}%, Status: $overallStatus")
    }
    
    private suspend fun collectHealthCheckResults(): List<HealthCheckResult> {
        return try {
            // Perform a quick health check to get current results
            val healthStatus = appStartupVerifier.quickHealthCheck()
            healthStatus.checkResults
        } catch (e: Exception) {
            logger.error(TAG, "Failed to collect health check results", e)
            emptyList()
        }
    }
    
    private fun createHealthCheckSummary(results: List<HealthCheckResult>, timestamp: Instant): HealthCheckSummary {
        val totalChecks = results.size
        val passedChecks = results.count { it is HealthCheckResult.Success }
        val failedChecks = results.count { it is HealthCheckResult.Failure }
        val warningChecks = results.count { it is HealthCheckResult.Warning }
        val criticalFailures = results.filterIsInstance<HealthCheckResult.Failure>()
            .count { isHealthCheckCritical(it.checkName) }
        
        val averageDuration = if (results.isNotEmpty()) {
            val totalDuration = results.sumOf { it.durationMs }
            Duration.parse("${totalDuration / results.size}ms")
        } else {
            Duration.ZERO
        }
        
        return HealthCheckSummary(
            totalChecks = totalChecks,
            passedChecks = passedChecks,
            failedChecks = failedChecks,
            warningChecks = warningChecks,
            criticalFailures = criticalFailures,
            lastCheckTime = timestamp,
            averageCheckDuration = averageDuration
        )
    }
    
    private suspend fun collectPerformanceMetrics(): PerformanceMetricsSummary {
        return try {
            val metrics = metricsCollector.metrics.value
            
            PerformanceMetricsSummary(
                averageLatency = metrics.performance.averageLatency,
                errorRate = metrics.performance.errorRate,
                throughput = metrics.performance.throughput,
                memoryUsage = (metrics.performance.memoryUsage / (1024 * 1024)).toLong(), // Convert to MB
                connectionCount = metrics.performance.connectionCount,
                dataQuality = metrics.dataQuality.reliability.toDouble(),
                positionAccuracy = metrics.dataQuality.accuracy
            )
        } catch (e: Exception) {
            logger.error(TAG, "Failed to collect performance metrics", e)
            PerformanceMetricsSummary(
                averageLatency = Duration.ZERO,
                errorRate = 0.0,
                throughput = 0.0,
                memoryUsage = 0L,
                connectionCount = 0,
                dataQuality = 0.0,
                positionAccuracy = 0.0
            )
        }
    }
    
    private fun createAlertSummary(alerts: List<PerformanceAlert>): AlertSummary {
        val totalAlerts = alerts.size
        val emergencyAlerts = alerts.count { it.level == PerformanceAlertLevel.EMERGENCY }
        val criticalAlerts = alerts.count { it.level == PerformanceAlertLevel.CRITICAL }
        val warningAlerts = alerts.count { it.level == PerformanceAlertLevel.WARNING }
        
        // Get recent alerts (last 10 minutes)
        val recentCutoff = Clock.System.now() - 10.minutes
        val recentAlerts = alerts.filter { it.timestamp >= recentCutoff }
            .sortedByDescending { it.timestamp }
            .take(10)
        
        // Calculate alert trends (simplified)
        val alertTrends = mapOf(
            "latency" to TrendDirection.STABLE,
            "error_rate" to TrendDirection.STABLE,
            "memory_usage" to TrendDirection.STABLE
        )
        
        return AlertSummary(
            totalAlerts = totalAlerts,
            emergencyAlerts = emergencyAlerts,
            criticalAlerts = criticalAlerts,
            warningAlerts = warningAlerts,
            recentAlerts = recentAlerts,
            alertTrends = alertTrends
        )
    }
    
    private fun calculateOverallStatus(
        healthResults: List<HealthCheckResult>,
        alerts: List<PerformanceAlert>
    ): PerformanceAlertLevel {
        // Check for emergency alerts
        if (alerts.any { it.level == PerformanceAlertLevel.EMERGENCY }) {
            return PerformanceAlertLevel.EMERGENCY
        }
        
        // Check for critical health check failures
        val criticalFailures = healthResults.filterIsInstance<HealthCheckResult.Failure>()
            .filter { isHealthCheckCritical(it.checkName) }
        
        if (criticalFailures.isNotEmpty()) {
            return PerformanceAlertLevel.EMERGENCY
        }
        
        // Check for critical alerts
        if (alerts.any { it.level == PerformanceAlertLevel.CRITICAL }) {
            return PerformanceAlertLevel.CRITICAL
        }
        
        // Check for any health check failures
        if (healthResults.any { it is HealthCheckResult.Failure }) {
            return PerformanceAlertLevel.CRITICAL
        }
        
        // Check for warning alerts
        if (alerts.any { it.level == PerformanceAlertLevel.WARNING }) {
            return PerformanceAlertLevel.WARNING
        }
        
        // Check for health check warnings
        if (healthResults.any { it is HealthCheckResult.Warning }) {
            return PerformanceAlertLevel.WARNING
        }
        
        return PerformanceAlertLevel.NORMAL
    }
    
    private fun calculateHealthScore(
        healthResults: List<HealthCheckResult>,
        performanceMetrics: PerformanceMetricsSummary,
        alerts: List<PerformanceAlert>
    ): Double {
        var score = 100.0
        
        // Deduct points for health check failures
        val failedChecks = healthResults.count { it is HealthCheckResult.Failure }
        val criticalFailures = healthResults.filterIsInstance<HealthCheckResult.Failure>()
            .count { isHealthCheckCritical(it.checkName) }
        
        score -= (failedChecks * 10.0) // -10 points per failed check
        score -= (criticalFailures * 20.0) // Additional -20 points for critical failures
        
        // Deduct points for warnings
        val warningChecks = healthResults.count { it is HealthCheckResult.Warning }
        score -= (warningChecks * 5.0) // -5 points per warning
        
        // Deduct points for alerts
        val emergencyAlerts = alerts.count { it.level == PerformanceAlertLevel.EMERGENCY }
        val criticalAlerts = alerts.count { it.level == PerformanceAlertLevel.CRITICAL }
        val warningAlerts = alerts.count { it.level == PerformanceAlertLevel.WARNING }
        
        score -= (emergencyAlerts * 25.0) // -25 points per emergency alert
        score -= (criticalAlerts * 15.0) // -15 points per critical alert
        score -= (warningAlerts * 5.0) // -5 points per warning alert
        
        // Deduct points for poor performance metrics
        if (performanceMetrics.errorRate > 0.05) { // > 5% error rate
            score -= 15.0
        }
        
        if (performanceMetrics.dataQuality < 0.90) { // < 90% data quality
            score -= 20.0
        }
        
        if (performanceMetrics.positionAccuracy < 0.95) { // < 95% position accuracy
            score -= 15.0
        }
        
        // Ensure score is between 0 and 100
        return maxOf(0.0, minOf(100.0, score))
    }
    
    private fun generateRecommendations(
        healthResults: List<HealthCheckResult>,
        performanceMetrics: PerformanceMetricsSummary,
        alerts: List<PerformanceAlert>,
        monitoringDashboard: com.example.pravaahan.core.monitoring.MonitoringDashboard
    ): List<SystemRecommendation> {
        val recommendations = mutableListOf<SystemRecommendation>()
        val now = Clock.System.now()
        
        // Critical health check failures
        val criticalFailures = healthResults.filterIsInstance<HealthCheckResult.Failure>()
            .filter { isHealthCheckCritical(it.checkName) }
        
        criticalFailures.forEach { failure ->
            recommendations.add(
                SystemRecommendation(
                    priority = RecommendationPriority.CRITICAL,
                    category = "Health Check",
                    title = "Critical Health Check Failure",
                    description = "Health check '${failure.checkName}' is failing: ${failure.message}",
                    action = "Investigate and resolve the underlying issue immediately",
                    estimatedImpact = "High - System stability at risk",
                    timestamp = now
                )
            )
        }
        
        // Performance recommendations
        if (performanceMetrics.errorRate > 0.05) {
            recommendations.add(
                SystemRecommendation(
                    priority = RecommendationPriority.HIGH,
                    category = "Performance",
                    title = "High Error Rate Detected",
                    description = "Error rate is ${(performanceMetrics.errorRate * 100).toInt()}%, exceeding 5% threshold",
                    action = "Review error logs and implement additional error handling",
                    estimatedImpact = "Medium - User experience degradation",
                    timestamp = now
                )
            )
        }
        
        if (performanceMetrics.memoryUsage > 200) {
            recommendations.add(
                SystemRecommendation(
                    priority = RecommendationPriority.HIGH,
                    category = "Resource",
                    title = "High Memory Usage",
                    description = "Memory usage is ${performanceMetrics.memoryUsage}MB, approaching limits",
                    action = "Optimize memory usage or upgrade compute resources",
                    estimatedImpact = "Medium - Performance degradation risk",
                    timestamp = now
                )
            )
        }
        
        if (performanceMetrics.dataQuality < 0.90) {
            recommendations.add(
                SystemRecommendation(
                    priority = RecommendationPriority.CRITICAL,
                    category = "Data Quality",
                    title = "Poor Data Quality",
                    description = "Data quality is ${(performanceMetrics.dataQuality * 100).toInt()}%, below 90% threshold",
                    action = "Investigate data sources and validation processes",
                    estimatedImpact = "High - Railway safety implications",
                    timestamp = now
                )
            )
        }
        
        // Alert-based recommendations
        alerts.filter { it.level == PerformanceAlertLevel.EMERGENCY }.forEach { alert ->
            alert.recommendedAction?.let { action ->
                recommendations.add(
                    SystemRecommendation(
                        priority = RecommendationPriority.CRITICAL,
                        category = "Emergency Alert",
                        title = alert.message,
                        description = alert.details,
                        action = action,
                        estimatedImpact = "Critical - Immediate attention required",
                        timestamp = alert.timestamp
                    )
                )
            }
        }
        
        // Add monitoring insights as recommendations
        monitoringDashboard.actionableInsights.forEach { insight ->
            val priority = when (insight.severity) {
                com.example.pravaahan.core.monitoring.InsightSeverity.CRITICAL -> RecommendationPriority.CRITICAL
                com.example.pravaahan.core.monitoring.InsightSeverity.HIGH -> RecommendationPriority.HIGH
                com.example.pravaahan.core.monitoring.InsightSeverity.MEDIUM -> RecommendationPriority.MEDIUM
                else -> RecommendationPriority.LOW
            }
            
            recommendations.add(
                SystemRecommendation(
                    priority = priority,
                    category = "AI Insights",
                    title = insight.title,
                    description = insight.description,
                    action = insight.recommendations.joinToString("; "),
                    estimatedImpact = when (insight.severity) {
                        com.example.pravaahan.core.monitoring.InsightSeverity.CRITICAL -> "Critical - System optimization required"
                        com.example.pravaahan.core.monitoring.InsightSeverity.HIGH -> "High - Performance improvement opportunity"
                        com.example.pravaahan.core.monitoring.InsightSeverity.MEDIUM -> "Medium - Efficiency enhancement"
                        else -> "Low - Minor optimization"
                    },
                    timestamp = insight.timestamp
                )
            )
        }
        
        // Add correlation-based recommendations
        monitoringDashboard.topCorrelations.forEach { correlation ->
            if (correlation.strength == com.example.pravaahan.core.monitoring.CorrelationStrength.VERY_STRONG) {
                recommendations.add(
                    SystemRecommendation(
                        priority = RecommendationPriority.MEDIUM,
                        category = "Correlation Analysis",
                        title = "Strong Metric Correlation Detected",
                        description = "Strong correlation (${String.format("%.3f", correlation.coefficient)}) between ${correlation.metric1} and ${correlation.metric2}",
                        action = "Monitor both metrics together for predictive insights",
                        estimatedImpact = "Medium - Improved monitoring and prediction capabilities",
                        timestamp = correlation.timestamp
                    )
                )
            }
        }
        
        // Add anomaly-based recommendations
        monitoringDashboard.criticalAnomalies.forEach { anomaly ->
            recommendations.add(
                SystemRecommendation(
                    priority = when (anomaly.severity) {
                        com.example.pravaahan.core.monitoring.AnomalySeverity.CRITICAL -> RecommendationPriority.CRITICAL
                        com.example.pravaahan.core.monitoring.AnomalySeverity.HIGH -> RecommendationPriority.HIGH
                        else -> RecommendationPriority.MEDIUM
                    },
                    category = "Anomaly Detection",
                    title = "Metric Anomaly Detected",
                    description = anomaly.description,
                    action = "Investigate root cause of anomaly in ${anomaly.metricName}",
                    estimatedImpact = when (anomaly.severity) {
                        com.example.pravaahan.core.monitoring.AnomalySeverity.CRITICAL -> "Critical - Immediate investigation required"
                        com.example.pravaahan.core.monitoring.AnomalySeverity.HIGH -> "High - System stability concern"
                        else -> "Medium - Performance monitoring alert"
                    },
                    timestamp = anomaly.timestamp
                )
            )
        }
        
        return recommendations.sortedBy { it.priority }
    }
    
    private fun updateHistoricalData(timestamp: Instant, healthScore: Double) {
        // Add current health score to history
        healthScoreHistory.add(timestamp to healthScore)
        
        // Keep only last 24 hours of data
        val cutoffTime = timestamp - Duration.parse("24h")
        healthScoreHistory.removeAll { it.first < cutoffTime }
    }
    
    private fun isHealthCheckCritical(checkName: String): Boolean {
        return when (checkName) {
            "Real-Time Position Monitoring",
            "Position Data Quality",
            "Supabase Connection",
            "Database Access",
            "Dependency Injection" -> true
            else -> false
        }
    }
    
    private fun getConnectionStatus(metrics: PerformanceMetricsSummary): String {
        return when {
            metrics.errorRate > 0.10 -> "Unstable"
            metrics.errorRate > 0.05 -> "Degraded"
            metrics.connectionCount == 0 -> "Disconnected"
            else -> "Connected"
        }
    }
    
    private fun getDataQualityStatus(metrics: PerformanceMetricsSummary): String {
        return when {
            metrics.dataQuality >= 0.95 -> "Excellent"
            metrics.dataQuality >= 0.90 -> "Good"
            metrics.dataQuality >= 0.80 -> "Fair"
            else -> "Poor"
        }
    }
    
    private fun createInitialDashboard(): SystemHealthDashboard {
        val now = Clock.System.now()
        
        return SystemHealthDashboard(
            overallStatus = PerformanceAlertLevel.NORMAL,
            lastUpdated = now,
            systemUptime = Duration.ZERO,
            healthCheckResults = emptyList(),
            criticalHealthIssues = emptyList(),
            healthCheckSummary = HealthCheckSummary(
                totalChecks = 0,
                passedChecks = 0,
                failedChecks = 0,
                warningChecks = 0,
                criticalFailures = 0,
                lastCheckTime = now,
                averageCheckDuration = Duration.ZERO
            ),
            performanceMetrics = PerformanceMetricsSummary(
                averageLatency = Duration.ZERO,
                errorRate = 0.0,
                throughput = 0.0,
                memoryUsage = 0L,
                connectionCount = 0,
                dataQuality = 1.0,
                positionAccuracy = 1.0
            ),
            activeAlerts = emptyList(),
            alertSummary = AlertSummary(
                totalAlerts = 0,
                emergencyAlerts = 0,
                criticalAlerts = 0,
                warningAlerts = 0,
                recentAlerts = emptyList(),
                alertTrends = emptyMap()
            ),
            recommendations = emptyList(),
            healthScore = 100.0,
            operationalMode = AlertThresholdConfig.OperationalMode.NORMAL,
            connectionStatus = "Unknown",
            dataQualityStatus = "Unknown"
        )
    }
}