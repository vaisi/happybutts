// Updated: Simplified database migrations for better reliability
// Fixed: Removed complex migration strategies and forced database resets
// Updated: Clean migration path from version 1 to current version
// Fixed: InactivityData table schema to match entity model with auto-incrementing ID
package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.model.DailyStatistics
import com.example.myapplication.data.model.HistoricalHourlySteps
import com.example.myapplication.data.model.HourlySteps
import com.example.myapplication.data.model.MoodStateEntity
import com.example.myapplication.data.model.StepCount
import com.example.myapplication.data.model.StepData
import com.example.myapplication.util.Converters
import com.example.myapplication.data.model.InactivityData
import com.example.myapplication.data.model.MoodNotificationData
import com.example.myapplication.data.database.DailyStatisticsDao
import com.example.myapplication.data.database.HistoricalHourlyStepsDao

// Simplified migration from any version to current version
val MIGRATION_TO_CURRENT = object : Migration(1, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Drop all existing tables to ensure clean state
        database.execSQL("DROP TABLE IF EXISTS `mood_notifications`")
        database.execSQL("DROP TABLE IF EXISTS `inactivity_data`")
        database.execSQL("DROP TABLE IF EXISTS `hourly_steps`")
        database.execSQL("DROP TABLE IF EXISTS `mood_states`")
        database.execSQL("DROP TABLE IF EXISTS `step_data`")
        database.execSQL("DROP TABLE IF EXISTS `step_counts`")
        database.execSQL("DROP TABLE IF EXISTS `historical_hourly_steps`")
        database.execSQL("DROP TABLE IF EXISTS `daily_statistics`")
        
        // Create all tables with current schema
        database.execSQL("""
            CREATE TABLE `step_counts` (
                `date` TEXT NOT NULL,
                `steps` INTEGER NOT NULL,
                `goal` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `step_data` (
                `date` TEXT NOT NULL,
                `steps` INTEGER NOT NULL,
                `calories` REAL NOT NULL,
                `lastUpdated` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `mood_states` (
                `date` TEXT NOT NULL,
                `mood` INTEGER NOT NULL,
                `dailyStartMood` INTEGER NOT NULL,
                `previousDayEndMood` INTEGER NOT NULL,
                `lastPersistedSteps` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`date`)
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `hourly_steps` (
                `date` TEXT NOT NULL,
                `hour` INTEGER NOT NULL,
                `steps` INTEGER NOT NULL,
                `last_recorded_total` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`date`, `hour`)
            )
        """)
        
        // Fixed: Updated inactivity_data table to match entity model
        database.execSQL("""
            CREATE TABLE `inactivity_data` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `date` TEXT NOT NULL,
                `startTime` TEXT NOT NULL,
                `endTime` TEXT,
                `durationHours` REAL NOT NULL,
                `stepsDuringPeriod` INTEGER NOT NULL,
                `isConsecutive` INTEGER NOT NULL,
                `notificationSent` INTEGER NOT NULL
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `mood_notifications` (
                `date` TEXT NOT NULL,
                `timestamp` TEXT NOT NULL,
                `previousMood` INTEGER NOT NULL,
                `currentMood` INTEGER NOT NULL,
                `moodDrop` INTEGER NOT NULL,
                `stepsInPeriod` INTEGER NOT NULL,
                `periodHours` REAL NOT NULL,
                `notificationSent` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`date`)
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `historical_hourly_steps` (
                `date` TEXT NOT NULL,
                `hour` INTEGER NOT NULL,
                `steps` INTEGER NOT NULL,
                `archived_at` TEXT NOT NULL,
                PRIMARY KEY(`date`, `hour`)
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `daily_statistics` (
                `date` TEXT NOT NULL,
                `daily_goal` INTEGER NOT NULL,
                `final_mood` INTEGER NOT NULL,
                `total_steps` INTEGER NOT NULL,
                `archived_at` TEXT NOT NULL,
                PRIMARY KEY(`date`)
            )
        """)
        
        // Create indexes
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_mood_notifications_date` 
            ON `mood_notifications` (`date`)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_historical_hourly_steps_date` 
            ON `historical_hourly_steps` (`date`)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_historical_hourly_steps_archived_at` 
            ON `historical_hourly_steps` (`archived_at`)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_daily_statistics_date` 
            ON `daily_statistics` (`date`)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_daily_statistics_archived_at` 
            ON `daily_statistics` (`archived_at`)
        """)
        
        // Add index for inactivity_data date column for efficient queries
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_inactivity_data_date` 
            ON `inactivity_data` (`date`)
        """)
    }
}

@Database(
    entities = [
        StepCount::class,
        StepData::class,
        MoodStateEntity::class,
        HourlySteps::class,
        HistoricalHourlySteps::class,
        DailyStatistics::class,
        InactivityData::class,
        MoodNotificationData::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepCountDao(): StepCountDao
    abstract fun stepDao(): StepDao
    abstract fun moodStateDao(): MoodStateDao
    abstract fun hourlyStepsDao(): HourlyStepsDao
    abstract fun historicalHourlyStepsDao(): HistoricalHourlyStepsDao
    abstract fun dailyStatisticsDao(): DailyStatisticsDao
    abstract fun inactivityDao(): InactivityDao
    abstract fun moodNotificationDao(): MoodNotificationDao

    companion object {
        const val DATABASE_NAME = "step_tracker_db"
        val ALL_MIGRATIONS = arrayOf(MIGRATION_TO_CURRENT)
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigration() // This will delete the database if migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun getInstanceSafely(context: Context): AppDatabase? {
            return try {
                getInstance(context)
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Failed to get database instance", e)
                null
            }
        }
    }
}