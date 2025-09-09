package com.example.pravaahan.data.mapper

import com.example.pravaahan.data.dto.TrainPositionDto
import com.example.pravaahan.domain.model.TrainPosition
import kotlinx.datetime.Instant

/**
 * Mapper for converting between TrainPosition domain model and TrainPositionDto.
 * Handles serialization/deserialization and validation during mapping.
 */
object TrainPositionMapper {
    
    /**
     * Converts TrainPositionDto to TrainPosition domain model.
     * Performs validation and throws exceptions for invalid data.
     */
    fun toDomain(dto: TrainPositionDto): TrainPosition {
        return TrainPosition(
            trainId = dto.trainId,
            latitude = dto.latitude,
            longitude = dto.longitude,
            speed = dto.speed,
            heading = dto.heading,
            timestamp = Instant.parse(dto.timestamp),
            sectionId = dto.sectionId,
            accuracy = dto.accuracy
        )
    }
    
    /**
     * Converts TrainPosition domain model to TrainPositionDto.
     * Includes additional metadata for database storage.
     */
    fun toDto(
        domain: TrainPosition,
        signalStrength: Double? = null,
        dataSource: String = "GPS",
        validationStatus: String = "PENDING"
    ): TrainPositionDto {
        return TrainPositionDto(
            trainId = domain.trainId,
            latitude = domain.latitude,
            longitude = domain.longitude,
            speed = domain.speed,
            heading = domain.heading,
            timestamp = domain.timestamp.toString(),
            sectionId = domain.sectionId,
            accuracy = domain.accuracy,
            signalStrength = signalStrength,
            dataSource = dataSource,
            validationStatus = validationStatus
        )
    }
    
    /**
     * Converts a list of DTOs to domain models with error handling.
     * Returns only valid positions and logs invalid ones.
     */
    fun toDomainList(dtos: List<TrainPositionDto>): List<TrainPosition> {
        return dtos.mapNotNull { dto ->
            try {
                toDomain(dto)
            } catch (e: Exception) {
                // Log the error but don't fail the entire operation
                println("Failed to convert TrainPositionDto to domain: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Converts a list of domain models to DTOs.
     */
    fun toDtoList(
        domains: List<TrainPosition>,
        defaultSignalStrength: Double? = null,
        defaultDataSource: String = "GPS",
        defaultValidationStatus: String = "PENDING"
    ): List<TrainPositionDto> {
        return domains.map { domain ->
            toDto(
                domain = domain,
                signalStrength = defaultSignalStrength,
                dataSource = defaultDataSource,
                validationStatus = defaultValidationStatus
            )
        }
    }
}