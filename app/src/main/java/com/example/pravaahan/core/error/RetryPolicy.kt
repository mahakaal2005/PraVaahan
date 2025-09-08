package com.example.pravaahan.core.error

import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

/**
 * Retry policy configuration for different types of operations
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000L,
    val maxDelayMs: Long = 30000L,
    val backoffMultiplier: Double = 2.0,
    val jitterFactor: Double = 0.1,
    val retryableErrors: Set<Class<out AppError>> = defaultRetryableErrors
) {
    companion object {
        /**
         * Default set of retryable error types
         */
        val defaultRetryableErrors = setOf(
            AppError.NetworkError.NoConnection::class.java,
            AppError.NetworkError.Timeout::class.java,
            AppError.DatabaseError.ConnectionFailed::class.java,
            AppError.RealtimeError.ConnectionLost::class.java,
            AppError.RealtimeError.SubscriptionFailed::class.java
        )
        
        /**
         * Retry policy for critical railway operations
         */
        val CRITICAL_OPERATIONS = RetryPolicy(
            maxAttempts = 5,
            initialDelayMs = 500L,
            maxDelayMs = 10000L,
            backoffMultiplier = 1.5,
            jitterFactor = 0.05
        )
        
        /**
         * Retry policy for real-time data operations
         */
        val REALTIME_OPERATIONS = RetryPolicy(
            maxAttempts = 3,
            initialDelayMs = 1000L,
            maxDelayMs = 5000L,
            backoffMultiplier = 2.0,
            jitterFactor = 0.1
        )
        
        /**
         * Retry policy for background operations
         */
        val BACKGROUND_OPERATIONS = RetryPolicy(
            maxAttempts = 3,
            initialDelayMs = 2000L,
            maxDelayMs = 60000L,
            backoffMultiplier = 2.0,
            jitterFactor = 0.2
        )
        
        /**
         * No retry policy for operations that should not be retried
         */
        val NO_RETRY = RetryPolicy(
            maxAttempts = 1,
            retryableErrors = emptySet()
        )
    }
    
    /**
     * Determines if an error is retryable based on the policy
     */
    fun isRetryable(error: AppError): Boolean {
        return retryableErrors.any { it.isInstance(error) }
    }
    
    /**
     * Calculates the delay for the next retry attempt
     */
    fun calculateDelay(attempt: Int): Long {
        val baseDelay = initialDelayMs * backoffMultiplier.pow(attempt - 1)
        val jitter = baseDelay * jitterFactor * Random.nextDouble(-1.0, 1.0)
        return (baseDelay + jitter).toLong().coerceAtMost(maxDelayMs)
    }
}

/**
 * Retry mechanism utility for executing operations with retry logic
 */
class RetryMechanism(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "RetryMechanism"
    }
    
    /**
     * Executes an operation with retry logic based on the provided policy
     */
    suspend fun <T> executeWithRetry(
        operation: String,
        policy: RetryPolicy = RetryPolicy(),
        block: suspend (attempt: Int) -> Result<T>
    ): Result<T> {
        var lastError: AppError? = null
        
        repeat(policy.maxAttempts) { attempt ->
            val attemptNumber = attempt + 1
            
            try {
                logger.debug(TAG, "Executing $operation (attempt $attemptNumber/${policy.maxAttempts})")
                
                val result = block(attemptNumber)
                
                return result.fold(
                    onSuccess = { value ->
                        if (attemptNumber > 1) {
                            logger.info(TAG, "$operation succeeded on attempt $attemptNumber")
                        }
                        Result.success(value)
                    },
                    onFailure = { throwable ->
                        val error = if (throwable is AppError) throwable else {
                            AppError.SystemError.UnexpectedError(
                                message = throwable.message ?: "Unknown error",
                                cause = throwable
                            )
                        }
                        
                        lastError = error
                        
                        if (!policy.isRetryable(error)) {
                            logger.warn(TAG, "$operation failed with non-retryable error: ${error.getUserMessage()}")
                            return Result.failure(error)
                        }
                        
                        if (attemptNumber < policy.maxAttempts) {
                            val delayMs = policy.calculateDelay(attemptNumber)
                            logger.warn(TAG, "$operation failed (attempt $attemptNumber), retrying in ${delayMs}ms: ${error.getUserMessage()}")
                            delay(delayMs)
                        } else {
                            logger.error(TAG, "$operation failed after $attemptNumber attempts: ${error.getUserMessage()}")
                        }
                        
                        // Continue to next attempt
                        null
                    }
                ) ?: return@repeat // Continue if result was null (retry case)
                
            } catch (e: Exception) {
                val error = AppError.SystemError.UnexpectedError(
                    message = e.message ?: "Unexpected error during retry execution",
                    cause = e
                )
                lastError = error
                logger.error(TAG, "$operation failed with unexpected error on attempt $attemptNumber", e)
                
                if (attemptNumber >= policy.maxAttempts) {
                    return Result.failure(error)
                }
            }
        }
        
        return Result.failure(lastError ?: AppError.SystemError.UnexpectedError("Operation failed after retries", null))
    }
    
    /**
     * Executes an operation with retry logic, converting exceptions to AppError
     */
    suspend fun <T> executeWithRetryAndErrorHandling(
        operation: String,
        policy: RetryPolicy = RetryPolicy(),
        errorHandler: ErrorHandler,
        block: suspend (attempt: Int) -> T
    ): Result<T> {
        return executeWithRetry(operation, policy) { attempt ->
            try {
                val result = block(attempt)
                Result.success(result)
            } catch (e: Exception) {
                val appError = errorHandler.handleError(e)
                Result.failure(appError)
            }
        }
    }
}