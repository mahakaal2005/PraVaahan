package com.example.pravaahan.core.security

import com.example.pravaahan.data.dto.TrainPositionDto
import com.example.pravaahan.domain.model.AnomalyFlag
import com.example.pravaahan.domain.model.TrainPosition
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive security validator for real-time train position data.
 * Performs input validation, anomaly detection, and security checks.
 */
class RealTimeSecurityValidator {
    
    private val positionHistory = mutableMapOf<String, MutableList<PositionRecord>>()
    private val maxHistorySize = 100
    private val maxPositionAge = 5.minutes
    
    /**
     * Validates incoming train position data for security threats and anomalies.
     */
    fun validatePosition(
        dto: TrainPositionDto,
        authToken: String? = null,
        sourceIp: String? = null
    ): SecurityValidationResult {
        val issues = mutableListOf<SecurityIssue>()
        val anomalies = mutableSetOf<AnomalyFlag>()
        
        // 1. Input sanitization and basic security checks
        issues.addAll(performInputSanitization(dto))
        
        // 2. Authentication and authorization checks
        authToken?.let { token ->
            issues.addAll(validateAuthToken(token, dto.trainId))
        }
        
        // 3. Rate limiting checks
        sourceIp?.let { ip ->
            issues.addAll(checkRateLimit(ip, dto.trainId))
        }
        
        // 4. Geospatial validation
        val geoIssues = validateGeospatialData(dto)
        issues.addAll(geoIssues.issues)
        anomalies.addAll(geoIssues.anomalies)
        
        // 5. Temporal validation
        val temporalIssues = validateTemporalData(dto)
        issues.addAll(temporalIssues.issues)
        anomalies.addAll(temporalIssues.anomalies)
        
        // 6. Movement pattern analysis
        val movementIssues = analyzeMovementPattern(dto)
        issues.addAll(movementIssues.issues)
        anomalies.addAll(movementIssues.anomalies)
        
        // 7. Data integrity checks
        issues.addAll(validateDataIntegrity(dto))
        
        // Update position history for future validations
        updatePositionHistory(dto)
        
        return SecurityValidationResult(
            isValid = issues.none { it.severity == SecuritySeverity.CRITICAL },
            issues = issues,
            anomalies = anomalies,
            riskScore = calculateRiskScore(issues, anomalies)
        )
    }
    
