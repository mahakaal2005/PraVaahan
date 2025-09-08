package com.example.pravaahan.domain.usecase

import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.repository.ConflictRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving a specific conflict by ID with real-time updates
 * Provides reactive stream of individual conflict data
 */
class GetConflictByIdUseCase @Inject constructor(
    private val conflictRepository: ConflictRepository
) {
    /**
     * Get a specific conflict by ID as a reactive Flow
     * @param conflictId Unique identifier for the conflict
     * @return Flow of conflict data with real-time updates, null if not found
     */
    operator fun invoke(conflictId: String): Flow<ConflictAlert?> {
        return conflictRepository.getConflictById(conflictId)
    }
}