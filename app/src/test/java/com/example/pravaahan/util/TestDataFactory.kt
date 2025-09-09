package com.example.pravaahan.util

import com.example.pravaahan.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Factory for creating test data with realistic railway scenarios
 */
object TestDataFactory {
    
    private val currentTime = Clock.System.now()
    
    /**
     * Creates a test train with default values
     */
    fun createTrain(
        id: String = "train_001",
        name: String = "Rajdhani Express",
        trainNumber: String = "12001",
        currentLocation: Location = createLocation(),
        destination: Location = createLocation(28.6139, 77.2090, "NEW_DELHI"),
        status: TrainStatus = TrainStatus.ON_TIME,
        priority: TrainPriority = TrainPriority.HIGH,
        speed: Double = 120.0,
        estimatedArrival: Instant = currentTime.plus(2.hours),
        createdAt: Instant = currentTime.minus(1.hours),
        updatedAt: Instant = currentTime
    ): Train {
        return Train(
            id = id,
            name = name,
            trainNumber = trainNumber,
            currentLocation = currentLocation,
            destination = destination,
            status = status,
            priority = priority,
            speed = speed,
            estimatedArrival = estimatedArrival,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    /**
     * Creates a test location with default values
     */
    fun createLocation(
        latitude: Double = 19.0760,
        longitude: Double = 72.8777,
        sectionId: String = "MUMBAI_CENTRAL"
    ): Location {
        return Location(
            latitude = latitude,
            longitude = longitude,
            sectionId = sectionId
        )
    }
    
    /**
     * Creates a test conflict alert
     */
    fun createConflictAlert(
        id: String = "conflict_001",
        trainsInvolved: List<String> = listOf("train_001", "train_002"),
        conflictType: ConflictType = ConflictType.POTENTIAL_COLLISION,
        detectedAt: Instant = currentTime,
        estimatedImpactTime: Instant? = currentTime.plus(30.minutes),
        recommendation: String = "Reduce speed of Train 001 to 80 km/h",
        severity: ConflictSeverity = ConflictSeverity.HIGH
    ): ConflictAlert {
        return ConflictAlert(
            id = id,
            type = conflictType,
            severity = severity,
            involvedTrains = trainsInvolved,
            description = recommendation,
            timestamp = detectedAt,
            trainsInvolved = trainsInvolved,
            conflictType = conflictType,
            detectedAt = detectedAt,
            estimatedImpactTime = estimatedImpactTime,
            recommendation = recommendation
        )
    }
    
    /**
     * Creates a test track
     */
    fun createTrack(
        id: String = "track_001",
        sectionId: String = "MUMBAI_CENTRAL",
        sectionName: String = "Mumbai Central Section",
        capacity: Int = 2,
        isActive: Boolean = true
    ): Track {
        return Track(
            id = id,
            sectionId = sectionId,
            sectionName = sectionName,
            capacity = capacity,
            isActive = isActive
        )
    }
    
    /**
     * Creates a list of test trains for various scenarios
     */
    fun createTrainList(): List<Train> {
        return listOf(
            createTrain(
                id = "train_001",
                name = "Rajdhani Express",
                status = TrainStatus.ON_TIME,
                priority = TrainPriority.EXPRESS,
                speed = 130.0
            ),
            createTrain(
                id = "train_002", 
                name = "Shatabdi Express",
                status = TrainStatus.DELAYED,
                priority = TrainPriority.HIGH,
                speed = 85.0,
                currentLocation = createLocation(19.0760, 72.8777, "MUMBAI_CENTRAL"),
                estimatedArrival = currentTime.plus(3.hours)
            ),
            createTrain(
                id = "train_003",
                name = "Local Passenger",
                status = TrainStatus.STOPPED,
                priority = TrainPriority.LOW,
                speed = 0.0,
                currentLocation = createLocation(19.0544, 72.8311, "DADAR"),
                estimatedArrival = currentTime.plus(45.minutes)
            ),
            createTrain(
                id = "train_004",
                name = "Freight Express",
                status = TrainStatus.MAINTENANCE,
                priority = TrainPriority.MEDIUM,
                speed = 0.0,
                currentLocation = createLocation(19.0176, 72.8562, "LOWER_PAREL"),
                estimatedArrival = currentTime.plus(6.hours)
            )
        )
    }
    
    /**
     * Creates conflict scenarios for testing
     */
    fun createConflictScenarios(): List<ConflictAlert> {
        return listOf(
            createConflictAlert(
                id = "conflict_001",
                trainsInvolved = listOf("train_001", "train_002"),
                conflictType = ConflictType.POTENTIAL_COLLISION,
                severity = ConflictSeverity.HIGH,
                recommendation = "Reduce speed of Rajdhani Express to 80 km/h and hold Shatabdi at current signal"
            ),
            createConflictAlert(
                id = "conflict_002",
                trainsInvolved = listOf("train_002", "train_003"),
                conflictType = ConflictType.TRACK_CONGESTION,
                severity = ConflictSeverity.MEDIUM,
                recommendation = "Divert Local Passenger to alternate platform"
            ),
            createConflictAlert(
                id = "conflict_003",
                trainsInvolved = listOf("train_001"),
                conflictType = ConflictType.SIGNAL_FAILURE,
                severity = ConflictSeverity.CRITICAL,
                recommendation = "Emergency stop - Manual signal control required"
            )
        )
    }
    
    /**
     * Creates error scenarios for testing
     */
    fun createErrorScenarios(): Map<String, Exception> {
        return mapOf(
            "network_error" to java.net.UnknownHostException("Network unreachable"),
            "timeout_error" to java.net.SocketTimeoutException("Connection timeout"),
            "api_error" to RuntimeException("API returned 500 Internal Server Error"),
            "data_error" to IllegalArgumentException("Invalid train data format")
        )
    }
}