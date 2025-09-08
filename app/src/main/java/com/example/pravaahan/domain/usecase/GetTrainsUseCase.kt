package com.example.pravaahan.domain.usecase

import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.repository.TrainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving train data
 * Provides real-time stream of trains for the dashboard
 */
class GetTrainsUseCase @Inject constructor(
    private val trainRepository: TrainRepository
) {
    /**
     * Gets a real-time stream of all trains
     * Trains are provided as they are received from the repository
     */
    operator fun invoke(): Flow<List<Train>> {
        return trainRepository.getTrainsRealtime()
    }
}