package com.example.pravaahan.presentation.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pravaahan.core.error.AppError
import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.error.getUserMessage
import com.example.pravaahan.core.error.launchWithErrorHandling
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.usecase.GetConflictsUseCase
import com.example.pravaahan.domain.usecase.GetTrainsUseCase
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
 * ViewModel for the Dashboard screen
 * Manages UI state and handles user actions for the main dashboard
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTrainsUseCase: GetTrainsUseCase,
    private val getConflictsUseCase: GetConflictsUseCase,
    private val errorHandler: ErrorHandler,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        logger.debug("DashboardViewModel", "Initializing Dashboard ViewModel")
        loadData()
    }

    /**
     * Handle user actions from the UI
     */
    fun handleAction(action: DashboardAction) {
        logger.debug("DashboardViewModel", "Handling action: ${action::class.simpleName}")
        
        when (action) {
            is DashboardAction.LoadData -> loadData()
            is DashboardAction.RefreshData -> refreshData()
            is DashboardAction.NavigateToTrain -> {
                logger.debug("DashboardViewModel", "Navigate to train: ${action.trainId}")
                // Navigation is handled by the UI layer
            }
            is DashboardAction.NavigateToConflict -> {
                logger.debug("DashboardViewModel", "Navigate to conflict: ${action.conflictId}")
                // Navigation is handled by the UI layer
            }
            is DashboardAction.NavigateToSettings -> {
                logger.debug("DashboardViewModel", "Navigate to settings")
                // Navigation is handled by the UI layer
            }
            is DashboardAction.ClearError -> clearError()
            is DashboardAction.Retry -> retry()
        }
    }

    /**
     * Load initial data for the dashboard
     */
    private fun loadData() {
        logger.debug("DashboardViewModel", "Loading dashboard data")
        
        _uiState.update { currentState: DashboardUiState -> currentState.copy(isLoading = true, error = null) }

        launchWithErrorHandling(
            logger = logger,
            errorHandler = errorHandler,
            operation = "load_dashboard_data",
            onError = { appError ->
                _uiState.update { currentState: DashboardUiState -> 
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = appError.getUserMessage()
                    )
                }
            }
        ) {
            // Combine trains and conflicts data streams
            combine(
                getTrainsUseCase(),
                getConflictsUseCase()
            ) { trains, conflicts ->
                DashboardData(trains, conflicts)
            }
            .catch { exception ->
                val appError = errorHandler.handleError(exception)
                logger.error("DashboardViewModel", "Error loading data", exception)
                _uiState.update { currentState: DashboardUiState -> 
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = appError.getUserMessage()
                    )
                }
            }
            .collect { data ->
                logger.debug("DashboardViewModel", "Data loaded: ${data.trains.size} trains, ${data.conflicts.size} conflicts")
                
                _uiState.update { currentState: DashboardUiState ->
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        trains = data.trains,
                        conflicts = data.conflicts,
                        systemStatus = currentState.systemStatus.copy(
                            supabaseConnected = true,
                            realtimeActive = true,
                            aiEngineOnline = true,
                            lastUpdated = System.currentTimeMillis()
                        ),
                        error = null
                    )
                }
            }
        }
    }

    /**
     * Refresh dashboard data
     */
    private fun refreshData() {
        logger.debug("DashboardViewModel", "Refreshing dashboard data")
        
        _uiState.update { currentState: DashboardUiState -> currentState.copy(isRefreshing = true, error = null) }
        loadData()
    }

    /**
     * Clear current error state
     */
    private fun clearError() {
        logger.debug("DashboardViewModel", "Clearing error state")
        _uiState.update { currentState: DashboardUiState -> currentState.copy(error = null) }
    }

    /**
     * Retry failed operation
     */
    private fun retry() {
        logger.debug("DashboardViewModel", "Retrying failed operation")
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        logger.debug("DashboardViewModel", "ViewModel cleared")
    }

    /**
     * Data class to combine trains and conflicts
     */
    private data class DashboardData(
        val trains: List<com.example.pravaahan.domain.model.Train>,
        val conflicts: List<com.example.pravaahan.domain.model.ConflictAlert>
    )
}