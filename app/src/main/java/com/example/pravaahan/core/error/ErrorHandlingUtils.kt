package com.example.pravaahan.core.error

import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry

/**
 * Utility functions for common error handling patterns
 */
object ErrorHandlingUtils {
    
    /**
     * Wraps a Flow with error handling and retry logic
     */
    fun <T> Flow<T>.handleErrorsWithRetry(
        logger: Logger,
        errorHandler: ErrorHandler,
        operation: String,
        maxRetries: Long = 2
    ): Flow<Result<T>> {
        return this
            .map { Result.success(it) }
            .retry(maxRetries) { cause ->
                val appError = errorHandler.handleError(cause)
                val shouldRetry = when (appError) {
                    is AppError.NetworkError.NoConnection,
                    is AppError.NetworkError.Timeout,
                    is AppError.DatabaseError.ConnectionFailed,
                    is AppError.RealtimeError.ConnectionLost -> true
                    else -> false
                }
                
                if (shouldRetry) {
                    logger.warn("ErrorHandlingUtils", "Retrying $operation due to: ${appError.getUserMessage()}")
                }
                
                shouldRetry
            }
            .catch { cause ->
                val appError = errorHandler.handleError(cause)
                logger.error("ErrorHandlingUtils", "Operation $operation failed: ${appError.getUserMessage()}")
                emit(Result.failure(appError))
            }
    }
    
    /**
     * Wraps a suspend function with error handling
     */
    suspend fun <T> safeExecute(
        operation: String,
        logger: Logger,
        errorHandler: ErrorHandler,
        block: suspend () -> T
    ): Result<T> {
        return try {
            logger.debug("ErrorHandlingUtils", "Executing: $operation")
            val result = block()
            logger.debug("ErrorHandlingUtils", "Completed: $operation")
            Result.success(result)
        } catch (e: Exception) {
            val appError = errorHandler.handleError(e)
            logger.error("ErrorHandlingUtils", "Failed: $operation - ${appError.getUserMessage()}")
            Result.failure(appError)
        }
    }
    
    /**
     * Validates input and returns appropriate validation error
     */
    fun validateInput(
        field: String,
        value: String?,
        validator: (String) -> Boolean,
        errorMessage: String
    ): Result<String> {
        return when {
            value.isNullOrBlank() -> Result.failure(
                AppError.ValidationError.MissingRequiredField(field)
            )
            !validator(value) -> Result.failure(
                AppError.ValidationError.InvalidInput(field, errorMessage)
            )
            else -> Result.success(value)
        }
    }
    
    /**
     * Validates train ID format
     */
    fun validateTrainId(trainId: String?): Result<String> {
        return validateInput(
            field = "Train ID",
            value = trainId,
            validator = { it.matches(Regex("^[A-Z0-9-]+$")) && it.length >= 3 },
            errorMessage = "Train ID must contain only letters, numbers, and hyphens, minimum 3 characters"
        )
    }
    
    /**
     * Validates conflict ID format
     */
    fun validateConflictId(conflictId: String?): Result<String> {
        return validateInput(
            field = "Conflict ID",
            value = conflictId,
            validator = { it.matches(Regex("^conflict-[a-z0-9-]+$")) },
            errorMessage = "Conflict ID must start with 'conflict-' followed by alphanumeric characters"
        )
    }
    
    /**
     * Validates controller action text
     */
    fun validateControllerAction(action: String?): Result<String> {
        return validateInput(
            field = "Controller Action",
            value = action,
            validator = { it.length >= 10 && it.length <= 500 },
            errorMessage = "Controller action must be between 10 and 500 characters"
        )
    }
    
    /**
     * Creates a railway-specific error based on operation context
     */
    fun createRailwayError(
        operation: String,
        entityId: String,
        message: String
    ): AppError.RailwayError {
        return when {
            operation.contains("conflict", ignoreCase = true) -> 
                AppError.RailwayError.ConflictResolutionFailed(entityId, message)
            operation.contains("train", ignoreCase = true) -> 
                AppError.RailwayError.TrainControlFailed(entityId, message)
            operation.contains("signal", ignoreCase = true) -> 
                AppError.RailwayError.SignalSystemError(entityId, message)
            operation.contains("track", ignoreCase = true) -> 
                AppError.RailwayError.TrackOccupancyError(entityId, message)
            operation.contains("ai", ignoreCase = true) || operation.contains("recommendation", ignoreCase = true) -> 
                AppError.RailwayError.AIRecommendationFailed(entityId, message)
            else -> AppError.RailwayError.TrainControlFailed(entityId, message)
        }
    }
    
    /**
     * Determines if an error requires immediate attention from controllers
     */
    fun isHighPriorityError(error: AppError): Boolean {
        return when (error) {
            is AppError.RailwayError.SafetyViolation,
            is AppError.RailwayError.EmergencyProtocolActivated,
            is AppError.RailwayError.CriticalSystemFailure,
            is AppError.RailwayError.ConflictResolutionFailed,
            is AppError.SystemError.ServiceUnavailable -> true
            is AppError.NetworkError -> false // Network errors are usually temporary
            is AppError.ValidationError -> false // Validation errors are user input issues
            else -> false
        }
    }
    
    /**
     * Gets appropriate recovery action for an error
     */
    fun getRecoveryAction(error: AppError): String {
        return when (error) {
            is AppError.NetworkError.NoConnection -> "Check network connection and retry"
            is AppError.NetworkError.Timeout -> "Retry the operation"
            is AppError.DatabaseError.ConnectionFailed -> "Check system status and retry"
            is AppError.AuthError.TokenExpired -> "Please log in again"
            is AppError.ValidationError -> "Correct the input and try again"
            is AppError.RailwayError.ConflictResolutionFailed -> "Review conflict details and try manual resolution"
            is AppError.RailwayError.TrainControlFailed -> "Check train status and retry operation"
            is AppError.RailwayError.CriticalSystemFailure -> "Contact control center immediately"
            is AppError.RealtimeError.ConnectionLost -> "System will attempt to reconnect automatically"
            else -> "Contact technical support if the problem persists"
        }
    }
}