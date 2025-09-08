package com.example.pravaahan.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the severity level of a conflict alert
 */
@Serializable
enum class ConflictSeverity {
    /**
     * Critical conflicts requiring immediate emergency action
     * Examples: Imminent collision, signal failure in active section
     */
    CRITICAL,
    
    /**
     * High priority conflicts requiring urgent attention
     * Examples: Potential collision within 5 minutes, track congestion
     */
    HIGH,
    
    /**
     * Medium priority conflicts requiring attention
     * Examples: Schedule conflicts, minor delays
     */
    MEDIUM,
    
    /**
     * Low priority conflicts for monitoring
     * Examples: Minor schedule adjustments, informational alerts
     */
    LOW
}