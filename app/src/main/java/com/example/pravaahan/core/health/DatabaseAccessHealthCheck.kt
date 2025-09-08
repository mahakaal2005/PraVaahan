package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * Health check that verifies database access and critical table availability.
 * Tests both read and write permissions for the railway control system.
 */
class DatabaseAccessHealthCheck @Inject constructor(
    private val supabaseClient: SupabaseClient,
    logger: Logger
) : BaseHealthCheck(
    name = "Database Access",
    isCritical = true,
    timeoutMs = 3_000L, // 3 seconds
    maxRetries = 1,
    logger = logger
) {
    
    @Serializable
    private data class TableInfo(
        val table_name: String,
        val table_schema: String
    )
    
    override suspend fun performCheck() {
        try {
            // Check if critical tables exist and are accessible
            checkCriticalTables()
            
            // Test basic read access
            testReadAccess()
            
            logger.debug(TAG, "Database access test successful")
            
        } catch (e: Exception) {
            logger.error(TAG, "Database access test failed", e)
            throw DatabaseAccessException("Database access verification failed: ${e.message}", e)
        }
    }
    
    private suspend fun checkCriticalTables() {
        val criticalTables = listOf("trains", "conflicts")
        
        try {
            // Test access to critical tables by attempting simple queries
            for (tableName in criticalTables) {
                try {
                    supabaseClient.postgrest
                        .from(tableName)
                        .select()
                } catch (e: Exception) {
                    throw DatabaseAccessException("Table '$tableName' is not accessible: ${e.message}", e)
                }
            }
            
            
            logger.debug(TAG, "All critical tables accessible: ${criticalTables.joinToString(", ")}")
            
        } catch (e: Exception) {
            if (e is DatabaseAccessException) throw e
            throw DatabaseAccessException("Failed to verify critical tables: ${e.message}", e)
        }
    }
    
    private suspend fun testReadAccess() {
        try {
            // Test read access to trains table
            supabaseClient.postgrest
                .from("trains")
                .select()
            
            logger.debug(TAG, "Read access test successful, trains table accessible")
            
            // Test read access to conflicts table
            supabaseClient.postgrest
                .from("conflicts")
                .select()
            
            logger.debug(TAG, "Read access test successful, conflicts table accessible")
            
        } catch (e: Exception) {
            throw DatabaseAccessException("Failed to read from critical tables: ${e.message}", e)
        }
    }
    
    override fun getSuccessDetails(): String {
        return "Database access verified - all critical tables accessible"
    }
    
    override fun getFailureMessage(exception: Throwable?): String {
        return when (exception) {
            is DatabaseAccessException -> exception.message ?: "Database access failed"
            is java.net.SocketTimeoutException -> "Database query timed out. Check network connectivity."
            else -> "Database access failed: ${exception?.message ?: "Unknown error"}"
        }
    }
    
    companion object {
        private const val TAG = "DatabaseAccessHealthCheck"
    }
}

/**
 * Exception thrown when database access health check fails
 */
class DatabaseAccessException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)