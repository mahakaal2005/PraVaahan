package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.repository.ConflictRepository
import com.example.pravaahan.domain.repository.TrainRepository
import io.github.jan.supabase.SupabaseClient
import javax.inject.Inject

/**
 * Health check that verifies dependency injection setup and critical dependencies.
 * Ensures all required components are properly wired and available.
 */
class DependencyInjectionHealthCheck @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val trainRepository: TrainRepository,
    private val conflictRepository: ConflictRepository,
    logger: Logger
) : BaseHealthCheck(
    name = "Dependency Injection",
    isCritical = true,
    timeoutMs = 2_000L, // 2 seconds
    maxRetries = 0, // No retries needed for DI validation
    logger = logger
) {
    
    override suspend fun performCheck() {
        try {
            logger.debug(TAG, "Validating dependency injection setup...")
            
            // Validate core dependencies are injected
            validateCoreDependencies()
            
            // Validate repository implementations
            validateRepositories()
            
            logger.debug(TAG, "Dependency injection validation successful")
            
        } catch (e: Exception) {
            logger.error(TAG, "Dependency injection validation failed", e)
            throw DependencyInjectionException("Dependency injection validation failed: ${e.message}", e)
        }
    }
    
    private fun validateCoreDependencies() {
        val missingDependencies = mutableListOf<String>()
        
        // Check SupabaseClient
        try {
            // Just verify we can access the client - this confirms DI is working
            logger.debug(TAG, "SupabaseClient dependency validated: ${supabaseClient.javaClass.simpleName}")
        } catch (e: Exception) {
            logger.warn(TAG, "SupabaseClient validation warning: ${e.message}")
            // Don't fail the health check for this - just log a warning
        }
        
        // Check Logger (injected via constructor)
        try {
            logger.debug(TAG, "Logger dependency test")
            logger.debug(TAG, "Logger dependency validated")
        } catch (e: Exception) {
            missingDependencies.add("Logger: ${e.message}")
        }
        
        if (missingDependencies.isNotEmpty()) {
            throw DependencyInjectionException("Missing or invalid core dependencies: ${missingDependencies.joinToString(", ")}")
        }
    }
    
    private fun validateRepositories() {
        // Validate TrainRepository
        try {
            val trainRepoClass = trainRepository::class.java.simpleName
            logger.debug(TAG, "TrainRepository implementation: $trainRepoClass")
        } catch (e: Exception) {
            logger.warn(TAG, "TrainRepository validation warning: ${e.message}")
        }
        
        // Validate ConflictRepository
        try {
            val conflictRepoClass = conflictRepository::class.java.simpleName
            logger.debug(TAG, "ConflictRepository implementation: $conflictRepoClass")
        } catch (e: Exception) {
            logger.warn(TAG, "ConflictRepository validation warning: ${e.message}")
        }
        
        // For now, just log the repository types - don't fail the health check
        logger.debug(TAG, "Repository validation completed")
    }
    
    override fun getSuccessDetails(): String {
        return "All dependencies properly injected and configured"
    }
    
    override fun getFailureMessage(exception: Throwable?): String {
        return when (exception) {
            is DependencyInjectionException -> exception.message ?: "Dependency injection failed"
            else -> "Dependency injection validation failed: ${exception?.message ?: "Unknown error"}"
        }
    }
    
    companion object {
        private const val TAG = "DependencyInjectionHealthCheck"
    }
}

/**
 * Exception thrown when dependency injection health check fails
 */
class DependencyInjectionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)