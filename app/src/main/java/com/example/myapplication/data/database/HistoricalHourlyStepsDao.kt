// Created: HistoricalHourlyStepsDao for managing archived hourly steps data
package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.HistoricalHourlySteps
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HistoricalHourlyStepsDao {
    @Query("SELECT * FROM historical_hourly_steps WHERE date = :date ORDER BY hour ASC")
    fun getHistoricalHourlyStepsForDate(date: LocalDate): Flow<List<HistoricalHourlySteps>>

    @Query("SELECT * FROM historical_hourly_steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, hour ASC")
    fun getHistoricalHourlyStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HistoricalHourlySteps>>

    @Query("SELECT * FROM historical_hourly_steps WHERE date = :date AND hour = :hour")
    suspend fun getHistoricalHourlyStepsForHour(date: LocalDate, hour: Int): HistoricalHourlySteps?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalHourlySteps(historicalHourlySteps: HistoricalHourlySteps)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalHourlyStepsList(historicalHourlyStepsList: List<HistoricalHourlySteps>)

    @Query("SELECT SUM(steps) FROM historical_hourly_steps WHERE date = :date")
    fun getTotalHistoricalStepsForDate(date: LocalDate): Flow<Int?>

    @Query("DELETE FROM historical_hourly_steps WHERE date < :beforeDate")
    suspend fun deleteOldHistoricalSteps(beforeDate: LocalDate)

    @Query("SELECT COUNT(*) FROM historical_hourly_steps WHERE date = :date")
    suspend fun getHistoricalStepsCountForDate(date: LocalDate): Int
} 