package com.example.pravaahan.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for PraVaahan app
 * Using Jetpack Navigation Compose with serializable routes
 */
@Serializable
sealed interface Screen {
    
    /**
     * Dashboard screen - Main overview of all trains and system status
     */
    @Serializable
    data object Dashboard : Screen
    
    /**
     * Train details screen - Detailed view of a specific train
     * @param trainId Unique identifier for the train
     */
    @Serializable
    data class TrainDetails(val trainId: String) : Screen
    
    /**
     * Conflict resolution screen - Handle train conflicts and AI recommendations
     * @param conflictId Unique identifier for the conflict
     */
    @Serializable
    data class ConflictResolution(val conflictId: String) : Screen
    
    /**
     * Settings screen - App configuration and preferences
     */
    @Serializable
    data object Settings : Screen
}

/**
 * Extension function to get the route name for logging and analytics
 */
fun Screen.getRouteName(): String = when (this) {
    is Screen.Dashboard -> "Dashboard"
    is Screen.TrainDetails -> "TrainDetails"
    is Screen.ConflictResolution -> "ConflictResolution"
    is Screen.Settings -> "Settings"
}