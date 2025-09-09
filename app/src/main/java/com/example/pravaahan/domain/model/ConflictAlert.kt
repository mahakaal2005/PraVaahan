package com.example.pravaahan.domain.model

import kotlinx.datetime.Instant

/**
 * Represents a conflict alert in the railway system
 */
data class ConflictAlert(
    val id: String,
    val type: ConflictType,
    val severity: ConflictSeverity,
    val involvedTrains: List<String>,
    val description: String,
    val timestamp: Instant,
    val resolved: Boolean = false,
    // Backward compatibility properties
    val trainsInvolved: List<String> = involvedTrains,
    val conflictType: ConflictType = type,
    val detectedAt: Instant = timestamp,
    val estimatedImpactTime: Instant? = null,
    val recommendation: String? = null,
    val isResolved: Boolean = resolved,
    val resolvedAt: Instant? = null,
    val controllerAction: String? = null
)

