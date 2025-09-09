package com.example.pravaahan.data.mapper

import com.example.pravaahan.data.dto.ConflictDto
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.model.ConflictType
import kotlinx.datetime.Instant

/**
 * Extension functions for mapping between ConflictDto and ConflictAlert domain model
 */

/**
 * Maps ConflictDto to ConflictAlert domain model
 * @return Result containing ConflictAlert or error if mapping fails
 */
fun ConflictDto.toDomain(): Result<ConflictAlert> {
    return try {
        val conflictTypeEnum = try {
            ConflictType.valueOf(conflictType)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid conflict type: $conflictType", e)
        }

        val severityEnum = try {
            ConflictSeverity.valueOf(severity)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid conflict severity: $severity", e)
        }

        val detectedAtInstant = try {
            Instant.parse(detectedAt)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid detected_at timestamp: $detectedAt", e)
        }

        val estimatedImpactTimeInstant = estimatedImpactTime?.let { timestamp ->
            try {
                Instant.parse(timestamp)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid estimated_impact_time timestamp: $timestamp", e)
            }
        }

        val resolvedAtInstant = resolvedAt?.let { timestamp ->
            try {
                Instant.parse(timestamp)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid resolved_at timestamp: $timestamp", e)
            }
        }

        val conflictAlert = ConflictAlert(
            id = id,
            type = conflictTypeEnum,
            severity = severityEnum,
            involvedTrains = trainsInvolved,
            description = "Conflict detected between trains: ${trainsInvolved.joinToString(", ")}",
            timestamp = detectedAtInstant,
            resolved = isResolved,
            estimatedImpactTime = estimatedImpactTimeInstant,
            recommendation = aiRecommendation,
            resolvedAt = resolvedAtInstant,
            controllerAction = controllerAction
        )

        Result.success(conflictAlert)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Maps ConflictAlert domain model to ConflictDto
 * @return ConflictDto representation of the ConflictAlert
 */
fun ConflictAlert.toDto(): ConflictDto {
    return ConflictDto(
        id = id,
        trainsInvolved = trainsInvolved,
        conflictType = conflictType.name,
        severity = severity.name,
        detectedAt = detectedAt.toString(),
        estimatedImpactTime = estimatedImpactTime?.toString(),
        aiRecommendation = recommendation ?: "No recommendation available",
        isResolved = isResolved,
        resolvedAt = resolvedAt?.toString(),
        controllerAction = controllerAction,
        createdAt = detectedAt.toString(), // Use detectedAt as createdAt
        updatedAt = resolvedAt?.toString()
    )
}

/**
 * Maps a list of ConflictDto to a list of ConflictAlert domain models
 * @return Result containing list of ConflictAlerts or error if any mapping fails
 */
fun List<ConflictDto>.toDomainList(): Result<List<ConflictAlert>> {
    return try {
        val conflicts = mutableListOf<ConflictAlert>()
        for (dto in this) {
            val conflictResult = dto.toDomain()
            if (conflictResult.isSuccess) {
                conflicts.add(conflictResult.getOrThrow())
            } else {
                return Result.failure(conflictResult.exceptionOrNull() ?: Exception("Unknown mapping error"))
            }
        }
        Result.success(conflicts)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Maps a list of ConflictAlert domain models to a list of ConflictDto
 * @return List of ConflictDto representations
 */
fun List<ConflictAlert>.toDtoList(): List<ConflictDto> {
    return map { it.toDto() }
}