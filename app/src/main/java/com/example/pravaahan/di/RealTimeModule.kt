package com.example.pravaahan.di

import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector
import com.example.pravaahan.core.resilience.RealTimeCircuitBreaker
import com.example.pravaahan.core.security.RealTimeSecurityValidator
import com.example.pravaahan.data.service.SupabaseRealTimePositionService
import com.example.pravaahan.domain.service.RealTimePositionService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for real-time components
 * 
 * Provides circuit breaker, metrics collector, security validator,
 * and real-time position service implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RealTimeModule {
    
    /**
     * Bind the Supabase implementation to the interface
     */
    @Binds
    abstract fun bindRealTimePositionService(
        supabaseRealTimePositionService: SupabaseRealTimePositionService
    ): RealTimePositionService
    
    companion object {
        
        /**
         * Provide circuit breaker for real-time connections
         */
        @Provides
        @Singleton
        fun provideRealTimeCircuitBreaker(): RealTimeCircuitBreaker {
            return RealTimeCircuitBreaker()
        }
        
        /**
         * Provide security validator for real-time data
         */
        @Provides
        @Singleton
        fun provideRealTimeSecurityValidator(): RealTimeSecurityValidator {
            return RealTimeSecurityValidator()
        }
    }
}