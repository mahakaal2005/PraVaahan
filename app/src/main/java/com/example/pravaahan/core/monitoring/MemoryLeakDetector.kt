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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Memory leak detection and monitoring for long-running railway operations.
 * Tracks memory usage patterns, detects potential leaks, and triggers cleanup.
 */
@Singleton
class MemoryLeakDetector @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "MemoryLeakDetector"
        private const val MONITORING_INTERVAL_MS = 30_000L // 30 seconds
        private const val MEMORY_LEAK_THRESHOLD_MB = 50 // MB increase over baseline
        private const val CRITICAL_MEMORY_THRESHOLD_MB = 200 // MB absolute threshold
        private const val GC_SUGGESTION_THRESHOLD_MB = 100 // MB threshold for GC suggestion
        private const val HISTORY_SIZE = 100 // Number of memory samples to keep
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runtime = Runtime.getRuntime()
    
    private val _memoryUsage = MutableStateFlow<MemoryUsage>(getCurrentMemoryUsage())
    val memoryUsage: StateFlow<MemoryUsage> = _memoryUsage.asStateFlow()
    
    private val _memoryLeaks = MutableStateFlow<List<MemoryLeak>>(emptyList())
    val memoryLeaks: StateFlow<List<MemoryLeak>> = _memoryLeaks.asStateFlow()
    
    private val _memoryAlerts = MutableStateFlow<List<MemoryAlert>>(emptyList())
    val memoryAlerts: StateFlow<List<MemoryAlert>> = _memoryAlerts.asStateFlow()
    
    // Memory usage history for trend analysis
    private val memoryHistory = mutableListOf<MemorySnapshot>()
    private var baselineMemoryMB: Long = 0
    private var isMonitoring = false
    
    init {
        logger.info(TAG, "MemoryLeakDetector initialized")
        establishBaseline()
        startMonitoring()
    }

    /**
     * Start continuous memory monitoring
     */
    private fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        logger.info(TAG, "Starting continuous memory monitoring")
        
        scope.launch {
            while (isActive) {
                try {
                    monitorMemoryUsage()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    logger.error(TAG, "Error during memory monitoring", e)
                    delay(MONITORING_INTERVAL_MS)
                }
            }
        }
    }
    
    /**
     * Establish baseline memory usage
     */
    private fun establishBaseline() {
        // Force garbage collection to get clean baseline
        System.gc()
        Thread.sleep(100)
        System.gc()
        
        val usage = getCurrentMemoryUsage()
        baselineMemoryMB = usage.usedMemoryMB
        
        logger.logPerformanceMetric(TAG, "baseline_established", baselineMemoryMB.toFloat(), "MB")
    }
    
    /**
     * Monitor current memory usage and detect anomalies
     */
    private fun monitorMemoryUsage() {
        val currentUsage = getCurrentMemoryUsage()
        _memoryUsage.value = currentUsage
        
        // Add to history
        val snapshot = MemorySnapshot(
            timestamp = Clock.System.now(),
            usedMemoryMB = currentUsage.usedMemoryMB,
            freeMemoryMB = currentUsage.freeMemoryMB,
            totalMemoryMB = currentUsage.totalMemoryMB
        )
        
        addToHistory(snapshot)
        
        // Analyze for memory leaks
        analyzeMemoryTrends()
        
        // Check for critical memory usage
        checkCriticalMemoryUsage(currentUsage)
        
        // Log memory metrics
        logger.logPerformanceMetric(TAG, "memory_usage", currentUsage.usedMemoryMB.toFloat(), "MB")
    }
    
    /**
     * Get current memory usage statistics
     */
    private fun getCurrentMemoryUsage(): MemoryUsage {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        return MemoryUsage(
            usedMemoryMB = usedMemory / (1024 * 1024),
            freeMemoryMB = freeMemory / (1024 * 1024),
            totalMemoryMB = totalMemory / (1024 * 1024),
            maxMemoryMB = maxMemory / (1024 * 1024),
            heapUsagePercent = ((usedMemory.toDouble() / maxMemory.toDouble()) * 100).toInt(),
            timestamp = Clock.System.now()
        )
    }
    
    /**
     * Add memory snapshot to history
     */
    private fun addToHistory(snapshot: MemorySnapshot) {
        memoryHistory.add(snapshot)
        
        // Keep only recent history
        if (memoryHistory.size > HISTORY_SIZE) {
            memoryHistory.removeAt(0)
        }
    }
    
    /**
     * Analyze memory trends to detect potential leaks
     */
    private fun analyzeMemoryTrends() {
        if (memoryHistory.size < 10) return // Need sufficient data
        
        val recentSnapshots = memoryHistory.takeLast(10)
        val oldestSnapshot = recentSnapshots.first()
        val newestSnapshot = recentSnapshots.last()
        
        val memoryIncrease = newestSnapshot.usedMemoryMB - oldestSnapshot.usedMemoryMB
        val timeSpan = newestSnapshot.timestamp - oldestSnapshot.timestamp
        
        // Check for sustained memory growth
        if (memoryIncrease > MEMORY_LEAK_THRESHOLD_MB) {
            val leak = MemoryLeak(
                id = generateLeakId(),
                detectedAt = Clock.System.now(),
                memoryIncreaseMB = memoryIncrease,
                timeSpanMinutes = timeSpan.inWholeMinutes,
                severity = when {
                    memoryIncrease > CRITICAL_MEMORY_THRESHOLD_MB -> MemoryLeakSeverity.CRITICAL
                    memoryIncrease > GC_SUGGESTION_THRESHOLD_MB -> MemoryLeakSeverity.HIGH
                    else -> MemoryLeakSeverity.MEDIUM
                },
                description = "Sustained memory growth detected: ${memoryIncrease}MB over ${timeSpan.inWholeMinutes} minutes"
            )
            
            reportMemoryLeak(leak)
        }
        
        // Check for memory growth rate
        val growthRateMBPerMinute = if (timeSpan.inWholeMinutes > 0) {
            memoryIncrease.toDouble() / timeSpan.inWholeMinutes.toDouble()
        } else 0.0
        
        if (growthRateMBPerMinute > 5.0) { // More than 5MB per minute growth
            val alert = MemoryAlert(
                id = generateAlertId(),
                type = MemoryAlertType.RAPID_GROWTH,
                message = "Rapid memory growth detected: ${String.format("%.2f", growthRateMBPerMinute)} MB/min",
                severity = MemoryAlertSeverity.WARNING,
                timestamp = Clock.System.now(),
                metadata = mapOf(
                    "growth_rate_mb_per_min" to growthRateMBPerMinute.toString(),
                    "memory_increase_mb" to memoryIncrease.toString(),
                    "time_span_minutes" to timeSpan.inWholeMinutes.toString()
                )
            )
            
            reportMemoryAlert(alert)
        }
    }
    
    /**
     * Check for critical memory usage that requires immediate attention
     */
    private fun checkCriticalMemoryUsage(usage: MemoryUsage) {
        // Check heap usage percentage
        if (usage.heapUsagePercent > 90) {
            val alert = MemoryAlert(
                id = generateAlertId(),
                type = MemoryAlertType.CRITICAL_USAGE,
                message = "Critical heap usage: ${usage.heapUsagePercent}%",
                severity = MemoryAlertSeverity.CRITICAL,
                timestamp = Clock.System.now(),
                metadata = mapOf(
                    "heap_usage_percent" to usage.heapUsagePercent.toString(),
                    "used_memory_mb" to usage.usedMemoryMB.toString(),
                    "max_memory_mb" to usage.maxMemoryMB.toString()
                )
            )
            
            reportMemoryAlert(alert)
            
            // Suggest garbage collection
            suggestGarbageCollection("Critical heap usage detected")
        }
        
        // Check absolute memory usage
        if (usage.usedMemoryMB > baselineMemoryMB + CRITICAL_MEMORY_THRESHOLD_MB) {
            val alert = MemoryAlert(
                id = generateAlertId(),
                type = MemoryAlertType.MEMORY_LEAK_SUSPECTED,
                message = "Suspected memory leak: ${usage.usedMemoryMB - baselineMemoryMB}MB above baseline",
                severity = MemoryAlertSeverity.HIGH,
                timestamp = Clock.System.now(),
                metadata = mapOf(
                    "current_memory_mb" to usage.usedMemoryMB.toString(),
                    "baseline_memory_mb" to baselineMemoryMB.toString(),
                    "increase_mb" to (usage.usedMemoryMB - baselineMemoryMB).toString()
                )
            )
            
            reportMemoryAlert(alert)
        }
    }
    
    /**
     * Report a detected memory leak
     */
    private fun reportMemoryLeak(leak: MemoryLeak) {
        val currentLeaks = _memoryLeaks.value.toMutableList()
        currentLeaks.add(leak)
        
        // Keep only recent leaks (last 50)
        if (currentLeaks.size > 50) {
            currentLeaks.removeAt(0)
        }
        
        _memoryLeaks.value = currentLeaks
        
        logger.error(TAG, "MEMORY LEAK DETECTED: ${leak.description}")
        logger.logPerformanceMetric(TAG, "memory_leak_detected", leak.memoryIncreaseMB.toFloat(), "MB")
        
        // Suggest cleanup for high severity leaks
        if (leak.severity == MemoryLeakSeverity.HIGH || leak.severity == MemoryLeakSeverity.CRITICAL) {
            suggestGarbageCollection("Memory leak detected: ${leak.description}")
        }
    }
    
    /**
     * Report a memory alert
     */
    private fun reportMemoryAlert(alert: MemoryAlert) {
        val currentAlerts = _memoryAlerts.value.toMutableList()
        currentAlerts.add(alert)
        
        // Keep only recent alerts (last 100)
        if (currentAlerts.size > 100) {
            currentAlerts.removeAt(0)
        }
        
        _memoryAlerts.value = currentAlerts
        
        when (alert.severity) {
            MemoryAlertSeverity.CRITICAL -> logger.error(TAG, "CRITICAL MEMORY ALERT: ${alert.message}")
            MemoryAlertSeverity.HIGH -> logger.warn(TAG, "HIGH MEMORY ALERT: ${alert.message}")
            MemoryAlertSeverity.WARNING -> logger.warn(TAG, "MEMORY WARNING: ${alert.message}")
            MemoryAlertSeverity.INFO -> logger.info(TAG, "MEMORY INFO: ${alert.message}")
        }
    }
    
    /**
     * Suggest garbage collection when memory issues are detected
     */
    private fun suggestGarbageCollection(reason: String) {
        logger.warn(TAG, "Suggesting garbage collection: $reason")
        
        scope.launch {
            try {
                // Suggest GC (don't force it as it can impact performance)
                System.gc()
                
                // Wait a bit and check if memory improved
                delay(2.seconds)
                val afterGC = getCurrentMemoryUsage()
                
                logger.logPerformanceMetric(TAG, "gc_suggested", afterGC.usedMemoryMB.toFloat(), "MB")
                
            } catch (e: Exception) {
                logger.error(TAG, "Error during GC suggestion", e)
            }
        }
    }
    
    /**
     * Get memory statistics for monitoring dashboard
     */
    fun getMemoryStatistics(): MemoryStatistics {
        val currentUsage = _memoryUsage.value
        val recentLeaks = _memoryLeaks.value.filter { 
            it.detectedAt > Clock.System.now().minus(1.minutes) 
        }
        val recentAlerts = _memoryAlerts.value.filter { 
            it.timestamp > Clock.System.now().minus(5.minutes) 
        }
        
        return MemoryStatistics(
            currentUsageMB = currentUsage.usedMemoryMB,
            baselineUsageMB = baselineMemoryMB,
            heapUsagePercent = currentUsage.heapUsagePercent,
            totalLeaksDetected = _memoryLeaks.value.size,
            recentLeaks = recentLeaks.size,
            totalAlerts = _memoryAlerts.value.size,
            recentAlerts = recentAlerts.size,
            memoryGrowthMB = currentUsage.usedMemoryMB - baselineMemoryMB,
            isHealthy = currentUsage.heapUsagePercent < 80 && 
                       (currentUsage.usedMemoryMB - baselineMemoryMB) < MEMORY_LEAK_THRESHOLD_MB
        )
    }
    
    /**
     * Force memory analysis (for testing or manual triggers)
     */
    fun forceMemoryAnalysis() {
        scope.launch {
            logger.info(TAG, "Forcing memory analysis")
            monitorMemoryUsage()
        }
    }
    
    /**
     * Clear old memory data (for maintenance)
     */
    fun clearOldData(olderThan: Instant) {
        memoryHistory.removeAll { it.timestamp < olderThan }
        
        val filteredLeaks = _memoryLeaks.value.filter { it.detectedAt > olderThan }
        _memoryLeaks.value = filteredLeaks
        
        val filteredAlerts = _memoryAlerts.value.filter { it.timestamp > olderThan }
        _memoryAlerts.value = filteredAlerts
        
        logger.info(TAG, "Cleared old memory data, remaining: ${memoryHistory.size} snapshots, " +
                         "${filteredLeaks.size} leaks, ${filteredAlerts.size} alerts")
    }
    
    private fun generateLeakId(): String = "LEAK_${Clock.System.now().toEpochMilliseconds()}"
    private fun generateAlertId(): String = "ALERT_${Clock.System.now().toEpochMilliseconds()}"
}

