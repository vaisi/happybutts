package com.example.myapplication.data.repository

import com.example.myapplication.data.database.StepDao
import com.example.myapplication.data.model.StepData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.foundation.clickable

@Singleton
class StepRepository @Inject constructor(
    private val stepDao: StepDao
) {
    fun getTodaySteps(): Flow<StepData?> {
        return stepDao.getStepsForDateFlow(LocalDate.now())
    }

    fun getWeeklySteps(): Flow<List<StepData>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        return stepDao.getStepsForDateRange(startDate, endDate)
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
}