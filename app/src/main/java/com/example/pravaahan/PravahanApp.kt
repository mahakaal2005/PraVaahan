package com.example.pravaahan

import android.app.Application
import com.example.pravaahan.core.health.AppHealthStatus
import com.example.pravaahan.core.health.AppStartupVerifier
import com.example.pravaahan.core.health.HealthCheckConfig
import com.example.pravaahan.core.logging.Logger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PravahanApp : Application() {
    
    @Inject
    lateinit var logger: Logger
    
    @Inject
    lateinit var appStartupVerifier: AppStartupVerifier
    
    // Application-scoped coroutine scope for health checks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Health status accessible to other components
    @Volatile
    var appHealthStatus: AppHealthStatus? = null
        private set
    
    companion object {
        private const val TAG = "PravahanApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Use Android Log directly initially to avoid Hilt injection issues
        android.util.Log.i(TAG, "PraVaahan railway control application starting...")
        android.util.Log.i(TAG, "App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        android.util.Log.i(TAG, "Build type: ${BuildConfig.BUILD_TYPE}")
        android.util.Log.i(TAG, "Debug mode: ${BuildConfig.DEBUG}")
        android.util.Log.i(TAG, "Target SDK: 35")
        
        setupCrashReporting()
        
        // Perform startup health checks after Hilt injection is complete
        performStartupHealthChecks()
        
        android.util.Log.i(TAG, "Application initialization completed")
    }
    
    private fun performStartupHealthChecks() {
        android.util.Log.i(TAG, "Initiating comprehensive startup health verification...")
        
        applicationScope.launch {
            try {
                val config = HealthCheckConfig(
                    runInParallel = true,
                    failFastOnCritical = false, // Get full picture even if some critical checks fail
                    includeNonCritical = true,
                    globalTimeoutMs = 30_000L // 30 seconds for startup
                )
                
                val healthStatus = appStartupVerifier.verifyAppStartup(config)
                appHealthStatus = healthStatus
                
                when (healthStatus) {
                    is AppHealthStatus.Healthy -> {
                        logger.info(TAG, "🚀 PraVaahan startup verification SUCCESSFUL - All systems operational")
                        logger.info(TAG, "Railway control system ready for operations")
                        android.util.Log.i(TAG, "✅ All health checks passed - System ready")
                    }
                    is AppHealthStatus.Degraded -> {
                        logger.warn(TAG, "⚠️ PraVaahan startup verification DEGRADED - Some non-critical issues detected")
                        logger.warn(TAG, "Railway control system operational with limited functionality")
                        android.util.Log.w(TAG, "⚠️ System degraded - ${healthStatus.warnings.size} warnings")
                        logHealthIssues(healthStatus)
                    }
                    is AppHealthStatus.Unhealthy -> {
                        logger.error(TAG, "🚨 PraVaahan startup verification FAILED - Critical systems unavailable")
                        logger.error(TAG, "Railway control system may not function properly")
                        android.util.Log.e(TAG, "🚨 CRITICAL: ${healthStatus.criticalFailures.size} critical failures detected")
                        logHealthIssues(healthStatus)
                        
                        // In a production railway system, you might want to:
                        // 1. Show a critical error screen
                        // 2. Attempt automatic recovery
                        // 3. Alert system administrators
                        // 4. Fall back to offline mode if available
                    }
                }
                
            } catch (e: Exception) {
                logger.error(TAG, "Startup health verification failed with exception", e)
                android.util.Log.e(TAG, "Health check system failed", e)
                appHealthStatus = AppHealthStatus.Unhealthy(
                    timestamp = System.currentTimeMillis(),
                    checkResults = emptyList(),
                    totalDurationMs = 0,
                    criticalFailures = emptyList(),
                    nonCriticalFailures = emptyList()
                )
            }
        }
    }
    
    private fun logHealthIssues(healthStatus: AppHealthStatus) {
        when (healthStatus) {
            is AppHealthStatus.Degraded -> {
                healthStatus.warnings.forEach { warning ->
                    logger.warn(TAG, "Health Warning - ${warning.checkName}: ${warning.message}")
                    android.util.Log.w(TAG, "⚠️ ${warning.checkName}: ${warning.message}")
                }
            }
            is AppHealthStatus.Unhealthy -> {
                healthStatus.criticalFailures.forEach { failure ->
                    logger.error(TAG, "CRITICAL FAILURE - ${failure.checkName}: ${failure.message}", failure.error)
                    android.util.Log.e(TAG, "🚨 CRITICAL: ${failure.checkName} - ${failure.message}")
                }
                healthStatus.nonCriticalFailures.forEach { failure ->
                    logger.warn(TAG, "Non-critical failure - ${failure.checkName}: ${failure.message}", failure.error)
                    android.util.Log.w(TAG, "❌ ${failure.checkName}: ${failure.message}")
                }
            }
            else -> {} // Healthy status doesn't need issue logging
        }
    }
    
    /**
     * Performs a runtime health check that can be called by other components
     */
    fun performRuntimeHealthCheck(callback: (AppHealthStatus) -> Unit) {
        applicationScope.launch {
            try {
                val healthStatus = appStartupVerifier.quickHealthCheck()
                appHealthStatus = healthStatus
                callback(healthStatus)
            } catch (e: Exception) {
                logger.error(TAG, "Runtime health check failed", e)
                android.util.Log.e(TAG, "Runtime health check failed", e)
                val unhealthyStatus = AppHealthStatus.Unhealthy(
                    timestamp = System.currentTimeMillis(),
                    checkResults = emptyList(),
                    totalDurationMs = 0,
                    criticalFailures = emptyList(),
                    nonCriticalFailures = emptyList()
                )
                appHealthStatus = unhealthyStatus
                callback(unhealthyStatus)
            }
        }
    }
    
    private fun setupCrashReporting() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            android.util.Log.e(TAG, "🚨 CRITICAL: Uncaught exception in thread ${thread.name}", exception)
            android.util.Log.e(TAG, "🚨 CRITICAL: Railway control system crashed - immediate attention required")
            
            // Log critical system information
            android.util.Log.e(TAG, "CRITICAL: System Info - Available memory: ${Runtime.getRuntime().freeMemory()}")
            android.util.Log.e(TAG, "CRITICAL: System Info - Total memory: ${Runtime.getRuntime().totalMemory()}")
            
            // In a production railway system, this would trigger:
            // 1. Immediate alert to control center
            // 2. Automatic failover to backup systems
            // 3. Crash report to development team
            // 4. System state preservation for analysis
            
            // Call the default handler to maintain normal crash behavior
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        android.util.Log.i(TAG, "Enhanced crash reporting configured for railway system")
    }
}
