package com.example.pravaahan.presentation.ui.screens.conflictresolution

import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.Train

/**
 * UI state for the Conflict Resolution screen
 * Represents all the data needed to render conflict resolution interface
 */
data class ConflictResolutionUiState(
    val isLoading: Boolean = false,
    val conflict: ConflictAlert? = null,
    val involvedTrains: List<Train> = emptyList(),
    val manualOverrideText: String = "",
    val isSubmittingResolution: Boolean = false,
    val resolutionSuccess: Boolean = false,
    val error: String? = null
)

/**
 * User actions that can be performed on the Conflict Resolution screen
 */
sealed class ConflictResolutionAction {
    /**
     * Load conflict details and related data
     */
    data class LoadConflictDetails(val conflictId: String) : ConflictResolutionAction()
    
    /**
     * Accept the AI recommendation for conflict resolution
     */
    data object AcceptRecommendation : ConflictResolutionAction()
    
    /**
     * Update manual override text
     */
    data class UpdateManualOverrideText(val text: String) : ConflictResolutionAction()
    
    /**
     * Submit manual override for conflict resolution
     */
    data object SubmitManualOverride : ConflictResolutionAction()
    
    /**
     * Navigate to train details screen
     */
    data class NavigateToTrain(val trainId: String) : ConflictResolutionAction()
    
    /**
     * Navigate back to previous screen
     */
    data object NavigateBack : ConflictResolutionAction()
    
    /**
     * Clear current error state
     */
    data object ClearError : ConflictResolutionAction()
    
    /**
     * Clear success state and reset form
     */
    data object ClearSuccess : ConflictResolutionAction()
    
    /**
     * Retry failed operation
     */
    data object Retry : ConflictResolutionAction()
}