package com.example.pravaahan.core.reliability

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import com.example.pravaahan.domain.model.RealTimeTrainState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

/**
 * Local data cache for offline mode support in railway systems.
 * Provides persistent storage of critical train data for offline operations.
 */
@Singleton
class LocalDataCache @Inject constructor(
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "LocalDataCache"
        private const val MAX_CACHED_POSITIONS = 1000
        private const val MAX_CACHED_STATES = 500
        private const val CACHE_EXPIRY_HOURS = 24L
    }
    
    // In-memory cache - in production, this would be backed by Room database
    private val cachedPositions = mutableMapOf<String, TrainPosition>()
    private val cachedTrainStates = mutableMapOf<String, RealTimeTrainState>()
    private var lastUpdateTime = Instant.DISTANT_PAST
    
    /**
     * Caches train positions for offline access
     */
    suspend fun cachePositions(positions: List<TrainPosition>) {
        val startTime = System.currentTimeMillis()
        
        positions.forEach { position ->
            cachedPositions[position.trainId] = position
        }
        
        // Maintain cache size limit
        if (cachedPositions.size > MAX_CACHED_POSITIONS) {
            cleanupOldPositions()
        }
        
        lastUpdateTime = Clock.System.now()
        
        val duration = System.currentTimeMillis() - startTime
        logger.debug(TAG, "Cached ${positions.size} positions in ${duration}ms")
    }
    
    /**
     * Caches train states for offline access
     */
    suspend fun cacheTrainStates(states: List<RealTimeTrainState>) {
        val startTime = System.currentTimeMillis()
        
        states.forEach { state ->
            cachedTrainStates[state.trainId] = state
        }
        
        // Maintain cache size limit
        if (cachedTrainStates.size > MAX_CACHED_STATES) {
            cleanupOldStates()
        }
        
        val duration = System.currentTimeMillis() - startTime
        logger.debug(TAG, "Cached ${states.size} train states in ${duration}ms")
    }
    
    /**
     * Gets cached train positions
     */
    suspend fun getCachedPositions(): List<TrainPosition> {
        cleanupExpiredData()
        return cachedPositions.values.toList()
    }
    
    /**
     * Gets cached train states
     */
    suspend fun getCachedTrainStates(): List<RealTimeTrainState> {
        cleanupExpiredData()
        return cachedTrainStates.values.toList()
    }
    
    /**
     * Gets cached position for a specific train
     */
    suspend fun getCachedPosition(trainId: String): TrainPosition? {
        cleanupExpiredData()
        return cachedPositions[trainId]
    }
    
    /**
     * Gets cached state for a specific train
     */
    suspend fun getCachedTrainState(trainId: String): RealTimeTrainState? {
        cleanupExpiredData()
        return cachedTrainStates[trainId]
    }
    
    /**
     * Gets count of cached trains
     */
    fun getCachedTrainCount(): Int {
        return cachedPositions.size
    }
    
    /**
     * Gets last update time
     */
    fun getLastUpdateTime(): Instant {
        return lastUpdateTime
    }
    
    /**
     * Checks if cached data is still valid
     */
    fun isCacheValid(): Boolean {
        val now = Clock.System.now()
        val cacheAge = now - lastUpdateTime
        return cacheAge.inWholeHours < CACHE_EXPIRY_HOURS
    }
    
    /**
     * Cleans up old positions to maintain cache size
     */
    private fun cleanupOldPositions() {
        if (cachedPositions.size <= MAX_CACHED_POSITIONS) return
        
        val sortedPositions = cachedPositions.entries.sortedBy { it.value.timestamp }
        val toRemove = sortedPositions.take(cachedPositions.size - MAX_CACHED_POSITIONS + 50)
        
        toRemove.forEach { entry ->
            cachedPositions.remove(entry.key)
        }
        
        logger.debug(TAG, "Cleaned up ${toRemove.size} old positions")
    }
    
    /**
     * Cleans up old states to maintain cache size
     */
    private fun cleanupOldStates() {
        if (cachedTrainStates.size <= MAX_CACHED_STATES) return
        
        val sortedStates = cachedTrainStates.entries.sortedBy { it.value.lastUpdateTime }
        val toRemove = sortedStates.take(cachedTrainStates.size - MAX_CACHED_STATES + 25)
        
        toRemove.forEach { entry ->
            cachedTrainStates.remove(entry.key)
        }
        
        logger.debug(TAG, "Cleaned up ${toRemove.size} old train states")
    }
    
    /**
     * Cleans up expired data
     */
    private fun cleanupExpiredData() {
        val now = Clock.System.now()
        val expiryTime = now.minus(CACHE_EXPIRY_HOURS.hours)
        
        // Remove expired positions
        val expiredPositions = cachedPositions.entries.filter { 
            it.value.timestamp < expiryTime 
        }
        expiredPositions.forEach { entry ->
            cachedPositions.remove(entry.key)
        }
        
        // Remove expired states
        val expiredStates = cachedTrainStates.entries.filter { 
            it.value.lastUpdateTime < expiryTime 
        }
        expiredStates.forEach { entry ->
            cachedTrainStates.remove(entry.key)
        }
        
        if (expiredPositions.isNotEmpty() || expiredStates.isNotEmpty()) {
            logger.debug(TAG, "Cleaned up ${expiredPositions.size} expired positions and ${expiredStates.size} expired states")
        }
    }
    
    /**
     * Clears all cached data
     */
    suspend fun clearCache() {
        val positionCount = cachedPositions.size
        val stateCount = cachedTrainStates.size
        
        cachedPositions.clear()
        cachedTrainStates.clear()
        lastUpdateTime = Instant.DISTANT_PAST
        
        logger.info(TAG, "Cleared cache: $positionCount positions, $stateCount states")
    }
    
    /**
     * Clears cache for a specific train
     */
    suspend fun clearTrainCache(trainId: String) {
        val hadPosition = cachedPositions.remove(trainId) != null
        val hadState = cachedTrainStates.remove(trainId) != null
        
        if (hadPosition || hadState) {
            logger.debug(TAG, "Cleared cache for train $trainId")
        }
    }
    
    /**
     * Gets cache statistics
     */
    fun getCacheStats(): CacheStats {
        val now = Clock.System.now()
        val cacheAge = now - lastUpdateTime
        
        return CacheStats(
            cachedPositions = cachedPositions.size,
            cachedStates = cachedTrainStates.size,
            lastUpdateTime = lastUpdateTime,
            cacheAgeHours = cacheAge.inWholeHours,
            isValid = isCacheValid(),
            maxPositions = MAX_CACHED_POSITIONS,
            maxStates = MAX_CACHED_STATES
        )
    }
    
    /**
     * Preloads essential data for offline mode
     */
    suspend fun preloadEssentialData(
        positions: List<TrainPosition>,
        states: List<RealTimeTrainState>
    ) {
        logger.info(TAG, "Preloading essential data for offline mode")
        
        cachePositions(positions)
        cacheTrainStates(states)
        
        logger.info(TAG, "Preloaded ${positions.size} positions and ${states.size} states")
    }
    
    /**
     * Exports cache data for backup
     */
    suspend fun exportCacheData(): CacheExport {
        return CacheExport(
            positions = cachedPositions.values.toList(),
            states = cachedTrainStates.values.toList(),
            exportTime = Clock.System.now(),
            lastUpdateTime = lastUpdateTime
        )
    }
    
    /**
     * Imports cache data from backup
     */
    suspend fun importCacheData(export: CacheExport) {
        logger.info(TAG, "Importing cache data from backup")
        
        cachePositions(export.positions)
        cacheTrainStates(export.states)
        
        // Use the more recent update time
        if (export.lastUpdateTime > lastUpdateTime) {
            lastUpdateTime = export.lastUpdateTime
        }
        
        logger.info(TAG, "Imported ${export.positions.size} positions and ${export.states.size} states")
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val cachedPositions: Int,
    val cachedStates: Int,
    val lastUpdateTime: Instant,
    val cacheAgeHours: Long,
    val isValid: Boolean,
    val maxPositions: Int,
    val maxStates: Int
)

/**
 * Cache export data structure
 */
data class CacheExport(
    val positions: List<TrainPosition>,
    val states: List<RealTimeTrainState>,
    val exportTime: Instant,
    val lastUpdateTime: Instant
)