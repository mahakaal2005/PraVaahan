package com.example.pravaahan.core.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pravaahan.core.logging.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Extension functions for ViewModel error handling
 */

/**
 * Launches a coroutine with proper error handling for ViewModels
 */
fun ViewModel.launchWithErrorHandling(
    logger: Logger,
    errorHandler: ErrorHandler,
    operation: String,
    onError: (AppError) -> Unit = {},
    block: suspend () -> Unit
) {
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        val appError = errorHandler.handleError(exception)
        errorHandler.logError(appError, operation)
        onError(appError)
    }
    
    viewModelScope.launch(exceptionHandler) {
        try {
            logger.debug("ViewModel", "Starting operation: $operation")
            block()
            logger.debug("ViewModel", "Completed operation: $operation")
        } catch (e: Exception) {
            val appError = errorHandler.handleError(e)
            errorHandler.logError(appError, operation)
            onError(appError)
        }
    }
}

/**
 * Wraps a Flow with comprehensive error handling for ViewModels
 */
fun <T> Flow<T>.handleViewModelErrors(
    logger: Logger,
    errorHandler: ErrorHandler,
    operation: String
): Flow<Result<T>> {
    return this
        .map { Result.success(it) }
        .onStart {
            logger.debug("ViewModel", "Starting flow operation: $operation")
        }
        .catch { exception ->
            val appError = errorHandler.handleError(exception)
            errorHandler.logError(appError, operation)
            logger.error("ViewModel", "Flow operation failed: $operation", exception)
            emit(Result.failure<T>(appError))
        }
}

/**
 * Error boundary for UI state updates
 */
inline fun <T> safeStateUpdate(
    logger: Logger,
    operation: String,
    crossinline update: () -> T
): T? {
    return try {
        update()
    } catch (e: Exception) {
        logger.error("ViewModel", "State update failed: $operation", e)
        null
    }
}

/**
 * Creates a standardized error context for railway operations
 */
fun createRailwayErrorContext(
    trainId: String? = null,
    conflictId: String? = null,
    controllerId: String? = null,
    sectionId: String? = null,
    additionalContext: Map<String, Any> = emptyMap()
): Map<String, Any> {
    val context = mutableMapOf<String, Any>()
    
    trainId?.let { context["trainId"] = it }
    conflictId?.let { context["conflictId"] = it }
    controllerId?.let { context["controllerId"] = it }
    sectionId?.let { context["sectionId"] = it }
    context["timestamp"] = System.currentTimeMillis()
    context["system"] = "PraVaahan"
    
    context.putAll(additionalContext)
    
    return context
}

/**
 * Error recovery strategies for different error types
 */
object ErrorRecoveryStrategies {
    
    /**
     * Determines if an operation should be retried automatically
     */
    fun shouldAutoRetry(error: AppError): Boolean {
        return when (error) {
            is AppError.NetworkError.NoConnection,
            is AppError.NetworkError.Timeout,
            is AppError.DatabaseError.ConnectionFailed,
            is AppError.RealtimeError.ConnectionLost,
            is AppError.RealtimeError.SubscriptionFailed -> true
            else -> false
        }
    }
    
    /**
     * Gets the appropriate retry policy for an error type
     */
    fun getRetryPolicy(error: AppError): RetryPolicy {
        return when (error) {
            is AppError.RailwayError -> RetryPolicy.CRITICAL_OPERATIONS
            is AppError.RealtimeError -> RetryPolicy.REALTIME_OPERATIONS
            is AppError.NetworkError -> RetryPolicy.BACKGROUND_OPERATIONS
            else -> RetryPolicy.NO_RETRY
        }
    }
    
    /**
     * Determines if an error requires immediate user notification
     */
    fun requiresImmediateNotification(error: AppError): Boolean {
        return when (error) {
            is AppError.RailwayError.CriticalSystemFailure,
            is AppError.RailwayError.SafetyViolation,
            is AppError.RailwayError.EmergencyProtocolActivated -> true
            else -> false
        }
    }
    
    /**
     * Gets fallback data for when primary data source fails
     */
    fun <T> getFallbackData(
        operation: String,
        error: AppError,
        defaultValue: T
    ): T {
        // For railway operations, we might want to return cached data
        // or safe default values rather than empty data
        return when (error) {
            is AppError.NetworkError,
            is AppError.DatabaseError.ConnectionFailed,
            is AppError.RealtimeError -> {
                // Return cached data or safe defaults
                defaultValue
            }
            else -> defaultValue
        }
    }
}