package com.example.pravaahan.data.mapper

import com.example.pravaahan.data.dto.TrainDto
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainPriority
import com.example.pravaahan.domain.model.TrainStatus
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class TrainMapperTest {

    @Test
    fun `toDomain maps TrainDto to Train correctly`() {
        // Arrange
        val trainDto = TrainDto(
            id = "test-id",
            name = "Express 101",
            trainNumber = "EXP101",
            currentLatitude = 28.6139,
            currentLongitude = 77.2090,
            destinationLatitude = 19.0760,
            destinationLongitude = 72.8777,
            sectionId = "SEC001",
            status = "ON_TIME",
            priority = "EXPRESS",
            speed = 120.5,
            estimatedArrival = "2024-01-15T10:30:00Z",
            createdAt = "2024-01-15T08:00:00Z",
            updatedAt = "2024-01-15T08:30:00Z"
        )

        // Act
        val result = trainDto.toDomain()

        // Assert
        assertTrue(result.isSuccess)
        val train = result.getOrThrow()
        
        assertEquals("test-id", train.id)
        assertEquals("Express 101", train.name)
        assertEquals("EXP101", train.trainNumber)
        assertEquals(28.6139, train.currentLocation.latitude)
        assertEquals(77.2090, train.currentLocation.longitude)
        assertEquals("SEC001", train.currentLocation.sectionId)
        assertEquals(19.0760, train.destination.latitude)
        assertEquals(72.8777, train.destination.longitude)
        assertEquals(TrainStatus.ON_TIME, train.status)
        assertEquals(TrainPriority.EXPRESS, train.priority)
        assertEquals(120.5, train.speed)
        assertEquals(Instant.parse("2024-01-15T10:30:00Z"), train.estimatedArrival)
        assertEquals(Instant.parse("2024-01-15T08:00:00Z"), train.createdAt)
        assertEquals(Instant.parse("2024-01-15T08:30:00Z"), train.updatedAt)
    }

    @Test
    fun `toDomain handles null coordinates correctly`() {
        // Arrange
        val trainDto = TrainDto(
            id = "test-id",
            name = "Express 101",
            trainNumber = "EXP101",
            currentLatitude = null,
            currentLongitude = null,
            destinationLatitude = null,
            destinationLongitude = null,
            sectionId = "SEC001",
            status = "ON_TIME",
            priority = "EXPRESS",
            speed = null,
            estimatedArrival = null,
            createdAt = "2024-01-15T08:00:00Z",
            updatedAt = "2024-01-15T08:30:00Z"
        )

        // Act
        val result = trainDto.toDomain()

        // Assert
        assertTrue(result.isSuccess)
        val train = result.getOrThrow()
        
        assertEquals(0.0, train.currentLocation.latitude)
        assertEquals(0.0, train.currentLocation.longitude)
        assertEquals("SEC001", train.currentLocation.sectionId)
        assertEquals(0.0, train.destination.latitude)
        assertEquals(0.0, train.destination.longitude)
        assertEquals(0.0, train.speed)
        assertEquals(Instant.DISTANT_FUTURE, train.estimatedArrival)
    }

    @Test
    fun `toDomain fails with invalid status`() {
        // Arrange
        val trainDto = TrainDto(
            id = "test-id",
            name = "Express 101",
            trainNumber = "EXP101",
            currentLatitude = 28.6139,
            currentLongitude = 77.2090,
            destinationLatitude = 19.0760,
            destinationLongitude = 72.8777,
            sectionId = "SEC001",
            status = "INVALID_STATUS",
            priority = "EXPRESS",
            speed = 120.5,
            estimatedArrival = "2024-01-15T10:30:00Z",
            createdAt = "2024-01-15T08:00:00Z",
            updatedAt = "2024-01-15T08:30:00Z"
        )

        // Act
        val result = trainDto.toDomain()

        // Assert
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `toDomain fails with invalid priority`() {
        // Arrange
        val trainDto = TrainDto(
            id = "test-id",
            name = "Express 101",
            trainNumber = "EXP101",
            currentLatitude = 28.6139,
            currentLongitude = 77.2090,
            destinationLatitude = 19.0760,
            destinationLongitude = 72.8777,
            sectionId = "SEC001",
            status = "ON_TIME",
            priority = "INVALID_PRIORITY",
            speed = 120.5,
            estimatedArrival = "2024-01-15T10:30:00Z",
            createdAt = "2024-01-15T08:00:00Z",
            updatedAt = "2024-01-15T08:30:00Z"
        )

        // Act
        val result = trainDto.toDomain()

        // Assert
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `toDto maps Train to TrainDto correctly`() {
        // Arrange
        val train = Train(
            id = "test-id",
            name = "Express 101",
            trainNumber = "EXP101",
            currentLocation = Location(28.6139, 77.2090, "SEC001"),
            destination = Location(19.0760, 72.8777, "SEC001"),
            status = TrainStatus.ON_TIME,
            priority = TrainPriority.EXPRESS,
            speed = 120.5,
            estimatedArrival = Instant.parse("2024-01-15T10:30:00Z"),
            createdAt = Instant.parse("2024-01-15T08:00:00Z"),
            updatedAt = Instant.parse("2024-01-15T08:30:00Z")
        )

        // Act
        val trainDto = train.toDto()

        // Assert
        assertEquals("test-id", trainDto.id)
        assertEquals("Express 101", trainDto.name)
        assertEquals("EXP101", trainDto.trainNumber)
        assertEquals(28.6139, trainDto.currentLatitude)
        assertEquals(77.2090, trainDto.currentLongitude)
        assertEquals(19.0760, trainDto.destinationLatitude)
        assertEquals(72.8777, trainDto.destinationLongitude)
        assertEquals("SEC001", trainDto.sectionId)
        assertEquals("ON_TIME", trainDto.status)
        assertEquals("EXPRESS", trainDto.priority)
        assertEquals(120.5, trainDto.speed)
        assertEquals("2024-01-15T10:30:00Z", trainDto.estimatedArrival)
        assertEquals("2024-01-15T08:00:00Z", trainDto.createdAt)
        assertEquals("2024-01-15T08:30:00Z", trainDto.updatedAt)
    }

    @Test
    fun `toDomainList maps list of TrainDto correctly`() {
        // Arrange
        val trainDtos = listOf(
            TrainDto(
                id = "test-id-1",
                name = "Express 101",
                trainNumber = "EXP101",
                currentLatitude = 28.6139,
                currentLongitude = 77.2090,
                destinationLatitude = 19.0760,
                destinationLongitude = 72.8777,
                sectionId = "SEC001",
                status = "ON_TIME",
                priority = "EXPRESS",
                speed = 120.5,
                estimatedArrival = "2024-01-15T10:30:00Z",
                createdAt = "2024-01-15T08:00:00Z",
                updatedAt = "2024-01-15T08:30:00Z"
            ),
            TrainDto(
                id = "test-id-2",
                name = "Local 202",
                trainNumber = "LOC202",
                currentLatitude = 19.0760,
                currentLongitude = 72.8777,
                destinationLatitude = 28.6139,
                destinationLongitude = 77.2090,
                sectionId = "SEC002",
                status = "DELAYED",
                priority = "LOW",
                speed = 60.0,
                estimatedArrival = "2024-01-15T12:00:00Z",
                createdAt = "2024-01-15T08:00:00Z",
                updatedAt = "2024-01-15T09:00:00Z"
            )
        )

        // Act
        val result = trainDtos.toDomainList()

        // Assert
        assertTrue(result.isSuccess)
        val trains = result.getOrThrow()
        assertEquals(2, trains.size)
        assertEquals("test-id-1", trains[0].id)
        assertEquals("test-id-2", trains[1].id)
        assertEquals(TrainStatus.ON_TIME, trains[0].status)
        assertEquals(TrainStatus.DELAYED, trains[1].status)
    }

    @Test
    fun `toDomainList fails if any mapping fails`() {
        // Arrange
        val trainDtos = listOf(
            TrainDto(
                id = "test-id-1",
                name = "Express 101",
                trainNumber = "EXP101",
                currentLatitude = 28.6139,
                currentLongitude = 77.2090,
                destinationLatitude = 19.0760,
                destinationLongitude = 72.8777,
                sectionId = "SEC001",
                status = "ON_TIME",
                priority = "EXPRESS",
                speed = 120.5,
                estimatedArrival = "2024-01-15T10:30:00Z",
                createdAt = "2024-01-15T08:00:00Z",
                updatedAt = "2024-01-15T08:30:00Z"
            ),
            TrainDto(
                id = "test-id-2",
                name = "Invalid Train",
                trainNumber = "INV001",
                currentLatitude = 19.0760,
                currentLongitude = 72.8777,
                destinationLatitude = 28.6139,
                destinationLongitude = 77.2090,
                sectionId = "SEC002",
                status = "INVALID_STATUS", // This will cause failure
                priority = "LOW",
                speed = 60.0,
                estimatedArrival = "2024-01-15T12:00:00Z",
                createdAt = "2024-01-15T08:00:00Z",
                updatedAt = "2024-01-15T09:00:00Z"
            )
        )

        // Act
        val result = trainDtos.toDomainList()

        // Assert
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}