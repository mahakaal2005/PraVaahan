package com.example.pravaahan.core.reliability

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.model.TrainPosition
import com.example.pravaahan.domain.model.RealTimeTrainState
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * Manages offline mode operations and data synchronization
 * for railway systems when real-time connectivity is unavailable.
 */
@Singleton
class OfflineModeManager @Inject constructor(
    private val logger: Logger,
    private val networkMonitor: NetworkConnectivityMonitor
) {
    
    companion object {
        private const val TAG = "OfflineModeManager"
        private const val OFFLINE_DATA_VALIDITY_MINUTES = 30L
        private const val SYNC_RETRY_ATTEMPTS = 3
    }
    
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()
    
    private val _offlineCapabilities = MutableStateFlow(OfflineCapabilities())
    val offlineCapabilities: StateFlow<OfflineCapabilities> = _offlineCapabilities.asStateFlow()
    
    private val pendingSyncOperations = mutableListOf<SyncOperation>()
    
    init {
        monitorNetworkStatus()
    }
    
    /**
     * Monitors network status and manages offline mode transitions
     */
    private fun monitorNetworkStatus() {
        networkMonitor.isNetworkAvailable.onEach { isAvailable ->
            if (!isAvailable && !_isOfflineMode.value) {
                enterOfflineMode()
            } else if (isAvailable && _isOfflineMode.value) {
                exitOfflineMode()
            }
        }.launchIn(kotlinx.coroutines.GlobalScope)
    }
    
    /**
     * Enters offline mode with graceful degradation
     */
    private suspend fun enterOfflineMode() {
        logger.warn(TAG, "Entering offline mode")
        _isOfflineMode.value = true
        
        // Assess available offline capabilities
        val capabilities = assessOfflineCapabilities()
        _offlineCapabilities.value = capabilities
        
        logger.info(TAG, "Offline capabilities: $capabilities")
    }
    
    /**
     * Exits offline mode and initiates data synchronization
     */
    private suspend fun exitOfflineMode() {
        logger.info(TAG, "Exiting offline mode")
        
        // Synchronize pending operations
        synchronizePendingOperations()
        
        _isOfflineMode.value = false
        _offlineCapabilities.value = OfflineCapabilities()
        
        logger.info(TAG, "Successfully returned to online mode")
    }
    
    /**
     * Assesses what capabilities are available in offline mode
     */
    private suspend fun assessOfflineCapabilities(): OfflineCapabilities {
        // Simplified offline capabilities without local cache
        return OfflineCapabilities(
            canShowCachedPositions = false,
            canShowStaticMap = true,
            canLogUserActions = true,
            canShowLastKnownStatus = false,
            dataValidityMinutes = 0L,
            cachedTrainCount = 0
        )
    }
    
    /**
     * Gets cached train positions for offline display
     */
    suspend fun getCachedTrainPositions(): List<TrainPosition> {
        return if (_isOfflineMode.value) {
            logger.debug(TAG, "No cached positions available without local cache")
            emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * Gets cached train states for offline display
     */
    suspend fun getCachedTrainStates(): List<RealTimeTrainState> {
        return if (_isOfflineMode.value) {
            logger.debug(TAG, "No cached train states available without local cache")
            emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * Queues an operation for synchronization when online
     */
    suspend fun queueForSync(operation: SyncOperation) {
        pendingSyncOperations.add(operation)
        logger.debug(TAG, "Queued operation for sync: ${operation.type}")
        
        // If we're online, try to sync immediately
        if (!_isOfflineMode.value) {
            synchronizePendingOperations()
        }
    }
    
    /**
     * Synchronizes all pending operations
     */
    private suspend fun synchronizePendingOperations() {
        if (pendingSyncOperations.isEmpty()) return
        
        logger.info(TAG, "Synchronizing ${pendingSyncOperations.size} pending operations")
        
        val iterator = pendingSyncOperations.iterator()
        var syncedCount = 0
        var failedCount = 0
        
        while (iterator.hasNext()) {
            val operation = iterator.next()
            
            try {
                val success = executeSyncOperation(operation)
                if (success) {
                    iterator.remove()
                    syncedCount++
                    logger.debug(TAG, "Successfully synced operation: ${operation.type}")
                } else {
                    failedCount++
                    logger.warn(TAG, "Failed to sync operation: ${operation.type}")
                }
            } catch (e: Exception) {
                failedCount++
                logger.error(TAG, "Error syncing operation ${operation.type}: ${e.message}")
            }
        }
        
        logger.info(TAG, "Sync completed: $syncedCount successful, $failedCount failed")
    }
    
    /**
     * Executes a single sync operation
     */
    private suspend fun executeSyncOperation(operation: SyncOperation): Boolean {
        return try {
            when (operation.type) {
                SyncOperationType.USER_ACTION -> {
                    // Sync user actions like manual overrides
                    syncUserAction(operation)
                }
                SyncOperationType.STATUS_UPDATE -> {
                    // Sync status updates made offline
                    syncStatusUpdate(operation)
                }
                SyncOperationType.CONFLICT_RESOLUTION -> {
                    // Sync conflict resolution decisions
                    syncConflictResolution(operation)
                }
            }
        } catch (e: Exception) {
            logger.error(TAG, "Sync operation failed: ${e.message}")
            false
        }
    }
    
    /**
     * Syncs user action operations
     */
    private suspend fun syncUserAction(operation: SyncOperation): Boolean {
        // Implementation would sync user actions to server
        logger.debug(TAG, "Syncing user action: ${operation.data}")
        return true // Simulate success
    }
    
    /**
     * Syncs status update operations
     */
    private suspend fun syncStatusUpdate(operation: SyncOperation): Boolean {
        // Implementation would sync status updates to server
        logger.debug(TAG, "Syncing status update: ${operation.data}")
        return true // Simulate success
    }
    
    /**
     * Syncs conflict resolution operations
     */
    private suspend fun syncConflictResolution(operation: SyncOperation): Boolean {
        // Implementation would sync conflict resolutions to server
        logger.debug(TAG, "Syncing conflict resolution: ${operation.data}")
        return true // Simulate success
    }
    
    /**
     * Gets offline mode statistics
     */
    fun getOfflineStats(): OfflineStats {
        return OfflineStats(
            isOfflineMode = _isOfflineMode.value,
            pendingSyncOperations = pendingSyncOperations.size,
            capabilities = _offlineCapabilities.value,
            networkQuality = networkMonitor.getCurrentNetworkQuality()
        )
    }
    
    /**
     * Forces offline mode for testing
     */
    suspend fun forceOfflineMode(enabled: Boolean) {
        if (enabled) {
            enterOfflineMode()
        } else {
            exitOfflineMode()
        }
        logger.info(TAG, "Forced offline mode: $enabled")
    }
    
    /**
     * Clears all pending sync operations
     */
    fun clearPendingOperations() {
        val count = pendingSyncOperations.size
        pendingSyncOperations.clear()
        logger.info(TAG, "Cleared $count pending sync operations")
    }
}

/**
 * Offline capabilities assessment
 */
data class OfflineCapabilities(
    val canShowCachedPositions: Boolean = false,
    val canShowStaticMap: Boolean = false,
    val canLogUserActions: Boolean = false,
    val canShowLastKnownStatus: Boolean = false,
    val dataValidityMinutes: Long = 0L,
    val cachedTrainCount: Int = 0
)

/**
 * Sync operation for offline-to-online synchronization
 */
data class SyncOperation(
    val type: SyncOperationType,
    val data: Map<String, Any>,
    val timestamp: Instant = Clock.System.now(),
    val retryCount: Int = 0
)

/**
 * Types of sync operations
 */
enum class SyncOperationType {
    USER_ACTION,
    STATUS_UPDATE,
    CONFLICT_RESOLUTION
}

/**
 * Offline mode statistics
 */
data class OfflineStats(
    val isOfflineMode: Boolean,
    val pendingSyncOperations: Int,
    val capabilities: OfflineCapabilities,
    val networkQuality: NetworkQuality
)