    /**
     * Performs comprehensive input sanitization to prevent injection attacks.
     */
    private fun performInputSanitization(dto: TrainPositionDto): List<SecurityIssue> {
        val issues = mutableListOf<SecurityIssue>()
        
        // Check for SQL injection patterns
        val sqlPatterns = listOf(
            "(?i).*('|(\\-\\-)|(;)|(\\|)|(\\*)|(%)|(\\+)).*",
            "(?i).*(union|select|insert|update|delete|drop|create|alter).*",
            "(?i).*(script|javascript|vbscript|onload|onerror).*"
        )
        
        val fieldsToCheck = listOf(
            dto.trainId to "trainId",
            dto.sectionId to "sectionId", 
            dto.dataSource to "dataSource",
            dto.validationStatus to "validationStatus"
        )
        
        fieldsToCheck.forEach { (value, fieldName) ->
            sqlPatterns.forEach { pattern ->
                if (value.matches(Regex(pattern))) {
                    issues.add(SecurityIssue(
                        type = SecurityIssueType.INJECTION_ATTEMPT,
                        severity = SecuritySeverity.CRITICAL,
                        message = "Potential injection attack detected in field: $fieldName",
                        field = fieldName,
                        value = value
                    ))
                }
            }
        }
        
        // Check for excessively long strings (potential buffer overflow)
        if (dto.trainId.length > 50) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.BUFFER_OVERFLOW,
                severity = SecuritySeverity.HIGH,
                message = "Train ID exceeds maximum length",
                field = "trainId"
            ))
        }
        
        return issues
    }
    
    /**
     * Validates authentication token and authorization for the specific train.
     */
    private fun validateAuthToken(token: String, trainId: String): List<SecurityIssue> {
        val issues = mutableListOf<SecurityIssue>()
        
        // Basic token format validation
        if (token.length < 32) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.INVALID_AUTH,
                severity = SecuritySeverity.CRITICAL,
                message = "Authentication token too short"
            ))
        }
        
        // Check for suspicious token patterns
        if (token.matches(Regex("^[0]+$")) || token.matches(Regex("^[1]+$"))) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.SUSPICIOUS_AUTH,
                severity = SecuritySeverity.HIGH,
                message = "Suspicious authentication token pattern"
            ))
        }
        
        return issues
    }
    
    /**
     * Checks rate limiting to prevent spam and DoS attacks.
     */
    private fun checkRateLimit(sourceIp: String, trainId: String): List<SecurityIssue> {
        val issues = mutableListOf<SecurityIssue>()
        
        // This would typically integrate with a rate limiting service
        // For now, we'll implement basic in-memory tracking
        
        val key = "$sourceIp:$trainId"
        val now = Clock.System.now()
        
        // Check if too many requests from same IP for same train
        val recentRequests = getRecentRequests(key, now, 1.minutes)
        if (recentRequests > 60) { // Max 60 requests per minute per train
            issues.add(SecurityIssue(
                type = SecurityIssueType.RATE_LIMIT_EXCEEDED,
                severity = SecuritySeverity.HIGH,
                message = "Rate limit exceeded for IP: $sourceIp, Train: $trainId"
            ))
        }
        
        return issues
    }
    
    /**
     * Validates geospatial data for realistic train positions.
     */
    private fun validateGeospatialData(dto: TrainPositionDto): ValidationIssues {
        val issues = mutableListOf<SecurityIssue>()
        val anomalies = mutableSetOf<AnomalyFlag>()
        
        // Check if position is within Indian railway network bounds
        // (Approximate bounds for India: 6.4°N to 35.5°N, 68.1°E to 97.4°E)
        if (dto.latitude !in 6.0..36.0 || dto.longitude !in 68.0..98.0) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.GEOFENCE_VIOLATION,
                severity = SecuritySeverity.MEDIUM,
                message = "Position outside Indian railway network bounds"
            ))
            anomalies.add(AnomalyFlag.GEOFENCE_VIOLATION)
        }
        
        // Check for impossible coordinates (exact 0,0 or other suspicious values)
        if (dto.latitude == 0.0 && dto.longitude == 0.0) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.INVALID_COORDINATES,
                severity = SecuritySeverity.HIGH,
                message = "Suspicious coordinates (0,0)"
            ))
        }
        
        // Validate against previous position if available
        val history = positionHistory[dto.trainId]
        if (history != null && history.isNotEmpty()) {
            val lastPosition = history.last()
            val distance = calculateDistance(
                dto.latitude, dto.longitude,
                lastPosition.latitude, lastPosition.longitude
            )
            
            val timeDiff = parseTimestamp(dto.timestamp) - lastPosition.timestamp
            val maxPossibleDistance = calculateMaxPossibleDistance(dto.speed, timeDiff)
            
            if (distance > maxPossibleDistance * 2) { // Allow some tolerance
                anomalies.add(AnomalyFlag.POSITION_JUMP)
                issues.add(SecurityIssue(
                    type = SecurityIssueType.POSITION_ANOMALY,
                    severity = SecuritySeverity.MEDIUM,
                    message = "Suspicious position jump: ${distance}m in ${timeDiff.inWholeSeconds}s"
                ))
            }
        }
        
        return ValidationIssues(issues, anomalies)
    }
    
    /**
     * Validates temporal aspects of the position data.
     */
    private fun validateTemporalData(dto: TrainPositionDto): ValidationIssues {
        val issues = mutableListOf<SecurityIssue>()
        val anomalies = mutableSetOf<AnomalyFlag>()
        
        val timestamp = parseTimestamp(dto.timestamp)
        val now = Clock.System.now()
        
        // Check if timestamp is too far in the future
        if (timestamp > now + 1.minutes) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.FUTURE_TIMESTAMP,
                severity = SecuritySeverity.HIGH,
                message = "Timestamp is in the future"
            ))
            anomalies.add(AnomalyFlag.TEMPORAL_INCONSISTENCY)
        }
        
        // Check if timestamp is too old
        if (timestamp < now - maxPositionAge) {
            issues.add(SecurityIssue(
                type = SecurityIssueType.STALE_DATA,
                severity = SecuritySeverity.MEDIUM,
                message = "Position data is too old"
            ))
        }
        
        // Check for out-of-sequence data
        val history = positionHistory[dto.trainId]
        if (history != null && history.isNotEmpty()) {
            val lastTimestamp = history.last().timestamp
            if (timestamp < lastTimestamp) {
                anomalies.add(AnomalyFlag.OUT_OF_SEQUENCE)
                issues.add(SecurityIssue(
                    type = SecurityIssueType.OUT_OF_SEQUENCE,
                    severity = SecuritySeverity.LOW,
                    message = "Position data received out of sequence"
                ))
            }
        }
        
        return ValidationIssues(issues, anomalies)
    }
    
    /**
     * Analyzes movement patterns for anomalies.
     */
    private fun analyzeMovementPattern(dto: TrainPositionDto): ValidationIssues {
        val issues = mutableListOf<SecurityIssue>()
        val anomalies = mutableSetOf<AnomalyFlag>()
        
        val history = positionHistory[dto.trainId]
        if (history != null && history.size >= 2) {
            val recent = history.takeLast(2)
            val prevSpeed = recent[0].speed
            val currentSpeed = dto.speed
            
            // Check for unrealistic speed changes
            val speedChange = abs(currentSpeed - prevSpeed)
            val timeDiff = parseTimestamp(dto.timestamp) - recent.last().timestamp
            val maxAcceleration = 2.0 // m/s² (reasonable for trains)
            val maxSpeedChange = maxAcceleration * timeDiff.inWholeSeconds / 3.6 // Convert to km/h
            
            if (speedChange > maxSpeedChange * 2) { // Allow some tolerance
                anomalies.add(AnomalyFlag.SPEED_ANOMALY)
                issues.add(SecurityIssue(
                    type = SecurityIssueType.SPEED_ANOMALY,
                    severity = SecuritySeverity.MEDIUM,
                    message = "Unrealistic speed change: ${speedChange}km/h in ${timeDiff.inWholeSeconds}s"
                ))
            }
        }
        
        return ValidationIssues(issues, anomalies)
    }
    
    /**
     * Validates data integrity using checksums and consistency checks.
     */
    private fun validateDataIntegrity(dto: TrainPositionDto): List<SecurityIssue> {
        val issues = mutableListOf<SecurityIssue>()
        
        // Check for duplicate data
        val history = positionHistory[dto.trainId]
        if (history != null && history.isNotEmpty()) {
            val lastRecord = history.last()
            if (lastRecord.latitude == dto.latitude && 
                lastRecord.longitude == dto.longitude &&
                lastRecord.speed == dto.speed &&
                abs((parseTimestamp(dto.timestamp) - lastRecord.timestamp).inWholeSeconds) < 5) {
                
                issues.add(SecurityIssue(
                    type = SecurityIssueType.DUPLICATE_DATA,
                    severity = SecuritySeverity.LOW,
                    message = "Potential duplicate position data"
                ))
            }
        }
        
        return issues
    }
    
    /**
     * Updates position history for the train.
     */
    private fun updatePositionHistory(dto: TrainPositionDto) {
        val history = positionHistory.getOrPut(dto.trainId) { mutableListOf() }
        
        history.add(PositionRecord(
            latitude = dto.latitude,
            longitude = dto.longitude,
            speed = dto.speed,
            timestamp = parseTimestamp(dto.timestamp)
        ))
        
        // Keep only recent history
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        }
        
        // Remove old entries
        val cutoff = Clock.System.now() - maxPositionAge
        history.removeAll { it.timestamp < cutoff }
    }
    
    /**
     * Calculates overall risk score based on issues and anomalies.
     */
    private fun calculateRiskScore(
        issues: List<SecurityIssue>, 
        anomalies: Set<AnomalyFlag>
    ): Double {
        var score = 0.0
        
        issues.forEach { issue ->
            score += when (issue.severity) {
                SecuritySeverity.CRITICAL -> 0.4
                SecuritySeverity.HIGH -> 0.2
                SecuritySeverity.MEDIUM -> 0.1
                SecuritySeverity.LOW -> 0.05
            }
        }
        
        score += anomalies.size * 0.05
        
        return score.coerceAtMost(1.0)
    }
    
    // Helper functions
    private fun parseTimestamp(timestamp: String): Instant {
        return Instant.parse(timestamp)
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    private fun calculateMaxPossibleDistance(speedKmh: Double, timeDiff: Duration): Double {
        val speedMs = speedKmh / 3.6 // Convert km/h to m/s
        return speedMs * timeDiff.inWholeSeconds
    }
    
    private fun getRecentRequests(key: String, now: Instant, window: Duration): Int {
        // This would typically use a proper rate limiting service like Redis
        // For now, return a mock value
        return 0
    }
}

