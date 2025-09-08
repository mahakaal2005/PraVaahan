package com.example.pravaahan.presentation.ui.screens.traindetails

import com.example.pravaahan.domain.model.ConflictAlert
import com.example.pravaahan.domain.model.Train

/**
 * UI state for the Train Details screen
 * Represents all the data needed to render train details
 */
data class TrainDetailsUiState(
    val isLoading: Boolean = false,
    val train: Train? = null,
    val relatedConflicts: List<ConflictAlert> = emptyList(),
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val error: String? = null,
    val isRefreshing: Boolean = false
)

/**
 * Performance metrics for the train
 */
data class PerformanceMetrics(
    val onTimePercentage: Double = 0.0,
    val averageSpeed: Double = 0.0,
    val fuelEfficiency: String = "Unknown",
    val lastUpdated: Long = 0L
)

/**
 * User actions that can be performed on the Train Details screen
 */
sealed class TrainDetailsAction {
    /**
     * Load train details and related data
     */
    data class LoadTrainDetails(val trainId: String) : TrainDetailsAction()
    
    /**
     * Refresh train data
     */
    data object RefreshData : TrainDetailsAction()
    
    /**
     * Navigate to conflict resolution screen
     */
    data class NavigateToConflict(val conflictId: String) : TrainDetailsAction()
    
    /**
     * Navigate back to previous screen
     */
    data object NavigateBack : TrainDetailsAction()
    
    /**
     * Clear current error state
     */
    data object ClearError : TrainDetailsAction()
    
    /**
     * Retry failed operation
     */
    data object Retry : TrainDetailsAction()
}