package com.example.pravaahan.core.health

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Utility functions for displaying health status in the UI
 */
object HealthStatusUtils {
    
    /**
     * Gets a user-friendly status message for the overall app health
     */
    fun getStatusMessage(healthStatus: AppHealthStatus): String {
        return when (healthStatus) {
            is AppHealthStatus.Healthy -> "All systems operational"
            is AppHealthStatus.Degraded -> {
                val issueCount = healthStatus.warnings.size
                "System operational with $issueCount warning${if (issueCount != 1) "s" else ""}"
            }
            is AppHealthStatus.Unhealthy -> {
                val criticalCount = healthStatus.criticalFailures.size
                val nonCriticalCount = healthStatus.nonCriticalFailures.size
                val totalIssues = criticalCount + nonCriticalCount
                "$criticalCount critical failure${if (criticalCount != 1) "s" else ""}" +
                if (nonCriticalCount > 0) ", $nonCriticalCount other issue${if (nonCriticalCount != 1) "s" else ""}" else ""
            }
        }
    }
    
    /**
     * Gets a short status text for display in compact UI elements
     */
    fun getShortStatusText(healthStatus: AppHealthStatus): String {
        return when (healthStatus) {
            is AppHealthStatus.Healthy -> "Healthy"
            is AppHealthStatus.Degraded -> "Degraded"
            is AppHealthStatus.Unhealthy -> "Unhealthy"
        }
    }
    
    /**
     * Gets the appropriate icon for the health status
     */
    fun getStatusIcon(healthStatus: AppHealthStatus): ImageVector {
        return when (healthStatus) {
            is AppHealthStatus.Healthy -> Icons.Default.CheckCircle
            is AppHealthStatus.Degraded -> Icons.Default.Warning
            is AppHealthStatus.Unhealthy -> Icons.Default.Close
        }
    }
    
    /**
     * Gets the appropriate color for the health status
     */
    @Composable
    fun getStatusColor(healthStatus: AppHealthStatus): Color {
        return when (healthStatus) {
            is AppHealthStatus.Healthy -> MaterialTheme.colorScheme.primary
            is AppHealthStatus.Degraded -> MaterialTheme.colorScheme.tertiary
            is AppHealthStatus.Unhealthy -> MaterialTheme.colorScheme.error
        }
    }
    
    /**
     * Gets detailed information about health check results
     */
    fun getDetailedResults(healthStatus: AppHealthStatus): List<HealthCheckDisplayInfo> {
        return healthStatus.checkResults.map { result ->
            when (result) {
                is HealthCheckResult.Success -> HealthCheckDisplayInfo(
                    name = result.checkName,
                    status = "Passed",
                    message = result.details ?: "Check completed successfully",
                    duration = result.durationMs,
                    isSuccess = true,
                    isWarning = false,
                    isError = false
                )
                is HealthCheckResult.Warning -> HealthCheckDisplayInfo(
                    name = result.checkName,
                    status = "Warning",
                    message = result.message,
                    duration = result.durationMs,
                    isSuccess = false,
                    isWarning = true,
                    isError = false
                )
                is HealthCheckResult.Failure -> HealthCheckDisplayInfo(
                    name = result.checkName,
                    status = "Failed",
                    message = result.message,
                    duration = result.durationMs,
                    isSuccess = false,
                    isWarning = false,
                    isError = true
                )
            }
        }
    }
    
    /**
     * Formats duration in milliseconds to a human-readable string
     */
    fun formatDuration(durationMs: Long): String {
        return when {
            durationMs < 1000 -> "${durationMs}ms"
            durationMs < 60000 -> String.format("%.1fs", durationMs / 1000.0)
            else -> String.format("%.1fm", durationMs / 60000.0)
        }
    }
    
    /**
     * Gets a summary of the health check execution
     */
    fun getExecutionSummary(healthStatus: AppHealthStatus): String {
        val totalChecks = healthStatus.checkResults.size
        val successCount = healthStatus.checkResults.count { it is HealthCheckResult.Success }
        val warningCount = healthStatus.checkResults.count { it is HealthCheckResult.Warning }
        val failureCount = healthStatus.checkResults.count { it is HealthCheckResult.Failure }
        val duration = formatDuration(healthStatus.totalDurationMs)
        
        return "Completed $totalChecks checks in $duration: " +
                "$successCount passed, $warningCount warnings, $failureCount failed"
    }
    
    /**
     * Determines if the app should be considered operational based on health status
     */
    fun isAppOperational(healthStatus: AppHealthStatus?): Boolean {
        return when (healthStatus) {
            is AppHealthStatus.Healthy -> true
            is AppHealthStatus.Degraded -> true
            is AppHealthStatus.Unhealthy -> healthStatus.criticalFailures.isEmpty()
            null -> false // No health status available
        }
    }
    
    /**
     * Gets recommendations for addressing health issues
     */
    fun getHealthRecommendations(healthStatus: AppHealthStatus): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (healthStatus) {
            is AppHealthStatus.Healthy -> {
                recommendations.add("All systems are functioning normally.")
            }
            is AppHealthStatus.Degraded -> {
                healthStatus.warnings.forEach { warning ->
                    when (warning.checkName) {
                        "Database Access" -> recommendations.add("Database performance may be slow. Monitor query times.")
                        "Realtime Connection" -> recommendations.add("Real-time updates may be delayed. Check network stability.")
                        "Navigation Setup" -> recommendations.add("Some navigation features may not work correctly.")
                        else -> recommendations.add("Monitor ${warning.checkName} for potential issues.")
                    }
                }
            }
            is AppHealthStatus.Unhealthy -> {
                healthStatus.criticalFailures.forEach { failure ->
                    when (failure.checkName) {
                        "Supabase Connection" -> recommendations.add("Check internet connection and Supabase service status.")
                        "Database Access" -> recommendations.add("Verify database configuration and permissions.")
                        "Realtime Connection" -> recommendations.add("Real-time features unavailable. Check network connectivity.")
                        "Dependency Injection" -> recommendations.add("App configuration error. Restart the application.")
                        else -> recommendations.add("Critical issue with ${failure.checkName}. Contact support.")
                    }
                }
                
                if (healthStatus.criticalFailures.isNotEmpty()) {
                    recommendations.add("Railway control functions may be limited. Use caution when making operational decisions.")
                }
            }
        }
        
        return recommendations
    }
}

/**
 * Data class for displaying health check information in the UI
 */
data class HealthCheckDisplayInfo(
    val name: String,
    val status: String,
    val message: String,
    val duration: Long,
    val isSuccess: Boolean,
    val isWarning: Boolean,
    val isError: Boolean
)