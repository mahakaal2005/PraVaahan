package com.example.pravaahan.domain.usecase

import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.repository.ConflictRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving active conflict alerts
 * Provides real-time stream of conflicts for the dashboard
 */
class GetConflictsUseCase @Inject constructor(
    private val conflictRepository: ConflictRepository
) {
    /**
     * Gets a real-time stream of active conflicts
     * Conflicts are sorted by severity (critical first) and detection time
     */
    operator fun invoke(): Flow<List<ConflictAlert>> {
        return conflictRepository.getActiveConflicts()
    }
}