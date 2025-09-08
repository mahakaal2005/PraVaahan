package com.example.pravaahan.presentation.ui.screens.traindetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pravaahan.core.error.AppError
import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.error.getUserMessage
import com.example.pravaahan.core.error.launchWithErrorHandling
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.usecase.GetConflictsUseCase
import com.example.pravaahan.domain.usecase.GetTrainByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Train Details screen
 * Manages UI state and handles user actions for individual train details
 */
@HiltViewModel
class TrainDetailsViewModel @Inject constructor(
    private val getTrainByIdUseCase: GetTrainByIdUseCase,
    private val getConflictsUseCase: GetConflictsUseCase,
    private val errorHandler: ErrorHandler,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainDetailsUiState())
    val uiState: StateFlow<TrainDetailsUiState> = _uiState.asStateFlow()

    private var currentTrainId: String? = null

    /**
     * Handle user actions from the UI
     */
    fun handleAction(action: TrainDetailsAction) {
        logger.debug("TrainDetailsViewModel", "Handling action: ${action::class.simpleName}")
        
        when (action) {
            is TrainDetailsAction.LoadTrainDetails -> loadTrainDetails(action.trainId)
            is TrainDetailsAction.RefreshData -> refreshData()
            is TrainDetailsAction.NavigateToConflict -> {
                logger.debug("TrainDetailsViewModel", "Navigate to conflict: ${action.conflictId}")
                // Navigation is handled by the UI layer
            }
            is TrainDetailsAction.NavigateBack -> {
                logger.debug("TrainDetailsViewModel", "Navigate back")
                // Navigation is handled by the UI layer
            }
            is TrainDetailsAction.ClearError -> clearError()
            is TrainDetailsAction.Retry -> retry()
        }
    }

    /**
     * Load train details and related data
     */
    private fun loadTrainDetails(trainId: String) {
        logger.debug("TrainDetailsViewModel", "Loading train details for ID: $trainId")
        
        currentTrainId = trainId
        _uiState.update { currentState: TrainDetailsUiState -> currentState.copy(isLoading = true, error = null) }

        launchWithErrorHandling(
            logger = logger,
            errorHandler = errorHandler,
            operation = "load_train_details",
            onError = { appError ->
                _uiState.update { currentState: TrainDetailsUiState -> 
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = appError.getUserMessage()
                    )
                }
            }
        ) {
            // Combine train details and conflicts data streams
            combine(
                getTrainByIdUseCase(trainId),
                getConflictsUseCase()
            ) { train, allConflicts ->
                TrainDetailsData(
                    train = train,
                    relatedConflicts = allConflicts.filter { conflict ->
                        conflict.trainsInvolved.contains(trainId)
                    }
                )
            }
            .catch { exception ->
                val appError = errorHandler.handleError(exception)
                logger.error("TrainDetailsViewModel", "Error loading train details", exception)
                _uiState.update { currentState: TrainDetailsUiState -> 
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = appError.getUserMessage()
                    )
                }
            }
            .collect { data ->
                if (data.train != null) {
                    logger.debug("TrainDetailsViewModel", "Train details loaded: ${data.train.name}")
                    
                    _uiState.update { currentState: TrainDetailsUiState ->
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            train = data.train,
                            relatedConflicts = data.relatedConflicts,
                            performanceMetrics = calculatePerformanceMetrics(data.train),
                            error = null
                        )
                    }
                } else {
                    logger.warn("TrainDetailsViewModel", "Train not found: $trainId")
                    val appError = AppError.BusinessError.TrainNotFound(trainId)
                    _uiState.update { currentState: TrainDetailsUiState -> 
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = appError.getUserMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Refresh train data
     */
    private fun refreshData() {
        logger.debug("TrainDetailsViewModel", "Refreshing train data")
        
        currentTrainId?.let { trainId ->
            _uiState.update { currentState: TrainDetailsUiState -> currentState.copy(isRefreshing = true, error = null) }
            loadTrainDetails(trainId)
        }
    }

    /**
     * Clear current error state
     */
    private fun clearError() {
        logger.debug("TrainDetailsViewModel", "Clearing error state")
        _uiState.update { currentState: TrainDetailsUiState -> currentState.copy(error = null) }
    }

    /**
     * Retry failed operation
     */
    private fun retry() {
        logger.debug("TrainDetailsViewModel", "Retrying failed operation")
        currentTrainId?.let { trainId ->
            loadTrainDetails(trainId)
        }
    }

    /**
     * Calculate performance metrics for the train
     */
    private fun calculatePerformanceMetrics(train: com.example.pravaahan.domain.model.Train): PerformanceMetrics {
        // TODO: Implement actual performance calculation logic
        return PerformanceMetrics(
            onTimePercentage = when (train.status) {
                com.example.pravaahan.domain.model.TrainStatus.ON_TIME -> 94.0
                com.example.pravaahan.domain.model.TrainStatus.DELAYED -> 78.0
                else -> 85.0
            },
            averageSpeed = train.speed * 0.85, // Approximate average
            fuelEfficiency = when {
                train.speed > 100 -> "Good"
                train.speed > 60 -> "Average"
                else -> "Poor"
            },
            lastUpdated = System.currentTimeMillis()
        )
    }

    override fun onCleared() {
        super.onCleared()
        logger.debug("TrainDetailsViewModel", "ViewModel cleared")
    }

    /**
     * Data class to combine train and related conflicts
     */
    private data class TrainDetailsData(
        val train: com.example.pravaahan.domain.model.Train?,
        val relatedConflicts: List<com.example.pravaahan.domain.model.ConflictAlert>
    )
}