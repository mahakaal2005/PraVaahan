package com.example.pravaahan.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.pravaahan.presentation.ui.screens.ConflictResolutionScreen
import com.example.pravaahan.presentation.ui.screens.DashboardScreen
import com.example.pravaahan.presentation.ui.screens.SettingsScreen
import com.example.pravaahan.presentation.ui.screens.TrainDetailsScreen

/**
 * Central navigation composable for PraVaahan app
 * Implements type-safe navigation using Jetpack Navigation Compose
 */
@Composable
fun PravahanNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Screen = Screen.Dashboard
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Dashboard - Main screen with train overview
        composable<Screen.Dashboard> {
            DashboardScreen(
                onTrainClick = { trainId ->
                    navController.navigate(Screen.TrainDetails(trainId = trainId))
                },
                onConflictClick = { conflictId ->
                    navController.navigate(Screen.ConflictResolution(conflictId = conflictId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings)
                }
            )
        }
        
        // Train Details - Detailed view of a specific train
        composable<Screen.TrainDetails> { backStackEntry ->
            val trainDetails = backStackEntry.toRoute<Screen.TrainDetails>()
            TrainDetailsScreen(
                trainId = trainDetails.trainId,
                onBackClick = {
                    navController.popBackStack()
                },
                onConflictClick = { conflictId ->
                    navController.navigate(Screen.ConflictResolution(conflictId = conflictId))
                }
            )
        }
        
        // Conflict Resolution - Handle train conflicts and AI recommendations
        composable<Screen.ConflictResolution> { backStackEntry ->
            val conflictResolution = backStackEntry.toRoute<Screen.ConflictResolution>()
            ConflictResolutionScreen(
                conflictId = conflictResolution.conflictId,
                onBackClick = {
                    navController.popBackStack()
                },
                onTrainClick = { trainId ->
                    navController.navigate(Screen.TrainDetails(trainId = trainId))
                }
            )
        }
        
        // Settings - App configuration and preferences
        composable<Screen.Settings> {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Navigation actions interface for dependency injection and testing
 */
interface NavigationActions {
    fun navigateToTrainDetails(trainId: String)
    fun navigateToConflictResolution(conflictId: String)
    fun navigateToSettings()
    fun navigateBack()
}

/**
 * Implementation of navigation actions using NavHostController
 */
class NavigationActionsImpl(
    private val navController: NavHostController
) : NavigationActions {
    
    override fun navigateToTrainDetails(trainId: String) {
        navController.navigate(Screen.TrainDetails(trainId = trainId))
    }
    
    override fun navigateToConflictResolution(conflictId: String) {
        navController.navigate(Screen.ConflictResolution(conflictId = conflictId))
    }
    
    override fun navigateToSettings() {
        navController.navigate(Screen.Settings)
    }
    
    override fun navigateBack() {
        navController.popBackStack()
    }
}