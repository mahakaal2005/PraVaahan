package com.example.pravaahan.presentation.ui.components.animation

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import com.example.pravaahan.presentation.ui.components.map.MapState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimizes animation performance by determining which trains are visible and need animation.
 * Implements viewport culling and buffer zones for smooth entry/exit animations.
 */
@Singleton
class VisibilityOptimizer @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "VisibilityOptimizer"
        private const val VISIBILITY_BUFFER = 100f // Extra pixels around viewport for smooth transitions
        private const val PRIORITY_BUFFER_MULTIPLIER = 1.5f // Extra buffer for high-priority trains
    }
    
    /**
     * Determines if a train position is visible within the current viewport.
     */
    fun isTrainVisible(
        position: TrainPosition,
        mapState: MapState,
        trainPriority: com.example.pravaahan.domain.model.TrainPriority? = null
    ): Boolean {
        val screenPosition = mapState.railwayToScreen(
            Offset(position.latitude.toFloat(), position.longitude.toFloat())
        )
        
        val buffer = when (trainPriority) {
            com.example.pravaahan.domain.model.TrainPriority.EXPRESS,
            com.example.pravaahan.domain.model.TrainPriority.HIGH -> VISIBILITY_BUFFER * PRIORITY_BUFFER_MULTIPLIER
            else -> VISIBILITY_BUFFER
        }
        
        val canvasSize = mapState.canvasSize
        val isVisible = screenPosition.x >= -buffer && 
                       screenPosition.x <= canvasSize.width + buffer &&
                       screenPosition.y >= -buffer && 
                       screenPosition.y <= canvasSize.height + buffer
        
        if (!isVisible) {
            logger.debug(TAG, "Train ${position.trainId} not visible: position=(${screenPosition.x}, ${screenPosition.y}), canvas=${canvasSize.width}x${canvasSize.height}")
        }
        
        return isVisible
    }
    
    /**
     * Calculates visibility for multiple trains and returns optimization recommendations.
     */
    fun calculateVisibilityBatch(
        positions: List<TrainPosition>,
        mapState: MapState,
        trainPriorities: Map<String, com.example.pravaahan.domain.model.TrainPriority> = emptyMap()
    ): VisibilityResult {
        val visibleTrains = mutableListOf<String>()
        val hiddenTrains = mutableListOf<String>()
        val priorityVisible = mutableListOf<String>()
        
        positions.forEach { position ->
            val priority = trainPriorities[position.trainId]
            val isVisible = isTrainVisible(position, mapState, priority)
            
            if (isVisible) {
                visibleTrains.add(position.trainId)
                if (priority == com.example.pravaahan.domain.model.TrainPriority.EXPRESS || 
                    priority == com.example.pravaahan.domain.model.TrainPriority.HIGH) {
                    priorityVisible.add(position.trainId)
                }
            } else {
                hiddenTrains.add(position.trainId)
            }
        }
        
        logger.debug(TAG, "Visibility batch: ${visibleTrains.size} visible, ${hiddenTrains.size} hidden, ${priorityVisible.size} priority")
        
        return VisibilityResult(
            visibleTrains = visibleTrains,
            hiddenTrains = hiddenTrains,
            priorityVisible = priorityVisible,
            totalProcessed = positions.size
        )
    }
    
    /**
     * Determines animation priority based on visibility and train importance.
     */
    fun getAnimationPriority(
        trainId: String,
        isVisible: Boolean,
        trainPriority: com.example.pravaahan.domain.model.TrainPriority?,
        isMoving: Boolean
    ): AnimationPriority {
        return when {
            !isVisible -> AnimationPriority.NONE
            trainPriority == com.example.pravaahan.domain.model.TrainPriority.EXPRESS -> AnimationPriority.CRITICAL
            trainPriority == com.example.pravaahan.domain.model.TrainPriority.HIGH && isMoving -> AnimationPriority.HIGH
            isMoving -> AnimationPriority.MEDIUM
            else -> AnimationPriority.LOW
        }
    }
    
    /**
     * Calculates optimal frame rate based on visible train count and device performance.
     */
    fun calculateOptimalFrameRate(visibleTrainCount: Int, devicePerformanceLevel: DevicePerformanceLevel): Int {
        return when (devicePerformanceLevel) {
            DevicePerformanceLevel.HIGH -> when {
                visibleTrainCount <= 20 -> 60
                visibleTrainCount <= 40 -> 45
                else -> 30
            }
            DevicePerformanceLevel.MEDIUM -> when {
                visibleTrainCount <= 15 -> 45
                visibleTrainCount <= 30 -> 30
                else -> 20
            }
            DevicePerformanceLevel.LOW -> when {
                visibleTrainCount <= 10 -> 30
                visibleTrainCount <= 20 -> 20
                else -> 15
            }
        }
    }
}

/**
 * Result of visibility calculation for a batch of trains.
 */
data class VisibilityResult(
    val visibleTrains: List<String>,
    val hiddenTrains: List<String>,
    val priorityVisible: List<String>,
    val totalProcessed: Int
) {
    val visibilityRatio: Float = if (totalProcessed > 0) visibleTrains.size.toFloat() / totalProcessed else 0f
}

/**
 * Animation priority levels for resource allocation.
 */
enum class AnimationPriority {
    NONE,     // No animation needed
    LOW,      // Basic animation, lower frame rate acceptable
    MEDIUM,   // Standard animation quality
    HIGH,     // High-quality animation for important trains
    CRITICAL  // Maximum quality for express/emergency trains
}

/**
 * Device performance levels for adaptive animation quality.
 */
enum class DevicePerformanceLevel {
    LOW,    // Older devices, limited resources
    MEDIUM, // Mid-range devices
    HIGH    // High-end devices, full performance
}