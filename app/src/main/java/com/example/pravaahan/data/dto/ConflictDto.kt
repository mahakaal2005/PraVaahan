package com.example.pravaahan.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for ConflictAlert entity in Supabase database
 * Maps to the 'conflicts' table with snake_case column names
 */
@Serializable
data class ConflictDto(
    val id: String,
    @SerialName("trains_involved")
    val trainsInvolved: List<String>, // JSON array of train IDs
    @SerialName("conflict_type")
    val conflictType: String,
    val severity: String,
    @SerialName("detected_at")
    val detectedAt: String, // ISO 8601 timestamp string
    @SerialName("estimated_impact_time")
    val estimatedImpactTime: String?, // ISO 8601 timestamp string
    @SerialName("ai_recommendation")
    val aiRecommendation: String,
    @SerialName("is_resolved")
    val isResolved: Boolean = false,
    @SerialName("resolved_at")
    val resolvedAt: String?, // ISO 8601 timestamp string
    @SerialName("controller_action")
    val controllerAction: String?,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
)