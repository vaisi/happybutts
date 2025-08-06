// Created: DailyStatisticsDao for managing archived daily statistics data
package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.DailyStatistics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyStatisticsDao {
    @Query("SELECT * FROM daily_statistics WHERE date = :date")
    suspend fun getDailyStatisticsForDate(date: LocalDate): DailyStatistics?

    @Query("SELECT * FROM daily_statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getDailyStatisticsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyStatistics>>

    @Query("SELECT * FROM daily_statistics ORDER BY date DESC LIMIT :limit")
    fun getRecentDailyStatistics(limit: Int): Flow<List<DailyStatistics>>

    @Query("SELECT AVG(final_mood) FROM daily_statistics WHERE date BETWEEN :startDate AND :endDate")
    fun getAverageMoodForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Double?>

    @Query("SELECT AVG(total_steps) FROM daily_statistics WHERE date BETWEEN :startDate AND :endDate")
    fun getAverageStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Double?>

    @Query("SELECT COUNT(*) FROM daily_statistics WHERE date BETWEEN :startDate AND :endDate AND total_steps >= daily_goal")
    fun getGoalAchievementCount(startDate: LocalDate, endDate: LocalDate): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStatistics(dailyStatistics: DailyStatistics)

    @Query("DELETE FROM daily_statistics WHERE date < :beforeDate")
    suspend fun deleteOldDailyStatistics(beforeDate: LocalDate)

    @Query("SELECT COUNT(*) FROM daily_statistics WHERE date = :date")
    suspend fun getDailyStatisticsCountForDate(date: LocalDate): Int
} 