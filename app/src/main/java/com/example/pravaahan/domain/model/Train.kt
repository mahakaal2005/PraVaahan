package com.example.pravaahan.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Core domain model representing a train in the PraVaahan system
 */
@Serializable
data class Train(
    val id: String,
    val name: String,
    val trainNumber: String,
    val currentLocation: Location,
    val destination: Location,
    val status: TrainStatus,
    val priority: TrainPriority,
    val speed: Double, // km/h
    val estimatedArrival: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /**
     * Calculates remaining distance to destination
     */
    fun remainingDistance(): Double = currentLocation.distanceTo(destination)
    
    /**
     * Checks if train is currently moving
     */
    fun isMoving(): Boolean = speed > 0.0 && status == TrainStatus.ON_TIME
    
    /**
     * Checks if train requires immediate attention
     */
    fun requiresAttention(): Boolean = status in listOf(
        TrainStatus.EMERGENCY,
        TrainStatus.STOPPED,
        TrainStatus.MAINTENANCE
    )
}