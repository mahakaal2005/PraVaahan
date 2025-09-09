package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Main coordinator for app startup verification and health checks.
 * Manages execution of all health checks and provides comprehensive status reporting.
 * 
 * Enhanced with comprehensive real-time monitoring capabilities for railway operations.
 */
@Singleton
class AppStartupVerifier @Inject constructor(
    private val supabaseConnectionHealthCheck: SupabaseConnectionHealthCheck,
    private val databaseAccessHealthCheck: DatabaseAccessHealthCheck,
    private val realtimeConnectionHealthCheck: RealtimeConnectionHealthCheck,
    private val navigationHealthCheck: NavigationHealthCheck,
    private val dependencyInjectionHealthCheck: DependencyInjectionHealthCheck,
    private val realTimeHealthCheck: RealTimeHealthCheck,
    private val positionDataQualityHealthCheck: PositionDataQualityHealthCheck,
    private val logger: Logger
) {
    
    private val allHealthChecks = listOf(
        dependencyInjectionHealthCheck, // Run first - everything depends on DI
        supabaseConnectionHealthCheck,
        databaseAccessHealthCheck,
        realtimeConnectionHealthCheck,
        realTimeHealthCheck, // Comprehensive real-time monitoring
        positionDataQualityHealthCheck, // NEW: Position data quality validation
        navigationHealthCheck
    )
    
    /**
     * Performs comprehensive app startup verification.
     * Returns overall health status with detailed results.
     */
    suspend fun verifyAppStartup(config: HealthCheckConfig = HealthCheckConfig()): AppHealthStatus {
        logger.info(TAG, "Starting app startup verification with ${allHealthChecks.size} health checks")
        val startTime = System.currentTimeMillis()
        
        return try {
            val totalDuration = measureTimeMillis {
                withTimeout(config.globalTimeoutMs) {
                    if (config.runInParallel) {
                        runHealthChecksInParallel(config)
                    } else {
                        runHealthChecksSequentially(config)
                    }
                }
            }
            
            val results = getLatestResults()
            val healthStatus = analyzeResults(results, totalDuration, startTime)
            
            logHealthStatus(healthStatus)
            healthStatus
            
        } catch (e: Exception) {
            logger.error(TAG, "App startup verification failed with exception", e)
            
            AppHealthStatus.Unhealthy(
                timestamp = startTime,
                checkResults = getLatestResults(),
                totalDurationMs = System.currentTimeMillis() - startTime,
                criticalFailures = listOf(
                    HealthCheckResult.Failure(
                        checkName = "Startup Verification",
                        durationMs = System.currentTimeMillis() - startTime,
                        timestamp = startTime,
                        error = e,
                        message = "Startup verification failed: ${e.message}"
                    )
                ),
                nonCriticalFailures = emptyList()
            )
        }
    }
    
    /**
     * Performs health checks with real-time progress updates.
     */
    fun verifyAppStartupWithProgress(config: HealthCheckConfig = HealthCheckConfig()): Flow<HealthCheckProgress> = flow {
        logger.info(TAG, "Starting app startup verification with progress updates")
        
        for (healthCheck in allHealthChecks) {
            if (!config.includeNonCritical && !healthCheck.isCritical) {
                continue
            }
            
            healthCheck.checkWithProgress().collect { progress ->
                emit(progress)
                
                // If this is a critical failure and fail-fast is enabled, stop here
                if (config.failFastOnCritical && 
                    progress is HealthCheckProgress.Completed && 
                    progress.result is HealthCheckResult.Failure && 
                    healthCheck.isCritical) {
                    
                    logger.error(TAG, "Critical health check failed, stopping verification: ${healthCheck.name}")
                    return@collect
                }
            }
        }
    }
    
    /**
     * Runs a quick health check of critical components only.
     */
    suspend fun quickHealthCheck(): AppHealthStatus {
        val criticalChecks = allHealthChecks.filter { it.isCritical }
        logger.info(TAG, "Running quick health check on ${criticalChecks.size} critical components")
        
        val config = HealthCheckConfig(
            runInParallel = true,
            failFastOnCritical = false,
            includeNonCritical = false,
            globalTimeoutMs = 15_000L // 15 seconds for quick check
        )
        
        return verifyAppStartup(config)
    }
    
    private suspend fun runHealthChecksInParallel(config: HealthCheckConfig) = coroutineScope {
        val checksToRun = if (config.includeNonCritical) {
            allHealthChecks
        } else {
            allHealthChecks.filter { it.isCritical }
        }
        
        logger.debug(TAG, "Running ${checksToRun.size} health checks in parallel")
        
        val deferredResults = checksToRun.map { healthCheck ->
            async {
                try {
                    val result = healthCheck.check()
                    healthCheckResults[healthCheck.name] = result
                    
                    // If critical check fails and fail-fast is enabled, we could cancel others
                    // but for now we let all complete to get full picture
                    result
                } catch (e: Exception) {
                    val failureResult = HealthCheckResult.Failure(
                        checkName = healthCheck.name,
                        durationMs = 0,
                        timestamp = System.currentTimeMillis(),
                        error = e,
                        message = "Health check execution failed: ${e.message}"
                    )
                    healthCheckResults[healthCheck.name] = failureResult
                    failureResult
                }
            }
        }
        
        deferredResults.awaitAll()
    }
    
    private suspend fun runHealthChecksSequentially(config: HealthCheckConfig) {
        val checksToRun = if (config.includeNonCritical) {
            allHealthChecks
        } else {
            allHealthChecks.filter { it.isCritical }
        }
        
        logger.debug(TAG, "Running ${checksToRun.size} health checks sequentially")
        
        for (healthCheck in checksToRun) {
            try {
                val result = healthCheck.check()
                healthCheckResults[healthCheck.name] = result
                
                // If critical check fails and fail-fast is enabled, stop here
                if (config.failFastOnCritical && 
                    result is HealthCheckResult.Failure && 
                    healthCheck.isCritical) {
                    
                    logger.error(TAG, "Critical health check failed, stopping verification: ${healthCheck.name}")
                    break
                }
                
            } catch (e: Exception) {
                val failureResult = HealthCheckResult.Failure(
                    checkName = healthCheck.name,
                    durationMs = 0,
                    timestamp = System.currentTimeMillis(),
                    error = e,
                    message = "Health check execution failed: ${e.message}"
                )
                healthCheckResults[healthCheck.name] = failureResult
                
                if (config.failFastOnCritical && healthCheck.isCritical) {
                    logger.error(TAG, "Critical health check failed, stopping verification: ${healthCheck.name}")
                    break
                }
            }
        }
    }
    
    private fun analyzeResults(results: List<HealthCheckResult>, totalDuration: Long, timestamp: Long): AppHealthStatus {
        val failures = results.filterIsInstance<HealthCheckResult.Failure>()
        val warnings = results.filterIsInstance<HealthCheckResult.Warning>()
        val criticalFailures = failures.filter { failure ->
            allHealthChecks.find { it.name == failure.checkName }?.isCritical == true
        }
        val nonCriticalFailures = failures - criticalFailures.toSet()
        
        return when {
            criticalFailures.isNotEmpty() -> {
                AppHealthStatus.Unhealthy(
                    timestamp = timestamp,
                    checkResults = results,
                    totalDurationMs = totalDuration,
                    criticalFailures = criticalFailures,
                    nonCriticalFailures = nonCriticalFailures
                )
            }
            warnings.isNotEmpty() || nonCriticalFailures.isNotEmpty() -> {
                AppHealthStatus.Degraded(
                    timestamp = timestamp,
                    checkResults = results,
                    totalDurationMs = totalDuration,
                    warnings = warnings
                )
            }
            else -> {
                AppHealthStatus.Healthy(
                    timestamp = timestamp,
                    checkResults = results,
                    totalDurationMs = totalDuration
                )
            }
        }
    }
    
    private fun getLatestResults(): List<HealthCheckResult> {
        return healthCheckResults.values.toList()
    }
    
    private fun logHealthStatus(status: AppHealthStatus) {
        when (status) {
            is AppHealthStatus.Healthy -> {
                logger.info(TAG, "✅ App startup verification PASSED - All systems healthy (${status.totalDurationMs}ms)")
                status.checkResults.forEach { result ->
                    when (result) {
                        is HealthCheckResult.Success -> logger.info(TAG, "  ✅ ${result.checkName}: OK (${result.durationMs}ms)")
                        else -> {} // Should not happen in healthy status
                    }
                }
            }
            is AppHealthStatus.Degraded -> {
                logger.warn(TAG, "⚠️ App startup verification DEGRADED - Some issues detected (${status.totalDurationMs}ms)")
                status.checkResults.forEach { result ->
                    when (result) {
                        is HealthCheckResult.Success -> logger.info(TAG, "  ✅ ${result.checkName}: OK (${result.durationMs}ms)")
                        is HealthCheckResult.Warning -> logger.warn(TAG, "  ⚠️ ${result.checkName}: ${result.message} (${result.durationMs}ms)")
                        is HealthCheckResult.Failure -> logger.warn(TAG, "  ❌ ${result.checkName}: ${result.message} (${result.durationMs}ms)")
                    }
                }
            }
            is AppHealthStatus.Unhealthy -> {
                logger.error(TAG, "❌ App startup verification FAILED - Critical issues detected (${status.totalDurationMs}ms)")
                status.checkResults.forEach { result ->
                    when (result) {
                        is HealthCheckResult.Success -> logger.info(TAG, "  ✅ ${result.checkName}: OK (${result.durationMs}ms)")
                        is HealthCheckResult.Warning -> logger.warn(TAG, "  ⚠️ ${result.checkName}: ${result.message} (${result.durationMs}ms)")
                        is HealthCheckResult.Failure -> {
                            val isCritical = allHealthChecks.find { it.name == result.checkName }?.isCritical == true
                            val prefix = if (isCritical) "🚨" else "❌"
                            logger.error(TAG, "  $prefix ${result.checkName}: ${result.message} (${result.durationMs}ms)", result.error)
                        }
                    }
                }
            }
        }
    }
    
    // Cache for health check results
    private val healthCheckResults = mutableMapOf<String, HealthCheckResult>()
    
    companion object {
        private const val TAG = "AppStartupVerifier"
    }
}