package com.example.pravaahan.core.performance

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import com.example.pravaahan.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors device performance metrics to enable adaptive optimization
 * based on current system conditions.
 */
@Singleton
class DevicePerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "DevicePerformanceMonitor"
        private const val LOW_MEMORY_THRESHOLD = 0.8f // 80% memory usage
        private const val LOW_BATTERY_THRESHOLD = 20 // 20% battery
        private const val HIGH_CPU_THRESHOLD = 0.7f // 70% CPU usage
    }
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private val _performanceMetrics = MutableStateFlow(DevicePerformanceMetrics())
    val performanceMetrics: StateFlow<DevicePerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    /**
     * Gets current device performance metrics
     */
    suspend fun getCurrentMetrics(): DevicePerformanceMetrics {
        val metrics = DevicePerformanceMetrics(
            cpuUsage = getCpuUsage(),
            memoryUsage = getMemoryUsage(),
            frameRate = getEstimatedFrameRate(),
            batteryLevel = getBatteryLevel(),
            thermalState = getThermalState(),
            isPowerSaveMode = isPowerSaveMode(),
            availableMemoryMB = getAvailableMemoryMB(),
            totalMemoryMB = getTotalMemoryMB()
        )
        
        _performanceMetrics.value = metrics
        
        logger.debug(TAG, "Performance metrics: CPU=${metrics.cpuUsage}%, " +
                "Memory=${metrics.memoryUsage}%, Battery=${metrics.batteryLevel}%, " +
                "Thermal=${metrics.thermalState}")
        
        return metrics
    }
    
    /**
     * Estimates CPU usage based on available system information
     */
    private fun getCpuUsage(): Float {
        return try {
            // This is a simplified estimation - in production, you might use
            // more sophisticated CPU monitoring techniques
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            // Use memory pressure as a proxy for CPU usage
            val memoryPressure = 1.0f - (memInfo.availMem.toFloat() / memInfo.totalMem.toFloat())
            (memoryPressure * 100f).coerceIn(0f, 100f)
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to get CPU usage: ${e.message}")
            50f // Default moderate usage
        }
    }
    
    /**
     * Gets current memory usage percentage
     */
    private fun getMemoryUsage(): Float {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val usedMemory = memInfo.totalMem - memInfo.availMem
            val memoryUsage = (usedMemory.toFloat() / memInfo.totalMem.toFloat()) * 100f
            
            memoryUsage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to get memory usage: ${e.message}")
            50f
        }
    }
    
    /**
     * Estimates current frame rate based on system performance
     */
    private fun getEstimatedFrameRate(): Float {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            // Estimate frame rate based on available memory and thermal state
            val memoryFactor = memInfo.availMem.toFloat() / memInfo.totalMem.toFloat()
            val thermalFactor = when (getThermalState()) {
                0 -> 1.0f // Normal
                1 -> 0.8f // Light throttling
                2 -> 0.6f // Moderate throttling
                3 -> 0.4f // Severe throttling
                else -> 0.3f // Critical throttling
            }
            
            val estimatedFps = 60f * memoryFactor * thermalFactor
            estimatedFps.coerceIn(10f, 60f)
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to estimate frame rate: ${e.message}")
            30f
        }
    }
    
    /**
     * Gets current battery level percentage
     */
    private fun getBatteryLevel(): Float {
        return try {
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            batteryLevel.toFloat().coerceIn(0f, 100f)
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to get battery level: ${e.message}")
            50f
        }
    }
    
    /**
     * Gets thermal throttling state
     */
    private fun getThermalState(): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                powerManager.currentThermalStatus
            } else {
                0 // Assume normal for older devices
            }
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to get thermal state: ${e.message}")
            0
        }
    }
    
    /**
     * Checks if device is in power save mode
     */
    private fun isPowerSaveMode(): Boolean {
        return try {
            powerManager.isPowerSaveMode
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to check power save mode: ${e.message}")
            false
        }
    }
    
    /**
     * Gets available memory in MB
     */
    private fun getAvailableMemoryMB(): Long {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.availMem / (1024 * 1024)
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to get available memory: ${e.message}")
            512L // Default 512MB
        }
    }
    
    /**
     * Gets total memory in MB
     */
    private fun getTotalMemoryMB(): Long {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem / (1024 * 1024)
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to get total memory: ${e.message}")
            2048L // Default 2GB
        }
    }
    
    /**
     * Determines current performance mode based on metrics
     */
    fun getPerformanceMode(): PerformanceMode {
        val metrics = _performanceMetrics.value
        
        return when {
            metrics.batteryLevel < LOW_BATTERY_THRESHOLD || 
            metrics.isPowerSaveMode ||
            metrics.thermalState >= 3 -> PerformanceMode.BATTERY_SAVER
            
            metrics.memoryUsage > LOW_MEMORY_THRESHOLD ||
            metrics.cpuUsage > HIGH_CPU_THRESHOLD ||
            metrics.thermalState >= 2 -> PerformanceMode.BALANCED
            
            else -> PerformanceMode.PERFORMANCE
        }
    }
    
    /**
     * Checks if device is under memory pressure
     */
    fun isMemoryPressureHigh(): Boolean {
        return _performanceMetrics.value.memoryUsage > LOW_MEMORY_THRESHOLD
    }
    
    /**
     * Checks if device is thermally throttled
     */
    fun isThermallyThrottled(): Boolean {
        return _performanceMetrics.value.thermalState >= 2
    }
    
    /**
     * Gets performance optimization recommendations
     */
    fun getOptimizationRecommendations(): List<OptimizationRecommendation> {
        val metrics = _performanceMetrics.value
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        if (metrics.memoryUsage > LOW_MEMORY_THRESHOLD) {
            recommendations.add(OptimizationRecommendation.REDUCE_MEMORY_USAGE)
        }
        
        if (metrics.batteryLevel < LOW_BATTERY_THRESHOLD) {
            recommendations.add(OptimizationRecommendation.REDUCE_CPU_USAGE)
            recommendations.add(OptimizationRecommendation.REDUCE_UPDATE_FREQUENCY)
        }
        
        if (metrics.thermalState >= 2) {
            recommendations.add(OptimizationRecommendation.REDUCE_RENDERING_QUALITY)
            recommendations.add(OptimizationRecommendation.THROTTLE_ANIMATIONS)
        }
        
        if (metrics.frameRate < 30f) {
            recommendations.add(OptimizationRecommendation.OPTIMIZE_FRAME_RATE)
        }
        
        return recommendations
    }
}

/**
 * Device performance metrics data class
 */
data class DevicePerformanceMetrics(
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val frameRate: Float = 60f,
    val batteryLevel: Float = 100f,
    val thermalState: Int = 0,
    val isPowerSaveMode: Boolean = false,
    val availableMemoryMB: Long = 0L,
    val totalMemoryMB: Long = 0L
)

/**
 * Performance modes for adaptive optimization
 */
enum class PerformanceMode {
    PERFORMANCE,    // High performance, no restrictions
    BALANCED,       // Balanced performance and efficiency
    BATTERY_SAVER   // Maximum efficiency, reduced performance
}

/**
 * Optimization recommendations based on device state
 */
enum class OptimizationRecommendation {
    REDUCE_MEMORY_USAGE,
    REDUCE_CPU_USAGE,
    REDUCE_UPDATE_FREQUENCY,
    REDUCE_RENDERING_QUALITY,
    THROTTLE_ANIMATIONS,
    OPTIMIZE_FRAME_RATE,
    ENABLE_CACHING,
    REDUCE_NETWORK_REQUESTS
}