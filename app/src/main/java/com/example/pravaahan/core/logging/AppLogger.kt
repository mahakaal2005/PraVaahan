package com.example.pravaahan.core.logging

import android.util.Log
import com.example.pravaahan.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of Logger interface for the PraVaahan application
 * Handles different logging behaviors for debug and release builds
 */
@Singleton
class AppLogger @Inject constructor() : Logger {
    
    companion object {
        private const val APP_TAG = "PraVaahan"
        private const val CRITICAL_TAG = "PraVaahan_CRITICAL"
    }
    
    override fun debug(tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG || BuildConfig.ENABLE_DETAILED_LOGGING) {
            Log.d("$APP_TAG:$tag", message, throwable)
        }
    }
    
    override fun info(tag: String, message: String, throwable: Throwable?) {
        Log.i("$APP_TAG:$tag", message, throwable)
        // In production, could send to analytics service
        if (!BuildConfig.DEBUG) {
            // TODO: Send to analytics/monitoring service
        }
    }
    
    override fun warn(tag: String, message: String, throwable: Throwable?) {
        Log.w("$APP_TAG:$tag", message, throwable)
        // In production, could send to monitoring service
        if (!BuildConfig.DEBUG) {
            // TODO: Send to monitoring service
        }
    }
    
    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e("$APP_TAG:$tag", message, throwable)
        // Always send errors to crash reporting
        if (!BuildConfig.DEBUG) {
            // TODO: Send to crash reporting service (Firebase Crashlytics)
        }
    }
    
    override fun critical(tag: String, message: String, throwable: Throwable?) {
        // Critical logs are always logged regardless of build type
        Log.e("$CRITICAL_TAG:$tag", "CRITICAL: $message", throwable)
        
        // Always send critical logs to monitoring
        // TODO: Send to real-time monitoring system for train operations
        
        // In debug mode, also print to system error
        if (BuildConfig.DEBUG) {
            System.err.println("CRITICAL ERROR in $tag: $message")
            throwable?.printStackTrace()
        }
    }
    
    override fun logAccessibilityEvent(tag: String, event: String, message: String) {
        Log.i("$APP_TAG:$tag", "Accessibility Event [$event]: $message")
        // In production, could send to accessibility analytics
        if (!BuildConfig.DEBUG) {
            // TODO: Send to accessibility monitoring service
        }
    }
}