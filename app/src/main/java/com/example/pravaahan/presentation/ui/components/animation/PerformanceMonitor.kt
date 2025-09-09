package com.example.pravaahan.presentation.ui.components.animation

import androidx.compose.runtime.*
import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Monitors animation performance metrics including frame rates, memory usage, and CPU utilization.
 * Provides real-time feedback for adaptive performance optimization.
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val METRICS_WINDOW_SIZE = 60 // Track last 60 measurements
        private const val WARNING_FRAME_TIME_MS = 16.67f // 60fps threshold
        private const val CRITICAL_FRAME_TIME_MS = 33.33f // 30fps threshold
    }
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val frameTimeHistory = mutableListOf<Float>()
    private val memoryUsageHistory = mutableListOf<Long>()
    private var totalAnimations = 0
    private var activeAnimations = 0
    private var droppedFrames = 0
    
    /**
     * Records the time taken for an animation frame.
     */
    fun recordFrameTime(frameTimeMs: Float) {
        synchronized(frameTimeHistory) {
            frameTimeHistory.add(frameTimeMs)
            if (frameTimeHistory.size > METRICS_WINDOW_SIZE) {
                frameTimeHistory.removeAt(0)
            }
            
            // Check for dropped frames
            if (frameTimeMs > WARNING_FRAME_TIME_MS) {
                droppedFrames++
                if (frameTimeMs > CRITICAL_FRAME_TIME_MS) {
                    logger.warn(TAG, "Critical frame time detected: ${frameTimeMs}ms (target: ${WARNING_FRAME_TIME_MS}ms)")
                }
            }
            
            updateMetrics()
        }
    }
    
    /**
     * Records memory usage for animation system.
     */
    fun recordMemoryUsage(memoryBytes: Long) {
        synchronized(memoryUsageHistory) {
            memoryUsageHistory.add(memoryBytes)
            if (memoryUsageHistory.size > METRICS_WINDOW_SIZE) {
                memoryUsageHistory.removeAt(0)
            }
            
            updateMetrics()
        }
    }
    
    /**
     * Records animation lifecycle events.
     */
    fun recordAnimationStart(trainId: String) {
        totalAnimations++
        activeAnimations++
        logger.debug(TAG, "Animation started for train $trainId (active: $activeAnimations)")
    }
    
    /**
     * Records animation completion.
     */
    fun recordAnimationEnd(trainId: String, durationMs: Long) {
        activeAnimations = (activeAnimations - 1).coerceAtLeast(0)
        logger.debug(TAG, "Animation completed for train $trainId in ${durationMs}ms (active: $activeAnimations)")
    }
    
    /**
     * Measures and records the execution time of an animation operation.
     */
    fun <T> measureAnimationOperation(
        operationName: String,
        operation: () -> T
    ): T {
        val result: T
        val executionTime = measureTimeMillis {
            result = operation()
        }
        
        recordFrameTime(executionTime.toFloat())
        
        if (executionTime > WARNING_FRAME_TIME_MS) {
            logger.warn(TAG, "Slow animation operation '$operationName': ${executionTime}ms")
        }
        
        return result
    }
    
    /**
     * Gets current performance recommendations based on metrics.
     */
    fun getPerformanceRecommendations(): List<PerformanceRecommendation> {
        val metrics = _performanceMetrics.value
        val recommendations = mutableListOf<PerformanceRecommendation>()
        
        // Frame rate recommendations
        if (metrics.averageFrameTime > CRITICAL_FRAME_TIME_MS) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.REDUCE_QUALITY,
                    message = "Frame rate critically low (${metrics.currentFps}fps). Consider reducing animation quality.",
                    severity = RecommendationSeverity.CRITICAL
                )
            )
        } else if (metrics.averageFrameTime > WARNING_FRAME_TIME_MS) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.OPTIMIZE_ANIMATIONS,
                    message = "Frame rate below target (${metrics.currentFps}fps). Consider optimizing animations.",
                    severity = RecommendationSeverity.WARNING
                )
            )
        }
        
        // Memory recommendations
        if (metrics.memoryUsageMB > 100) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.REDUCE_MEMORY,
                    message = "High memory usage (${metrics.memoryUsageMB}MB). Consider reducing active animations.",
                    severity = RecommendationSeverity.WARNING
                )
            )
        }
        
        // Active animation recommendations
        if (metrics.activeAnimations > 50) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.LIMIT_ANIMATIONS,
                    message = "High number of active animations (${metrics.activeAnimations}). Consider visibility culling.",
                    severity = RecommendationSeverity.INFO
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Resets all performance metrics (useful for testing or after configuration changes).
     */
    fun resetMetrics() {
        synchronized(frameTimeHistory) {
            frameTimeHistory.clear()
        }
        synchronized(memoryUsageHistory) {
            memoryUsageHistory.clear()
        }
        totalAnimations = 0
        activeAnimations = 0
        droppedFrames = 0
        
        _performanceMetrics.value = PerformanceMetrics()
        logger.info(TAG, "Performance metrics reset")
    }
    
    private fun updateMetrics() {
        val avgFrameTime = if (frameTimeHistory.isNotEmpty()) {
            frameTimeHistory.average().toFloat()
        } else 0f
        
        val currentFps = if (avgFrameTime > 0) {
            (1000f / avgFrameTime).toInt()
        } else 0
        
        val memoryUsage = if (memoryUsageHistory.isNotEmpty()) {
            memoryUsageHistory.last()
        } else 0L
        
        val memoryUsageMB = (memoryUsage / (1024 * 1024)).toInt()
        
        _performanceMetrics.value = PerformanceMetrics(
            averageFrameTime = avgFrameTime,
            currentFps = currentFps,
            droppedFrames = droppedFrames,
            activeAnimations = activeAnimations,
            totalAnimations = totalAnimations,
            memoryUsageMB = memoryUsageMB
        )
    }
}

/**
 * Performance metrics for animation system.
 */
data class PerformanceMetrics(
    val averageFrameTime: Float = 0f,
    val currentFps: Int = 0,
    val droppedFrames: Int = 0,
    val activeAnimations: Int = 0,
    val totalAnimations: Int = 0,
    val memoryUsageMB: Int = 0
) {
    val isPerformanceGood: Boolean = averageFrameTime <= 16.67f && droppedFrames < 5
    val isPerformanceCritical: Boolean = averageFrameTime > 33.33f || droppedFrames > 20
}

/**
 * Performance recommendation for optimization.
 */
data class PerformanceRecommendation(
    val type: RecommendationType,
    val message: String,
    val severity: RecommendationSeverity
)

/**
 * Types of performance recommendations.
 */
enum class RecommendationType {
    REDUCE_QUALITY,
    OPTIMIZE_ANIMATIONS,
    REDUCE_MEMORY,
    LIMIT_ANIMATIONS,
    INCREASE_QUALITY
}

/**
 * Severity levels for performance recommendations.
 */
enum class RecommendationSeverity {
    INFO,
    WARNING,
    CRITICAL
}