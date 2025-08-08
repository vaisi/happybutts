// Updated: ServiceModule to include notification services and missing dependencies
package com.example.myapplication.di

import android.content.Context
import android.hardware.SensorManager
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.data.repository.InactivityRepository
import com.example.myapplication.data.repository.MoodNotificationRepository
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.service.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext context: Context): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @Provides
    @Singleton
    fun provideHardwareStepDetector(
        @ApplicationContext context: Context
    ): HardwareStepDetector {
        return HardwareStepDetector(context)
    }

    @Provides
    @Singleton
    fun provideAccelerometerStepDetector(
        @ApplicationContext context: Context
    ): AccelerometerStepDetector {
        return AccelerometerStepDetector(context)
    }

    @Provides
    @Singleton
    fun provideUnifiedStepCounterService(
        @ApplicationContext context: Context,
        hardwareStepDetector: HardwareStepDetector,
        accelerometerStepDetector: AccelerometerStepDetector,
        stepCountRepository: StepCountRepository,
        inactivityRepository: InactivityRepository
    ): UnifiedStepCounterService {
        return UnifiedStepCounterService(
            context,
            hardwareStepDetector,
            accelerometerStepDetector,
            stepCountRepository,
            inactivityRepository
        )
    }

    @Provides
    @Singleton
    fun provideMoodNotificationService(
        @ApplicationContext context: Context,
        moodNotificationRepository: MoodNotificationRepository
    ): MoodNotificationService {
        return MoodNotificationService(context, moodNotificationRepository)
    }

    @Provides
    @Singleton
    fun provideInactivityNotificationService(
        @ApplicationContext context: Context,
        inactivityRepository: InactivityRepository
    ): InactivityNotificationService {
        return InactivityNotificationService(context, inactivityRepository)
    }

    @Provides
    @Singleton
    fun provideSimpleHourlyAggregator(
        @ApplicationContext context: Context,
        stepCountRepository: StepCountRepository,
        hourlyStepsDao: com.example.myapplication.data.database.HourlyStepsDao
    ): SimpleHourlyAggregator {
        return SimpleHourlyAggregator(
            context,
            stepCountRepository,
            hourlyStepsDao
        )
    }
} 