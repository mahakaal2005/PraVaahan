package com.example.pravaahan.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a geographical location with railway section information
 */
@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val sectionId: String = "",
    val name: String = "Unknown Location"
) {
    /**
     * Calculates distance to another location in kilometers
     */
    fun distanceTo(other: Location): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}