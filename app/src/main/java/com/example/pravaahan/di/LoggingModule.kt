package com.example.pravaahan.di

import com.example.pravaahan.core.logging.AppLogger
import com.example.pravaahan.core.logging.Logger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing logging dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {
    
    @Binds
    @Singleton
    abstract fun bindLogger(appLogger: AppLogger): Logger
}