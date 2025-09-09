package com.example.pravaahan.domain.repository

import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainStatus
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.model.TrainPosition
import com.example.pravaahan.domain.model.RealTimeTrainState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for train data operations
 */
interface TrainRepository {
    /**
     * Get real-time stream of all trains
     */
    fun getTrainsRealtime(): Flow<List<Train>>
    
    /**
     * Get a specific train by ID as a Flow
     */
    fun getTrainById(id: String): Flow<Train?>
    
    /**
     * Update train status
     */
    suspend fun updateTrainStatus(id: String, status: TrainStatus): Result<Unit>
    
    /**
     * Update train location
     */
    suspend fun updateTrainLocation(id: String, location: Location): Result<Unit>
    
    /**
     * Get trains by status
     */
    suspend fun getTrainsByStatus(status: TrainStatus): Result<List<Train>>
    
    /**
     * Get trains in a specific section
     */
    suspend fun getTrainsInSection(sectionId: String): Result<List<Train>>
    
    // NEW: Real-time position methods
    
    /**
     * Get real-time train positions for a specific section
     */
    fun getTrainPositionsRealtime(sectionId: String): Flow<List<TrainPosition>>
    
    /**
     * Get real-time train states combining train metadata with position data
     */
    fun getRealTimeTrainStates(sectionId: String): Flow<List<RealTimeTrainState>>
    
    /**
     * Update a train's real-time position
     */
    suspend fun updateTrainPosition(position: TrainPosition): Result<Unit>
}