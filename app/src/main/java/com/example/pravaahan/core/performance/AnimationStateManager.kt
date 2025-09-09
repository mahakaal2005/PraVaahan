package com.example.pravaahan.core.performance

import com.example.pravaahan.core.logging.Logger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * Manages animation states for train markers with memory-efficient pooling
 * and automatic cleanup of inactive animations.
 */
@Singleton
class AnimationStateManager @Inject constructor(
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "AnimationStateManager"
        private const val MAX_ANIMATION_STATES = 200
        private const val CLEANUP_INTERVAL_MINUTES = 5L
        private const val INACTIVE_THRESHOLD_MINUTES = 10L
    }
    
    private val animationStates = mutableMapOf<String, AnimationState>()
    private val statePool = mutableListOf<AnimationState>()
    private var lastCleanupTime = Clock.System.now()
    
    /**
     * Gets or creates an animation state for a train
     */
    fun getAnimationState(trainId: String): AnimationState {
        return animationStates.getOrPut(trainId) {
            createAnimationState(trainId)
        }.also {
            it.lastAccessTime = Clock.System.now()
        }
    }
    
    /**
     * Updates the target position for a train's animation
     */
    fun updateTarget(trainId: String, targetX: Float, targetY: Float) {
        val state = getAnimationState(trainId)
        state.updateTarget(targetX, targetY)
        
        logger.debug(TAG, "Updated target for train $trainId: ($targetX, $targetY)")
    }
    
    /**
     * Updates animation progress for a train
     */
    fun updateProgress(trainId: String, progress: Float) {
        val state = animationStates[trainId]
        if (state != null) {
            state.updateProgress(progress)
            
            if (!state.isAnimating && progress >= 1.0f) {
                logger.debug(TAG, "Animation completed for train $trainId")
            }
        }
    }
    
    /**
     * Gets current position for a train
     */
    fun getCurrentPosition(trainId: String): Pair<Float, Float> {
        val state = animationStates[trainId]
        return state?.getCurrentPosition() ?: (0f to 0f)
    }
    
    /**
     * Checks if a train is currently animating
     */
    fun isAnimating(trainId: String): Boolean {
        return animationStates[trainId]?.isAnimating ?: false
    }
    
    /**
     * Removes animation state for a train
     */
    fun removeAnimationState(trainId: String) {
        val state = animationStates.remove(trainId)
        if (state != null) {
            recycleState(state)
            logger.debug(TAG, "Removed animation state for train $trainId")
        }
    }
    
    /**
     * Gets the number of active animation states
     */
    fun getActiveStateCount(): Int = animationStates.size
    
    /**
     * Gets all currently animating trains
     */
    fun getAnimatingTrains(): List<String> {
        return animationStates.entries
            .filter { it.value.isAnimating }
            .map { it.key }
    }
    
    /**
     * Performs periodic cleanup of inactive states
     */
    fun performCleanup() {
        if (shouldPerformCleanup()) {
            cleanupInactiveStates()
        }
    }
    
    /**
     * Forces cleanup when memory pressure is high
     */
    fun forceCleanup() {
        cleanupInactiveStates()
        logger.info(TAG, "Forced cleanup completed")
    }
    
    /**
     * Gets memory usage statistics
     */
    fun getMemoryStats(): AnimationMemoryStats {
        val activeStates = animationStates.size
        val pooledStates = statePool.size
        val animatingCount = animationStates.values.count { it.isAnimating }
        val estimatedMemoryKB = (activeStates + pooledStates) * 0.5f // ~0.5KB per state
        
        return AnimationMemoryStats(
            activeStates = activeStates,
            pooledStates = pooledStates,
            animatingCount = animatingCount,
            estimatedMemoryKB = estimatedMemoryKB,
            maxStates = MAX_ANIMATION_STATES
        )
    }
    
    /**
     * Creates a new animation state, using pooled instance if available
     */
    private fun createAnimationState(trainId: String): AnimationState {
        return if (statePool.isNotEmpty()) {
            val state = statePool.removeLastOrNull() ?: AnimationState(trainId)
            state.reset(trainId)
            state
        } else {
            AnimationState(trainId)
        }
    }
    
    /**
     * Recycles an animation state back to the pool
     */
    private fun recycleState(state: AnimationState) {
        if (statePool.size < MAX_ANIMATION_STATES / 4) { // Keep pool size reasonable
            state.reset("")
            statePool.add(state)
        }
    }
    
    /**
     * Checks if cleanup should be performed
     */
    private fun shouldPerformCleanup(): Boolean {
        val now = Clock.System.now()
        return (now - lastCleanupTime).inWholeMinutes >= CLEANUP_INTERVAL_MINUTES ||
               animationStates.size > MAX_ANIMATION_STATES
    }
    
    /**
     * Cleans up inactive animation states
     */
    private fun cleanupInactiveStates() {
        val now = Clock.System.now()
        val cutoffTime = now.minus(INACTIVE_THRESHOLD_MINUTES.minutes)
        val iterator = animationStates.iterator()
        var removedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val state = entry.value
            
            if (state.lastAccessTime < cutoffTime && !state.isAnimating) {
                recycleState(state)
                iterator.remove()
                removedCount++
            }
        }
        
        lastCleanupTime = now
        
        if (removedCount > 0) {
            logger.info(TAG, "Cleanup completed: removed $removedCount inactive animation states")
        }
        
        // Force removal of oldest states if still over limit
        if (animationStates.size > MAX_ANIMATION_STATES) {
            forceRemoveOldestStates()
        }
    }
    
    /**
     * Forces removal of oldest states when over capacity
     */
    private fun forceRemoveOldestStates() {
        val sortedStates = animationStates.entries.sortedBy { it.value.lastAccessTime }
        val toRemove = sortedStates.take(animationStates.size - MAX_ANIMATION_STATES + 20)
        
        toRemove.forEach { entry ->
            recycleState(entry.value)
            animationStates.remove(entry.key)
        }
        
        logger.warn(TAG, "Force cleanup: removed ${toRemove.size} oldest animation states")
    }
}

