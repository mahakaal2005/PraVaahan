package com.example.pravaahan.di

import com.example.pravaahan.core.logging.Logger
import com.example.pravaahan.presentation.ui.components.animation.AnimationPool
import com.example.pravaahan.presentation.ui.components.animation.PerformanceMonitor
import com.example.pravaahan.presentation.ui.components.animation.VisibilityOptimizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for animation system components.
 * Provides singleton instances of animation optimization services.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnimationModule {
    
    @Provides
    @Singleton
    fun provideAnimationPool(logger: Logger): AnimationPool {
        return AnimationPool(logger)
    }
    
    @Provides
    @Singleton
    fun provideVisibilityOptimizer(logger: Logger): VisibilityOptimizer {
        return VisibilityOptimizer(logger)
    }
    
    @Provides
    @Singleton
    fun providePerformanceMonitor(logger: Logger): PerformanceMonitor {
        return PerformanceMonitor(logger)
    }
}