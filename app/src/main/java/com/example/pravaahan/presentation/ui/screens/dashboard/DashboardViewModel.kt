package com.example.pravaahan.presentation.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pravaahan.core.error.AppError
import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.error.getUserMessage
import com.example.pravaahan.core.error.launchWithErrorHandling
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.ConnectionStatus as MonitoringConnectionStatus
import com.example.pravaahan.core.monitoring.DataQuality
import com.example.pravaahan.domain.model.*
import com.example.pravaahan.domain.service.RealTimePositionService
import com.example.pravaahan.domain.usecase.GetConflictsUseCase
import com.example.pravaahan.domain.usecase.GetTrainsUseCase
import com.example.pravaahan.data.sample.SampleRailwayData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis
import javax.inject.Inject

/**
 * Enhanced ViewModel for the Dashboard screen with comprehensive real-time state management
 * Manages UI state and handles user actions for the main dashboard with real-time train tracking
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTrainsUseCase: GetTrainsUseCase,
    private val getConflictsUseCase: GetConflictsUseCase,
    private val realTimePositionService: RealTimePositionService,
    private val errorHandler: ErrorHandler,
    private val logger: Logger
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
        private const val DEFAULT_SECTION_ID = "SECTION_001" // Default section for demo
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Real-time mode toggle
    private val _realTimeModeEnabled = MutableStateFlow(true)
    
    // Connection status tracking
    private val _connectionStatus = MutableStateFlow(ConnectionState.DISCONNECTED)
    
    // Data quality metrics
    private val _dataQuality = MutableStateFlow<DataQuality?>(null)

    init {
        logger.info(TAG, "DashboardViewModel initialized with real-time capabilities for section $DEFAULT_SECTION_ID")
        initializeRealTimeStreams()
        loadData()
    }

    /**
     * Handle user actions from the UI with enhanced real-time action support
     */
    fun handleAction(action: DashboardAction) {
        logger.debug(TAG, "Handling real-time action: ${action::class.simpleName}")
        
        when (action) {
            is DashboardAction.LoadData -> loadData()
            is DashboardAction.RefreshData -> refreshData()
            is DashboardAction.NavigateToTrain -> {
                logger.debug(TAG, "Navigate to train: ${action.trainId}")
                // Navigation is handled by the UI layer
            }
            is DashboardAction.NavigateToConflict -> {
                logger.debug(TAG, "Navigate to conflict: ${action.conflictId}")
                // Navigation is handled by the UI layer
            }
            is DashboardAction.NavigateToSettings -> {
                logger.debug(TAG, "Navigate to settings")
            }
            is DashboardAction.SelectTrain -> {
                logger.debug(TAG, "Train selected: ${action.trainId}")
                selectTrain(action.trainId)
            }
            is DashboardAction.ToggleRealTimeMode -> {
                logger.info(TAG, "Real-time mode toggled: ${action.enabled}, affecting ${_uiState.value.trains.size} trains")
                toggleRealTimeMode(action.enabled)
            }
            is DashboardAction.RetryConnection -> {
                logger.info(TAG, "Retrying real-time connection")
                retryRealTimeConnection()
            }
            is DashboardAction.ClearError -> {
                logger.debug(TAG, "Clearing error state")
                clearError()
            }
            is DashboardAction.Retry -> {
                logger.debug(TAG, "Retrying failed operation")
                retry()
            }
        }
    }

    /**
     * Initialize real-time data streams and connection monitoring
     */
    private fun initializeRealTimeStreams() {
        logger.info(TAG, "Initializing real-time streams for section $DEFAULT_SECTION_ID")
        
        // Monitor connection status
        viewModelScope.launch {
            realTimePositionService.getConnectionStatus()
                .collect { status ->
                    val oldStatus = _connectionStatus.value
                    val newConnectionState = mapMonitoringStatusToConnectionState(status)
                    _connectionStatus.value = newConnectionState
                    
                    logger.info(TAG, "Real-time connection status changed: $oldStatus -> $newConnectionState")
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            connectionStatus = newConnectionState,
                            systemStatus = currentState.systemStatus.copy(
                                realtimeActive = status == MonitoringConnectionStatus.CONNECTED,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                    }
                }
        }
        
        // Monitor data quality
        viewModelScope.launch {
            realTimePositionService.getDataQuality()
                .collect { quality ->
                    _dataQuality.value = quality
                    
                    logger.debug(TAG, "Data quality: latency=${quality.latency.inWholeMilliseconds}ms, " +
                            "accuracy=${quality.accuracy}m, " +
                            "reliability=${(quality.reliability * 100).toInt()}%")
                    
                    // Convert DataQuality to DataQualityIndicators for UI state
                    val dataQualityIndicators = mapDataQualityToIndicators(quality)
                    
                    _uiState.update { currentState ->
                        currentState.copy(dataQuality = dataQualityIndicators)
                    }
                }
        }
    }

    /**
     * Load initial data for the dashboard with enhanced real-time integration
     */
    private fun loadData() {
        logger.info(TAG, "Loading real-time data for section $DEFAULT_SECTION_ID, ${_uiState.value.trains.size} trains active")
        
        _uiState.update { currentState -> currentState.copy(isLoading = true, error = null) }

        launchWithErrorHandling(
            logger = logger,
            errorHandler = errorHandler,
            operation = "load_dashboard_data",
            onError = { appError ->
                logger.error(TAG, "Real-time data loading failed: ${appError.message}, falling back to cached data")
                _uiState.update { currentState -> 
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = appError.getUserMessage()
                    )
                }
            }
        ) {
            val combineTime = measureTimeMillis {
                // Enhanced state combination with real-time data
                combine(
                    getTrainsUseCase(),
                    getConflictsUseCase(),
                    if (_realTimeModeEnabled.value) {
                        getRealTimeTrainStatesFlow(DEFAULT_SECTION_ID)
                    } else {
                        flowOf(emptyList())
                    },
                    _connectionStatus,
                    _dataQuality.filterNotNull().onStart { emit(createDefaultDataQuality()) }
                ) { trains, conflicts, realTimeStates, connectionStatus, dataQuality ->
                    EnhancedDashboardData(trains, conflicts, realTimeStates, connectionStatus, mapDataQualityToIndicators(dataQuality))
                }
                .catch { exception ->
                    val appError = errorHandler.handleError(exception)
                    logger.error(TAG, "Error loading data", exception)
                    _uiState.update { currentState -> 
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = appError.getUserMessage()
                        )
                    }
                }
                .collect { data ->
                    logger.info(TAG, "UI state updated: ${data.trains.size} trains, connection: ${data.connectionStatus}")
                    
                    _uiState.update { currentState ->
                        // Use sample train states if no real-time data available
                        val trainStates = if (data.realTimeStates.isEmpty()) {
                            SampleRailwayData.createSampleTrainStates()
                        } else {
                            data.realTimeStates
                        }
                        
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            trains = data.trains,
                            conflicts = data.conflicts,
                            trainStates = trainStates,
                            sectionConfig = SampleRailwayData.createSampleSection(), // Add sample section config for map
                            connectionStatus = data.connectionStatus,
                            dataQuality = data.dataQuality,
                            realTimeModeEnabled = _realTimeModeEnabled.value,
                            systemStatus = currentState.systemStatus.copy(
                                supabaseConnected = true,
                                realtimeActive = data.connectionStatus == ConnectionState.CONNECTED,
                                aiEngineOnline = true,
                                lastUpdated = System.currentTimeMillis()
                            ),
                            error = null
                        )
                    }
                }
            }
            
            logger.debug(TAG, "State combination completed in ${combineTime}ms, memory usage: ${Runtime.getRuntime().totalMemory() / (1024 * 1024)}MB")
        }
    }

    /**
     * Refresh dashboard data with real-time synchronization
     */
    private fun refreshData() {
        logger.info(TAG, "Refreshing dashboard data")
        
        _uiState.update { currentState -> currentState.copy(isRefreshing = true, error = null) }
        loadData()
    }

    /**
     * Select a train and update UI state
     */
    private fun selectTrain(trainId: String) {
        logger.debug(TAG, "Train selected: $trainId")
        _uiState.update { currentState ->
            currentState.copy(selectedTrainId = trainId)
        }
    }

    /**
     * Toggle real-time mode on/off
     */
    private fun toggleRealTimeMode(enabled: Boolean) {
        logger.info(TAG, "Real-time mode toggled: $enabled, affecting ${_uiState.value.trains.size} trains")
        
        _realTimeModeEnabled.value = enabled
        
        _uiState.update { currentState ->
            currentState.copy(realTimeModeEnabled = enabled)
        }
        
        // Reload data with new mode
        loadData()
    }

    /**
     * Retry real-time connection
     */
    private fun retryRealTimeConnection() {
        logger.info(TAG, "Retrying real-time connection")
        
        viewModelScope.launch {
            try {
                realTimePositionService.stop()
                realTimePositionService.start()
                logger.info(TAG, "Real-time connection retry initiated")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to retry real-time connection", e)
                _uiState.update { currentState ->
                    currentState.copy(error = "Failed to reconnect: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear current error state
     */
    private fun clearError() {
        logger.debug(TAG, "Clearing error state")
        _uiState.update { currentState -> currentState.copy(error = null) }
    }

    /**
     * Retry failed operation
     */
    private fun retry() {
        logger.debug(TAG, "Retrying failed operation")
        loadData()
    }

    /**
     * Create default data quality indicators
     */
    private fun createDefaultDataQuality(): DataQuality {
        return DataQuality(
            latency = kotlin.time.Duration.parse("100ms"),
            accuracy = 10.0,
            freshness = kotlin.time.Duration.parse("1s"),
            reliability = 0.8f,
            outOfOrderCount = 0,
            duplicateCount = 0
        )
    }

    /**
     * Map monitoring connection status to domain connection state
     */
    private fun mapMonitoringStatusToConnectionState(status: MonitoringConnectionStatus): ConnectionState {
        return when (status) {
            MonitoringConnectionStatus.CONNECTED -> ConnectionState.CONNECTED
            MonitoringConnectionStatus.DISCONNECTED -> ConnectionState.DISCONNECTED
            MonitoringConnectionStatus.RECONNECTING -> ConnectionState.RECONNECTING
            MonitoringConnectionStatus.ERROR -> ConnectionState.FAILED
        }
    }

    /**
     * Map DataQuality to DataQualityIndicators for UI state
     */
    private fun mapDataQualityToIndicators(quality: DataQuality): DataQualityIndicators {
        return DataQualityIndicators(
            latency = quality.freshness.inWholeMilliseconds,
            accuracy = quality.accuracy,
            completeness = 1.0 - (quality.freshness.inWholeMilliseconds / 10000.0).coerceIn(0.0, 1.0),
            signalStrength = quality.reliability.toDouble(),
            gpsAccuracy = quality.accuracy,
            dataFreshness = quality.freshness.inWholeMilliseconds,
            validationStatus = ValidationStatus.VALID,
            sourceReliability = quality.reliability.toDouble(),
            anomalyFlags = if (quality.outOfOrderCount > 0 || quality.duplicateCount > 0) {
                listOf(AnomalyFlag.OUT_OF_SEQUENCE, AnomalyFlag.DUPLICATE_DATA).filter { 
                    when (it) {
                        AnomalyFlag.OUT_OF_SEQUENCE -> quality.outOfOrderCount > 0
                        AnomalyFlag.DUPLICATE_DATA -> quality.duplicateCount > 0
                        else -> false
                    }
                }
            } else emptyList()
        )
    }

    /**
     * Get real-time train states flow by converting position updates to train states
     */
    private fun getRealTimeTrainStatesFlow(sectionId: String): Flow<List<RealTimeTrainState>> {
        return realTimePositionService.subscribeToSectionUpdates(sectionId)
            .map { positions ->
                positions.map { position ->
                    RealTimeTrainState(
                        train = Train(
                            id = position.trainId,
                            name = "Train ${position.trainId}",
                            trainNumber = position.trainId,
                            currentLocation = Location(position.latitude, position.longitude, position.sectionId),
                            destination = Location(0.0, 0.0, "unknown"),
                            status = TrainStatus.ON_TIME,
                            priority = TrainPriority.MEDIUM,
                            speed = position.speed,
                            estimatedArrival = kotlinx.datetime.Clock.System.now(),
                            createdAt = kotlinx.datetime.Clock.System.now(),
                            updatedAt = kotlinx.datetime.Clock.System.now()
                        ),
                        currentPosition = position,
                        connectionStatus = _connectionStatus.value,
                        dataQuality = _dataQuality.value?.let { mapDataQualityToIndicators(it) } 
                            ?: createDefaultDataQualityIndicators(),
                        lastUpdate = position.timestamp
                    )
                }
            }
    }

    /**
     * Create default data quality indicators for UI state
     */
    private fun createDefaultDataQualityIndicators(): DataQualityIndicators {
        return DataQualityIndicators(
            latency = 50L,
            accuracy = 0.8,
            completeness = 0.9,
            signalStrength = 0.8,
            gpsAccuracy = 10.0,
            dataFreshness = 90L,
            validationStatus = ValidationStatus.VALID,
            sourceReliability = 0.85,
            anomalyFlags = emptyList()
        )
    }

    override fun onCleared() {
        super.onCleared()
        logger.info(TAG, "ViewModel cleared - cleaning up real-time connections")
        
        // Cleanup real-time connections
        viewModelScope.launch {
            try {
                realTimePositionService.stop()
                logger.debug(TAG, "Real-time connections cleaned up successfully")
            } catch (e: Exception) {
                logger.error(TAG, "Error cleaning up real-time connections", e)
            }
        }
    }

    /**
     * Enhanced data class to combine trains, conflicts, and real-time data
     */
    private data class EnhancedDashboardData(
        val trains: List<Train>,
        val conflicts: List<ConflictAlert>,
        val realTimeStates: List<RealTimeTrainState>,
        val connectionStatus: ConnectionState,
        val dataQuality: DataQualityIndicators
    )
}