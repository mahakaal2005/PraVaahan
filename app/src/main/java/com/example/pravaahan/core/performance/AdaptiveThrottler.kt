package com.example.pravaahan.core.performance

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Adaptive throttling system that adjusts update rates based on system performance
 * and data volume to maintain optimal real-time performance.
 */
@Singleton
class AdaptiveThrottler @Inject constructor(
    private val logger: Logger,
    private val deviceMonitor: DevicePerformanceMonitor
) {
    
    companion object {
        private const val TAG = "AdaptiveThrottler"
        private const val MIN_SAMPLE_RATE_MS = 50L
        private const val MAX_SAMPLE_RATE_MS = 1000L
        private const val DEFAULT_SAMPLE_RATE_MS = 200L
        private const val PERFORMANCE_THRESHOLD_MS = 100L
    }
    
    private var currentSampleRate = DEFAULT_SAMPLE_RATE_MS
    private var lastAdjustmentTime = Clock.System.now()
    private val recentPerformanceMetrics = mutableListOf<Long>()
    
    /**
     * Throttle position updates based on current system performance
     */
    suspend fun throttle(updates: List<TrainPosition>): List<TrainPosition> {
        if (updates.isEmpty()) return updates
        
        adjustSampleRateBasedOnPerformance()
        
        return if (shouldThrottle()) {
            val throttledUpdates = selectRepresentativeUpdates(updates)
            logger.debug(TAG, "Throttled ${updates.size} updates to ${throttledUpdates.size}")
            throttledUpdates
        } else {
            updates
        }
    }
    
    /**
     * Get current sample rate for flow operations
     */
    fun getCurrentSampleRate(): Long = currentSampleRate
    
    /**
     * Adjust sample rate based on recent performance metrics
     */
    private suspend fun adjustSampleRateBasedOnPerformance() {
        val now = Clock.System.now()
        val timeSinceLastAdjustment = now - lastAdjustmentTime
        
        // Only adjust every few seconds to avoid oscillation
        if (timeSinceLastAdjustment.inWholeSeconds < 3) return
        
        val deviceMetrics = deviceMonitor.getCurrentMetrics()
        val performanceMode = deviceMonitor.getPerformanceMode()
        
        when (performanceMode) {
            PerformanceMode.BATTERY_SAVER -> {
                // Aggressive throttling for battery saving
                currentSampleRate = maxOf(currentSampleRate * 2, MAX_SAMPLE_RATE_MS)
                logger.info(TAG, "Battery saver mode: increased throttling to ${currentSampleRate}ms")
            }
            PerformanceMode.BALANCED -> {
                // Moderate throttling for balanced performance
                if (deviceMetrics.memoryUsage > 80f || deviceMetrics.thermalState >= 2) {
                    currentSampleRate = minOf(currentSampleRate * 1.5f, MAX_SAMPLE_RATE_MS.toFloat()).toLong()
                    logger.info(TAG, "Balanced mode with pressure: increased throttling to ${currentSampleRate}ms")
                }
            }
            PerformanceMode.PERFORMANCE -> {
                // Reduce throttling for better performance
                if (deviceMetrics.memoryUsage < 50f && deviceMetrics.thermalState == 0) {
                    currentSampleRate = maxOf(currentSampleRate / 2, MIN_SAMPLE_RATE_MS)
                    logger.info(TAG, "Performance mode: reduced throttling to ${currentSampleRate}ms")
                }
            }
        }
        
        lastAdjustmentTime = now
    }
    
    /**
     * Determine if throttling should be applied
     */
    private fun shouldThrottle(): Boolean {
        return currentSampleRate > MIN_SAMPLE_RATE_MS
    }
    
    /**
     * Select representative updates when throttling is needed
     */
    private fun selectRepresentativeUpdates(updates: List<TrainPosition>): List<TrainPosition> {
        if (updates.size <= 1) return updates
        
        // Group by train ID and select most recent for each train
        val updatesByTrain = updates.groupBy { it.trainId }
        
        return updatesByTrain.mapNotNull { (trainId, positions) ->
            // For each train, select the most recent position
            val mostRecent = positions.maxByOrNull { it.timestamp }
            
            // Also include any position with significant movement
            val significantMovements = positions.filter { position ->
                positions.any { other ->
                    other != position && hasSignificantMovement(position, other)
                }
            }
            
            // Return most recent plus any significant movements
            (listOfNotNull(mostRecent) + significantMovements).distinctBy { it.timestamp }
        }.flatten()
    }
    
    /**
     * Check if there's significant movement between two positions
     */
    private fun hasSignificantMovement(pos1: TrainPosition, pos2: TrainPosition): Boolean {
        val latDiff = kotlin.math.abs(pos1.latitude - pos2.latitude)
        val lngDiff = kotlin.math.abs(pos1.longitude - pos2.longitude)
        val speedDiff = kotlin.math.abs(pos1.speed - pos2.speed)
        
        return latDiff > 0.001 || lngDiff > 0.001 || speedDiff > 5.0
    }
    
    /**
     * Reset throttling to default settings
     */
    fun reset() {
        currentSampleRate = DEFAULT_SAMPLE_RATE_MS
        lastAdjustmentTime = Clock.System.now()
        recentPerformanceMetrics.clear()
        logger.info(TAG, "Reset throttling to default settings")
    }
    
    /**
     * Get current throttling statistics
     */
    fun getThrottlingStats(): Map<String, Any> {
        return mapOf(
            "currentSampleRate" to currentSampleRate,
            "minSampleRate" to MIN_SAMPLE_RATE_MS,
            "maxSampleRate" to MAX_SAMPLE_RATE_MS,
            "isThrottling" to shouldThrottle()
        )
    }
}