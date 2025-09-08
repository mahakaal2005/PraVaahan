package com.example.pravaahan.fake

import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.ConflictResolution
import com.example.pravaahan.domain.model.ConflictSeverity
import com.example.pravaahan.domain.model.ControllerAction
import com.example.pravaahan.domain.repository.ConflictRepository
import com.example.pravaahan.util.TestDataFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of ConflictRepository for testing
 * Provides realistic conflict scenarios and controllable behavior
 */
class FakeConflictRepository : ConflictRepository {
    
    private val _conflicts = MutableStateFlow<List<ConflictAlert>>(emptyList())
    private val _controllerActions = mutableListOf<ControllerAction>()
    private val _errors = mutableMapOf<String, Exception>()
    private var _shouldThrowError = false
    private var _networkDelay = 0L
    
    init {
        // Initialize with default test conflict scenarios
        _conflicts.value = TestDataFactory.createConflictScenarios()
    }
    
    override fun getActiveConflicts(): Flow<List<ConflictAlert>> {
        return _conflicts.asStateFlow()
    }
    
    override fun getAllConflicts(): Flow<List<ConflictAlert>> {
        return _conflicts.asStateFlow()
    }
    
    override fun getConflictById(id: String): Flow<ConflictAlert?> {
        return _conflicts.map { conflicts -> conflicts.find { it.id == id } }
    }
    
    override suspend fun getConflictsBySeverity(severity: ConflictSeverity): Result<List<ConflictAlert>> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to get conflicts by severity"))
        }
        
        val filteredConflicts = _conflicts.value.filter { it.severity == severity }
        return Result.success(filteredConflicts)
    }
    
    override suspend fun getConflictsForTrain(trainId: String): Result<List<ConflictAlert>> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to get conflicts for train"))
        }
        
        val filteredConflicts = _conflicts.value.filter { it.trainsInvolved.contains(trainId) }
        return Result.success(filteredConflicts)
    }
    
    override suspend fun resolveConflict(conflictId: String, controllerAction: String): Result<Unit> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to resolve conflict"))
        }
        
        // Remove resolved conflict from active conflicts
        _conflicts.value = _conflicts.value.filter { it.id != conflictId }
        
        // Log the resolution action
        val action = ControllerAction(
            conflictId = conflictId,
            action = controllerAction,
            timestamp = kotlinx.datetime.Clock.System.now()
        )
        _controllerActions.add(action)
        
        return Result.success(Unit)
    }
    
    override suspend fun createConflict(conflict: ConflictAlert): Result<String> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to create conflict"))
        }
        
        _conflicts.value = _conflicts.value + conflict
        return Result.success(conflict.id)
    }
    
    override suspend fun acceptRecommendation(conflictId: String, controllerId: String): Result<Unit> {
        return resolveConflict(conflictId, "ACCEPT_RECOMMENDATION by $controllerId")
    }
    
    override suspend fun submitManualOverride(
        conflictId: String,
        controllerId: String,
        overrideInstructions: String,
        reason: String
    ): Result<Unit> {
        return resolveConflict(conflictId, "MANUAL_OVERRIDE by $controllerId: $overrideInstructions (Reason: $reason)")
    }
    
    override suspend fun logControllerAction(
        conflictId: String,
        action: String,
        controllerId: String
    ): Result<Unit> {
        simulateNetworkDelay()
        
        if (_shouldThrowError) {
            return Result.failure(_errors["api_error"] ?: RuntimeException("Failed to log controller action"))
        }
        
        val controllerAction = ControllerAction(
            conflictId = conflictId,
            action = action,
            timestamp = kotlinx.datetime.Clock.System.now(),
            controllerId = controllerId
        )
        _controllerActions.add(controllerAction)
        return Result.success(Unit)
    }
    
    // Test control methods
    
    /**
     * Sets the conflicts data for testing
     */
    fun setConflicts(conflicts: List<ConflictAlert>) {
        _conflicts.value = conflicts
    }
    
    /**
     * Adds a single conflict to the current list
     */
    fun addConflict(conflict: ConflictAlert) {
        _conflicts.value = _conflicts.value + conflict
    }
    
    /**
     * Removes a conflict from the current list
     */
    fun removeConflict(conflictId: String) {
        _conflicts.value = _conflicts.value.filter { it.id != conflictId }
    }
    
    /**
     * Simulates network errors for testing error handling
     */
    fun setShouldThrowError(shouldThrow: Boolean, error: Exception? = null) {
        _shouldThrowError = shouldThrow
        if (error != null) {
            _errors["test_error"] = error
        }
    }
    
    /**
     * Sets network delay for testing loading states
     */
    fun setNetworkDelay(delayMs: Long) {
        _networkDelay = delayMs
    }
    
    /**
     * Clears all data and resets to initial state
     */
    fun reset() {
        _conflicts.value = TestDataFactory.createConflictScenarios()
        _controllerActions.clear()
        _errors.clear()
        _shouldThrowError = false
        _networkDelay = 0L
    }
    
    /**
     * Simulates new conflict detection
     */
    fun simulateNewConflict(conflict: ConflictAlert) {
        _conflicts.value = _conflicts.value + conflict
    }
    
    /**
     * Gets current conflicts for verification in tests
     */
    fun getCurrentConflicts(): List<ConflictAlert> = _conflicts.value
    
    /**
     * Gets logged controller actions for verification in tests
     */
    fun getControllerActions(): List<ControllerAction> = _controllerActions.toList()
    
    /**
     * Clears controller action history
     */
    fun clearControllerActions() {
        _controllerActions.clear()
    }
    
    private suspend fun simulateNetworkDelay() {
        if (_networkDelay > 0) {
            kotlinx.coroutines.delay(_networkDelay)
        }
    }
}