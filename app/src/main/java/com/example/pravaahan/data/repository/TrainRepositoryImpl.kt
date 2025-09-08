package com.example.pravaahan.data.repository

import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.data.dto.TrainDto
import com.example.pravaahan.data.mapper.toDomain
import com.example.pravaahan.data.mapper.toDomainList
import com.example.pravaahan.domain.model.Location
import com.example.pravaahan.domain.model.Train
import com.example.pravaahan.domain.model.TrainStatus
import com.example.pravaahan.domain.repository.TrainRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TrainRepository using Supabase
 */
@Singleton
class TrainRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val logger: Logger,
    private val errorHandler: ErrorHandler
) : TrainRepository {
    
    companion object {
        private const val TAG = "TrainRepository"
        private const val TRAINS_TABLE = "trains"
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