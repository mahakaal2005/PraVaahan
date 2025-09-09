package com.example.pravaahan.core.health

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configurable alert thresholds for health monitoring system.
 * 
 * Provides runtime configuration of alert thresholds for different metrics
 * and operational modes (normal, high-traffic, maintenance, etc.).
 */
@Singleton
class AlertThresholdConfig @Inject constructor() {
    
    /**
     * Operational modes that affect threshold values
     */
    enum class OperationalMode {
        NORMAL,      // Standard operations
        HIGH_TRAFFIC, // Peak hours with higher traffic
        MAINTENANCE, // Maintenance mode with relaxed thresholds
        EMERGENCY    // Emergency mode with strict thresholds
    }
    
    /**
     * Alert threshold configuration for different metrics
     */
    data class ThresholdSet(
        // Latency thresholds (milliseconds)
        val warningLatencyMs: Long = 1000L,
        val criticalLatencyMs: Long = 3000L,
        val emergencyLatencyMs: Long = 5000L,
        
        // Error rate thresholds (0.0 to 1.0)
        val warningErrorRate: Double = 0.02, // 2%
        val criticalErrorRate: Double = 0.05, // 5%
        val emergencyErrorRate: Double = 0.10, // 10%
        
        // Memory usage thresholds (MB)
        val warningMemoryMB: Long = 100L,
        val criticalMemoryMB: Long = 200L,
        val emergencyMemoryMB: Long = 300L,
        
        // Connection thresholds (count)
        val warningConnections: Int = 80,
        val criticalConnections: Int = 90,
        val emergencyConnections: Int = 95,
        
        // Data quality thresholds (0.0 to 1.0)
        val warningDataQuality: Double = 0.90, // 90%
        val criticalDataQuality: Double = 0.85, // 85%
        val emergencyDataQuality: Double = 0.80, // 80%
        
        // Position accuracy thresholds (0.0 to 1.0)
        val warningPositionAccuracy: Double = 0.95, // 95%
        val criticalPositionAccuracy: Double = 0.90, // 90%
        val emergencyPositionAccuracy: Double = 0.85, // 85%
        
        // Throughput thresholds (messages per second)
        val warningThroughput: Double = 100.0,
        val criticalThroughput: Double = 50.0,
        val emergencyThroughput: Double = 10.0,
        
        // Circuit breaker thresholds
        val warningFailureRate: Double = 0.10, // 10%
        val criticalFailureRate: Double = 0.20, // 20%
        val emergencyFailureRate: Double = 0.30  // 30%
    )
    
    // Default threshold sets for different operational modes
    private val defaultThresholds = mapOf(
        OperationalMode.NORMAL to ThresholdSet(),
        
        OperationalMode.HIGH_TRAFFIC to ThresholdSet(
            warningLatencyMs = 1500L,
            criticalLatencyMs = 4000L,
            emergencyLatencyMs = 6000L,
            warningErrorRate = 0.03,
            criticalErrorRate = 0.07,
            emergencyErrorRate = 0.12,
            warningMemoryMB = 150L,
            criticalMemoryMB = 250L,
            emergencyMemoryMB = 350L
        ),
        
        OperationalMode.MAINTENANCE to ThresholdSet(
            warningLatencyMs = 2000L,
            criticalLatencyMs = 5000L,
            emergencyLatencyMs = 8000L,
            warningErrorRate = 0.05,
            criticalErrorRate = 0.10,
            emergencyErrorRate = 0.15,
            warningDataQuality = 0.85,
            criticalDataQuality = 0.80,
            emergencyDataQuality = 0.75
        ),
        
        OperationalMode.EMERGENCY to ThresholdSet(
            warningLatencyMs = 500L,
            criticalLatencyMs = 1500L,
            emergencyLatencyMs = 3000L,
            warningErrorRate = 0.01,
            criticalErrorRate = 0.03,
            emergencyErrorRate = 0.05,
            warningDataQuality = 0.98,
            criticalDataQuality = 0.95,
            emergencyDataQuality = 0.90,
            warningPositionAccuracy = 0.98,
            criticalPositionAccuracy = 0.95,
            emergencyPositionAccuracy = 0.90
        )
    )
    
