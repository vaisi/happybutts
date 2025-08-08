// Updated: Consolidated database module with all DAOs and proper migration
// Fixed: Removed references to old migrations that no longer exist
package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.database.DailyStatisticsDao
import com.example.myapplication.data.database.HistoricalHourlyStepsDao
import com.example.myapplication.data.database.HourlyStepsDao
import com.example.myapplication.data.database.MoodStateDao
import com.example.myapplication.data.database.StepCountDao
import com.example.myapplication.data.database.StepDao
import com.example.myapplication.data.database.InactivityDao
import com.example.myapplication.data.database.MoodNotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(*AppDatabase.ALL_MIGRATIONS)
        .build()
    }

    @Provides
    @Singleton
    fun provideStepCountDao(database: AppDatabase): StepCountDao {
        return database.stepCountDao()
    }

    @Provides
    @Singleton
    fun provideStepDao(database: AppDatabase): StepDao {
        return database.stepDao()
    }

    @Provides
    @Singleton
    fun provideMoodStateDao(database: AppDatabase): MoodStateDao {
        return database.moodStateDao()
    }

    @Provides
    @Singleton
    fun provideHourlyStepsDao(database: AppDatabase): HourlyStepsDao {
        return database.hourlyStepsDao()
    }

    @Provides
    @Singleton
    fun provideHistoricalHourlyStepsDao(database: AppDatabase): HistoricalHourlyStepsDao {
        return database.historicalHourlyStepsDao()
    }

    @Provides
    @Singleton
    fun provideDailyStatisticsDao(database: AppDatabase): DailyStatisticsDao {
        return database.dailyStatisticsDao()
    }

    @Provides
    @Singleton
    fun provideInactivityDao(database: AppDatabase): InactivityDao {
        return database.inactivityDao()
    }

    @Provides
    @Singleton
    fun provideMoodNotificationDao(database: AppDatabase): MoodNotificationDao {
        return database.moodNotificationDao()
    }
}

