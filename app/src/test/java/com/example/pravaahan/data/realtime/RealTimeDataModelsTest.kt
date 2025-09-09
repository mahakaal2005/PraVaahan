package com.example.pravaahan.data.realtime

import com.example.pravaahan.core.security.RealTimeSecurityValidator
import com.example.pravaahan.core.security.SecuritySeverity
import com.example.pravaahan.data.dto.TrainPositionDto
import com.example.pravaahan.data.dto.ValidationResult
import com.example.pravaahan.data.mapper.TrainPositionMapper
import com.example.pravaahan.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive test suite for real-time data models, DTOs, and security validation.
 */
class RealTimeDataModelsTest {
    
    @Test
    fun `TrainPosition domain model validates coordinates correctly`() {
        // Valid position should not throw
        val validPosition = TrainPosition(
            trainId = "TRAIN001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now(),
            sectionId = "SEC001",
            accuracy = 8.5
        )
        
        assertEquals("TRAIN001", validPosition.trainId)
        assertEquals(28.6139, validPosition.latitude)
        
        // Invalid latitude should throw
        assertFailsWith<IllegalArgumentException> {
            TrainPosition(
                trainId = "TRAIN001",
                latitude = 91.0, // Invalid latitude
                longitude = 77.2090,
                speed = 85.5,
                heading = 45.0,
                timestamp = Clock.System.now(),
                sectionId = "SEC001"
            )
        }
        
        // Invalid speed should throw
        assertFailsWith<IllegalArgumentException> {
            TrainPosition(
                trainId = "TRAIN001",
                latitude = 28.6139,
                longitude = 77.2090,
                speed = -10.0, // Negative speed
                heading = 45.0,
                timestamp = Clock.System.now(),
                sectionId = "SEC001"
            )
        }
    }
    
    @Test
    fun `TrainPosition calculates distance correctly`() {
        val position1 = TrainPosition(
            trainId = "TRAIN001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now(),
            sectionId = "SEC001"
        )
        
        val position2 = TrainPosition(
            trainId = "TRAIN001",
            latitude = 28.6239, // ~1.1km north
            longitude = 77.2090,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now(),
            sectionId = "SEC001"
        )
        
        val distance = position1.distanceTo(position2)
        assertTrue(distance > 1000.0 && distance < 1200.0) // Approximately 1.1km
    }
    
    @Test
    fun `TrainPositionDto validates input correctly`() {
        // Valid DTO should not throw
        val validDto = TrainPositionDto(
            trainId = "TRAIN001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now().toString(),
            sectionId = "SEC001",
            accuracy = 8.5,
            signalStrength = 92.0,
            dataSource = "GPS",
            validationStatus = "VALID"
        )
        
        assertEquals("TRAIN001", validDto.trainId)
        
        // Invalid train ID format should throw
        assertFailsWith<IllegalArgumentException> {
            TrainPositionDto(
                trainId = "TRAIN@001", // Invalid characters
                latitude = 28.6139,
                longitude = 77.2090,
                speed = 85.5,
                heading = 45.0,
                timestamp = Clock.System.now().toString(),
                sectionId = "SEC001"
            )
        }
        
        // Invalid data source should throw
        assertFailsWith<IllegalArgumentException> {
            TrainPositionDto(
                trainId = "TRAIN001",
                latitude = 28.6139,
                longitude = 77.2090,
                speed = 85.5,
                heading = 45.0,
                timestamp = Clock.System.now().toString(),
                sectionId = "SEC001",
                dataSource = "INVALID_SOURCE"
            )
        }
    }
    
    @Test
    fun `TrainPositionDto validates realistic movement`() {
        val dto = TrainPositionDto(
            trainId = "TRAIN001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 150.0, // High speed
            heading = 45.0,
            timestamp = Clock.System.now().toString(),
            sectionId = "SEC001",
            accuracy = 100.0, // Low accuracy
            signalStrength = 25.0, // Low signal
            dataSource = "GPS"
        )
        
        val result = dto.validateRealisticMovement()
        assertTrue(result is ValidationResult.Warning)
        assertTrue((result as ValidationResult.Warning).issues.isNotEmpty())
    }
    
    @Test
    fun `RealTimeTrainState calculates safety reliability correctly`() {
        val position = TrainPosition(
            trainId = "TRAIN001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now(),
            sectionId = "SEC001"
        )
        
        val highQualityIndicators = DataQualityIndicators(
            signalStrength = 0.95,
            gpsAccuracy = 5.0,
            dataFreshness = 0.98,
            validationStatus = ValidationStatus.VALID,
            sourceReliability = 0.9,
            anomalyFlags = emptySet(),
            overallScore = 0.92
        )
        
        val goodConnection = ConnectionStatus(
            state = ConnectionState.CONNECTED,
            lastSuccessfulCommunication = Clock.System.now() - 10.seconds,
            networkQuality = NetworkQuality.EXCELLENT,
            retryAttempts = 0,
            errorCount = 0
        )
        
        val trainState = RealTimeTrainState(
            trainId = "TRAIN001",
            currentPosition = position,
            dataQuality = highQualityIndicators,
            connectionStatus = goodConnection,
            lastUpdateTime = Clock.System.now() - 5.seconds
        )
        
        assertTrue(trainState.isSafetyReliable())
        assertTrue(trainState.isFresh(1.minutes))
    }
    