    // Current configuration state
    private val _currentMode = MutableStateFlow(OperationalMode.NORMAL)
    val currentMode: StateFlow<OperationalMode> = _currentMode.asStateFlow()
    
    private val _customThresholds = MutableStateFlow<Map<OperationalMode, ThresholdSet>>(emptyMap())
    val customThresholds: StateFlow<Map<OperationalMode, ThresholdSet>> = _customThresholds.asStateFlow()
    
    /**
     * Get current threshold set based on operational mode
     */
    fun getCurrentThresholds(): ThresholdSet {
        val mode = _currentMode.value
        return _customThresholds.value[mode] ?: defaultThresholds[mode] ?: defaultThresholds[OperationalMode.NORMAL]!!
    }
    
    /**
     * Get thresholds for a specific operational mode
     */
    fun getThresholds(mode: OperationalMode): ThresholdSet {
        return _customThresholds.value[mode] ?: defaultThresholds[mode] ?: defaultThresholds[OperationalMode.NORMAL]!!
    }
    
    /**
     * Set the current operational mode
     */
    fun setOperationalMode(mode: OperationalMode) {
        _currentMode.value = mode
    }
    
    /**
     * Update thresholds for a specific operational mode
     */
    fun updateThresholds(mode: OperationalMode, thresholds: ThresholdSet) {
        val currentCustom = _customThresholds.value.toMutableMap()
        currentCustom[mode] = thresholds
        _customThresholds.value = currentCustom
    }
    
    /**
     * Reset thresholds for a specific mode to defaults
     */
    fun resetToDefaults(mode: OperationalMode) {
        val currentCustom = _customThresholds.value.toMutableMap()
        currentCustom.remove(mode)
        _customThresholds.value = currentCustom
    }
    
    /**
     * Reset all thresholds to defaults
     */
    fun resetAllToDefaults() {
        _customThresholds.value = emptyMap()
        _currentMode.value = OperationalMode.NORMAL
    }
    
