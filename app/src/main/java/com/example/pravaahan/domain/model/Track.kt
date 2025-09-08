package com.example.pravaahan.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a railway track section in the system
 */
@Serializable
data class Track(
    val id: String,
    val sectionId: String,
    val sectionName: String,
    val capacity: Int = 1,
    val isActive: Boolean = true,
    val signalStatus: SignalStatus = SignalStatus.GREEN,
    val occupiedBy: List<String> = emptyList() // Train IDs currently on this track
) {
    /**
     * Checks if track is available for new trains
     */
    fun isAvailable(): Boolean = isActive && occupiedBy.size < capacity
    
    /**
     * Checks if track is at full capacity
     */
    fun isAtCapacity(): Boolean = occupiedBy.size >= capacity
    
    /**
     * Gets remaining capacity
     */
    fun remainingCapacity(): Int = (capacity - occupiedBy.size).coerceAtLeast(0)
}

/**
 * Represents railway signal status
 */
@Serializable
enum class SignalStatus {
    GREEN,    // Clear to proceed
    YELLOW,   // Caution, prepare to stop
    RED,      // Stop
    FLASHING  // Special condition
}