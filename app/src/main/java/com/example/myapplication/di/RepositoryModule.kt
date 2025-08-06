// Created: RepositoryModule for Hilt dependency injection
package com.example.myapplication.di

import com.example.myapplication.data.database.StepCountDao
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.data.repository.StepCountRepositoryImpl
import com.example.myapplication.service.UnifiedStepCounterService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindStepCountRepository(
        stepCountRepositoryImpl: StepCountRepositoryImpl
    ): StepCountRepository
} 