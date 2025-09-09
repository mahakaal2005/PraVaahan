package com.example.pravaahan.di

import android.content.Context
import com.example.pravaahan.core.reliability.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for reliability and error handling components
 */
@Module
@InstallIn(SingletonComponent::class)
object ReliabilityModule {
    
    @Provides
    @Singleton
    fun provideNetworkConnectivityMonitor(
        @ApplicationContext context: Context,
        logger: com.example.pravaahan.core.logging.Logger
    ): NetworkConnectivityMonitor {
        return NetworkConnectivityMonitor(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideRealTimeConnectionManager(
        logger: com.example.pravaahan.core.logging.Logger,
        networkMonitor: NetworkConnectivityMonitor
    ): RealTimeConnectionManager {
        return RealTimeConnectionManager(logger, networkMonitor)
    }
    
    @Provides
    @Singleton
    fun provideOfflineModeManager(
        logger: com.example.pravaahan.core.logging.Logger,
        networkMonitor: NetworkConnectivityMonitor
    ): OfflineModeManager {
        return OfflineModeManager(logger, networkMonitor)
    }
}