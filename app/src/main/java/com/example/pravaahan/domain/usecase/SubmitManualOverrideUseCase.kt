package com.example.pravaahan.domain.usecase

import com.example.pravaahan.domain.repository.ConflictRepository
import javax.inject.Inject

/**
 * Use case for submitting manual override for conflict resolution
 * Handles the business logic for controller manual interventions
 */
class SubmitManualOverrideUseCase @Inject constructor(
    private val conflictRepository: ConflictRepository
) {
    /**
     * Submit a manual override for a specific conflict
     * @param conflictId Unique identifier for the conflict
     * @param controllerId ID of the controller submitting the override
     * @param overrideInstructions Manual instructions from the controller
     * @param reason Reason for overriding the AI recommendation
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        conflictId: String,
        controllerId: String,
        overrideInstructions: String,
        reason: String = ""
    ): Result<Unit> {
        return try {
            // Validate input
            if (overrideInstructions.isBlank()) {
                return Result.failure(IllegalArgumentException("Override instructions cannot be empty"))
            }
            
            conflictRepository.submitManualOverride(
                conflictId = conflictId,
                controllerId = controllerId,
                overrideInstructions = overrideInstructions,
                reason = reason
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}