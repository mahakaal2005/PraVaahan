package com.example.pravaahan.core.monitoring

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.logging.logSecurityEvent
import com.example.pravaahan.domain.model.TrainPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Security event monitoring and anomaly detection for railway systems.
 * Monitors for suspicious patterns, data anomalies, and potential security threats.
 */
@Singleton
class SecurityEventMonitor @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "SecurityEventMonitor"
        private const val MAX_SPEED_THRESHOLD = 300.0 // km/h - Maximum realistic train speed
        private const val POSITION_JUMP_THRESHOLD = 50.0 // km - Maximum realistic position jump
        private const val RAPID_UPDATE_THRESHOLD = 100 // Updates per minute threshold
        private const val ANOMALY_HISTORY_SIZE = 1000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _securityEvents = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val securityEvents: StateFlow<List<SecurityEvent>> = _securityEvents.asStateFlow()
    
    private val _anomalyCount = MutableStateFlow(0)
    val anomalyCount: StateFlow<Int> = _anomalyCount.asStateFlow()
    
    private val _threatLevel = MutableStateFlow(ThreatLevel.LOW)
    val threatLevel: StateFlow<ThreatLevel> = _threatLevel.asStateFlow()
    
    // Track position history for anomaly detection
    private val positionHistory = mutableMapOf<String, MutableList<TrainPosition>>()
    private val updateFrequency = mutableMapOf<String, MutableList<Instant>>()
    
    init {
        logger.info(TAG, "SecurityEventMonitor initialized with comprehensive threat detection")
    }

    /**
     * Analyze train position for security anomalies and threats
     */
    fun analyzePosition(position: TrainPosition) {
        scope.launch {
            try {
                logger.debug(TAG, "Analyzing position for train ${position.trainId}")
                
                // Check for speed anomalies
                checkSpeedAnomaly(position)
                
                // Check for position jump anomalies
                checkPositionJumpAnomaly(position)
                
                // Check for rapid update frequency
                checkUpdateFrequencyAnomaly(position)
                
                // Check for coordinate validity
                checkCoordinateValidity(position)
                
                // Update position history
                updatePositionHistory(position)
                
                // Update threat level based on recent events
                updateThreatLevel()
                
            } catch (e: Exception) {
                logger.error(TAG, "Error analyzing position for train ${position.trainId}", e)
            }
        }
    }
    
    /**
     * Check for unrealistic speed values that might indicate data manipulation
     */
    private fun checkSpeedAnomaly(position: TrainPosition) {
        if (position.speed > MAX_SPEED_THRESHOLD) {
            val event = SecurityEvent(
                id = generateEventId(),
                type = SecurityEventType.SPEED_ANOMALY,
                trainId = position.trainId,
                description = "Unrealistic speed detected: ${position.speed} km/h (max: $MAX_SPEED_THRESHOLD km/h)",
                severity = SecuritySeverity.HIGH,
                timestamp = Clock.System.now(),
                metadata = mapOf(
                    "speed" to position.speed.toString(),
                    "threshold" to MAX_SPEED_THRESHOLD.toString(),
                    "latitude" to position.latitude.toString(),
                    "longitude" to position.longitude.toString()
                )
            )
            
            reportSecurityEvent(event)
            logger.logSecurityEvent(TAG, "speed_anomaly", "Train: ${position.trainId}, Detected unrealistic speed: ${position.speed} km/h")
        }
    }
    
    /**
     * Check for impossible position jumps that might indicate GPS spoofing
     */
    private fun checkPositionJumpAnomaly(position: TrainPosition) {
        val history = positionHistory[position.trainId]
        if (history != null && history.isNotEmpty()) {
            val lastPosition = history.last()
            val distance = calculateDistance(lastPosition, position)
            val timeDiff = (position.timestamp - lastPosition.timestamp).inWholeSeconds
            
            // Calculate maximum possible distance based on time and max speed
            val maxPossibleDistance = (MAX_SPEED_THRESHOLD / 3.6) * timeDiff / 1000.0 // Convert to km
            
            if (distance > maxPossibleDistance && distance > POSITION_JUMP_THRESHOLD) {
                val event = SecurityEvent(
                    id = generateEventId(),
                    type = SecurityEventType.POSITION_JUMP,
                    trainId = position.trainId,
                    description = "Impossible position jump detected: ${distance.toInt()} km in ${timeDiff}s",
                    severity = SecuritySeverity.CRITICAL,
                    timestamp = Clock.System.now(),
                    metadata = mapOf(
                        "distance_km" to distance.toString(),
                        "time_seconds" to timeDiff.toString(),
                        "max_possible_km" to maxPossibleDistance.toString(),
                        "from_lat" to lastPosition.latitude.toString(),
                        "from_lng" to lastPosition.longitude.toString(),
                        "to_lat" to position.latitude.toString(),
                        "to_lng" to position.longitude.toString()
                    )
                )
                
                reportSecurityEvent(event)
                logger.logSecurityEvent(TAG, "position_jump", "Train: ${position.trainId}, Impossible position jump: ${distance.toInt()} km in ${timeDiff}s")
            }
        }
    }
    
    /**
     * Check for suspiciously rapid update frequency that might indicate automated attacks
     */
    private fun checkUpdateFrequencyAnomaly(position: TrainPosition) {
        val updates = updateFrequency.getOrPut(position.trainId) { mutableListOf() }
        updates.add(position.timestamp)
        
        // Keep only updates from last minute
        val oneMinuteAgo = Clock.System.now().minus(1.minutes)
        updates.removeAll { it < oneMinuteAgo }
        
        if (updates.size > RAPID_UPDATE_THRESHOLD) {
            val event = SecurityEvent(
                id = generateEventId(),
                type = SecurityEventType.RAPID_UPDATES,
                trainId = position.trainId,
                description = "Suspicious update frequency: ${updates.size} updates/minute (threshold: $RAPID_UPDATE_THRESHOLD)",
                severity = SecuritySeverity.MEDIUM,
                timestamp = Clock.System.now(),
                metadata = mapOf(
                    "updates_per_minute" to updates.size.toString(),
                    "threshold" to RAPID_UPDATE_THRESHOLD.toString()
                )
            )
            
            reportSecurityEvent(event)
            logger.logSecurityEvent(TAG, "rapid_updates", "Train: ${position.trainId}, Suspicious update frequency: ${updates.size} updates/minute")
        }
    }
    
    /**
     * Check for invalid coordinate values that might indicate data corruption
     */
    private fun checkCoordinateValidity(position: TrainPosition) {
        val isValidLat = position.latitude in -90.0..90.0
        val isValidLng = position.longitude in -180.0..180.0
        
        if (!isValidLat || !isValidLng) {
            val event = SecurityEvent(
                id = generateEventId(),
                type = SecurityEventType.INVALID_COORDINATES,
                trainId = position.trainId,
                description = "Invalid coordinates detected: lat=${position.latitude}, lng=${position.longitude}",
                severity = SecuritySeverity.HIGH,
                timestamp = Clock.System.now(),
                metadata = mapOf(
                    "latitude" to position.latitude.toString(),
                    "longitude" to position.longitude.toString(),
                    "valid_lat" to isValidLat.toString(),
                    "valid_lng" to isValidLng.toString()
                )
            )
            
            reportSecurityEvent(event)
            logger.logSecurityEvent(TAG, "invalid_coordinates", "Train: ${position.trainId}, Invalid coordinates: lat=${position.latitude}, lng=${position.longitude}")
        }
    }
    
    /**
     * Update position history for anomaly detection
     */
    private fun updatePositionHistory(position: TrainPosition) {
        val history = positionHistory.getOrPut(position.trainId) { mutableListOf() }
        history.add(position)
        
        // Keep only recent positions (last 10 positions)
        if (history.size > 10) {
            history.removeAt(0)
        }
    }
    
    /**
     * Update overall threat level based on recent security events
     */
    private fun updateThreatLevel() {
        val recentEvents = _securityEvents.value.filter { 
            it.timestamp > Clock.System.now().minus(5.minutes) 
        }
        
        val newThreatLevel = when {
            recentEvents.any { it.severity == SecuritySeverity.CRITICAL } -> ThreatLevel.CRITICAL
            recentEvents.count { it.severity == SecuritySeverity.HIGH } >= 3 -> ThreatLevel.HIGH
            recentEvents.count { it.severity == SecuritySeverity.MEDIUM } >= 5 -> ThreatLevel.MEDIUM
            recentEvents.isNotEmpty() -> ThreatLevel.LOW
            else -> ThreatLevel.NONE
        }
        
        if (newThreatLevel != _threatLevel.value) {
            _threatLevel.value = newThreatLevel
            logger.logSecurityEvent(TAG, "threat_level_change", "Threat level changed to: $newThreatLevel")
        }
    }
    
    /**
     * Report a security event and update monitoring state
     */
    private fun reportSecurityEvent(event: SecurityEvent) {
        val currentEvents = _securityEvents.value.toMutableList()
        currentEvents.add(event)
        
        // Keep only recent events
        if (currentEvents.size > ANOMALY_HISTORY_SIZE) {
            currentEvents.removeAt(0)
        }
        
        _securityEvents.value = currentEvents
        _anomalyCount.value = currentEvents.size
        
        logger.logSecurityEvent(TAG, event.type.name.lowercase(), "Train: ${event.trainId}, ${event.description}")
        
        // Log critical events with more detail
        if (event.severity == SecuritySeverity.CRITICAL) {
            logger.error(TAG, "CRITICAL SECURITY EVENT: ${event.description} for train ${event.trainId}")
        }
    }
    
    /**
     * Calculate distance between two positions using Haversine formula
     */
    private fun calculateDistance(pos1: TrainPosition, pos2: TrainPosition): Double {
        val R = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(pos2.latitude - pos1.latitude)
        val dLon = Math.toRadians(pos2.longitude - pos1.longitude)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(pos1.latitude)) * kotlin.math.cos(Math.toRadians(pos2.latitude)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }
    
    /**
     * Generate unique event ID
     */
    private fun generateEventId(): String {
        return "SEC_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
    }
    
    /**
     * Get security statistics for monitoring dashboard
     */
    fun getSecurityStatistics(): SecurityStatistics {
        val events = _securityEvents.value
        val recentEvents = events.filter { 
            it.timestamp > Clock.System.now().minus(1.minutes) 
        }
        
        return SecurityStatistics(
            totalEvents = events.size,
            recentEvents = recentEvents.size,
            criticalEvents = events.count { it.severity == SecuritySeverity.CRITICAL },
            highSeverityEvents = events.count { it.severity == SecuritySeverity.HIGH },
            currentThreatLevel = _threatLevel.value,
            monitoredTrains = positionHistory.keys.size
        )
    }
    
    /**
     * Clear old security events (for maintenance)
     */
    fun clearOldEvents(olderThan: Instant) {
        val filteredEvents = _securityEvents.value.filter { it.timestamp > olderThan }
        _securityEvents.value = filteredEvents
        _anomalyCount.value = filteredEvents.size
        
        logger.info(TAG, "Cleared old security events, remaining: ${filteredEvents.size}")
    }
}

/**
 * Security event data class
 */
data class SecurityEvent(
    val id: String,
    val type: SecurityEventType,
    val trainId: String,
    val description: String,
    val severity: SecuritySeverity,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Types of security events
 */
enum class SecurityEventType {
    SPEED_ANOMALY,
    POSITION_JUMP,
    RAPID_UPDATES,
    INVALID_COORDINATES,
    DATA_CORRUPTION,
    UNAUTHORIZED_ACCESS
}

/**
 * Security event severity levels
 */
enum class SecuritySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Overall threat level
 */
enum class ThreatLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Security statistics for dashboard
 */
data class SecurityStatistics(
    val totalEvents: Int,
    val recentEvents: Int,
    val criticalEvents: Int,
    val highSeverityEvents: Int,
    val currentThreatLevel: ThreatLevel,
    val monitoredTrains: Int
)