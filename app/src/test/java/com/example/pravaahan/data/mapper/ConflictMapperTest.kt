package com.example.pravaahan.data.mapper

import com.example.pravaahan.data.dto.ConflictDto
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.model.ConflictType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class ConflictMapperTest {

    @Test
    fun `toDomain maps ConflictDto to ConflictAlert correctly`() {
        // Arrange
        val conflictDto = ConflictDto(
            id = "conflict-id",
            trainsInvolved = listOf("train-1", "train-2"),
            conflictType = "COLLISION_RISK",
            severity = "HIGH",
            detectedAt = "2024-01-15T09:00:00Z",
            estimatedImpactTime = "2024-01-15T09:30:00Z",
            aiRecommendation = "Reduce speed and maintain safe distance",
            isResolved = false,
            resolvedAt = null,
            controllerAction = null,
            createdAt = "2024-01-15T09:00:00Z",
            updatedAt = null
        )

        // Act
        val result = conflictDto.toDomain()

        // Assert
        assertTrue(result.isSuccess)
        val conflict = result.getOrThrow()
        
        assertEquals("conflict-id", conflict.id)
        assertEquals(listOf("train-1", "train-2"), conflict.trainsInvolved)
        assertEquals(ConflictType.POTENTIAL_COLLISION, conflict.conflictType)
        assertEquals(ConflictSeverity.HIGH, conflict.severity)
        assertEquals(Instant.parse("2024-01-15T09:00:00Z"), conflict.detectedAt)
        assertEquals(Instant.parse("2024-01-15T09:30:00Z"), conflict.estimatedImpactTime)
        assertEquals("Reduce speed and maintain safe distance", conflict.recommendation)
        assertFalse(conflict.isResolved)
        assertNull(conflict.resolvedAt)
        assertNull(conflict.controllerAction)
    }

    @Test
    fun `toDomain handles resolved conflict correctly`() {
        // Arrange
        val conflictDto = ConflictDto(
            id = "conflict-id",
            trainsInvolved = listOf("train-1"),
            conflictType = "TRACK_OCCUPATION",
            severity = "MEDIUM",
            detectedAt = "2024-01-15T09:00:00Z",
            estimatedImpactTime = "2024-01-15T09:30:00Z",
            aiRecommendation = "Wait for track clearance",
            isResolved = true,
            resolvedAt = "2024-01-15T09:15:00Z",
            controllerAction = "Manual override applied",
            createdAt = "2024-01-15T09:00:00Z",
            updatedAt = "2024-01-15T09:15:00Z"
        )

        // Act
        val result = conflictDto.toDomain()

        // Assert
        assertTrue(result.isSuccess)
        val conflict = result.getOrThrow()
        
        assertTrue(conflict.isResolved)
        assertEquals(Instant.parse("2024-01-15T09:15:00Z"), conflict.resolvedAt)
        assertEquals("Manual override applied", conflict.controllerAction)
    }

    @Test
    fun `toDomain handles null optional fields correctly`() {
        // Arrange
        val conflictDto = ConflictDto(
            id = "conflict-id",
            trainsInvolved = listOf("train-1"),
            conflictType = "SIGNAL_VIOLATION",
            severity = "LOW",
            detectedAt = "2024-01-15T09:00:00Z",
            estimatedImpactTime = null,
            aiRecommendation = "Check signal status",
            isResolved = false,
            resolvedAt = null,
            controllerAction = null,
            createdAt = "2024-01-15T09:00:00Z",
            updatedAt = null
        )

        // Act
        val result = conflictDto.toDomain()

        // Assert
        assertTrue(result.isSuccess)
        val conflict = result.getOrThrow()
        
        assertNull(conflict.estimatedImpactTime)
        assertNull(conflict.resolvedAt)
        assertNull(conflict.controllerAction)
    }

    @Test
    fun `toDomain fails with invalid conflict type`() {
        // Arrange
        val conflictDto = ConflictDto(
            id = "conflict-id",
            trainsInvolved = listOf("train-1"),
            conflictType = "INVALID_TYPE",
            severity = "HIGH",
            detectedAt = "2024-01-15T09:00:00Z",
            estimatedImpactTime = "2024-01-15T09:30:00Z",
            aiRecommendation = "Test recommendation",
            isResolved = false,
            resolvedAt = null,
            controllerAction = null,
            createdAt = "2024-01-15T09:00:00Z",
            updatedAt = null
        )

        // Act
        val result = conflictDto.toDomain()

        // Assert
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `toDomain fails with invalid severity`() {
        // Arrange
        val conflictDto = ConflictDto(
            id = "conflict-id",
            trainsInvolved = listOf("train-1"),
            conflictType = "COLLISION_RISK",
            severity = "INVALID_SEVERITY",
            detectedAt = "2024-01-15T09:00:00Z",
            estimatedImpactTime = "2024-01-15T09:30:00Z",
            aiRecommendation = "Test recommendation",
            isResolved = false,
            resolvedAt = null,
            controllerAction = null,
            createdAt = "2024-01-15T09:00:00Z",
            updatedAt = null
        )

        // Act
        val result = conflictDto.toDomain()

        // Assert
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `toDto maps ConflictAlert to ConflictDto correctly`() {
        // Arrange
        val conflict = ConflictAlert(
            id = "conflict-id",
            trainsInvolved = listOf("train-1", "train-2"),
            conflictType = ConflictType.POTENTIAL_COLLISION,
            severity = ConflictSeverity.CRITICAL,
            detectedAt = Instant.parse("2024-01-15T09:00:00Z"),
            estimatedImpactTime = Instant.parse("2024-01-15T09:30:00Z"),
            recommendation = "Emergency stop required",
            isResolved = false,
            resolvedAt = null,
            controllerAction = null
        )

        // Act
        val conflictDto = conflict.toDto()

        // Assert
        assertEquals("conflict-id", conflictDto.id)
        assertEquals(listOf("train-1", "train-2"), conflictDto.trainsInvolved)
        assertEquals("COLLISION_RISK", conflictDto.conflictType)
        assertEquals("CRITICAL", conflictDto.severity)
        assertEquals("2024-01-15T09:00:00Z", conflictDto.detectedAt)
        assertEquals("2024-01-15T09:30:00Z", conflictDto.estimatedImpactTime)
        assertEquals("Emergency stop required", conflictDto.aiRecommendation)
        assertFalse(conflictDto.isResolved)
        assertNull(conflictDto.resolvedAt)
        assertNull(conflictDto.controllerAction)
        assertEquals("2024-01-15T09:00:00Z", conflictDto.createdAt)
        assertNull(conflictDto.updatedAt)
    }

    @Test
    fun `toDto maps resolved ConflictAlert correctly`() {
        // Arrange
        val conflict = ConflictAlert(
            id = "conflict-id",
            trainsInvolved = listOf("train-1"),
            conflictType = ConflictType.TRACK_CONGESTION,
            severity = ConflictSeverity.MEDIUM,
            detectedAt = Instant.parse("2024-01-15T09:00:00Z"),
            estimatedImpactTime = Instant.parse("2024-01-15T09:30:00Z"),
            recommendation = "Wait for clearance",
            isResolved = true,
            resolvedAt = Instant.parse("2024-01-15T09:15:00Z"),
            controllerAction = "Manual override"
        )

        // Act
        val conflictDto = conflict.toDto()

        // Assert
        assertTrue(conflictDto.isResolved)
        assertEquals("2024-01-15T09:15:00Z", conflictDto.resolvedAt)
        assertEquals("Manual override", conflictDto.controllerAction)
        assertEquals("2024-01-15T09:00:00Z", conflictDto.createdAt)
        assertEquals("2024-01-15T09:15:00Z", conflictDto.updatedAt)
    }

    @Test
    fun `toDomainList maps list of ConflictDto correctly`() {
        // Arrange
        val conflictDtos = listOf(
            ConflictDto(
                id = "conflict-1",
                trainsInvolved = listOf("train-1"),
                conflictType = "COLLISION_RISK",
                severity = "HIGH",
                detectedAt = "2024-01-15T09:00:00Z",
                estimatedImpactTime = "2024-01-15T09:30:00Z",
                aiRecommendation = "Reduce speed",
                isResolved = false,
                resolvedAt = null,
                controllerAction = null,
                createdAt = "2024-01-15T09:00:00Z",
                updatedAt = null
            ),
            ConflictDto(
                id = "conflict-2",
                trainsInvolved = listOf("train-2", "train-3"),
                conflictType = "TRACK_OCCUPATION",
                severity = "MEDIUM",
                detectedAt = "2024-01-15T09:05:00Z",
                estimatedImpactTime = "2024-01-15T09:35:00Z",
                aiRecommendation = "Wait for clearance",
                isResolved = true,
                resolvedAt = "2024-01-15T09:10:00Z",
                controllerAction = "Manual override",
                createdAt = "2024-01-15T09:05:00Z",
                updatedAt = "2024-01-15T09:10:00Z"
            )
        )

        // Act
        val result = conflictDtos.toDomainList()

        // Assert
        assertTrue(result.isSuccess)
        val conflicts = result.getOrThrow()
        assertEquals(2, conflicts.size)
        assertEquals("conflict-1", conflicts[0].id)
        assertEquals("conflict-2", conflicts[1].id)
        assertEquals(ConflictType.POTENTIAL_COLLISION, conflicts[0].conflictType)
        assertEquals(ConflictType.TRACK_CONGESTION, conflicts[1].conflictType)
        assertFalse(conflicts[0].isResolved)
        assertTrue(conflicts[1].isResolved)
    }

    @Test
    fun `toDomainList fails if any mapping fails`() {
        // Arrange
        val conflictDtos = listOf(
            ConflictDto(
                id = "conflict-1",
                trainsInvolved = listOf("train-1"),
                conflictType = "COLLISION_RISK",
                severity = "HIGH",
                detectedAt = "2024-01-15T09:00:00Z",
                estimatedImpactTime = "2024-01-15T09:30:00Z",
                aiRecommendation = "Reduce speed",
                isResolved = false,
                resolvedAt = null,
                controllerAction = null,
                createdAt = "2024-01-15T09:00:00Z",
                updatedAt = null
            ),
            ConflictDto(
                id = "conflict-2",
                trainsInvolved = listOf("train-2"),
                conflictType = "INVALID_TYPE", // This will cause failure
                severity = "MEDIUM",
                detectedAt = "2024-01-15T09:05:00Z",
                estimatedImpactTime = "2024-01-15T09:35:00Z",
                aiRecommendation = "Test",
                isResolved = false,
                resolvedAt = null,
                controllerAction = null,
                createdAt = "2024-01-15T09:05:00Z",
                updatedAt = null
            )
        )

        // Act
        val result = conflictDtos.toDomainList()

        // Assert
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}