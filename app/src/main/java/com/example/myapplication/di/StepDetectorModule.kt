// Created: StepDetectorModule for dependency injection of step detectors
package com.example.myapplication.di

import com.example.myapplication.service.AccelerometerStepDetector
import com.example.myapplication.service.HardwareStepDetector
import com.example.myapplication.service.StepDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StepDetectorModule {
    @Binds
    @Singleton
    abstract fun bindHardwareStepDetector(impl: HardwareStepDetector): StepDetector

    @Binds
    @Singleton
    abstract fun bindAccelerometerStepDetector(impl: AccelerometerStepDetector): StepDetector
} 