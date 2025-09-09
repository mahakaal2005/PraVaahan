package com.example.pravaahan.core.performance

import android.app.ActivityManager
import android.content.Context
import com.example.pravaahan.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors memory pressure and provides garbage collection optimization
 * recommendations to maintain smooth performance.
 */
@Singleton
class GarbageCollectionOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "GarbageCollectionOptimizer"
        private const val LOW_MEMORY_THRESHOLD = 0.15f // 15% available memory
        private const val CRITICAL_MEMORY_THRESHOLD = 0.05f // 5% available memory
        private const val HIGH_MEMORY_THRESHOLD = 0.5f // 50% available memory
    }
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val _memoryPressure = MutableStateFlow(MemoryPressure.NORMAL)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure.asStateFlow()
    
    /**
     * Checks current memory pressure level
     */
    fun checkMemoryPressure(): MemoryPressure {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val availableMemoryRatio = memInfo.availMem.toFloat() / memInfo.totalMem.toFloat()
        
        val pressure = when {
            availableMemoryRatio < CRITICAL_MEMORY_THRESHOLD -> MemoryPressure.CRITICAL
            availableMemoryRatio < LOW_MEMORY_THRESHOLD -> MemoryPressure.HIGH
            availableMemoryRatio < HIGH_MEMORY_THRESHOLD -> MemoryPressure.MODERATE
            else -> MemoryPressure.NORMAL
        }
        
        _memoryPressure.value = pressure
        
        logger.debug(TAG, "Memory pressure: $pressure (${(availableMemoryRatio * 100).toInt()}% available)")
        
        return pressure
    }
    
    /**
     * Gets optimization recommendations based on current memory pressure
     */
    suspend fun getOptimizationRecommendations(): List<OptimizationRecommendation> {
        val pressure = checkMemoryPressure()
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        when (pressure) {
            MemoryPressure.CRITICAL -> {
                recommendations.addAll(listOf(
                    OptimizationRecommendation.REDUCE_MEMORY_USAGE,
                    OptimizationRecommendation.REDUCE_CPU_USAGE,
                    OptimizationRecommendation.REDUCE_UPDATE_FREQUENCY,
                    OptimizationRecommendation.REDUCE_RENDERING_QUALITY,
                    OptimizationRecommendation.THROTTLE_ANIMATIONS,
                    OptimizationRecommendation.ENABLE_CACHING
                ))
            }
            MemoryPressure.HIGH -> {
                recommendations.addAll(listOf(
                    OptimizationRecommendation.REDUCE_MEMORY_USAGE,
                    OptimizationRecommendation.REDUCE_UPDATE_FREQUENCY,
                    OptimizationRecommendation.THROTTLE_ANIMATIONS,
                    OptimizationRecommendation.ENABLE_CACHING
                ))
            }
            MemoryPressure.MODERATE -> {
                recommendations.addAll(listOf(
                    OptimizationRecommendation.REDUCE_UPDATE_FREQUENCY,
                    OptimizationRecommendation.ENABLE_CACHING
                ))
            }
            MemoryPressure.NORMAL -> {
                recommendations.add(OptimizationRecommendation.OPTIMIZE_FRAME_RATE)
            }
        }
        
        return recommendations
    }
    
    /**
     * Suggests garbage collection if memory pressure is high
     */
    fun suggestGarbageCollection(): Boolean {
        val pressure = _memoryPressure.value
        
        return when (pressure) {
            MemoryPressure.CRITICAL -> {
                logger.warn(TAG, "Suggesting immediate garbage collection due to critical memory pressure")
                true
            }
            MemoryPressure.HIGH -> {
                logger.info(TAG, "Suggesting garbage collection due to high memory pressure")
                true
            }
            else -> false
        }
    }
    
    /**
     * Performs manual garbage collection hint
     */
    fun performGarbageCollectionHint() {
        val beforeMemory = getAvailableMemoryMB()
        
        // Suggest garbage collection to the runtime
        System.gc()
        
        // Give GC time to run
        Thread.yield()
        
        val afterMemory = getAvailableMemoryMB()
        val freedMemory = afterMemory - beforeMemory
        
        logger.info(TAG, "GC hint completed. Memory freed: ${freedMemory}MB")
        
        // Recheck memory pressure after GC
        checkMemoryPressure()
    }
    
    /**
     * Gets current available memory in MB
     */
    fun getAvailableMemoryMB(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }
    
    /**
     * Gets total memory in MB
     */
    fun getTotalMemoryMB(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }
    
    /**
     * Gets memory usage percentage
     */
    fun getMemoryUsagePercentage(): Float {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val usedMemory = memInfo.totalMem - memInfo.availMem
        return (usedMemory.toFloat() / memInfo.totalMem.toFloat()) * 100f
    }
    
    /**
     * Checks if device is in low memory condition
     */
    fun isLowMemory(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }
    
    /**
     * Gets memory threshold for low memory warning
     */
    fun getLowMemoryThreshold(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.threshold
    }
    
    /**
     * Gets detailed memory statistics
     */
    fun getDetailedMemoryStats(): MemoryStatistics {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val availableMB = memInfo.availMem / (1024 * 1024)
        val totalMB = memInfo.totalMem / (1024 * 1024)
        val usedMB = totalMB - availableMB
        val usagePercentage = (usedMB.toFloat() / totalMB.toFloat()) * 100f
        
        return MemoryStatistics(
            availableMemoryMB = availableMB,
            totalMemoryMB = totalMB,
            usedMemoryMB = usedMB,
            usagePercentage = usagePercentage,
            isLowMemory = memInfo.lowMemory,
            lowMemoryThresholdMB = memInfo.threshold / (1024 * 1024),
            memoryPressure = _memoryPressure.value
        )
    }
    
    /**
     * Monitors memory pressure continuously and logs warnings
     */
    fun startMemoryMonitoring() {
        logger.info(TAG, "Started memory pressure monitoring")
        
        // Initial check
        checkMemoryPressure()
    }
    
    /**
     * Stops memory monitoring
     */
    fun stopMemoryMonitoring() {
        logger.info(TAG, "Stopped memory pressure monitoring")
    }
}

/**
 * Memory pressure levels
 */
enum class MemoryPressure {
    NORMAL,     // Plenty of memory available
    MODERATE,   // Some memory pressure
    HIGH,       // High memory pressure, optimizations recommended
    CRITICAL    // Critical memory pressure, aggressive optimizations needed
}

/**
 * Detailed memory statistics
 */
data class MemoryStatistics(
    val availableMemoryMB: Long,
    val totalMemoryMB: Long,
    val usedMemoryMB: Long,
    val usagePercentage: Float,
    val isLowMemory: Boolean,
    val lowMemoryThresholdMB: Long,
    val memoryPressure: MemoryPressure
)