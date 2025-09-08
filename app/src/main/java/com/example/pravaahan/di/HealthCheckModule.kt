package com.example.pravaahan.di

import com.example.pravaahan.core.health.AppStartupVerifier
import com.example.pravaahan.core.health.DatabaseAccessHealthCheck
import com.example.pravaahan.core.health.DependencyInjectionHealthCheck
import com.example.pravaahan.core.health.NavigationHealthCheck
import com.example.pravaahan.core.health.RealtimeConnectionHealthCheck
import com.example.pravaahan.core.health.SupabaseConnectionHealthCheck
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.domain.repository.ConflictRepository
import com.example.pravaahan.domain.repository.TrainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

/**
 * Hilt module that provides health check dependencies for the PraVaahan app.
 * Configures all health checks with appropriate dependencies and scoping.
 */
@Module
@InstallIn(SingletonComponent::class)
object HealthCheckModule {
    
    @Provides
    @Singleton
    fun provideSupabaseConnectionHealthCheck(
        supabaseClient: SupabaseClient,
        logger: Logger
    ): SupabaseConnectionHealthCheck {
        return SupabaseConnectionHealthCheck(supabaseClient, logger)
    }
    
    @Provides
    @Singleton
    fun provideDatabaseAccessHealthCheck(
        supabaseClient: SupabaseClient,
        logger: Logger
    ): DatabaseAccessHealthCheck {
        return DatabaseAccessHealthCheck(supabaseClient, logger)
    }
    
    @Provides
    @Singleton
    fun provideRealtimeConnectionHealthCheck(
        supabaseClient: SupabaseClient,
        logger: Logger
    ): RealtimeConnectionHealthCheck {
        return RealtimeConnectionHealthCheck(supabaseClient, logger)
    }
    
    @Provides
    @Singleton
    fun provideNavigationHealthCheck(
        logger: Logger
    ): NavigationHealthCheck {
        return NavigationHealthCheck(logger)
    }
    
    @Provides
    @Singleton
    fun provideDependencyInjectionHealthCheck(
        supabaseClient: SupabaseClient,
        trainRepository: TrainRepository,
        conflictRepository: ConflictRepository,
        logger: Logger
    ): DependencyInjectionHealthCheck {
        return DependencyInjectionHealthCheck(
            supabaseClient,
            trainRepository,
            conflictRepository,
            logger
        )
    }
    
    @Provides
    @Singleton
    fun provideAppStartupVerifier(
        supabaseConnectionHealthCheck: SupabaseConnectionHealthCheck,
        databaseAccessHealthCheck: DatabaseAccessHealthCheck,
        realtimeConnectionHealthCheck: RealtimeConnectionHealthCheck,
        navigationHealthCheck: NavigationHealthCheck,
        dependencyInjectionHealthCheck: DependencyInjectionHealthCheck,
        logger: Logger
    ): AppStartupVerifier {
        return AppStartupVerifier(
            supabaseConnectionHealthCheck,
            databaseAccessHealthCheck,
            realtimeConnectionHealthCheck,
            navigationHealthCheck,
            dependencyInjectionHealthCheck,
            logger
        )
    }
}