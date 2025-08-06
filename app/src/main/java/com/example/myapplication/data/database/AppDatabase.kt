// Updated: Added migration strategy and ensured proper entity references
// Fixed: Added migration from version 11 to 7 to handle corrupted database state
// Fixed: Updated database version to 7 and added missing migrations
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

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `mood_states` (
                `date` TEXT NOT NULL,
                `mood` INTEGER NOT NULL,
                `steps` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `isFinalized` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`date`)
            )
        """)
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `hourly_steps` (
                `date` TEXT NOT NULL,
                `hour` INTEGER NOT NULL,
                `steps` INTEGER NOT NULL,
                PRIMARY KEY(`date`, `hour`)
            )
        """)
        
        database.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS `index_hourly_steps_date_hour` 
            ON `hourly_steps` (`date`, `hour`)
        """)
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `inactivity_data` (
                `date` TEXT NOT NULL,
                `startTime` TEXT NOT NULL,
                `endTime` TEXT,
                `durationHours` REAL NOT NULL,
                `stepsDuringPeriod` INTEGER NOT NULL,
                `isConsecutive` INTEGER NOT NULL,
                `notificationSent` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
            )
        """)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `mood_notifications` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `timestamp` TEXT NOT NULL,
                `previousMood` INTEGER NOT NULL,
                `currentMood` INTEGER NOT NULL,
                `moodDrop` INTEGER NOT NULL,
                `stepsInPeriod` INTEGER NOT NULL,
                `periodHours` REAL NOT NULL,
                `notificationSent` INTEGER NOT NULL DEFAULT 0
            )
        """)
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_mood_notifications_date` 
            ON `mood_notifications` (`date`)
        """)
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Drop all existing tables
        database.execSQL("DROP TABLE IF EXISTS `mood_notifications`")
        database.execSQL("DROP TABLE IF EXISTS `inactivity_data`")
        database.execSQL("DROP TABLE IF EXISTS `hourly_steps`")
        database.execSQL("DROP TABLE IF EXISTS `mood_states`")
        database.execSQL("DROP TABLE IF EXISTS `step_data`")
        database.execSQL("DROP TABLE IF EXISTS `step_counts`")
        
        // Recreate all tables with correct schemas
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
                PRIMARY KEY(`date`, `hour`)
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `inactivity_data` (
                `date` TEXT NOT NULL,
                `startTime` TEXT NOT NULL,
                `endTime` TEXT,
                `durationHours` REAL NOT NULL,
                `stepsDuringPeriod` INTEGER NOT NULL,
                `isConsecutive` INTEGER NOT NULL,
                `notificationSent` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
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
            CREATE INDEX IF NOT EXISTS `index_mood_notifications_date` 
            ON `mood_notifications` (`date`)
        """)
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // This migration handles any schema changes from version 6 to 7
        // Since the schema appears to be the same, this is a no-op migration
        // but it ensures proper version tracking
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add historical hourly steps table for data preservation
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `historical_hourly_steps` (
                `date` TEXT NOT NULL,
                `hour` INTEGER NOT NULL,
                `steps` INTEGER NOT NULL,
                `archived_at` TEXT NOT NULL,
                PRIMARY KEY(`date`, `hour`)
            )
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_historical_hourly_steps_date` 
            ON `historical_hourly_steps` (`date`)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_historical_hourly_steps_archived_at` 
            ON `historical_hourly_steps` (`archived_at`)
        """)
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add daily statistics table for comprehensive daily metrics
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_statistics` (
                `date` TEXT NOT NULL,
                `daily_goal` INTEGER NOT NULL,
                `final_mood` INTEGER NOT NULL,
                `total_steps` INTEGER NOT NULL,
                `archived_at` TEXT NOT NULL,
                PRIMARY KEY(`date`)
            )
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_daily_statistics_date` 
            ON `daily_statistics` (`date`)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_daily_statistics_archived_at` 
            ON `daily_statistics` (`archived_at`)
        """)
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add last_recorded_total column to hourly_steps table for polling system
        database.execSQL("""
            ALTER TABLE `hourly_steps` 
            ADD COLUMN `last_recorded_total` INTEGER NOT NULL DEFAULT 0
        """)
    }
}

val MIGRATION_11_7 = object : Migration(11, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Handle migration from corrupted version 11 to version 7
        // Drop all tables and recreate them with the correct schema
        database.execSQL("DROP TABLE IF EXISTS `mood_notifications`")
        database.execSQL("DROP TABLE IF EXISTS `inactivity_data`")
        database.execSQL("DROP TABLE IF EXISTS `hourly_steps`")
        database.execSQL("DROP TABLE IF EXISTS `mood_states`")
        database.execSQL("DROP TABLE IF EXISTS `step_data`")
        database.execSQL("DROP TABLE IF EXISTS `step_counts`")
        
        // Recreate all tables with correct schemas
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
                PRIMARY KEY(`date`, `hour`)
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `inactivity_data` (
                `date` TEXT NOT NULL,
                `startTime` TEXT NOT NULL,
                `endTime` TEXT,
                `durationHours` REAL NOT NULL,
                `stepsDuringPeriod` INTEGER NOT NULL,
                `isConsecutive` INTEGER NOT NULL,
                `notificationSent` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
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
            CREATE INDEX IF NOT EXISTS `index_mood_notifications_date` 
            ON `mood_notifications` (`date`)
        """)
    }
}

val MIGRATION_ANY_TO_7 = object : Migration(1, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // This migration handles any version to version 7
        // It will drop and recreate all tables to ensure correct schema
        database.execSQL("DROP TABLE IF EXISTS `mood_notifications`")
        database.execSQL("DROP TABLE IF EXISTS `inactivity_data`")
        database.execSQL("DROP TABLE IF EXISTS `hourly_steps`")
        database.execSQL("DROP TABLE IF EXISTS `mood_states`")
        database.execSQL("DROP TABLE IF EXISTS `step_data`")
        database.execSQL("DROP TABLE IF EXISTS `step_counts`")
        
        // Recreate all tables with correct schemas
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
                PRIMARY KEY(`date`, `hour`)
            )
        """)
        
        database.execSQL("""
            CREATE TABLE `inactivity_data` (
                `date` TEXT NOT NULL,
                `startTime` TEXT NOT NULL,
                `endTime` TEXT,
                `durationHours` REAL NOT NULL,
                `stepsDuringPeriod` INTEGER NOT NULL,
                `isConsecutive` INTEGER NOT NULL,
                `notificationSent` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
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
            CREATE INDEX IF NOT EXISTS `index_mood_notifications_date` 
            ON `mood_notifications` (`date`)
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
        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_11_7, MIGRATION_ANY_TO_7)
        
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
                // If database access fails, trigger a reset
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("migration_failed", true).apply()
                null
            }
        }
    }
}