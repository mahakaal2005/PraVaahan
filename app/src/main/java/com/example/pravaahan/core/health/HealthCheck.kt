package com.example.pravaahan.core.health

import kotlinx.coroutines.flow.Flow

/**
 * Interface for individual health checks in the PraVaahan railway control system.
 * Each health check verifies a specific component or service.
 */
interface HealthCheck {
    /**
     * Unique identifier for this health check
     */
    val name: String
    
    /**
     * Whether this health check is critical for app startup.
     * Critical checks will prevent the app from starting if they fail.
     */
    val isCritical: Boolean
    
    /**
     * Timeout for this health check in milliseconds
     */
    val timeoutMs: Long
    
    /**
     * Number of retry attempts for this health check
     */
    val maxRetries: Int
    
    /**
     * Performs the health check and returns the result
     */
    suspend fun check(): HealthCheckResult
    
    /**
     * Performs the health check with real-time status updates
     */
    fun checkWithProgress(): Flow<HealthCheckProgress>
}

/**
 * Result of a health check operation
 */
sealed class HealthCheckResult {
    abstract val checkName: String
    abstract val durationMs: Long
    abstract val timestamp: Long
    
    data class Success(
        override val checkName: String,
        override val durationMs: Long,
        override val timestamp: Long,
        val details: String? = null
    ) : HealthCheckResult()
    
    data class Warning(
        override val checkName: String,
        override val durationMs: Long,
        override val timestamp: Long,
        val message: String,
        val details: String? = null
    ) : HealthCheckResult()
    
    data class Failure(
        override val checkName: String,
        override val durationMs: Long,
        override val timestamp: Long,
        val error: Throwable,
        val message: String,
        val retryAttempt: Int = 0
    ) : HealthCheckResult()
}

/**
 * Progress updates during health check execution
 */
sealed class HealthCheckProgress {
    abstract val checkName: String
    
    data class Started(override val checkName: String) : HealthCheckProgress()
    data class InProgress(override val checkName: String, val message: String) : HealthCheckProgress()
    data class Completed(override val checkName: String, val result: HealthCheckResult) : HealthCheckProgress()
}

/**
 * Overall health status of the application
 */
sealed class AppHealthStatus {
    abstract val timestamp: Long
    abstract val checkResults: List<HealthCheckResult>
    abstract val totalDurationMs: Long
    
    data class Healthy(
        override val timestamp: Long,
        override val checkResults: List<HealthCheckResult>,
        override val totalDurationMs: Long
    ) : AppHealthStatus()
    
    data class Degraded(
        override val timestamp: Long,
        override val checkResults: List<HealthCheckResult>,
        override val totalDurationMs: Long,
        val warnings: List<HealthCheckResult.Warning>
    ) : AppHealthStatus()
    
    data class Unhealthy(
        override val timestamp: Long,
        override val checkResults: List<HealthCheckResult>,
        override val totalDurationMs: Long,
        val criticalFailures: List<HealthCheckResult.Failure>,
        val nonCriticalFailures: List<HealthCheckResult.Failure>
    ) : AppHealthStatus()
}

/**
 * Configuration for health check execution
 */
data class HealthCheckConfig(
    val runInParallel: Boolean = true,
    val failFastOnCritical: Boolean = true,
    val includeNonCritical: Boolean = true,
    val globalTimeoutMs: Long = 30_000L // 30 seconds total timeout
)