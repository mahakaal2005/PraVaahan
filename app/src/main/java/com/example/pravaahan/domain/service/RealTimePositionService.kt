package com.example.pravaahan.domain.service

import com.example.pravaahan.core.monitoring.ConnectionStatus
import com.example.pravaahan.core.monitoring.DataQuality
import com.example.pravaahan.domain.model.TrainPosition
import kotlinx.coroutines.flow.Flow

/**
 * Service interface for real-time train position tracking and streaming.
 * Handles real-time subscriptions to position updates from various data sources.
 * 
 * Enhanced with circuit breaker pattern, comprehensive monitoring, and security validation.
 */
interface RealTimePositionService {
    
    /**
     * Subscribe to real-time position updates for all trains in a specific section.
     * 
     * @param sectionId The railway section ID to monitor
     * @return Flow of train positions for the section, updated in real-time
     */
    fun subscribeToSectionUpdates(sectionId: String): Flow<List<TrainPosition>>
    
    /**
     * Subscribe to real-time position updates for a specific train.
     * 
     * @param trainId The specific train ID to monitor
     * @return Flow of position updates for the train
     */
    fun subscribeToTrainUpdates(trainId: String): Flow<TrainPosition>
    
    /**
     * Update a train's position in the real-time system.
     * 
     * @param position The new train position to update
     * @return Result indicating success or failure
     */
    suspend fun updateTrainPosition(position: TrainPosition): Result<Unit>
    
    /**
     * Get the current connection status for real-time communication.
     * 
     * @return Flow of connection status updates
     */
    fun getConnectionStatus(): Flow<ConnectionStatus>
    
    /**
     * Get data quality indicators for the real-time position data.
     * 
     * @return Flow of data quality metrics
     */
    fun getDataQuality(): Flow<DataQuality>
    
    /**
     * Start the real-time service and establish connections.
     */
    suspend fun start()
    
    /**
     * Stop the real-time service and clean up resources.
     */
    suspend fun stop()
}