package com.example.myapplication.data.repository

import com.example.myapplication.data.database.StepDao
import com.example.myapplication.data.database.HourlyStepsDao
import com.example.myapplication.data.model.StepData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.foundation.clickable

@Singleton
class StepRepository @Inject constructor(
    private val stepDao: StepDao,
    private val hourlyStepsDao: HourlyStepsDao
) {
    fun getTodaySteps(): Flow<StepData?> {
        return stepDao.getStepsForDateFlow(LocalDate.now())
    }

    fun getWeeklySteps(): Flow<List<StepData>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        return stepDao.getStepsForDateRange(startDate, endDate)
    }

    fun getHourlyStepsForDate(date: LocalDate): Flow<List<com.example.myapplication.data.model.HourlySteps>> {
        return try {
            // FIXED: Use HourlyStepsDao instead of StepDao for hourly data
            hourlyStepsDao.getHourlyStepsForDate(date)
        } catch (e: Exception) {
            android.util.Log.e("StepRepository", "Error getting hourly steps for $date", e)
            kotlinx.coroutines.flow.flow { 
                // Return empty list as fallback
                emit(emptyList()) 
            }
        }
    }

    suspend fun updateSteps(date: LocalDate, steps: Int) {
        val calories = calculateCalories(steps)
        val existingData = stepDao.getStepsForDate(date)

        if (existingData != null) {
            stepDao.updateSteps(date, steps, calories, System.currentTimeMillis())
        } else {
            stepDao.insertOrUpdate(
                StepData(
                    date = date,
                    steps = steps,
                    calories = calories
                )
            )
        }
    }

    private fun calculateCalories(steps: Int): Double {
        // Basic calculation: approximately 0.04 calories per step
        return steps * 0.04
    }

    /**
     * Clear corrupted hourly data for today to allow rebuilding
     */
    suspend fun clearTodayHourlyData() {
        try {
            val today = LocalDate.now()
            hourlyStepsDao.deleteHourlyStepsForDate(today)
            android.util.Log.i("StepRepository", "Cleared corrupted hourly data for today")
        } catch (e: Exception) {
            android.util.Log.e("StepRepository", "Error clearing hourly data", e)
        }
    }
}