/**
 * Current memory usage information
 */
data class MemoryUsage(
    val usedMemoryMB: Long,
    val freeMemoryMB: Long,
    val totalMemoryMB: Long,
    val maxMemoryMB: Long,
    val heapUsagePercent: Int,
    val timestamp: Instant
)

/**
 * Memory snapshot for history tracking
 */
data class MemorySnapshot(
    val timestamp: Instant,
    val usedMemoryMB: Long,
    val freeMemoryMB: Long,
    val totalMemoryMB: Long
)

/**
 * Detected memory leak information
 */
data class MemoryLeak(
    val id: String,
    val detectedAt: Instant,
    val memoryIncreaseMB: Long,
    val timeSpanMinutes: Long,
    val severity: MemoryLeakSeverity,
    val description: String
)

/**
 * Memory leak severity levels
 */
enum class MemoryLeakSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Memory alert information
 */
data class MemoryAlert(
    val id: String,
    val type: MemoryAlertType,
    val message: String,
    val severity: MemoryAlertSeverity,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Types of memory alerts
 */
enum class MemoryAlertType {
    RAPID_GROWTH,
    CRITICAL_USAGE,
    MEMORY_LEAK_SUSPECTED,
    GC_RECOMMENDED
}

/**
 * Memory alert severity levels
 */
enum class MemoryAlertSeverity {
    INFO,
    WARNING,
    HIGH,
    CRITICAL
}

/**
 * Memory statistics for dashboard
 */
data class MemoryStatistics(
    val currentUsageMB: Long,
    val baselineUsageMB: Long,
    val heapUsagePercent: Int,
    val totalLeaksDetected: Int,
    val recentLeaks: Int,
    val totalAlerts: Int,
    val recentAlerts: Int,
    val memoryGrowthMB: Long,
    val isHealthy: Boolean
)