package com.example.pravaahan.di

import android.content.Context
import com.example.pravaahan.core.accessibility.*
import com.example.pravaahan.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for accessibility and mobile optimization components
 */
@Module
@InstallIn(SingletonComponent::class)
object AccessibilityModule {
    
    @Provides
    @Singleton
    fun provideHapticFeedbackManager(
        @ApplicationContext context: Context,
        logger: Logger
    ): HapticFeedbackManager {
        return HapticFeedbackManager(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideTouchInteractionOptimizer(
        logger: Logger,
        hapticManager: HapticFeedbackManager
    ): TouchInteractionOptimizer {
        return TouchInteractionOptimizer(logger, hapticManager)
    }
    
    @Provides
    @Singleton
    fun provideAccessibilityManager(
        @ApplicationContext context: Context,
        logger: Logger
    ): AccessibilityManager {
        return AccessibilityManager(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideResponsiveLayoutManager(
        logger: Logger
    ): ResponsiveLayoutManager {
        return ResponsiveLayoutManager(logger)
    }
    
    @Provides
    @Singleton
    fun provideBatteryOptimizationManager(
        @ApplicationContext context: Context,
        logger: Logger
    ): BatteryOptimizationManager {
        return BatteryOptimizationManager(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideHighContrastThemeManager(
        logger: Logger
    ): HighContrastThemeManager {
        return HighContrastThemeManager(logger)
    }
}