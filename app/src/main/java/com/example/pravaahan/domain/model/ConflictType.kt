package com.example.pravaahan.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents different types of conflicts that can occur between trains
 */
@Serializable
enum class ConflictType {
    /**
     * Potential collision between trains on the same track
     */
    POTENTIAL_COLLISION,
    
    /**
     * Track congestion with multiple trains in same section
     */
    TRACK_CONGESTION,
    
    /**
     * Signal failure affecting train operations
     */
    SIGNAL_FAILURE,
    
    /**
     * Schedule conflicts between trains
     */
    SCHEDULE_CONFLICT,
    
    /**
     * Maintenance window conflicts
     */
    MAINTENANCE_WINDOW
}