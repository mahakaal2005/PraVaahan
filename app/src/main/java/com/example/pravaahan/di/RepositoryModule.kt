package com.example.pravaahan.di

import com.example.pravaahan.data.repository.ConflictRepositoryImpl
import com.example.pravaahan.data.repository.TrainRepositoryImpl
import com.example.pravaahan.domain.repository.ConflictRepository
import com.example.pravaahan.domain.repository.TrainRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindTrainRepository(
        trainRepositoryImpl: TrainRepositoryImpl
    ): TrainRepository
    
    @Binds
    @Singleton
    abstract fun bindConflictRepository(
        conflictRepositoryImpl: ConflictRepositoryImpl
    ): ConflictRepository
}