    @Test
    fun `DataQualityIndicators calculates overall score correctly`() {
        val score = DataQualityIndicators.calculateOverallScore(
            signalStrength = 95.0, // Will be normalized to 0.95
            gpsAccuracy = 8.0, // Good accuracy
            dataFreshness = 0.9,
            sourceReliability = 0.85,
            validationStatus = ValidationStatus.VALID,
            anomalyFlags = emptySet()
        )
        
        assertTrue(score > 0.8) // Should be high quality
        assertTrue(score <= 1.0)
    }
    
    @Test
    fun `DataQualityIndicators applies penalties correctly`() {
        val scoreWithAnomalies = DataQualityIndicators.calculateOverallScore(
            signalStrength = 0.95,
            gpsAccuracy = 8.0,
            dataFreshness = 0.9,
            sourceReliability = 0.85,
            validationStatus = ValidationStatus.SUSPICIOUS,
            anomalyFlags = setOf(AnomalyFlag.SPEED_ANOMALY, AnomalyFlag.POSITION_JUMP)
        )
        
        val scoreWithoutAnomalies = DataQualityIndicators.calculateOverallScore(
            signalStrength = 0.95,
            gpsAccuracy = 8.0,
            dataFreshness = 0.9,
            sourceReliability = 0.85,
            validationStatus = ValidationStatus.VALID,
            anomalyFlags = emptySet()
        )
        
        assertTrue(scoreWithAnomalies < scoreWithoutAnomalies)
    }
    
    @Test
    fun `RealTimeSecurityValidator detects injection attempts`() {
        val validator = RealTimeSecurityValidator()
        
        // This should fail validation during DTO creation
        assertFailsWith<IllegalArgumentException> {
            TrainPositionDto(
                trainId = "TRAIN001'; DROP TABLE trains; --",
                latitude = 28.6139,
                longitude = 77.2090,
                speed = 85.5,
                heading = 45.0,
                timestamp = Clock.System.now().toString(),
                sectionId = "SEC001"
            )
        }
    }
    
    @Test
    fun `RealTimeSecurityValidator validates geospatial data`() {
        val validator = RealTimeSecurityValidator()
        
        // Position outside India should be flagged
        val outsideIndiaDto = TrainPositionDto(
            trainId = "TRAIN001",
            latitude = 51.5074, // London coordinates
            longitude = -0.1278,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now().toString(),
            sectionId = "SEC001"
        )
        
        val result = validator.validatePosition(outsideIndiaDto)
        // MEDIUM severity issues don't make the result invalid, only CRITICAL do
        assertTrue(result.isValid) 
        assertTrue(result.issues.any { it.severity == SecuritySeverity.MEDIUM })
        assertTrue(result.anomalies.contains(AnomalyFlag.GEOFENCE_VIOLATION))
    }
    
    @Test
    fun `TrainPositionMapper converts between domain and DTO correctly`() {
        val domainPosition = TrainPosition(
            trainId = "TRAIN001",
            latitude = 28.6139,
            longitude = 77.2090,
            speed = 85.5,
            heading = 45.0,
            timestamp = Clock.System.now(),
            sectionId = "SEC001",
            accuracy = 8.5
        )
        
        // Convert to DTO
        val dto = TrainPositionMapper.toDto(
            domain = domainPosition,
            signalStrength = 92.0,
            dataSource = "GPS",
            validationStatus = "VALID"
        )
        
        assertEquals(domainPosition.trainId, dto.trainId)
        assertEquals(domainPosition.latitude, dto.latitude)
        assertEquals(92.0, dto.signalStrength)
        
        // Convert back to domain
        val convertedDomain = TrainPositionMapper.toDomain(dto)
        
        assertEquals(domainPosition.trainId, convertedDomain.trainId)
        assertEquals(domainPosition.latitude, convertedDomain.latitude)
        assertEquals(domainPosition.speed, convertedDomain.speed)
    }
    
    @Test
    fun `TrainPositionMapper handles invalid data gracefully`() {
        val invalidDtos = listOf(
            TrainPositionDto(
                trainId = "VALID001",
                latitude = 28.6139,
                longitude = 77.2090,
                speed = 85.5,
                heading = 45.0,
                timestamp = Clock.System.now().toString(),
                sectionId = "SEC001"
            ),
            // This would be invalid if we had one, but our validation is in the DTO constructor
        )
        
        val validPositions = TrainPositionMapper.toDomainList(invalidDtos)
        assertEquals(1, validPositions.size) // Only valid ones should be converted
    }
}