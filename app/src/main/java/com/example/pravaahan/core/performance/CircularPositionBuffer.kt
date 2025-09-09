package com.example.pravaahan.core.performance

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * Memory-efficient circular buffer for storing train position history
 * with automatic cleanup and size management.
 */
@Singleton
class CircularPositionBuffer @Inject constructor(
    private val logger: Logger
) {
    
    companion object {
        private const val TAG = "CircularPositionBuffer"
        private const val DEFAULT_BUFFER_SIZE = 100
        private const val MAX_TRAINS = 500
        private const val CLEANUP_INTERVAL_MINUTES = 10L
    }
    
    private val trainBuffers = mutableMapOf<String, CircularBuffer<TrainPosition>>()
    private var lastCleanupTime = Clock.System.now()
    
    /**
     * Adds a position to the buffer for a specific train
     */
    fun addPosition(trainId: String, position: TrainPosition) {
        val buffer = trainBuffers.getOrPut(trainId) {
            CircularBuffer(DEFAULT_BUFFER_SIZE)
        }
        
        buffer.add(position)
        
        // Periodic cleanup to prevent memory leaks
        if (shouldPerformCleanup()) {
            performCleanup()
        }
        
        logger.debug(TAG, "Added position for train $trainId (buffer size: ${buffer.size()})")
    }
    
    /**
     * Gets the latest position for a train
     */
    fun getLatestPosition(trainId: String): TrainPosition? {
        return trainBuffers[trainId]?.latest()
    }
    
    /**
     * Gets position history for a train
     */
    fun getPositionHistory(trainId: String, maxCount: Int = DEFAULT_BUFFER_SIZE): List<TrainPosition> {
        val buffer = trainBuffers[trainId] ?: return emptyList()
        return buffer.getRecent(maxCount)
    }
    
    /**
     * Gets all positions for a train within a time range
     */
    fun getPositionsInTimeRange(
        trainId: String,
        startTime: Instant,
        endTime: Instant
    ): List<TrainPosition> {
        val buffer = trainBuffers[trainId] ?: return emptyList()
        return buffer.getAll().filter { position ->
            position.timestamp >= startTime && position.timestamp <= endTime
        }
    }
    
    /**
     * Clears the buffer for a specific train
     */
    fun clearTrain(trainId: String) {
        trainBuffers.remove(trainId)
        logger.debug(TAG, "Cleared buffer for train $trainId")
    }
    
    /**
     * Clears all buffers
     */
    fun clearAll() {
        val trainCount = trainBuffers.size
        trainBuffers.clear()
        logger.info(TAG, "Cleared all buffers ($trainCount trains)")
    }
    
    /**
     * Gets memory usage statistics
     */
    fun getMemoryStats(): MemoryStats {
        val bufferCount = trainBuffers.size
        val totalPositions = trainBuffers.values.sumOf { it.size() }
        val estimatedMemoryMB = calculateEstimatedMemoryUsage()
        
        return MemoryStats(
            bufferCount = bufferCount,
            totalPositions = totalPositions,
            estimatedMemoryMB = estimatedMemoryMB,
            maxTrains = MAX_TRAINS,
            bufferSizePerTrain = DEFAULT_BUFFER_SIZE
        )
    }
    
    /**
     * Checks if cleanup should be performed
     */
    private fun shouldPerformCleanup(): Boolean {
        val now = Clock.System.now()
        return (now - lastCleanupTime).inWholeMinutes >= CLEANUP_INTERVAL_MINUTES ||
               trainBuffers.size > MAX_TRAINS
    }
    
    /**
     * Performs cleanup of old and unused buffers
     */
    private fun performCleanup() {
        val now = Clock.System.now()
        val cutoffTime = now.minus(30.minutes) // Remove trains not updated in 30 minutes
        val iterator = trainBuffers.iterator()
        var removedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val latestPosition = entry.value.latest()
            
            if (latestPosition == null || latestPosition.timestamp < cutoffTime) {
                iterator.remove()
                removedCount++
            }
        }
        
        lastCleanupTime = now
        
        if (removedCount > 0) {
            logger.info(TAG, "Cleanup completed: removed $removedCount inactive train buffers")
        }
        
        // Force cleanup if still over limit
        if (trainBuffers.size > MAX_TRAINS) {
            forceCleanupOldestTrains()
        }
    }
    
    /**
     * Forces cleanup of oldest trains when over capacity
     */
    private fun forceCleanupOldestTrains() {
        val sortedTrains = trainBuffers.entries.sortedBy { entry ->
            entry.value.latest()?.timestamp ?: Instant.DISTANT_PAST
        }
        
        val toRemove = sortedTrains.take(trainBuffers.size - MAX_TRAINS + 50) // Remove extra for buffer
        toRemove.forEach { entry ->
            trainBuffers.remove(entry.key)
        }
        
        logger.warn(TAG, "Force cleanup: removed ${toRemove.size} oldest train buffers")
    }
    
    /**
     * Calculates estimated memory usage in MB
     */
    private fun calculateEstimatedMemoryUsage(): Float {
        val totalPositions = trainBuffers.values.sumOf { it.size() }
        // Rough estimate: ~200 bytes per TrainPosition object
        val estimatedBytes = totalPositions * 200L
        return estimatedBytes / (1024f * 1024f)
    }
}

/**
 * Generic circular buffer implementation
 */
private class CircularBuffer<T>(private val capacity: Int) {
    private val buffer = arrayOfNulls<Any?>(capacity)
    private var head = 0
    private var tail = 0
    private var size = 0
    
    fun add(item: T) {
        buffer[tail] = item
        tail = (tail + 1) % capacity
        
        if (size < capacity) {
            size++
        } else {
            head = (head + 1) % capacity
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun latest(): T? {
        if (size == 0) return null
        val latestIndex = if (tail == 0) capacity - 1 else tail - 1
        return buffer[latestIndex] as T?
    }
    
    @Suppress("UNCHECKED_CAST")
    fun getRecent(maxCount: Int): List<T> {
        if (size == 0) return emptyList()
        
        val count = minOf(maxCount, size)
        val result = mutableListOf<T>()
        
        for (i in 0 until count) {
            val index = (tail - 1 - i + capacity) % capacity
            buffer[index]?.let { result.add(it as T) }
        }
        
        return result
    }
    
    @Suppress("UNCHECKED_CAST")
    fun getAll(): List<T> {
        if (size == 0) return emptyList()
        
        val result = mutableListOf<T>()
        for (i in 0 until size) {
            val index = (head + i) % capacity
            buffer[index]?.let { result.add(it as T) }
        }
        
        return result
    }
    
    fun size(): Int = size
    
    fun isEmpty(): Boolean = size == 0
    
    fun isFull(): Boolean = size == capacity
}

/**
 * Memory usage statistics
 */
data class MemoryStats(
    val bufferCount: Int,
    val totalPositions: Int,
    val estimatedMemoryMB: Float,
    val maxTrains: Int,
    val bufferSizePerTrain: Int
)