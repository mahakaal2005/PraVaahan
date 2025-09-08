package com.example.pravaahan.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model representing a real-time train position with location, speed, timestamp, and data quality metrics.
 * This model follows railway industry standards for position tracking and includes comprehensive validation.
 */
data class TrainPosition(
    val trainId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val heading: Double,
    val timestamp: Instant,
    val sectionId: String,
    val accuracy: Double? = null
) {
    init {
        // Validate train ID
        require(trainId.isNotBlank()) { "Train ID cannot be blank" }
        
        // Validate geographic coordinates
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90 degrees, got: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180 degrees, got: $longitude" }
        
        // Validate speed (non-negative, realistic maximum for trains)
        require(speed >= 0.0) { "Speed cannot be negative, got: $speed" }
        require(speed <= 300.0) { "Speed exceeds maximum realistic train speed (300 km/h), got: $speed" }
        
        // Validate heading (0-360 degrees)
        require(heading in 0.0..360.0) { "Heading must be between 0 and 360 degrees, got: $heading" }
        
        // Validate section ID
        require(sectionId.isNotBlank()) { "Section ID cannot be blank" }
        
        // Validate accuracy if provided (must be positive)
        accuracy?.let { acc ->
            require(acc > 0.0) { "Accuracy must be positive, got: $acc" }
            require(acc <= 1000.0) { "Accuracy value seems unrealistic (>1000m), got: $acc" }
        }
    }
    
    /**
     * Calculates the distance between this position and another position in meters.
     * Uses the Haversine formula for great-circle distance calculation.
     */
    fun distanceTo(other: TrainPosition): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters
        
        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLatRad = Math.toRadians(other.latitude - latitude)
        val deltaLonRad = Math.toRadians(other.longitude - longitude)
        
        val a = kotlin.math.sin(deltaLatRad / 2) * kotlin.math.sin(deltaLatRad / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLonRad / 2) * kotlin.math.sin(deltaLonRad / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Checks if this position is moving based on speed threshold.
     */
    fun isMoving(speedThreshold: Double = 0.1): Boolean = speed > speedThreshold
    
    /**
     * Checks if this position data is fresh based on the given time threshold.
     */
    fun isFresh(now: Instant, maxAge: kotlin.time.Duration): Boolean {
        return (now - timestamp) <= maxAge
    }
}