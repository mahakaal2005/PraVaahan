package com.example.pravaahan.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Train entity in Supabase database
 * Maps to the 'trains' table with snake_case column names
 */
@Serializable
data class TrainDto(
    val id: String,
    val name: String,
    @SerialName("train_number")
    val trainNumber: String,
    @SerialName("current_latitude")
    val currentLatitude: Double?,
    @SerialName("current_longitude")
    val currentLongitude: Double?,
    @SerialName("destination_latitude")
    val destinationLatitude: Double?,
    @SerialName("destination_longitude")
    val destinationLongitude: Double?,
    @SerialName("section_id")
    val sectionId: String?,
    val status: String,
    val priority: String,
    val speed: Double?,
    @SerialName("estimated_arrival")
    val estimatedArrival: String?, // ISO 8601 timestamp string
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)