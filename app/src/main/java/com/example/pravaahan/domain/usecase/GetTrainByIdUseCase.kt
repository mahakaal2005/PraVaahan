package com.example.pravaahan.domain.usecase

import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.repository.TrainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving a specific train by ID with real-time updates
 * Provides reactive stream of individual train data
 */
class GetTrainByIdUseCase @Inject constructor(
    private val trainRepository: TrainRepository
) {
    /**
     * Get a specific train by ID as a reactive Flow
     * @param trainId Unique identifier for the train
     * @return Flow of train data with real-time updates, null if not found
     */
    operator fun invoke(trainId: String): Flow<Train?> {
        return trainRepository.getTrainById(trainId)
    }
}