package com.example.pravaahan.di

import com.example.pravaahan.core.error.ErrorHandler
import com.example.pravaahan.core.error.RetryMechanism
import com.example.pravaahan.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for error handling dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ErrorHandlingModule {

    @Provides
    @Singleton
    fun provideRetryMechanism(
        logger: Logger
    ): RetryMechanism {
        return RetryMechanism(logger)
    }
    
    // ErrorHandler is already provided by its @Inject constructor
    // but we can add additional error handling utilities here if needed
}