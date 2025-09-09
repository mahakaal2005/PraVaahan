package com.example.pravaahan.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for TrainPosition with Supabase serialization support.
 * Includes comprehensive validation and maps to database schema with snake_case naming.
 */
@Serializable
data class TrainPositionDto(
    @SerialName("train_id")
    val trainId: String,
    
    @SerialName("latitude")
    val latitude: Double,
    
    @SerialName("longitude")
    val longitude: Double,
    
    @SerialName("speed")
    val speed: Double,
    
    @SerialName("heading")
    val heading: Double,
    
    @SerialName("timestamp")
    val timestamp: String, // ISO 8601 format for Supabase compatibility
    
    @SerialName("section_id")
    val sectionId: String,
    
    @SerialName("accuracy")
    val accuracy: Double? = null,
    
    @SerialName("signal_strength")
    val signalStrength: Double? = null,
    
    @SerialName("data_source")
    val dataSource: String = "GPS",
    
    @SerialName("validation_status")
    val validationStatus: String = "PENDING",
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    init {
        // Validate train ID format and content
        require(trainId.isNotBlank()) { "Train ID cannot be blank" }
        require(trainId.length <= 50) { "Train ID too long: ${trainId.length} characters" }
        require(trainId.matches(Regex("^[A-Za-z0-9_-]+$"))) { 
            "Train ID contains invalid characters: $trainId" 
        }
        
        // Validate geographic coordinates with high precision
        require(latitude in -90.0..90.0) { 
            "Invalid latitude: $latitude (must be between -90 and 90)" 
        }
        require(longitude in -180.0..180.0) { 
            "Invalid longitude: $longitude (must be between -180 and 180)" 
        }
        
        // Validate speed constraints for railway operations
        require(speed >= 0.0) { "Speed cannot be negative: $speed" }
        require(speed <= 350.0) { 
            "Speed exceeds maximum railway speed limit (350 km/h): $speed" 
        }
        
        // Validate heading (compass bearing)
        require(heading in 0.0..360.0) { 
            "Invalid heading: $heading (must be between 0 and 360 degrees)" 
        }
        
        // Validate section ID
        require(sectionId.isNotBlank()) { "Section ID cannot be blank" }
        require(sectionId.length <= 20) { "Section ID too long: ${sectionId.length} characters" }
        require(sectionId.matches(Regex("^[A-Za-z0-9_-]+$"))) { 
            "Section ID contains invalid characters: $sectionId" 
        }
        
        // Validate accuracy if provided (GPS accuracy in meters)
        accuracy?.let { acc ->
            require(acc > 0.0) { "Accuracy must be positive: $acc" }
            require(acc <= 10000.0) { 
                "Accuracy value unrealistic (>10km): $acc meters" 
            }
        }
        
        // Validate signal strength if provided (0-100 scale)
        signalStrength?.let { strength ->
            require(strength in 0.0..100.0) { 
                "Signal strength must be between 0 and 100: $strength" 
            }
        }
        
        // Validate data source
        require(dataSource.isNotBlank()) { "Data source cannot be blank" }
        require(dataSource in listOf("GPS", "RFID", "MANUAL", "SENSOR", "ESTIMATED")) { 
            "Invalid data source: $dataSource" 
        }
        
        // Validate validation status
        require(validationStatus in listOf("PENDING", "VALID", "INVALID", "SUSPICIOUS")) { 
            "Invalid validation status: $validationStatus" 
        }
        
        // Validate timestamp format (basic ISO 8601 check)
        require(timestamp.matches(Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"))) { 
            "Invalid timestamp format: $timestamp (expected ISO 8601)" 
        }
    }
    
    /**
     * Validates that this position data represents a realistic train movement.
     * Performs additional business logic validation beyond basic field validation.
     */
    fun validateRealisticMovement(): ValidationResult {
        val issues = mutableListOf<String>()
        
        // Check for realistic speed-accuracy correlation
        accuracy?.let { acc ->
            if (speed > 100.0 && acc > 50.0) {
                issues.add("High speed with low accuracy may indicate GPS issues")
            }
        }
        
        // Check signal strength correlation with accuracy
        signalStrength?.let { strength ->
            accuracy?.let { acc ->
                if (strength < 30.0 && acc < 10.0) {
                    issues.add("Low signal strength but high accuracy is suspicious")
                }
            }
        }
        
        // Validate data source consistency
        if (dataSource == "GPS" && signalStrength == null) {
            issues.add("GPS data source should include signal strength")
        }
        
        return if (issues.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Warning(issues)
        }
    }
}

/**
 * Result of position data validation with different severity levels.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Warning(val issues: List<String>) : ValidationResult()
    data class Error(val issues: List<String>) : ValidationResult()
}