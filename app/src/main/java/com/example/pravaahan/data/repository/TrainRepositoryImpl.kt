package com.example.pravaahan.data.repository

import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.logging.logTrainOperation
import com.example.pravaahan.data.dto.TrainDto
import com.example.pravaahan.data.mapper.toDomain
import com.example.pravaahan.data.mapper.toDomainList
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainStatus
import com.example.pravaahan.domain.model.TrainPosition
import com.example.pravaahan.domain.model.RealTimeTrainState
import com.example.pravaahan.domain.model.DataQualityIndicators
import com.example.pravaahan.domain.model.ConnectionStatus
import com.example.pravaahan.domain.model.ConnectionState
import com.example.pravaahan.domain.model.NetworkQuality
import com.example.pravaahan.domain.model.ValidationStatus
import com.example.pravaahan.domain.repository.TrainRepository
import com.example.pravaahan.domain.service.RealTimePositionService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * Implementation of TrainRepository using Supabase
 */
@Singleton
class TrainRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val logger: Logger,
    private val errorHandler: ErrorHandler,
    // NEW: Add real-time service dependency
    private val realTimeService: RealTimePositionService
) : TrainRepository {
    
    companion object {
        private const val TAG = "TrainRepository" // KEEP existing tag for consistency
        private const val TRAINS_TABLE = "trains"
        private const val POSITIONS_TABLE = "train_positions" // NEW table
    }
    
    override fun getTrainsRealtime(): Flow<List<Train>> = flow {
        logger.info(TAG, "Starting train data flow")
        
        // For now, use a simple polling approach to avoid complex real-time setup
        while (true) {
            try {
                val trains = getAllTrains()
                logger.debug(TAG, "Emitting ${trains.size} trains")
                emit(trains)
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
            } catch (e: Exception) {
                logger.error(TAG, "Error fetching trains", e)
                emit(emptyList()) // Emit empty list on error
                kotlinx.coroutines.delay(10000) // Wait longer on error
            }
        }
    }.onStart {
        logger.debug(TAG, "Train flow started")
    }
    
    override fun getTrainById(id: String): Flow<Train?> = flow {
        logger.debug(TAG, "Starting train data flow for ID: $id")
        
        // Simple polling approach for single train
        while (true) {
            try {
                val response = supabaseClient.from(TRAINS_TABLE)
                    .select {
                        filter {
                            eq("id", id)
                        }
                    }
                    .decodeSingleOrNull<TrainDto>()
                
                val train = response?.toDomain()?.getOrNull()
                logger.debug(TAG, "Emitting train data: ${train?.name ?: "not found"}")
                emit(train)
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
            } catch (e: Exception) {
                logger.error(TAG, "Error fetching train by ID", e)
                emit(null)
                kotlinx.coroutines.delay(10000) // Wait longer on error
            }
        }
    }.onStart {
        logger.debug(TAG, "Train by ID flow started for: $id")
    }
    
    override suspend fun updateTrainStatus(id: String, status: TrainStatus): Result<Unit> {
        return errorHandler.safeCall("updateTrainStatus($id, $status)") {
            logger.info(TAG, "Updating train status: $id -> $status")
            
            supabaseClient.from(TRAINS_TABLE)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updated_at" to kotlinx.datetime.Clock.System.now().toString()
                    )
                ) {
                    filter {
                        eq("id", id)
                    }
                }
            
            logger.info(TAG, "Successfully updated train status: $id")
        }
    }
    
    override suspend fun updateTrainLocation(id: String, location: Location): Result<Unit> {
        return errorHandler.safeCall("updateTrainLocation($id)") {
            logger.info(TAG, "Updating train location: $id")
            
            supabaseClient.from(TRAINS_TABLE)
                .update(
                    mapOf(
                        "current_latitude" to location.latitude,
                        "current_longitude" to location.longitude,
                        "section_id" to location.sectionId,
                        "updated_at" to kotlinx.datetime.Clock.System.now().toString()
                    )
                ) {
                    filter {
                        eq("id", id)
                    }
                }
            
            logger.info(TAG, "Successfully updated train location: $id")
        }
    }
    
    override suspend fun getTrainsByStatus(status: TrainStatus): Result<List<Train>> {
        return errorHandler.safeCall("getTrainsByStatus($status)") {
            logger.debug(TAG, "Fetching trains by status: $status")
            
            val response = supabaseClient.from(TRAINS_TABLE)
                .select {
                    filter {
                        eq("status", status.name)
                    }
                }
                .decodeList<TrainDto>()
            
            val trains = response.toDomainList().getOrThrow()
            logger.info(TAG, "Successfully fetched ${trains.size} trains with status: $status")
            trains
        }
    }
    
    override suspend fun getTrainsInSection(sectionId: String): Result<List<Train>> {
        return errorHandler.safeCall("getTrainsInSection($sectionId)") {
            logger.debug(TAG, "Fetching trains in section: $sectionId")
            
            val response = supabaseClient.from(TRAINS_TABLE)
                .select {
                    filter {
                        eq("section_id", sectionId)
                    }
                }
                .decodeList<TrainDto>()
            
            val trains = response.toDomainList().getOrThrow()
            logger.info(TAG, "Successfully fetched ${trains.size} trains in section: $sectionId")
            trains
        }
    }
    
    // NEW: Real-time position methods (ADD to existing class)
    
    override fun getTrainPositionsRealtime(sectionId: String): Flow<List<TrainPosition>> {
        logger.info(TAG, "Starting real-time position subscription for section: $sectionId")
        
        return realTimeService.subscribeToSectionUpdates(sectionId)
            .catch { exception ->
                logger.error(TAG, "Real-time position stream error for section $sectionId", exception)
                emit(emptyList())
            }
            .onStart {
                logger.debug(TAG, "Real-time position flow started for section: $sectionId")
            }
    }
    
    override fun getRealTimeTrainStates(sectionId: String): Flow<List<RealTimeTrainState>> {
        logger.info(TAG, "Starting combined real-time train states for section: $sectionId")
        
        return combine(
            getTrainsRealtime(), // Use existing method
            getTrainPositionsRealtime(sectionId), // Use new method
            realTimeService.getConnectionStatus(),
            realTimeService.getDataQuality()
        ) { trains, positions, connectionStatus, dataQuality ->
            logger.debug(TAG, "Combining ${trains.size} trains with ${positions.size} positions")
            
            // Sort positions by timestamp to handle out-of-order updates
            val sortedPositions = positions.sortedBy { it.timestamp }
            
            trains.mapNotNull { train ->
                val currentPosition = findLatestValidPosition(train.id, sortedPositions)
                currentPosition?.let { position ->
                    // Validate position data consistency
                    if (isPositionValid(position, train)) {
                        logger.logTrainOperation(TAG, "position_update", train.id, 
                            "lat=${position.latitude}, lng=${position.longitude}, speed=${position.speed}")
                        
                        RealTimeTrainState(
                            train = train,
                            currentPosition = position,
                            connectionStatus = com.example.pravaahan.data.mapper.RealTimeStateMapper.toDomainConnectionStatus(connectionStatus),
                            dataQuality = com.example.pravaahan.data.mapper.RealTimeStateMapper.toDomainDataQuality(dataQuality),
                            lastUpdate = position.timestamp
                        )
                    } else {
                        logger.warn(TAG, "Invalid position data for train ${train.id}, using degraded state")
                        // Return state with degraded status
                        val degradedQuality = com.example.pravaahan.data.mapper.RealTimeStateMapper.toDomainDataQuality(dataQuality)
                        RealTimeTrainState(
                            train = train,
                            currentPosition = null,
                            connectionStatus = com.example.pravaahan.data.mapper.RealTimeStateMapper.toDomainConnectionStatus(connectionStatus),
                            dataQuality = degradedQuality.copy(accuracy = 0.3),
                            lastUpdate = Clock.System.now()
                        )
                    }
                }
            }.filterNotNull()
        }.catch { exception ->
            logger.error(TAG, "Error combining real-time train data for section $sectionId", exception)
            emit(emptyList())
        }
    }
    
    override suspend fun updateTrainPosition(position: TrainPosition): Result<Unit> {
        return errorHandler.safeCall("updateTrainPosition(${position.trainId})") {
            logger.logTrainOperation(TAG, "update_position", position.trainId, 
                "section=${position.sectionId}, speed=${position.speed}")
            
            realTimeService.updateTrainPosition(position).getOrThrow()
            
            logger.info(TAG, "Successfully updated position for train: ${position.trainId}")
        }
    }
    
    private fun findLatestValidPosition(trainId: String, positions: List<TrainPosition>): TrainPosition? {
        return positions
            .filter { it.trainId == trainId }
            .maxByOrNull { it.timestamp }
    }
    
    private fun isPositionValid(position: TrainPosition, train: Train): Boolean {
        // Validate position data consistency
        return position.latitude in -90.0..90.0 &&
               position.longitude in -180.0..180.0 &&
               position.speed >= 0.0 &&
               position.speed <= 300.0 && // Max realistic train speed
               position.timestamp <= Clock.System.now().plus(1.minutes) && // Not too far in future
               position.timestamp >= Clock.System.now().minus(10.minutes) // Not too old
    }
    
    /**
     * Helper function to get all trains
     */
    private suspend fun getAllTrains(): List<Train> {
        return try {
            val response = supabaseClient.from(TRAINS_TABLE)
                .select()
                .decodeList<TrainDto>()
            
            response.toDomainList().getOrElse { 
                logger.error(TAG, "Failed to map trains to domain models")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error(TAG, "Failed to fetch all trains", e)
            emptyList()
        }
    }
}