package com.example.pravaahan.core.error

/**
 * Sealed class hierarchy for application errors
 */
sealed class AppError : Exception() {
    
    /**
     * Network-related errors
     */
    sealed class NetworkError : AppError() {
        object NoConnection : NetworkError()
        object Timeout : NetworkError()
        data class HttpError(val code: Int, override val message: String) : NetworkError()
        data class UnknownNetworkError(override val message: String) : NetworkError()
    }
    
    /**
     * Database-related errors
     */
    sealed class DatabaseError : AppError() {
        object ConnectionFailed : DatabaseError()
        data class QueryFailed(override val message: String) : DatabaseError()
        data class DataNotFound(val entityId: String) : DatabaseError()
        data class DataCorrupted(override val message: String) : DatabaseError()
    }
    
    /**
     * Authentication and authorization errors
     */
    sealed class AuthError : AppError() {
        object NotAuthenticated : AuthError()
        object NotAuthorized : AuthError()
        object TokenExpired : AuthError()
        data class AuthenticationFailed(override val message: String) : AuthError()
    }
    
    /**
     * Validation errors
     */
    sealed class ValidationError : AppError() {
        data class InvalidInput(val field: String, override val message: String) : ValidationError()
        data class MissingRequiredField(val field: String) : ValidationError()
        data class InvalidFormat(val field: String, val expectedFormat: String) : ValidationError()
    }
    
    /**
     * Business logic errors
     */
    sealed class BusinessError : AppError() {
        data class ConflictNotResolvable(val conflictId: String, override val message: String) : BusinessError()
        data class TrainNotFound(val trainId: String) : BusinessError()
        data class InvalidTrainStatus(val trainId: String, val status: String) : BusinessError()
        data class OperationNotAllowed(override val message: String) : BusinessError()
    }
    
    /**
     * System errors
     */
    sealed class SystemError : AppError() {
        data class ConfigurationError(override val message: String) : SystemError()
        data class ServiceUnavailable(val service: String) : SystemError()
        data class UnexpectedError(override val message: String, override val cause: Throwable?) : SystemError()
    }
    
    /**
     * Real-time subscription errors
     */
    sealed class RealtimeError : AppError() {
        object SubscriptionFailed : RealtimeError()
        object ConnectionLost : RealtimeError()
        data class ChannelError(override val message: String) : RealtimeError()
    }
    
    /**
     * Railway-specific operational errors
     */
    sealed class RailwayError : AppError() {
        data class ConflictResolutionFailed(val conflictId: String, override val message: String) : RailwayError()
        data class TrainControlFailed(val trainId: String, override val message: String) : RailwayError()
        data class SignalSystemError(val signalId: String, override val message: String) : RailwayError()
        data class TrackOccupancyError(val trackId: String, override val message: String) : RailwayError()
        data class AIRecommendationFailed(val conflictId: String, override val message: String) : RailwayError()
        data class SafetyViolation(val violationType: String, override val message: String) : RailwayError()
        data class EmergencyProtocolActivated(val reason: String) : RailwayError()
        object CriticalSystemFailure : RailwayError()
    }
}

/**
 * Extension function to get user-friendly error message
 */
fun AppError.getUserMessage(): String {
    return when (this) {
        is AppError.NetworkError.NoConnection -> "No internet connection. Please check your network."
        is AppError.NetworkError.Timeout -> "Request timed out. Please try again."
        is AppError.NetworkError.HttpError -> "Server error ($code). Please try again later."
        is AppError.NetworkError.UnknownNetworkError -> "Network error occurred. Please try again."
        
        is AppError.DatabaseError.ConnectionFailed -> "Database connection failed. Please try again."
        is AppError.DatabaseError.QueryFailed -> "Data operation failed. Please try again."
        is AppError.DatabaseError.DataNotFound -> "Requested data not found."
        is AppError.DatabaseError.DataCorrupted -> "Data integrity error. Please contact support."
        
        is AppError.AuthError.NotAuthenticated -> "Please log in to continue."
        is AppError.AuthError.NotAuthorized -> "You don't have permission to perform this action."
        is AppError.AuthError.TokenExpired -> "Session expired. Please log in again."
        is AppError.AuthError.AuthenticationFailed -> "Authentication failed. Please check your credentials."
        
        is AppError.ValidationError.InvalidInput -> "Invalid $field: $message"
        is AppError.ValidationError.MissingRequiredField -> "$field is required."
        is AppError.ValidationError.InvalidFormat -> "$field must be in $expectedFormat format."
        
        is AppError.BusinessError.ConflictNotResolvable -> "Conflict cannot be resolved: $message"
        is AppError.BusinessError.TrainNotFound -> "Train not found."
        is AppError.BusinessError.InvalidTrainStatus -> "Invalid train status."
        is AppError.BusinessError.OperationNotAllowed -> message
        
        is AppError.SystemError.ConfigurationError -> "System configuration error. Please contact support."
        is AppError.SystemError.ServiceUnavailable -> "$service is currently unavailable."
        is AppError.SystemError.UnexpectedError -> "An unexpected error occurred. Please try again."
        
        is AppError.RealtimeError.SubscriptionFailed -> "Real-time updates unavailable. Data may be outdated."
        is AppError.RealtimeError.ConnectionLost -> "Real-time connection lost. Attempting to reconnect..."
        is AppError.RealtimeError.ChannelError -> "Real-time channel error: $message"
        
        is AppError.RailwayError.ConflictResolutionFailed -> "Failed to resolve conflict: $message"
        is AppError.RailwayError.TrainControlFailed -> "Train control operation failed: $message"
        is AppError.RailwayError.SignalSystemError -> "Signal system error: $message"
        is AppError.RailwayError.TrackOccupancyError -> "Track occupancy error: $message"
        is AppError.RailwayError.AIRecommendationFailed -> "AI recommendation system unavailable: $message"
        is AppError.RailwayError.SafetyViolation -> "Safety violation detected: $message"
        is AppError.RailwayError.EmergencyProtocolActivated -> "Emergency protocol activated: $reason"
        is AppError.RailwayError.CriticalSystemFailure -> "CRITICAL: System failure detected. Contact control center immediately."
    }
}