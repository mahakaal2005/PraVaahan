package com.example.pravaahan.core.health

import com.example.pravaahan.core.logging.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

/**
 * Health check that verifies Supabase client connection and basic API access.
 * This is a critical check for the railway control system.
 */
class SupabaseConnectionHealthCheck @Inject constructor(
    private val supabaseClient: SupabaseClient,
    logger: Logger
) : BaseHealthCheck(
    name = "Supabase Connection",
    isCritical = true,
    timeoutMs = 5_000L, // 5 seconds
    maxRetries = 2,
    logger = logger
) {
    
    override suspend fun performCheck() {
        try {
            // Test basic connectivity by performing a simple query
            // This will verify the client is configured correctly and can reach Supabase
            val response = supabaseClient.postgrest
                .from("trains")
                .select()
            
            logger.debug(TAG, "Supabase connection test successful")
            
        } catch (e: Exception) {
            logger.error(TAG, "Supabase connection test failed", e)
            throw SupabaseConnectionException("Failed to connect to Supabase: ${e.message}", e)
        }
    }
    
    override fun getSuccessDetails(): String {
        return "Successfully connected to Supabase backend"
    }
    
    override fun getFailureMessage(exception: Throwable?): String {
        return when (exception) {
            is SupabaseConnectionException -> exception.message ?: "Supabase connection failed"
            is java.net.UnknownHostException -> "Cannot reach Supabase servers. Check internet connection."
            is java.net.SocketTimeoutException -> "Connection to Supabase timed out. Check network connectivity."
            is javax.net.ssl.SSLException -> "SSL/TLS connection to Supabase failed. Check certificate configuration."
            else -> "Supabase connection failed: ${exception?.message ?: "Unknown error"}"
        }
    }
    
    companion object {
        private const val TAG = "SupabaseConnectionHealthCheck"
    }
}

/**
 * Exception thrown when Supabase connection health check fails
 */
class SupabaseConnectionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)