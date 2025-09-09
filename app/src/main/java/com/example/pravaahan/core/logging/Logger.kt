package com.example.pravaahan.core.logging

/**
 * Logger interface for the PraVaahan application
 * Provides different logging levels for various scenarios
 */
interface Logger {
    fun debug(tag: String, message: String, throwable: Throwable? = null)
    fun info(tag: String, message: String, throwable: Throwable? = null)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Special logging for critical train operations
     */
    fun critical(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Log accessibility events
     */
    fun logAccessibilityEvent(tag: String, event: String, message: String)
}