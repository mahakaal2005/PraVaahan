package com.example.pravaahan.core.error

/**
 * Exception thrown when Supabase connection fails
 */
class SupabaseConnectionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)