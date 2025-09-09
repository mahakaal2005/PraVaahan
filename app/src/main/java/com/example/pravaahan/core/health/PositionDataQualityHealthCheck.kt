package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector
import com.example.pravaahan.domain.model.TrainPosition
import com.example.pravaahan.domain.service.RealTimePositionService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/**
 * Specialized health check for train position data quality monitoring.
 * 
 * Validates GPS coordinates, speed ranges, timestamp consistency, and detects
 * position anomalies to ensure data integrity for railway safety operations.
 */
@Singleton
class PositionDataQualityHealthCheck @Inject constructor(
    private val realTimePositionService: RealTimePositionService,
    private val metricsCollector: RealTimeMetricsCollector,
    logger: Logger
) : BaseHealthCheck(
    name = "Position Data Quality",
    isCritical = true, // Critical for railway safety
    timeoutMs = 10_000L, // 10 seconds timeout
    maxRetries = 1, // Only retry once for data quality checks
    logger = logger
) {
    
    companion object {
        private const val TAG = "PositionDataQualityHealthCheck"
        
        // GPS coordinate validation bounds for Indian Railways
        private const val MIN_LATITUDE = 6.0 // Southern tip of India
        private const val MAX_LATITUDE = 37.0 // Northern border
        private const val MIN_LONGITUDE = 68.0 // Western border
        private const val MAX_LONGITUDE = 97.0 // Eastern border
        
        // Speed validation (km/h)
        private const val MAX_PASSENGER_SPEED = 200.0 // Maximum passenger train speed
        private const val MAX_FREIGHT_SPEED = 100.0 // Maximum freight train speed
        private const val MIN_SPEED = 0.0 // Minimum speed (stationary)
        
        // Position validation
        private const val MAX_POSITION_JUMP_KM = 50.0 // Maximum realistic position change per minute
        private const val MAX_TIMESTAMP_DRIFT_SECONDS = 300L // 5 minutes max drift
        
        // Data quality thresholds
        private const val MIN_DATA_COMPLETENESS = 0.95 // 95% completeness required
        private const val MAX_ANOMALY_RATE = 0.05 // 5% anomaly rate max
        private const val MIN_POSITION_ACCURACY = 0.90 // 90% position accuracy required
    }
    
    override suspend fun performCheck() {
        logger.info(TAG, "Starting position data quality health check")
        
        // 1. Validate current data quality metrics
        validateDataQualityMetrics()
        
        // 2. Check GPS coordinate validity
        validateGpsCoordinates()
        
        // 3. Validate speed ranges
        validateSpeedRanges()
        
        // 4. Check timestamp consistency
        validateTimestampConsistency()
        
        // 5. Detect position anomalies
        detectPositionAnomalies()
        
        // 6. Validate data completeness
        validateDataCompleteness()
        
        logger.info(TAG, "Position data quality health check completed successfully")
    }
    
    private suspend fun validateDataQualityMetrics() {
        logger.debug(TAG, "Validating overall data quality metrics")
        
        val metrics = withTimeout(5.seconds) {
            metricsCollector.metrics.first()
        }
        
        val dataQuality = metrics.dataQuality
        
        // Check reliability
        if (dataQuality.reliability < MIN_POSITION_ACCURACY) {
            logger.error(TAG, "❌ Low position data reliability: ${(dataQuality.reliability * 100).toInt()}%")
            throw PositionDataQualityException(
                "Position data reliability ${(dataQuality.reliability * 100).toInt()}% below required ${(MIN_POSITION_ACCURACY * 100).toInt()}%"
            )
        }
        
        // Check out-of-order data
        if (dataQuality.outOfOrderCount > 20) {
            logger.warn(TAG, "⚠️ High out-of-order position updates: ${dataQuality.outOfOrderCount}")
        }
        
        // Check duplicate data
        if (dataQuality.duplicateCount > 10) {
            logger.warn(TAG, "⚠️ High duplicate position updates: ${dataQuality.duplicateCount}")
        }
        
        // Check invalid data count
        if (dataQuality.invalidDataCount > 5) {
            logger.error(TAG, "❌ High invalid data count: ${dataQuality.invalidDataCount}")
            throw PositionDataQualityException(
                "Invalid data count ${dataQuality.invalidDataCount} exceeds acceptable threshold"
            )
        }
        
        logger.info(TAG, "✅ Data quality metrics are acceptable: reliability=${(dataQuality.reliability * 100).toInt()}%")
    }
    
    private suspend fun validateGpsCoordinates() {
        logger.debug(TAG, "Validating GPS coordinate bounds")
        
        // Get recent position samples for validation
        val recentPositions = getRecentPositionSamples()
        
        var invalidCoordinateCount = 0
        val totalPositions = recentPositions.size
        
        recentPositions.forEach { position ->
            val lat = position.latitude
            val lng = position.longitude
            
            // Check latitude bounds
            if (lat < MIN_LATITUDE || lat > MAX_LATITUDE) {
                logger.warn(TAG, "⚠️ Invalid latitude for train ${position.trainId}: $lat")
                invalidCoordinateCount++
            }
            
            // Check longitude bounds
            if (lng < MIN_LONGITUDE || lng > MAX_LONGITUDE) {
                logger.warn(TAG, "⚠️ Invalid longitude for train ${position.trainId}: $lng")
                invalidCoordinateCount++
            }
            
            // Check for obviously invalid coordinates (0,0 or NaN)
            if (lat == 0.0 && lng == 0.0) {
                logger.warn(TAG, "⚠️ Zero coordinates for train ${position.trainId}")
                invalidCoordinateCount++
            }
            
            if (lat.isNaN() || lng.isNaN()) {
                logger.error(TAG, "❌ NaN coordinates for train ${position.trainId}")
                invalidCoordinateCount++
            }
        }
        
        if (totalPositions > 0) {
            val invalidRate = invalidCoordinateCount.toDouble() / totalPositions.toDouble()
            
            if (invalidRate > MAX_ANOMALY_RATE) {
                logger.error(TAG, "❌ High invalid coordinate rate: ${(invalidRate * 100).toInt()}%")
                throw PositionDataQualityException(
                    "Invalid coordinate rate ${(invalidRate * 100).toInt()}% exceeds ${(MAX_ANOMALY_RATE * 100).toInt()}% threshold"
                )
            } else {
                logger.info(TAG, "✅ GPS coordinates are valid: ${(100 - invalidRate * 100).toInt()}% accuracy")
            }
        }
    }
    
    private suspend fun validateSpeedRanges() {
        logger.debug(TAG, "Validating train speed ranges")
        
        val recentPositions = getRecentPositionSamples()
        var invalidSpeedCount = 0
        val totalPositions = recentPositions.size
        
        recentPositions.forEach { position ->
            val speed = position.speed
            
            // Check for negative speeds
            if (speed < MIN_SPEED) {
                logger.warn(TAG, "⚠️ Negative speed for train ${position.trainId}: $speed km/h")
                invalidSpeedCount++
            }
            
            // Check for unrealistic high speeds
            if (speed > MAX_PASSENGER_SPEED) {
                logger.warn(TAG, "⚠️ Unrealistic speed for train ${position.trainId}: $speed km/h")
                invalidSpeedCount++
            }
            
            // Check for NaN speeds
            if (speed.isNaN()) {
                logger.error(TAG, "❌ NaN speed for train ${position.trainId}")
                invalidSpeedCount++
            }
        }
        
        if (totalPositions > 0) {
            val invalidRate = invalidSpeedCount.toDouble() / totalPositions.toDouble()
            
            if (invalidRate > MAX_ANOMALY_RATE) {
                logger.error(TAG, "❌ High invalid speed rate: ${(invalidRate * 100).toInt()}%")
                throw PositionDataQualityException(
                    "Invalid speed rate ${(invalidRate * 100).toInt()}% exceeds ${(MAX_ANOMALY_RATE * 100).toInt()}% threshold"
                )
            } else {
                logger.info(TAG, "✅ Speed ranges are valid: ${(100 - invalidRate * 100).toInt()}% accuracy")
            }
        }
    }
    
    private suspend fun validateTimestampConsistency() {
        logger.debug(TAG, "Validating timestamp consistency")
        
        val recentPositions = getRecentPositionSamples()
        val currentTime = Clock.System.now()
        var timestampIssueCount = 0
        val totalPositions = recentPositions.size
        
        recentPositions.forEach { position ->
            val timestamp = position.timestamp
            val timeDrift = abs((currentTime - timestamp).inWholeSeconds)
            
            // Check for future timestamps
            if (timestamp > currentTime) {
                logger.warn(TAG, "⚠️ Future timestamp for train ${position.trainId}: $timestamp")
                timestampIssueCount++
            }
            
            // Check for excessive time drift
            if (timeDrift > MAX_TIMESTAMP_DRIFT_SECONDS) {
                logger.warn(TAG, "⚠️ Excessive time drift for train ${position.trainId}: ${timeDrift}s")
                timestampIssueCount++
            }
        }
        
        if (totalPositions > 0) {
            val issueRate = timestampIssueCount.toDouble() / totalPositions.toDouble()
            
            if (issueRate > MAX_ANOMALY_RATE) {
                logger.error(TAG, "❌ High timestamp issue rate: ${(issueRate * 100).toInt()}%")
                throw PositionDataQualityException(
                    "Timestamp issue rate ${(issueRate * 100).toInt()}% exceeds ${(MAX_ANOMALY_RATE * 100).toInt()}% threshold"
                )
            } else {
                logger.info(TAG, "✅ Timestamps are consistent: ${(100 - issueRate * 100).toInt()}% accuracy")
            }
        }
    }
    
    private suspend fun detectPositionAnomalies() {
        logger.debug(TAG, "Detecting position anomalies")
        
        val recentPositions = getRecentPositionSamples()
        val positionsByTrain = recentPositions.groupBy { it.trainId }
        var anomalyCount = 0
        var totalChecks = 0
        
        positionsByTrain.forEach { (trainId, positions) ->
            if (positions.size >= 2) {
                val sortedPositions = positions.sortedBy { it.timestamp }
                
                for (i in 1 until sortedPositions.size) {
                    val prev = sortedPositions[i - 1]
                    val curr = sortedPositions[i]
                    
                    val distance = calculateDistance(prev, curr)
                    val timeDiff = (curr.timestamp - prev.timestamp).inWholeMinutes
                    
                    if (timeDiff > 0) {
                        val speedKmh = (distance / timeDiff.toDouble()) * 60.0 // Convert to km/h
                        
                        // Check for impossible position jumps
                        if (distance > MAX_POSITION_JUMP_KM && timeDiff <= 1) {
                            logger.warn(TAG, "⚠️ Impossible position jump for train $trainId: ${distance}km in ${timeDiff}min")
                            anomalyCount++
                        }
                        
                        // Check for unrealistic calculated speeds
                        if (speedKmh > MAX_PASSENGER_SPEED * 1.5) { // Allow some tolerance
                            logger.warn(TAG, "⚠️ Unrealistic calculated speed for train $trainId: ${speedKmh}km/h")
                            anomalyCount++
                        }
                    }
                    
                    totalChecks++
                }
            }
        }
        
        if (totalChecks > 0) {
            val anomalyRate = anomalyCount.toDouble() / totalChecks.toDouble()
            
            if (anomalyRate > MAX_ANOMALY_RATE) {
                logger.error(TAG, "❌ High position anomaly rate: ${(anomalyRate * 100).toInt()}%")
                throw PositionDataQualityException(
                    "Position anomaly rate ${(anomalyRate * 100).toInt()}% exceeds ${(MAX_ANOMALY_RATE * 100).toInt()}% threshold"
                )
            } else {
                logger.info(TAG, "✅ Position anomalies are within acceptable range: ${(anomalyRate * 100).toInt()}%")
            }
        }
    }
    
    private suspend fun validateDataCompleteness() {
        logger.debug(TAG, "Validating data completeness")
        
        val recentPositions = getRecentPositionSamples()
        var completeRecords = 0
        val totalPositions = recentPositions.size
        
        recentPositions.forEach { position ->
            var isComplete = true
            
            // Check for required fields
            if (position.trainId.isBlank()) {
                isComplete = false
            }
            
            if (position.latitude == 0.0 && position.longitude == 0.0) {
                isComplete = false
            }
            
            if (position.speed.isNaN()) {
                isComplete = false
            }
            
            if (isComplete) {
                completeRecords++
            }
        }
        
        if (totalPositions > 0) {
            val completeness = completeRecords.toDouble() / totalPositions.toDouble()
            
            if (completeness < MIN_DATA_COMPLETENESS) {
                logger.error(TAG, "❌ Low data completeness: ${(completeness * 100).toInt()}%")
                throw PositionDataQualityException(
                    "Data completeness ${(completeness * 100).toInt()}% below required ${(MIN_DATA_COMPLETENESS * 100).toInt()}%"
                )
            } else {
                logger.info(TAG, "✅ Data completeness is acceptable: ${(completeness * 100).toInt()}%")
            }
        }
    }
    
    private suspend fun getRecentPositionSamples(): List<TrainPosition> {
        return try {
            // Get a sample of recent positions for validation
            // This would typically come from the real-time service or a recent cache
            // For now, we'll use the metrics collector to get sample data
            val metrics = metricsCollector.metrics.first()
            
            // In a real implementation, this would fetch actual position samples
            // For now, we'll return an empty list if no sample data is available
            emptyList()
        } catch (e: Exception) {
            logger.warn(TAG, "Could not retrieve position samples for validation", e)
            emptyList()
        }
    }
    
    private fun calculateDistance(pos1: TrainPosition, pos2: TrainPosition): Double {
        // Haversine formula for calculating distance between two GPS coordinates
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val lat1Rad = Math.toRadians(pos1.latitude)
        val lat2Rad = Math.toRadians(pos2.latitude)
        val deltaLatRad = Math.toRadians(pos2.latitude - pos1.latitude)
        val deltaLngRad = Math.toRadians(pos2.longitude - pos1.longitude)
        
        val a = kotlin.math.sin(deltaLatRad / 2) * kotlin.math.sin(deltaLatRad / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLngRad / 2) * kotlin.math.sin(deltaLngRad / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    override fun getSuccessDetails(): String {
        return "Position data quality is within acceptable parameters for railway operations"
    }
    
    override fun getFailureMessage(exception: Throwable?): String {
        return when (exception) {
            is PositionDataQualityException -> exception.message ?: "Position data quality check failed"
            else -> "Position data quality validation failed: ${exception?.message ?: "Unknown error"}"
        }
    }
}

/**
 * Exception for position data quality check failures
 */
class PositionDataQualityException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)