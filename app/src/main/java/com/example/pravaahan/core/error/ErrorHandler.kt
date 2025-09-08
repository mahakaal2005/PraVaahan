package com.example.pravaahan.core.error

import com.example.pravaahan.core.logging.Logger
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handler for converting exceptions to AppError
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "ErrorHandler"
    }
    
    /**
     * Converts a throwable to an appropriate AppError
     */
    fun handleError(throwable: Throwable): AppError {
        logger.debug(TAG, "Handling error: ${throwable::class.simpleName} - ${throwable.message}")
        
        return when (throwable) {
            // Network errors
            is UnknownHostException -> {
                logger.warn(TAG, "Network connection error", throwable)
                AppError.NetworkError.NoConnection
            }
            is ConnectException -> {
                logger.warn(TAG, "Connection failed", throwable)
                AppError.NetworkError.NoConnection
            }
            is SocketTimeoutException -> {
                logger.warn(TAG, "Request timeout", throwable)
                AppError.NetworkError.Timeout
            }
            is HttpRequestException -> {
                logger.warn(TAG, "HTTP error", throwable)
                AppError.NetworkError.HttpError(
                    code = 0, // HttpRequestException doesn't expose status code directly
                    message = throwable.message ?: "HTTP request failed"
                )
            }
            
            // Supabase/Database errors
            is RestException -> {
                logger.error(TAG, "Supabase REST error", throwable)
                AppError.DatabaseError.QueryFailed(throwable.message ?: "Database query failed")
            }
            // Remove this as SupabaseConnectionException doesn't exist
            
            // Serialization errors
            is SerializationException -> {
                logger.error(TAG, "Data serialization error", throwable)
                AppError.DatabaseError.DataCorrupted(throwable.message ?: "Data format error")
            }
            
            // Validation errors
            is IllegalArgumentException -> {
                logger.warn(TAG, "Validation error", throwable)
                AppError.ValidationError.InvalidInput(
                    field = "unknown",
                    message = throwable.message ?: "Invalid input"
                )
            }
            
            // Business logic errors
            is NoSuchElementException -> {
                logger.warn(TAG, "Data not found", throwable)
                AppError.DatabaseError.DataNotFound("unknown")
            }
            
            // System errors
            is SecurityException -> {
                logger.error(TAG, "Security error", throwable)
                AppError.AuthError.NotAuthorized
            }
            
            // Unknown errors
            else -> {
                logger.error(TAG, "Unexpected error", throwable)
                AppError.SystemError.UnexpectedError(
                    message = throwable.message ?: "An unexpected error occurred",
                    cause = throwable
                )
            }
        }
    }
    
    /**
     * Handles error and returns a Result.failure
     */
    fun <T> handleErrorAsResult(throwable: Throwable): Result<T> {
        val appError = handleError(throwable)
        return Result.failure(appError)
    }
    
    /**
     * Safely executes a block and handles any errors
     */
    suspend fun <T> safeCall(
        operation: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            logger.debug(TAG, "Executing operation: $operation")
            val result = block()
            logger.debug(TAG, "Operation completed successfully: $operation")
            Result.success(result)
        } catch (e: Exception) {
            logger.error(TAG, "Operation failed: $operation", e)
            handleErrorAsResult(e)
        }
    }
    
    /**
     * Logs error details for debugging
     */
    fun logError(error: AppError, context: String = "") {
        val contextInfo = if (context.isNotEmpty()) " [$context]" else ""
        
        when (error) {
            is AppError.NetworkError -> logger.warn(TAG, "Network error$contextInfo: ${error.getUserMessage()}")
            is AppError.DatabaseError -> logger.error(TAG, "Database error$contextInfo: ${error.getUserMessage()}")
            is AppError.AuthError -> logger.warn(TAG, "Auth error$contextInfo: ${error.getUserMessage()}")
            is AppError.ValidationError -> logger.warn(TAG, "Validation error$contextInfo: ${error.getUserMessage()}")
            is AppError.BusinessError -> logger.warn(TAG, "Business error$contextInfo: ${error.getUserMessage()}")
            is AppError.SystemError -> logger.error(TAG, "System error$contextInfo: ${error.getUserMessage()}")
            is AppError.RealtimeError -> logger.warn(TAG, "Realtime error$contextInfo: ${error.getUserMessage()}")
            is AppError.RailwayError -> {
                // Railway errors require special attention due to safety implications
                when (error) {
                    is AppError.RailwayError.CriticalSystemFailure,
                    is AppError.RailwayError.SafetyViolation,
                    is AppError.RailwayError.EmergencyProtocolActivated -> {
                        logger.error(TAG, "CRITICAL RAILWAY ERROR$contextInfo: ${error.getUserMessage()}")
                    }
                    else -> logger.warn(TAG, "Railway error$contextInfo: ${error.getUserMessage()}")
                }
            }
        }
    }
    
    /**
     * Creates a comprehensive error report for railway operations
     */
    fun createErrorReport(
        error: AppError,
        operation: String,
        context: Map<String, Any> = emptyMap()
    ): ErrorReport {
        return ErrorReport(
            error = error,
            operation = operation,
            context = context,
            timestamp = System.currentTimeMillis(),
            isHighPriority = ErrorHandlingUtils.isHighPriorityError(error),
            recoveryAction = ErrorHandlingUtils.getRecoveryAction(error),
            userMessage = error.getUserMessage()
        )
    }
    
    /**
     * Handles railway-specific business rule violations
     */
    fun handleRailwayViolation(
        violationType: String,
        details: String,
        entityId: String = ""
    ): AppError.RailwayError {
        logger.error(TAG, "Railway safety violation: $violationType - $details")
        
        return when (violationType.lowercase()) {
            "safety" -> AppError.RailwayError.SafetyViolation(violationType, details)
            "emergency" -> AppError.RailwayError.EmergencyProtocolActivated(details)
            "critical" -> AppError.RailwayError.CriticalSystemFailure
            "conflict" -> AppError.RailwayError.ConflictResolutionFailed(entityId, details)
            "train_control" -> AppError.RailwayError.TrainControlFailed(entityId, details)
            "signal" -> AppError.RailwayError.SignalSystemError(entityId, details)
            "track" -> AppError.RailwayError.TrackOccupancyError(entityId, details)
            "ai" -> AppError.RailwayError.AIRecommendationFailed(entityId, details)
            else -> AppError.RailwayError.TrainControlFailed(entityId, details)
        }
    }
}

/**
 * Comprehensive error report for railway operations
 */
data class ErrorReport(
    val error: AppError,
    val operation: String,
    val context: Map<String, Any>,
    val timestamp: Long,
    val isHighPriority: Boolean,
    val recoveryAction: String,
    val userMessage: String
) {
    /**
     * Converts the error report to a formatted string for logging
     */
    fun toLogString(): String {
        val priority = if (isHighPriority) "[HIGH PRIORITY] " else ""
        val contextStr = if (context.isNotEmpty()) {
            context.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } else "No additional context"
        
        return """
            ${priority}Railway Error Report:
            Operation: $operation
            Error: ${error::class.simpleName} - $userMessage
            Context: $contextStr
            Recovery: $recoveryAction
            Timestamp: $timestamp
        """.trimIndent()
    }
}