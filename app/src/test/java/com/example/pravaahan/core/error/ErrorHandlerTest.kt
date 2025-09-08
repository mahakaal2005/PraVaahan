package com.example.pravaahan.core.error

import com.example.pravaahan.core.logging.Logger
import io.github.jan.supabase.exceptions.RestException
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.mock
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Tests for the ErrorHandler class
 */
class ErrorHandlerTest {

    private lateinit var logger: Logger
    private lateinit var errorHandler: ErrorHandler

    @BeforeEach
    fun setup() {
        logger = mock()
        errorHandler = ErrorHandler(logger)
    }

    @Test
    fun `handleError converts UnknownHostException to NetworkError NoConnection`() {
        val exception = UnknownHostException("Host not found")
        
        val result = errorHandler.handleError(exception)
        
        assertTrue(result is AppError.NetworkError.NoConnection)
    }

    @Test
    fun `handleError converts SocketTimeoutException to NetworkError Timeout`() {
        val exception = SocketTimeoutException("Connection timed out")
        
        val result = errorHandler.handleError(exception)
        
        assertTrue(result is AppError.NetworkError.Timeout)
    }

    @Test
    fun `handleError converts RestException to DatabaseError QueryFailed`() {
        val exception = RestException("Query failed", "Query failed", 400, "Query failed")
        
        val result = errorHandler.handleError(exception)
        
        assertTrue(result is AppError.DatabaseError.QueryFailed)
        assertEquals("Query failed", (result as AppError.DatabaseError.QueryFailed).message)
    }

    @Test
    fun `handleError converts SerializationException to DatabaseError DataCorrupted`() {
        val exception = SerializationException("Invalid JSON format")
        
        val result = errorHandler.handleError(exception)
        
        assertTrue(result is AppError.DatabaseError.DataCorrupted)
        assertEquals("Invalid JSON format", (result as AppError.DatabaseError.DataCorrupted).message)
    }

    @Test
    fun `handleError converts IllegalArgumentException to ValidationError InvalidInput`() {
        val exception = IllegalArgumentException("Invalid train ID format")
        
        val result = errorHandler.handleError(exception)
        
        assertTrue(result is AppError.ValidationError.InvalidInput)
        assertEquals("Invalid train ID format", (result as AppError.ValidationError.InvalidInput).message)
    }

    @Test
    fun `handleError converts SecurityException to AuthError NotAuthorized`() {
        val exception = SecurityException("Access denied")
        
        val result = errorHandler.handleError(exception)
        
        assertTrue(result is AppError.AuthError.NotAuthorized)
    }

    @Test
    fun `handleError converts unknown exception to SystemError UnexpectedError`() {
        val exception = RuntimeException("Unknown error")
        
        val result = errorHandler.handleError(exception)
        
        assertTrue(result is AppError.SystemError.UnexpectedError)
        assertEquals("Unknown error", (result as AppError.SystemError.UnexpectedError).message)
        assertEquals(exception, result.cause)
    }

    @Test
    fun `handleErrorAsResult returns Result failure with AppError`() {
        val exception = UnknownHostException("Host not found")
        
        val result: Result<String> = errorHandler.handleErrorAsResult(exception)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.NetworkError.NoConnection)
    }

    @Test
    fun `createErrorReport creates comprehensive error report`() {
        val error = AppError.RailwayError.ConflictResolutionFailed("conflict-001", "AI system unavailable")
        val context = mapOf("trainId" to "12001", "controllerId" to "ctrl-001")
        
        val report = errorHandler.createErrorReport(error, "resolve_conflict", context)
        
        assertEquals(error, report.error)
        assertEquals("resolve_conflict", report.operation)
        assertEquals(context, report.context)
        assertTrue(report.isHighPriority)
        assertEquals("Review conflict details and try manual resolution", report.recoveryAction)
        assertEquals("Failed to resolve conflict: AI system unavailable", report.userMessage)
    }

    @Test
    fun `handleRailwayViolation creates appropriate railway error`() {
        val error = errorHandler.handleRailwayViolation(
            violationType = "safety",
            details = "Train exceeding speed limit",
            entityId = "train-001"
        )
        
        assertTrue(error is AppError.RailwayError.SafetyViolation)
        assertEquals("safety", (error as AppError.RailwayError.SafetyViolation).violationType)
        assertEquals("Train exceeding speed limit", error.message)
    }
}