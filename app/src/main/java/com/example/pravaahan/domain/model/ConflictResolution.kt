package com.example.pravaahan.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a resolution action for a conflict alert
 */
@Serializable
sealed class ConflictResolution {
    /**
     * Accept the AI recommendation without modification
     */
    @Serializable
    object AcceptRecommendation : ConflictResolution()
    
    /**
     * Override the AI recommendation with manual action
     */
    @Serializable
    data class ManualOverride(
        val reason: String,
        val action: String
    ) : ConflictResolution()
}

/**
 * Represents an action taken by a railway controller
 */
@Serializable
data class ControllerAction(
    val conflictId: String,
    val action: String,
    val timestamp: Instant,
    val controllerId: String? = null,
    val notes: String? = null
)