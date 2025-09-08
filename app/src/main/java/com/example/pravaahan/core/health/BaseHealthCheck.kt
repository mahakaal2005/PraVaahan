package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlin.system.measureTimeMillis

/**
 * Base implementation of HealthCheck that provides common functionality
 * like timing, retries, and progress reporting.
 */
abstract class BaseHealthCheck(
    override val name: String,
    override val isCritical: Boolean,
    override val timeoutMs: Long,
    override val maxRetries: Int,
    protected val logger: Logger
) : HealthCheck {
    
    override suspend fun check(): HealthCheckResult {
        val startTime = System.currentTimeMillis()
        var lastException: Throwable? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                val duration = measureTimeMillis {
                    withTimeout(timeoutMs) {
                        performCheck()
                    }
                }
                
                logger.info(TAG, "Health check '$name' passed in ${duration}ms (attempt ${attempt + 1})")
                return HealthCheckResult.Success(
                    checkName = name,
                    durationMs = duration,
                    timestamp = startTime,
                    details = getSuccessDetails()
                )
                
            } catch (e: Exception) {
                lastException = e
                logger.warn(TAG, "Health check '$name' failed on attempt ${attempt + 1}: ${e.message}", e)
                
                if (attempt < maxRetries) {
                    // Wait before retry with exponential backoff
                    val delayMs = minOf(1000L * (1L shl attempt), 5000L)
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }
        
        val totalDuration = System.currentTimeMillis() - startTime
        logger.error(TAG, "Health check '$name' failed after ${maxRetries + 1} attempts", lastException)
        
        return HealthCheckResult.Failure(
            checkName = name,
            durationMs = totalDuration,
            timestamp = startTime,
            error = lastException ?: Exception("Unknown error"),
            message = getFailureMessage(lastException),
            retryAttempt = maxRetries
        )
    }
    
    override fun checkWithProgress(): Flow<HealthCheckProgress> = flow {
        emit(HealthCheckProgress.Started(name))
        
        var attempt = 0
        var lastException: Throwable? = null
        val startTime = System.currentTimeMillis()
        
        while (attempt <= maxRetries) {
            try {
                emit(HealthCheckProgress.InProgress(name, "Attempting check (${attempt + 1}/${maxRetries + 1})"))
                
                val duration = measureTimeMillis {
                    withTimeout(timeoutMs) {
                        performCheck()
                    }
                }
                
                val result = HealthCheckResult.Success(
                    checkName = name,
                    durationMs = duration,
                    timestamp = startTime,
                    details = getSuccessDetails()
                )
                
                emit(HealthCheckProgress.Completed(name, result))
                return@flow
                
            } catch (e: Exception) {
                lastException = e
                attempt++
                
                if (attempt <= maxRetries) {
                    emit(HealthCheckProgress.InProgress(name, "Retrying after failure: ${e.message}"))
                    val delayMs = minOf(1000L * (1L shl (attempt - 1)), 5000L)
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }
        
        val totalDuration = System.currentTimeMillis() - startTime
        val result = HealthCheckResult.Failure(
            checkName = name,
            durationMs = totalDuration,
            timestamp = startTime,
            error = lastException ?: Exception("Unknown error"),
            message = getFailureMessage(lastException),
            retryAttempt = maxRetries
        )
        
        emit(HealthCheckProgress.Completed(name, result))
    }
    
    /**
     * Performs the actual health check logic.
     * Implementations should throw an exception if the check fails.
     */
    protected abstract suspend fun performCheck()
    
    /**
     * Returns additional details when the health check succeeds
     */
    protected open fun getSuccessDetails(): String? = null
    
    /**
     * Returns a user-friendly error message when the health check fails
     */
    protected open fun getFailureMessage(exception: Throwable?): String {
        return exception?.message ?: "Health check failed for unknown reason"
    }
    
    companion object {
        private const val TAG = "BaseHealthCheck"
    }
}