package com.example.pravaahan.presentation.ui.screens.dashboard

import com.example.pravaahan.domain.model.*

/**
 * UI state for the Dashboard screen
 * Represents all the data needed to render the dashboard
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val trains: List<Train> = emptyList(),
    val conflicts: List<ConflictAlert> = emptyList(),
    val systemStatus: SystemStatus = SystemStatus(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    // Real-time map data
    val trainStates: List<RealTimeTrainState> = emptyList(),
    val sectionConfig: RailwaySectionConfig? = null,
    val connectionStatus: ConnectionState = ConnectionState.DISCONNECTED,
    val dataQuality: DataQualityIndicators? = null,
    val selectedTrainId: String? = null,
    val realTimeModeEnabled: Boolean = true
)

/**
 * System status information for the dashboard
 */
data class SystemStatus(
    val supabaseConnected: Boolean = false,
    val realtimeActive: Boolean = false,
    val aiEngineOnline: Boolean = false,
    val lastUpdated: Long = 0L
)

/**
 * User actions that can be performed on the Dashboard screen
 */
sealed class DashboardAction {
    /**
     * Load initial data for the dashboard
     */
    data object LoadData : DashboardAction()
    
    /**
     * Refresh all dashboard data
     */
    data object RefreshData : DashboardAction()
    
    /**
     * Navigate to train details screen
     */
    data class NavigateToTrain(val trainId: String) : DashboardAction()
    
    /**
     * Navigate to conflict resolution screen
     */
    data class NavigateToConflict(val conflictId: String) : DashboardAction()
    
    /**
     * Navigate to settings screen
     */
    data object NavigateToSettings : DashboardAction()
    
    /**
     * Clear current error state
     */
    data object ClearError : DashboardAction()
    
    /**
     * Retry failed operation
     */
    data object Retry : DashboardAction()
    
    // Real-time map actions
    /**
     * Select a train on the map
     */
    data class SelectTrain(val trainId: String) : DashboardAction()
    
    /**
     * Toggle real-time mode
     */
    data class ToggleRealTimeMode(val enabled: Boolean) : DashboardAction()
    
    /**
     * Retry real-time connection
     */
    data object RetryConnection : DashboardAction()
}