// Created: StepCountDao interface for database operations
package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.StepCount
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StepCountDao {
    @Query("SELECT * FROM step_counts WHERE date = :date")
    fun getStepCountForDate(date: LocalDate): Flow<StepCount?>

    @Query("SELECT * FROM step_counts WHERE date >= :startDate AND date <= :endDate")
    fun getStepCountHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<StepCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepCount(stepCount: StepCount)

    @Update
    suspend fun updateStepCount(stepCount: StepCount)

    @Query("DELETE FROM step_counts WHERE date < :beforeDate")
    suspend fun deleteOldStepCounts(beforeDate: LocalDate)
} 