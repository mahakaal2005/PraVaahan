package com.example.pravaahan.data.repository

import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.data.dto.ConflictDto
import com.example.pravaahan.data.mapper.toDomain
import com.example.pravaahan.data.mapper.toDomainList
import com.example.pravaahan.data.mapper.toDto
import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.repository.ConflictRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ConflictRepository using Supabase
 */
@Singleton
class ConflictRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val logger: Logger,
    private val errorHandler: ErrorHandler
) : ConflictRepository {
    
    companion object {
        private const val TAG = "ConflictRepository"
        private const val CONFLICTS_TABLE = "conflicts"
    }
    
    override fun getActiveConflicts(): Flow<List<ConflictAlert>> = flow {
        logger.info(TAG, "Starting active conflicts flow")
        
        while (true) {
            try {
                val conflicts = getActiveConflictsFromDb()
                logger.debug(TAG, "Emitting ${conflicts.size} active conflicts")
                emit(conflicts)
                kotlinx.coroutines.delay(3000) // Poll every 3 seconds for conflicts
            } catch (e: Exception) {
                logger.error(TAG, "Error fetching active conflicts", e)
                emit(emptyList())
                kotlinx.coroutines.delay(10000) // Wait longer on error
            }
        }
    }.onStart {
        logger.debug(TAG, "Active conflicts flow started")
    }
    
    override fun getAllConflicts(): Flow<List<ConflictAlert>> = flow {
        logger.info(TAG, "Starting all conflicts flow")
        
        while (true) {
            try {
                val conflicts = getAllConflictsFromDb()
                logger.debug(TAG, "Emitting ${conflicts.size} total conflicts")
                emit(conflicts)
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds for all conflicts
            } catch (e: Exception) {
                logger.error(TAG, "Error fetching all conflicts", e)
                emit(emptyList())
                kotlinx.coroutines.delay(10000) // Wait longer on error
            }
        }
    }.onStart {
        logger.debug(TAG, "All conflicts flow started")
    }
    
    override fun getConflictById(id: String): Flow<ConflictAlert?> = flow {
        logger.debug(TAG, "Starting conflict flow for ID: $id")
        
        while (true) {
            try {
                val response = supabaseClient.from(CONFLICTS_TABLE)
                    .select {
                        filter {
                            eq("id", id)
                        }
                    }
                    .decodeSingleOrNull<ConflictDto>()
                
                val conflict = response?.toDomain()?.getOrNull()
                logger.debug(TAG, "Emitting conflict data: ${conflict?.id ?: "not found"}")
                emit(conflict)
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
            } catch (e: Exception) {
                logger.error(TAG, "Error fetching conflict by ID", e)
                emit(null)
                kotlinx.coroutines.delay(10000) // Wait longer on error
            }
        }
    }.onStart {
        logger.debug(TAG, "Conflict by ID flow started for: $id")
    }
    
    override suspend fun getConflictsBySeverity(severity: ConflictSeverity): Result<List<ConflictAlert>> {
        return errorHandler.safeCall("getConflictsBySeverity($severity)") {
            logger.debug(TAG, "Fetching conflicts by severity: $severity")
            
            val response = supabaseClient.from(CONFLICTS_TABLE)
                .select {
                    filter {
                        eq("severity", severity.name)
                    }
                }
                .decodeList<ConflictDto>()
            
            val conflicts = response.toDomainList().getOrThrow()
            logger.info(TAG, "Successfully fetched ${conflicts.size} conflicts with severity: $severity")
            conflicts
        }
    }
    
    override suspend fun getConflictsForTrain(trainId: String): Result<List<ConflictAlert>> {
        return errorHandler.safeCall("getConflictsForTrain($trainId)") {
            logger.debug(TAG, "Fetching conflicts for train: $trainId")
            
            val response = supabaseClient.from(CONFLICTS_TABLE)
                .select {
                    filter {
                        contains("trains_involved", listOf(trainId))
                    }
                }
                .decodeList<ConflictDto>()
            
            val conflicts = response.toDomainList().getOrThrow()
            logger.info(TAG, "Successfully fetched ${conflicts.size} conflicts for train: $trainId")
            conflicts
        }
    }
    
    override suspend fun resolveConflict(
        conflictId: String, 
        controllerAction: String
    ): Result<Unit> {
        return errorHandler.safeCall("resolveConflict($conflictId)") {
            logger.info(TAG, "Resolving conflict: $conflictId with action: $controllerAction")
            
            supabaseClient.from(CONFLICTS_TABLE)
                .update(
                    mapOf(
                        "is_resolved" to true,
                        "resolved_at" to Clock.System.now().toString(),
                        "controller_action" to controllerAction,
                        "updated_at" to Clock.System.now().toString()
                    )
                ) {
                    filter {
                        eq("id", conflictId)
                    }
                }
            
            logger.info(TAG, "Successfully resolved conflict: $conflictId")
        }
    }
    
    override suspend fun createConflict(conflict: ConflictAlert): Result<String> {
        return errorHandler.safeCall("createConflict") {
            logger.info(TAG, "Creating new conflict for trains: ${conflict.trainsInvolved}")
            
            val conflictDto = conflict.toDto()
            
            val response = supabaseClient.from(CONFLICTS_TABLE)
                .insert(conflictDto)
                .decodeSingle<ConflictDto>()
            
            logger.info(TAG, "Successfully created conflict: ${response.id}")
            response.id
        }
    }
    
    override suspend fun logControllerAction(
        conflictId: String,
        action: String,
        controllerId: String
    ): Result<Unit> {
        return errorHandler.safeCall("logControllerAction($conflictId)") {
            logger.info(TAG, "Logging controller action for conflict: $conflictId")
            
            // For now, we'll update the conflict with the action
            // In a real system, this might go to a separate audit log table
            supabaseClient.from(CONFLICTS_TABLE)
                .update(
                    mapOf(
                        "controller_action" to "$action (Controller: $controllerId)",
                        "updated_at" to Clock.System.now().toString()
                    )
                ) {
                    filter {
                        eq("id", conflictId)
                    }
                }
            
            logger.info(TAG, "Successfully logged controller action for conflict: $conflictId")
        }
    }
    
    override suspend fun acceptRecommendation(
        conflictId: String,
        controllerId: String
    ): Result<Unit> {
        return errorHandler.safeCall("acceptRecommendation($conflictId, $controllerId)") {
            logger.info(TAG, "Accepting AI recommendation for conflict: $conflictId by controller: $controllerId")
            
            supabaseClient.from(CONFLICTS_TABLE)
                .update({
                    set("status", "resolved")
                    set("resolution_type", "ai_recommendation")
                    set("controller_id", controllerId)
                    set("resolved_at", kotlinx.datetime.Clock.System.now().toString())
                }) {
                    filter {
                        eq("id", conflictId)
                    }
                }
            
            // Log the controller action
            logControllerAction(conflictId, "accepted_ai_recommendation", controllerId)
            
            logger.info(TAG, "Successfully accepted recommendation for conflict: $conflictId")
        }
    }
    
    override suspend fun submitManualOverride(
        conflictId: String,
        controllerId: String,
        overrideInstructions: String,
        reason: String
    ): Result<Unit> {
        return errorHandler.safeCall("submitManualOverride($conflictId, $controllerId)") {
            logger.info(TAG, "Submitting manual override for conflict: $conflictId by controller: $controllerId")
            
            supabaseClient.from(CONFLICTS_TABLE)
                .update({
                    set("status", "resolved")
                    set("resolution_type", "manual_override")
                    set("controller_id", controllerId)
                    set("manual_override_instructions", overrideInstructions)
                    set("override_reason", reason)
                    set("resolved_at", kotlinx.datetime.Clock.System.now().toString())
                }) {
                    filter {
                        eq("id", conflictId)
                    }
                }
            
            // Log the controller action
            logControllerAction(conflictId, "manual_override: $overrideInstructions", controllerId)
            
            logger.info(TAG, "Successfully submitted manual override for conflict: $conflictId")
        }
    }
    
    /**
     * Helper function to get active conflicts from database
     */
    private suspend fun getActiveConflictsFromDb(): List<ConflictAlert> {
        return try {
            val response = supabaseClient.from(CONFLICTS_TABLE)
                .select {
                    filter {
                        eq("is_resolved", false)
                    }
                }
                .decodeList<ConflictDto>()
            
            response.toDomainList().getOrElse { 
                logger.error(TAG, "Failed to map active conflicts to domain models")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error(TAG, "Failed to fetch active conflicts from database", e)
            emptyList()
        }
    }
    
    /**
     * Helper function to get all conflicts from database
     */
    private suspend fun getAllConflictsFromDb(): List<ConflictAlert> {
        return try {
            val response = supabaseClient.from(CONFLICTS_TABLE)
                .select()
                .decodeList<ConflictDto>()
            
            response.toDomainList().getOrElse { 
                logger.error(TAG, "Failed to map all conflicts to domain models")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error(TAG, "Failed to fetch all conflicts from database", e)
            emptyList()
        }
    }
}