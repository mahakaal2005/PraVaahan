package com.example.pravaahan.presentation.ui.screens.conflictresolution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pravaahan.core.error.AppError
import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.error.ErrorHandlingUtils
import com.example.pravaahan.core.error.getUserMessage
import com.example.pravaahan.core.error.launchWithErrorHandling
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.usecase.AcceptRecommendationUseCase
import com.example.pravaahan.domain.usecase.GetConflictByIdUseCase
import com.example.pravaahan.domain.usecase.GetTrainsUseCase
import com.example.pravaahan.domain.usecase.SubmitManualOverrideUseCase
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
 * ViewModel for the Conflict Resolution screen
 * Manages UI state and handles user actions for conflict resolution
 */
@HiltViewModel
class ConflictResolutionViewModel @Inject constructor(
    private val getConflictByIdUseCase: GetConflictByIdUseCase,
    private val getTrainsUseCase: GetTrainsUseCase,
    private val acceptRecommendationUseCase: AcceptRecommendationUseCase,
    private val submitManualOverrideUseCase: SubmitManualOverrideUseCase,
    private val errorHandler: ErrorHandler,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConflictResolutionUiState())
    val uiState: StateFlow<ConflictResolutionUiState> = _uiState.asStateFlow()

    private var currentConflictId: String? = null

    /**
     * Handle user actions from the UI
     */
    fun handleAction(action: ConflictResolutionAction) {
        logger.debug("ConflictResolutionViewModel", "Handling action: ${action::class.simpleName}")
        
        when (action) {
            is ConflictResolutionAction.LoadConflictDetails -> loadConflictDetails(action.conflictId)
            is ConflictResolutionAction.AcceptRecommendation -> acceptRecommendation()
            is ConflictResolutionAction.UpdateManualOverrideText -> updateManualOverrideText(action.text)
            is ConflictResolutionAction.SubmitManualOverride -> submitManualOverride()
            is ConflictResolutionAction.NavigateToTrain -> {
                logger.debug("ConflictResolutionViewModel", "Navigate to train: ${action.trainId}")
                // Navigation is handled by the UI layer
            }
            is ConflictResolutionAction.NavigateBack -> {
                logger.debug("ConflictResolutionViewModel", "Navigate back")
                // Navigation is handled by the UI layer
            }
            is ConflictResolutionAction.ClearError -> clearError()
            is ConflictResolutionAction.ClearSuccess -> clearSuccess()
            is ConflictResolutionAction.Retry -> retry()
        }
    }

    /**
     * Load conflict details and related data
     */
    private fun loadConflictDetails(conflictId: String) {
        logger.debug("ConflictResolutionViewModel", "Loading conflict details for ID: $conflictId")
        
        // Validate conflict ID format
        val validationResult = ErrorHandlingUtils.validateConflictId(conflictId)
        if (validationResult.isFailure) {
            val appError = validationResult.exceptionOrNull() as? AppError ?: AppError.ValidationError.InvalidInput("conflictId", "Invalid conflict ID format")
            _uiState.update { currentState: ConflictResolutionUiState -> 
                currentState.copy(
                    isLoading = false,
                    error = appError.getUserMessage()
                )
            }
            return
        }
        
        currentConflictId = conflictId
        _uiState.update { currentState: ConflictResolutionUiState -> currentState.copy(isLoading = true, error = null) }

        launchWithErrorHandling(
            logger = logger,
            errorHandler = errorHandler,
            operation = "load_conflict_details",
            onError = { appError ->
                _uiState.update { currentState: ConflictResolutionUiState -> 
                    currentState.copy(
                        isLoading = false,
                        error = appError.getUserMessage()
                    )
                }
            }
        ) {
            // Combine conflict details and trains data streams
            combine(
                getConflictByIdUseCase(conflictId),
                getTrainsUseCase()
            ) { conflict, allTrains ->
                ConflictResolutionData(
                    conflict = conflict,
                    involvedTrains = if (conflict != null) {
                        allTrains.filter { train ->
                            conflict.trainsInvolved.contains(train.id)
                        }
                    } else {
                        emptyList()
                    }
                )
            }
            .catch { exception ->
                val appError = errorHandler.handleError(exception)
                logger.error("ConflictResolutionViewModel", "Error loading conflict details", exception)
                _uiState.update { currentState: ConflictResolutionUiState -> 
                    currentState.copy(
                        isLoading = false,
                        error = appError.getUserMessage()
                    )
                }
            }
            .collect { data ->
                if (data.conflict != null) {
                    logger.debug("ConflictResolutionViewModel", "Conflict details loaded: ${data.conflict.id}")
                    
                    _uiState.update { currentState: ConflictResolutionUiState ->
                        currentState.copy(
                            isLoading = false,
                            conflict = data.conflict,
                            involvedTrains = data.involvedTrains,
                            error = null
                        )
                    }
                } else {
                    logger.warn("ConflictResolutionViewModel", "Conflict not found: $conflictId")
                    val appError = AppError.DatabaseError.DataNotFound(conflictId)
                    _uiState.update { currentState: ConflictResolutionUiState -> 
                        currentState.copy(
                            isLoading = false,
                            error = appError.getUserMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Accept the AI recommendation for conflict resolution
     */
    private fun acceptRecommendation() {
        val conflictId = currentConflictId ?: return
        
        logger.debug("ConflictResolutionViewModel", "Accepting recommendation for conflict: $conflictId")
        
        _uiState.update { currentState: ConflictResolutionUiState -> currentState.copy(isSubmittingResolution = true, error = null) }

        launchWithErrorHandling(
            logger = logger,
            errorHandler = errorHandler,
            operation = "accept_recommendation",
            onError = { appError ->
                _uiState.update { currentState: ConflictResolutionUiState -> 
                    currentState.copy(
                        isSubmittingResolution = false,
                        error = appError.getUserMessage()
                    )
                }
            }
        ) {
            val result = acceptRecommendationUseCase(
                conflictId = conflictId,
                controllerId = "current_controller" // TODO: Get actual controller ID
            )
            
            result.fold(
                onSuccess = { _: Unit ->
                    logger.debug("ConflictResolutionViewModel", "Recommendation accepted successfully")
                    _uiState.update { currentState: ConflictResolutionUiState -> 
                        currentState.copy(
                            isSubmittingResolution = false,
                            resolutionSuccess = true
                        )
                    }
                },
                onFailure = { exception: Throwable ->
                    val appError = if (exception is AppError) exception else errorHandler.handleError(exception)
                    logger.error("ConflictResolutionViewModel", "Failed to accept recommendation", exception)
                    _uiState.update { currentState: ConflictResolutionUiState -> 
                        currentState.copy(
                            isSubmittingResolution = false,
                            error = appError.getUserMessage()
                        )
                    }
                }
            )
        }
    }

    /**
     * Update manual override text
     */
    private fun updateManualOverrideText(text: String) {
        _uiState.update { currentState: ConflictResolutionUiState -> currentState.copy(manualOverrideText = text) }
    }

    /**
     * Submit manual override for conflict resolution
     */
    private fun submitManualOverride() {
        val conflictId = currentConflictId ?: return
        val overrideText = _uiState.value.manualOverrideText
        
        // Validate controller action text
        val validationResult = ErrorHandlingUtils.validateControllerAction(overrideText)
        if (validationResult.isFailure) {
            val appError = validationResult.exceptionOrNull() as? AppError ?: AppError.ValidationError.InvalidInput("overrideText", "Invalid override text")
            _uiState.update { currentState: ConflictResolutionUiState -> 
                currentState.copy(error = appError.getUserMessage())
            }
            return
        }
        
        logger.debug("ConflictResolutionViewModel", "Submitting manual override for conflict: $conflictId")
        
        _uiState.update { currentState: ConflictResolutionUiState -> currentState.copy(isSubmittingResolution = true, error = null) }

        launchWithErrorHandling(
            logger = logger,
            errorHandler = errorHandler,
            operation = "submit_manual_override",
            onError = { appError ->
                _uiState.update { currentState: ConflictResolutionUiState -> 
                    currentState.copy(
                        isSubmittingResolution = false,
                        error = appError.getUserMessage()
                    )
                }
            }
        ) {
            val result = submitManualOverrideUseCase(
                conflictId = conflictId,
                controllerId = "current_controller", // TODO: Get actual controller ID
                overrideInstructions = overrideText,
                reason = "Manual controller intervention"
            )
            
            result.fold(
                onSuccess = { _: Unit ->
                    logger.debug("ConflictResolutionViewModel", "Manual override submitted successfully")
                    _uiState.update { currentState: ConflictResolutionUiState -> 
                        currentState.copy(
                            isSubmittingResolution = false,
                            resolutionSuccess = true,
                            manualOverrideText = ""
                        )
                    }
                },
                onFailure = { exception: Throwable ->
                    val appError = if (exception is AppError) exception else errorHandler.handleError(exception)
                    logger.error("ConflictResolutionViewModel", "Failed to submit manual override", exception)
                    _uiState.update { currentState: ConflictResolutionUiState -> 
                        currentState.copy(
                            isSubmittingResolution = false,
                            error = appError.getUserMessage()
                        )
                    }
                }
            )
        }
    }

    /**
     * Clear current error state
     */
    private fun clearError() {
        logger.debug("ConflictResolutionViewModel", "Clearing error state")
        _uiState.update { currentState: ConflictResolutionUiState -> currentState.copy(error = null) }
    }

    /**
     * Clear success state and reset form
     */
    private fun clearSuccess() {
        logger.debug("ConflictResolutionViewModel", "Clearing success state")
        _uiState.update { currentState: ConflictResolutionUiState -> 
            currentState.copy(
                resolutionSuccess = false,
                manualOverrideText = ""
            )
        }
    }

    /**
     * Retry failed operation
     */
    private fun retry() {
        logger.debug("ConflictResolutionViewModel", "Retrying failed operation")
        currentConflictId?.let { conflictId ->
            loadConflictDetails(conflictId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        logger.debug("ConflictResolutionViewModel", "ViewModel cleared")
    }

    /**
     * Data class to combine conflict and involved trains
     */
    private data class ConflictResolutionData(
        val conflict: com.example.pravaahan.domain.model.ConflictAlert?,
        val involvedTrains: List<com.example.pravaahan.domain.model.Train>
    )
}