package com.example.pravaahan.di

import com.example.pravaahan.core.health.AlertThresholdConfig
import com.example.pravaahan.core.health.AppStartupVerifier
import com.example.pravaahan.core.health.DatabaseAccessHealthCheck
import com.example.pravaahan.core.health.DependencyInjectionHealthCheck
import com.example.pravaahan.core.health.HealthDashboardService
import com.example.pravaahan.core.health.NavigationHealthCheck
import com.example.pravaahan.core.health.PositionDataQualityHealthCheck
import com.example.pravaahan.core.health.RealTimeHealthCheck
import com.example.pravaahan.core.health.RealTimePerformanceMonitor
import com.example.pravaahan.core.health.RealtimeConnectionHealthCheck
import com.example.pravaahan.core.health.SupabaseConnectionHealthCheck
import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.RealTimeMetricsCollector
import com.example.pravaahan.domain.repository.ConflictRepository
import com.example.pravaahan.domain.repository.TrainRepository
import com.example.pravaahan.domain.service.RealTimePositionService
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
    fun provideRealTimeHealthCheck(
        realTimePositionService: com.example.pravaahan.domain.service.RealTimePositionService,
        metricsCollector: com.example.pravaahan.core.monitoring.RealTimeMetricsCollector,
        circuitBreaker: com.example.pravaahan.core.resilience.RealTimeCircuitBreaker,
        logger: Logger
    ): RealTimeHealthCheck {
        return RealTimeHealthCheck(
            realTimePositionService,
            metricsCollector,
            circuitBreaker,
            logger
        )
    }
    
    @Provides
    @Singleton
    fun provideRealTimePerformanceMonitor(
        metricsCollector: com.example.pravaahan.core.monitoring.RealTimeMetricsCollector,
        circuitBreaker: com.example.pravaahan.core.resilience.RealTimeCircuitBreaker,
        alertThresholdConfig: AlertThresholdConfig,
        logger: Logger
    ): RealTimePerformanceMonitor {
        return RealTimePerformanceMonitor(
            metricsCollector,
            circuitBreaker,
            alertThresholdConfig,
            logger
        )
    }
    
    @Provides
    @Singleton
    fun providePositionDataQualityHealthCheck(
        realTimePositionService: RealTimePositionService,
        metricsCollector: RealTimeMetricsCollector,
        logger: Logger
    ): PositionDataQualityHealthCheck {
        return PositionDataQualityHealthCheck(
            realTimePositionService,
            metricsCollector,
            logger
        )
    }
    
    @Provides
    @Singleton
    fun provideAlertThresholdConfig(): AlertThresholdConfig {
        return AlertThresholdConfig()
    }
    
    @Provides
    @Singleton
    fun provideHealthDashboardService(
        appStartupVerifier: AppStartupVerifier,
        realTimePerformanceMonitor: RealTimePerformanceMonitor,
        metricsCollector: RealTimeMetricsCollector,
        alertThresholdConfig: AlertThresholdConfig,
        monitoringService: com.example.pravaahan.core.monitoring.MonitoringService,
        logger: Logger
    ): HealthDashboardService {
        return HealthDashboardService(
            appStartupVerifier,
            realTimePerformanceMonitor,
            metricsCollector,
            alertThresholdConfig,
            monitoringService,
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
        realTimeHealthCheck: RealTimeHealthCheck,
        positionDataQualityHealthCheck: PositionDataQualityHealthCheck,
        logger: Logger
    ): AppStartupVerifier {
        return AppStartupVerifier(
            supabaseConnectionHealthCheck,
            databaseAccessHealthCheck,
            realtimeConnectionHealthCheck,
            navigationHealthCheck,
            dependencyInjectionHealthCheck,
            realTimeHealthCheck,
            positionDataQualityHealthCheck,
            logger
        )
    }
}