package com.example.pravaahan.di

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.core.monitoring.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for comprehensive monitoring system
 */
@Module
@InstallIn(SingletonComponent::class)
object MonitoringModule {
    
    // @Provides
    // @Singleton
    // fun provideMetricsCorrelationEngine(
    //     logger: Logger
    // ): MetricsCorrelationEngine {
    //     return MetricsCorrelationEngine(logger)
    // }
    
    @Provides
    @Singleton
    fun provideAlertingSystem(
        logger: Logger
    ): AlertingSystem {
        return AlertingSystem(logger)
    }
    
    @Provides
    @Singleton
    fun provideRealTimeMetricsCollector(
        logger: Logger
    ): RealTimeMetricsCollector {
        return RealTimeMetricsCollector(logger)
    }
    
    @Provides
    @Singleton
    fun provideMemoryLeakDetector(
        logger: Logger
    ): MemoryLeakDetector {
        return MemoryLeakDetector(logger)
    }
    
    @Provides
    @Singleton
    fun provideSecurityEventMonitor(
        logger: Logger
    ): SecurityEventMonitor {
        return SecurityEventMonitor(logger)
    }
    
    @Provides
    @Singleton
    fun provideMonitoringService(
        metricsCorrelationEngine: MetricsCorrelationEngine,
        alertingSystem: AlertingSystem,
        realTimeMetricsCollector: RealTimeMetricsCollector,
        memoryLeakDetector: MemoryLeakDetector,
        securityEventMonitor: SecurityEventMonitor,
        logger: Logger
    ): MonitoringService {
        return MonitoringService(
            metricsCorrelationEngine = metricsCorrelationEngine,
            alertingSystem = alertingSystem,
            realTimeMetricsCollector = realTimeMetricsCollector,
            memoryLeakDetector = memoryLeakDetector,
            securityEventMonitor = securityEventMonitor,
            logger = logger
        )
    }
}