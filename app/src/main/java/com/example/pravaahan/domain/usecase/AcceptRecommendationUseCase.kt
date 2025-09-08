package com.example.pravaahan.domain.usecase

import com.example.pravaahan.domain.repository.ConflictRepository
import javax.inject.Inject

/**
 * Use case for accepting AI recommendation for conflict resolution
 * Handles the business logic for accepting system recommendations
 */
class AcceptRecommendationUseCase @Inject constructor(
    private val conflictRepository: ConflictRepository
) {
    /**
     * Accept the AI recommendation for a specific conflict
     * @param conflictId Unique identifier for the conflict
     * @param controllerId ID of the controller accepting the recommendation
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        conflictId: String,
        controllerId: String
    ): Result<Unit> {
        return try {
            conflictRepository.acceptRecommendation(conflictId, controllerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}