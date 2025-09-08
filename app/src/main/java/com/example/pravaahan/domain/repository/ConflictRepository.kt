package com.example.pravaahan.domain.repository

import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictSeverity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for conflict alert operations
 */
interface ConflictRepository {
    /**
     * Get real-time stream of active conflicts
     */
    fun getActiveConflicts(): Flow<List<ConflictAlert>>
    
    /**
     * Get all conflicts (active and resolved)
     */
    fun getAllConflicts(): Flow<List<ConflictAlert>>
    
    /**
     * Get a specific conflict by ID as a Flow
     */
    fun getConflictById(id: String): Flow<ConflictAlert?>
    
    /**
     * Get conflicts by severity level
     */
    suspend fun getConflictsBySeverity(severity: ConflictSeverity): Result<List<ConflictAlert>>
    
    /**
     * Get conflicts involving a specific train
     */
    suspend fun getConflictsForTrain(trainId: String): Result<List<ConflictAlert>>
    
    /**
     * Resolve a conflict with controller action
     */
    suspend fun resolveConflict(
        conflictId: String, 
        controllerAction: String
    ): Result<Unit>
    
    /**
     * Create a new conflict alert
     */
    suspend fun createConflict(conflict: ConflictAlert): Result<String>
    
    /**
     * Accept AI recommendation for conflict resolution
     */
    suspend fun acceptRecommendation(
        conflictId: String,
        controllerId: String
    ): Result<Unit>
    
    /**
     * Submit manual override for conflict resolution
     */
    suspend fun submitManualOverride(
        conflictId: String,
        controllerId: String,
        overrideInstructions: String,
        reason: String
    ): Result<Unit>
    
    /**
     * Log controller action for audit trail
     */
    suspend fun logControllerAction(
        conflictId: String,
        action: String,
        controllerId: String
    ): Result<Unit>
}