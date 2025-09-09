package com.example.pravaahan.presentation.ui.components.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory-efficient animation pool for managing reusable Animatable instances.
 * Reduces GC pressure by reusing animation objects for train markers.
 */
@Singleton
class AnimationPool @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "AnimationPool"
        private const val DEFAULT_POOL_SIZE = 100
        private const val MAX_POOL_SIZE = 200
    }
    
    private val availableAnimations = ConcurrentLinkedQueue<Animatable<Offset, *>>()
    private val activeAnimations = mutableSetOf<String>()
    private val animationMutex = Mutex()
    
    // Performance metrics
    private var totalRequests = 0
    private var poolHits = 0
    private var poolMisses = 0
    
    init {
        // Pre-populate pool with initial animations
        repeat(DEFAULT_POOL_SIZE) {
            availableAnimations.offer(createNewAnimation())
        }
        logger.info(TAG, "Animation pool initialized with $DEFAULT_POOL_SIZE animations")
    }
    
    /**
     * Acquires an animation from the pool or creates a new one if pool is empty.
     */
    suspend fun acquireAnimation(trainId: String): Animatable<Offset, *> = animationMutex.withLock {
        totalRequests++
        
        val animation = availableAnimations.poll()?.also {
            poolHits++
            logger.debug(TAG, "Animation acquired from pool for train $trainId (pool hit)")
        } ?: run {
            poolMisses++
            logger.debug(TAG, "Creating new animation for train $trainId (pool miss)")
            createNewAnimation()
        }
        
        activeAnimations.add(trainId)
        
        // Log performance metrics periodically
        if (totalRequests % 50 == 0) {
            val hitRate = (poolHits.toFloat() / totalRequests * 100).toInt()
            logger.info(TAG, "Pool performance: ${hitRate}% hit rate, ${activeAnimations.size} active, ${availableAnimations.size} available")
        }
        
        animation
    }
    
    /**
     * Returns an animation to the pool for reuse.
     */
    suspend fun releaseAnimation(trainId: String, animation: Animatable<Offset, *>) = animationMutex.withLock {
        if (activeAnimations.remove(trainId)) {
            // Reset animation to initial state
            animation.snapTo(Offset.Zero)
            
            // Only return to pool if we haven't exceeded max size
            if (availableAnimations.size < MAX_POOL_SIZE) {
                availableAnimations.offer(animation)
                logger.debug(TAG, "Animation returned to pool for train $trainId")
            } else {
                logger.debug(TAG, "Pool at capacity, discarding animation for train $trainId")
            }
        }
    }
    
    /**
     * Gets current pool statistics for monitoring.
     */
    fun getPoolStats(): PoolStats {
        return PoolStats(
            totalRequests = totalRequests,
            poolHits = poolHits,
            poolMisses = poolMisses,
            activeAnimations = activeAnimations.size,
            availableAnimations = availableAnimations.size,
            hitRate = if (totalRequests > 0) poolHits.toFloat() / totalRequests else 0f
        )
    }
    
    /**
     * Clears the pool and resets statistics (for testing or memory pressure).
     */
    suspend fun clearPool() = animationMutex.withLock {
        availableAnimations.clear()
        activeAnimations.clear()
        totalRequests = 0
        poolHits = 0
        poolMisses = 0
        logger.info(TAG, "Animation pool cleared")
    }
    
    private fun createNewAnimation(): Animatable<Offset, *> {
        return Animatable(Offset.Zero, Offset.VectorConverter)
    }
}

/**
 * Statistics for animation pool performance monitoring.
 */
data class PoolStats(
    val totalRequests: Int,
    val poolHits: Int,
    val poolMisses: Int,
    val activeAnimations: Int,
    val availableAnimations: Int,
    val hitRate: Float
)