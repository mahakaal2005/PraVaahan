package com.example.pravaahan.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a conflict alert between trains detected by the AI system
 */
@Serializable
data class ConflictAlert(
    val id: String,
    val trainsInvolved: List<String>, // Train IDs
    val conflictType: ConflictType,
    val severity: ConflictSeverity,
    val detectedAt: Instant,
    val estimatedImpactTime: Instant?,
    val recommendation: String,
    val isResolved: Boolean = false,
    val resolvedAt: Instant? = null,
    val controllerAction: String? = null
) {
    /**
     * Checks if this is a critical conflict requiring immediate action
     */
    fun isCritical(): Boolean = severity == ConflictSeverity.CRITICAL
    
    /**
     * Calculates time remaining until estimated impact
     */
    fun timeToImpact(currentTime: Instant): Long? {
        return estimatedImpactTime?.let { impact ->
            (impact.epochSeconds - currentTime.epochSeconds).coerceAtLeast(0)
        }
    }
    
    /**
     * Gets the number of trains involved in this conflict
     */
    fun trainCount(): Int = trainsInvolved.size
}