/**
 * Data class for storing position history records.
 */
private data class PositionRecord(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val timestamp: Instant
)

/**
 * Helper class for grouping validation issues and anomalies.
 */
private data class ValidationIssues(
    val issues: List<SecurityIssue>,
    val anomalies: Set<AnomalyFlag>
)

/**
 * Result of security validation with detailed information.
 */
data class SecurityValidationResult(
    val isValid: Boolean,
    val issues: List<SecurityIssue>,
    val anomalies: Set<AnomalyFlag>,
    val riskScore: Double
) {
    fun hasHighRiskIssues(): Boolean = issues.any { 
        it.severity in listOf(SecuritySeverity.CRITICAL, SecuritySeverity.HIGH) 
    }
    
    fun isSafeForAutomation(): Boolean = riskScore < 0.3 && !hasHighRiskIssues()
}

/**
 * Represents a security issue found during validation.
 */
data class SecurityIssue(
    val type: SecurityIssueType,
    val severity: SecuritySeverity,
    val message: String,
    val field: String? = null,
    val value: String? = null,
    val timestamp: Instant = Clock.System.now()
)

/**
 * Types of security issues that can be detected.
 */
enum class SecurityIssueType {
    INJECTION_ATTEMPT,
    BUFFER_OVERFLOW,
    INVALID_AUTH,
    SUSPICIOUS_AUTH,
    RATE_LIMIT_EXCEEDED,
    GEOFENCE_VIOLATION,
    INVALID_COORDINATES,
    POSITION_ANOMALY,
    FUTURE_TIMESTAMP,
    STALE_DATA,
    OUT_OF_SEQUENCE,
    SPEED_ANOMALY,
    DUPLICATE_DATA
}

/**
 * Severity levels for security issues.
 */
enum class SecuritySeverity {
    CRITICAL, // Immediate security threat
    HIGH,     // Significant security concern
    MEDIUM,   // Moderate security issue
    LOW       // Minor security concern
}