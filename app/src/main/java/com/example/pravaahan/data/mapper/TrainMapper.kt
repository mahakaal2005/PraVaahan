package com.example.pravaahan.data.mapper

import com.example.pravaahan.data.dto.TrainDto
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainPriority
import com.example.pravaahan.domain.model.TrainStatus
import kotlinx.datetime.Instant

/**
 * Extension functions for mapping between TrainDto and Train domain model
 */

/**
 * Maps TrainDto to Train domain model
 * @return Result containing Train or error if mapping fails
 */
fun TrainDto.toDomain(): Result<Train> {
    return try {
        val currentLocation = if (currentLatitude != null && currentLongitude != null) {
            Location(
                latitude = currentLatitude,
                longitude = currentLongitude,
                sectionId = sectionId ?: ""
            )
        } else {
            Location(0.0, 0.0, sectionId ?: "")
        }

        val destinationLocation = if (destinationLatitude != null && destinationLongitude != null) {
            Location(
                latitude = destinationLatitude,
                longitude = destinationLongitude,
                sectionId = sectionId ?: ""
            )
        } else {
            Location(0.0, 0.0, sectionId ?: "")
        }

        val trainStatus = try {
            TrainStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid train status: $status", e)
        }

        val trainPriority = try {
            TrainPriority.valueOf(priority)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid train priority: $priority", e)
        }

        val estimatedArrivalInstant = estimatedArrival?.let { timestamp ->
            try {
                Instant.parse(timestamp)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid estimated arrival timestamp: $timestamp", e)
            }
        } ?: Instant.DISTANT_FUTURE

        val createdAtInstant = try {
            Instant.parse(createdAt)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid created_at timestamp: $createdAt", e)
        }

        val updatedAtInstant = try {
            Instant.parse(updatedAt)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid updated_at timestamp: $updatedAt", e)
        }

        val train = Train(
            id = id,
            name = name,
            trainNumber = trainNumber,
            currentLocation = currentLocation,
            destination = destinationLocation,
            status = trainStatus,
            priority = trainPriority,
            speed = speed ?: 0.0,
            estimatedArrival = estimatedArrivalInstant,
            createdAt = createdAtInstant,
            updatedAt = updatedAtInstant
        )

        Result.success(train)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Maps Train domain model to TrainDto
 * @return TrainDto representation of the Train
 */
fun Train.toDto(): TrainDto {
    return TrainDto(
        id = id,
        name = name,
        trainNumber = trainNumber,
        currentLatitude = currentLocation.latitude,
        currentLongitude = currentLocation.longitude,
        destinationLatitude = destination.latitude,
        destinationLongitude = destination.longitude,
        sectionId = currentLocation.sectionId.takeIf { it.isNotEmpty() },
        status = status.name,
        priority = priority.name,
        speed = speed,
        estimatedArrival = estimatedArrival.toString(),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString()
    )
}

/**
 * Maps a list of TrainDto to a list of Train domain models
 * @return Result containing list of Trains or error if any mapping fails
 */
fun List<TrainDto>.toDomainList(): Result<List<Train>> {
    return try {
        val trains = mutableListOf<Train>()
        for (dto in this) {
            val trainResult = dto.toDomain()
            if (trainResult.isSuccess) {
                trains.add(trainResult.getOrThrow())
            } else {
                return Result.failure(trainResult.exceptionOrNull() ?: Exception("Unknown mapping error"))
            }
        }
        Result.success(trains)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Maps a list of Train domain models to a list of TrainDto
 * @return List of TrainDto representations
 */
fun List<Train>.toDtoList(): List<TrainDto> {
    return map { it.toDto() }
}