package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.presentation.navigation.Screen
import com.example.pravaahan.presentation.navigation.getRouteName
import javax.inject.Inject

/**
 * Health check that verifies navigation setup and screen definitions.
 * Ensures all critical screens are properly configured.
 */
class NavigationHealthCheck @Inject constructor(
    logger: Logger
) : BaseHealthCheck(
    name = "Navigation Setup",
    isCritical = false, // Non-critical - UI can still work with navigation issues
    timeoutMs = 1_000L, // 1 second
    maxRetries = 0, // No retries needed for navigation validation
    logger = logger
) {
    
    override suspend fun performCheck() {
        try {
            logger.debug(TAG, "Validating navigation setup...")
            
            // Validate all screen routes are properly defined
            validateScreenRoutes()
            
            // Validate route parameters
            validateRouteParameters()
            
            logger.debug(TAG, "Navigation setup validation successful")
            
        } catch (e: Exception) {
            logger.error(TAG, "Navigation setup validation failed", e)
            throw NavigationException("Navigation setup validation failed: ${e.message}", e)
        }
    }
    
    private fun validateScreenRoutes() {
        val requiredScreens = listOf(
            Screen.Dashboard,
            Screen.TrainDetails("test"),
            Screen.ConflictResolution("test"),
            Screen.Settings
        )
        
        val invalidRoutes = mutableListOf<String>()
        
        for (screen in requiredScreens) {
            try {
                // Validate screen can be created and has proper route name
                val routeName = screen.getRouteName()
                
                if (routeName.isBlank()) {
                    invalidRoutes.add("${screen::class.simpleName}: empty route name")
                    continue
                }
                
                logger.debug(TAG, "Screen ${screen::class.simpleName} has valid route name: $routeName")
                
            } catch (e: Exception) {
                invalidRoutes.add("${screen::class.simpleName}: ${e.message}")
            }
        }
        
        if (invalidRoutes.isNotEmpty()) {
            throw NavigationException("Invalid screen routes found: ${invalidRoutes.joinToString(", ")}")
        }
    }
    
    private fun validateRouteParameters() {
        try {
            // Test TrainDetails route parameter handling
            val trainId = "test_train_123"
            val trainDetailsScreen = Screen.TrainDetails(trainId)
            
            if (trainDetailsScreen.trainId != trainId) {
                throw NavigationException("TrainDetails route parameter not properly set")
            }
            
            // Test ConflictResolution route parameter handling
            val conflictId = "test_conflict_456"
            val conflictScreen = Screen.ConflictResolution(conflictId)
            
            if (conflictScreen.conflictId != conflictId) {
                throw NavigationException("ConflictResolution route parameter not properly set")
            }
            
            logger.debug(TAG, "Route parameter validation successful")
            
        } catch (e: Exception) {
            throw NavigationException("Route parameter validation failed: ${e.message}", e)
        }
    }
    
    override fun getSuccessDetails(): String {
        return "All navigation routes and parameters properly configured"
    }
    
    override fun getFailureMessage(exception: Throwable?): String {
        return when (exception) {
            is NavigationException -> exception.message ?: "Navigation setup failed"
            else -> "Navigation validation failed: ${exception?.message ?: "Unknown error"}"
        }
    }
    
    companion object {
        private const val TAG = "NavigationHealthCheck"
    }
}

/**
 * Exception thrown when navigation health check fails
 */
class NavigationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)