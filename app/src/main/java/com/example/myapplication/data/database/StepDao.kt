package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.StepData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate


@Dao
interface StepDao {
    @Query("SELECT * FROM step_data WHERE date = :date")
    suspend fun getStepsForDate(date: LocalDate): StepData?

    @Query("SELECT * FROM step_data WHERE date = :date")
    fun getStepsForDateFlow(date: LocalDate): Flow<StepData?>

    @Query("SELECT * FROM step_data WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<StepData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stepData: StepData)

    @Query("UPDATE step_data SET steps = :steps, calories = :calories, lastUpdated = :lastUpdated WHERE date = :date")
    suspend fun updateSteps(date: LocalDate, steps: Int, calories: Double, lastUpdated: Long)

    @Query("SELECT SUM(steps) FROM step_data WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Int?
}