/**
 * Animation state for a single train marker
 */
class AnimationState(
    var trainId: String,
    var currentX: Float = 0f,
    var currentY: Float = 0f,
    var targetX: Float = 0f,
    var targetY: Float = 0f,
    var animationProgress: Float = 0f,
    var isAnimating: Boolean = false,
    var lastAccessTime: Instant = Clock.System.now()
) {
    
    /**
     * Updates the target position and starts animation
     */
    fun updateTarget(newTargetX: Float, newTargetY: Float) {
        // If we're already at the target, no need to animate
        if (kotlin.math.abs(currentX - newTargetX) < 0.1f && 
            kotlin.math.abs(currentY - newTargetY) < 0.1f) {
            return
        }
        
        targetX = newTargetX
        targetY = newTargetY
        animationProgress = 0f
        isAnimating = true
        lastAccessTime = Clock.System.now()
    }
    
    /**
     * Updates animation progress (0.0 to 1.0)
     */
    fun updateProgress(progress: Float) {
        animationProgress = progress.coerceIn(0f, 1f)
        lastAccessTime = Clock.System.now()
        
        if (animationProgress >= 1f) {
            // Animation completed
            currentX = targetX
            currentY = targetY
            isAnimating = false
        }
    }
    
    /**
     * Gets current interpolated position
     */
    fun getCurrentPosition(): Pair<Float, Float> {
        return if (isAnimating) {
            val interpolatedX = lerp(currentX, targetX, animationProgress)
            val interpolatedY = lerp(currentY, targetY, animationProgress)
            interpolatedX to interpolatedY
        } else {
            currentX to currentY
        }
    }
    
    /**
     * Resets the state for reuse
     */
    fun reset(newTrainId: String) {
        trainId = newTrainId
        currentX = 0f
        currentY = 0f
        targetX = 0f
        targetY = 0f
        animationProgress = 0f
        isAnimating = false
        lastAccessTime = Clock.System.now()
    }
    
    /**
     * Snaps to target position without animation
     */
    fun snapToTarget() {
        currentX = targetX
        currentY = targetY
        animationProgress = 1f
        isAnimating = false
        lastAccessTime = Clock.System.now()
    }
    
    /**
     * Linear interpolation between two values
     */
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + fraction * (end - start)
    }
}

/**
 * Memory statistics for animation states
 */
data class AnimationMemoryStats(
    val activeStates: Int,
    val pooledStates: Int,
    val animatingCount: Int,
    val estimatedMemoryKB: Float,
    val maxStates: Int
)