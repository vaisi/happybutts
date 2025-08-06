// Updated: Removed circular dependency by removing UnifiedStepCounterService and updated to use dynamic goal from UserPreferences
package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.database.StepCountDao
import com.example.myapplication.data.model.StepCount
import com.example.myapplication.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface StepCountRepository {
    fun getTodayStepCount(): Flow<StepCount>
    suspend fun updateTodaySteps(steps: Int)
    suspend fun setDailyGoal(goal: Int)
    suspend fun resetDailySteps()
    suspend fun getLastSeenDate(): String
    suspend fun updateLastSeenDate(date: String)
}

@Singleton
class StepCountRepositoryImpl @Inject constructor(
    private val stepCountDao: StepCountDao,
    private val userPreferences: UserPreferences
) : StepCountRepository {
    private val TAG = "StepRepository"

    override fun getTodayStepCount(): Flow<StepCount> {
        val today = LocalDate.now()
        Log.d(TAG, "getTodayStepCount: Getting step count for $today")
        return stepCountDao.getStepCountForDate(today).map { stepCount ->
            stepCount ?: run {
                Log.i(TAG, "getTodayStepCount: No step count found for $today, creating new entry")
                // Use dynamic goal from UserPreferences
                val defaultGoal = userPreferences.dailyGoal.first()
                StepCount(today, 0, defaultGoal)
            }
        }
    }

    override suspend fun updateTodaySteps(steps: Int) {
        val today = LocalDate.now()
        Log.d(TAG, "updateTodaySteps: Updating steps for $today to $steps")
        try {
            val currentGoal = userPreferences.dailyGoal.first()
            stepCountDao.insertStepCount(StepCount(today, steps, currentGoal))
            Log.i(TAG, "updateTodaySteps: Successfully updated steps in database")
        } catch (e: Exception) {
            Log.e(TAG, "updateTodaySteps: Error updating steps", e)
        }
    }

    override suspend fun setDailyGoal(goal: Int) {
        val today = LocalDate.now()
        Log.d(TAG, "setDailyGoal: Setting daily goal to $goal for $today")
        try {
            stepCountDao.insertStepCount(StepCount(today, 0, goal))
            Log.i(TAG, "setDailyGoal: Successfully updated daily goal in database")
        } catch (e: Exception) {
            Log.e(TAG, "setDailyGoal: Error updating daily goal", e)
        }
    }

    override suspend fun resetDailySteps() {
        val today = LocalDate.now()
        Log.d(TAG, "resetDailySteps: Resetting steps for $today to 0")
        try {
            val currentGoal = userPreferences.dailyGoal.first()
            stepCountDao.insertStepCount(StepCount(today, 0, currentGoal))
            Log.i(TAG, "resetDailySteps: Successfully reset steps in database")
        } catch (e: Exception) {
            Log.e(TAG, "resetDailySteps: Error resetting steps", e)
        }
    }

    override suspend fun getLastSeenDate(): String {
        return userPreferences.lastSeenDate.first()
    }

    override suspend fun updateLastSeenDate(date: String) {
        userPreferences.updateLastSeenDate(date)
    }
} 