    /**
     * Get alert level for a latency value
     */
    fun getLatencyAlertLevel(latencyMs: Long): PerformanceAlertLevel {
        val thresholds = getCurrentThresholds()
        return when {
            latencyMs >= thresholds.emergencyLatencyMs -> PerformanceAlertLevel.EMERGENCY
            latencyMs >= thresholds.criticalLatencyMs -> PerformanceAlertLevel.CRITICAL
            latencyMs >= thresholds.warningLatencyMs -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Get alert level for an error rate value
     */
    fun getErrorRateAlertLevel(errorRate: Double): PerformanceAlertLevel {
        val thresholds = getCurrentThresholds()
        return when {
            errorRate >= thresholds.emergencyErrorRate -> PerformanceAlertLevel.EMERGENCY
            errorRate >= thresholds.criticalErrorRate -> PerformanceAlertLevel.CRITICAL
            errorRate >= thresholds.warningErrorRate -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Get alert level for memory usage
     */
    fun getMemoryAlertLevel(memoryMB: Long): PerformanceAlertLevel {
        val thresholds = getCurrentThresholds()
        return when {
            memoryMB >= thresholds.emergencyMemoryMB -> PerformanceAlertLevel.EMERGENCY
            memoryMB >= thresholds.criticalMemoryMB -> PerformanceAlertLevel.CRITICAL
            memoryMB >= thresholds.warningMemoryMB -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Get alert level for data quality
     */
    fun getDataQualityAlertLevel(quality: Double): PerformanceAlertLevel {
        val thresholds = getCurrentThresholds()
        return when {
            quality <= thresholds.emergencyDataQuality -> PerformanceAlertLevel.EMERGENCY
            quality <= thresholds.criticalDataQuality -> PerformanceAlertLevel.CRITICAL
            quality <= thresholds.warningDataQuality -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Get alert level for position accuracy
     */
    fun getPositionAccuracyAlertLevel(accuracy: Double): PerformanceAlertLevel {
        val thresholds = getCurrentThresholds()
        return when {
            accuracy <= thresholds.emergencyPositionAccuracy -> PerformanceAlertLevel.EMERGENCY
            accuracy <= thresholds.criticalPositionAccuracy -> PerformanceAlertLevel.CRITICAL
            accuracy <= thresholds.warningPositionAccuracy -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Get alert level for connection count
     */
    fun getConnectionAlertLevel(connectionCount: Int, maxConnections: Int): PerformanceAlertLevel {
        val thresholds = getCurrentThresholds()
        val percentage = (connectionCount.toDouble() / maxConnections.toDouble()) * 100
        
        return when {
            percentage >= thresholds.emergencyConnections -> PerformanceAlertLevel.EMERGENCY
            percentage >= thresholds.criticalConnections -> PerformanceAlertLevel.CRITICAL
            percentage >= thresholds.warningConnections -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Get alert level for throughput
     */
    fun getThroughputAlertLevel(throughput: Double): PerformanceAlertLevel {
        val thresholds = getCurrentThresholds()
        return when {
            throughput <= thresholds.emergencyThroughput -> PerformanceAlertLevel.EMERGENCY
            throughput <= thresholds.criticalThroughput -> PerformanceAlertLevel.CRITICAL
            throughput <= thresholds.warningThroughput -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Get alert level for circuit breaker failure rate
     */
    fun getFailureRateAlertLevel(failureRate: Double): PerformanceAlertLevel {
        val thresholds = getCurrentThresholds()
        return when {
            failureRate >= thresholds.emergencyFailureRate -> PerformanceAlertLevel.EMERGENCY
            failureRate >= thresholds.criticalFailureRate -> PerformanceAlertLevel.CRITICAL
            failureRate >= thresholds.warningFailureRate -> PerformanceAlertLevel.WARNING
            else -> PerformanceAlertLevel.NORMAL
        }
    }
    
    /**
     * Get recommended action for an alert level and metric type
     */
    fun getRecommendedAction(level: PerformanceAlertLevel, metricType: String): String? {
        return when (level) {
            PerformanceAlertLevel.EMERGENCY -> when (metricType) {
                "latency" -> "Immediate intervention required - check network connectivity and server load"
                "error_rate" -> "Immediate system intervention required - check service health"
                "memory_usage" -> "Immediate memory cleanup required - restart service if necessary"
                "data_quality" -> "Critical data quality issue - validate data sources immediately"
                "position_accuracy" -> "Critical position accuracy issue - check GPS systems"
                else -> "Immediate attention required for critical system issue"
            }
            PerformanceAlertLevel.CRITICAL -> when (metricType) {
                "latency" -> "Investigate network issues and optimize data processing"
                "error_rate" -> "Investigate error patterns and implement additional error handling"
                "memory_usage" -> "Implement memory optimization and garbage collection"
                "data_quality" -> "Investigate data quality issues and validate sources"
                "position_accuracy" -> "Check position validation systems and GPS accuracy"
                else -> "Critical issue requires immediate investigation"
            }
            PerformanceAlertLevel.WARNING -> when (metricType) {
                "latency" -> "Monitor network performance and consider optimization"
                "error_rate" -> "Monitor error patterns and review system logs"
                "memory_usage" -> "Monitor memory usage patterns and optimize if needed"
                "data_quality" -> "Monitor data quality trends and investigate sources"
                "position_accuracy" -> "Monitor position accuracy and validate GPS systems"
                else -> "Monitor situation and investigate if trend continues"
            }
            PerformanceAlertLevel.NORMAL -> null
        }
    }
}