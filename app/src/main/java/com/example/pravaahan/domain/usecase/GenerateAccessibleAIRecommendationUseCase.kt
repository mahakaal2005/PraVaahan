package com.example.pravaahan.domain.usecase

import com.example.pravaahan.core.ai.AccessibilityAwareAIService
import com.example.pravaahan.core.ai.AccessibleAIRecommendation
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.repository.ConflictRepository
import com.example.pravaahan.domain.repository.TrainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case for generating AI recommendations with full accessibility integration
 */
class GenerateAccessibleAIRecommendationUseCase @Inject constructor(
    private val trainRepository: TrainRepository,
    private val conflictRepository: ConflictRepository,
    private val accessibilityAwareAIService: AccessibilityAwareAIService
) {
    
    /**
     * Generate AI recommendation with accessibility features
     */
    suspend operator fun invoke(): Flow<AccessibleAIRecommendation?> {
        return combine(
            trainRepository.getTrainsRealtime(),
            conflictRepository.getActiveConflicts()
        ) { trains, conflicts ->
            if (trains.isNotEmpty() || conflicts.isNotEmpty()) {
                accessibilityAwareAIService.generateAccessibleRecommendation(trains, conflicts)
            } else {
                null
            }
        }
    }
}