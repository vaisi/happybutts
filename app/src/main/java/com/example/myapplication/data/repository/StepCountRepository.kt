// Updated: Removed circular dependency by removing UnifiedStepCounterService and updated to use dynamic goal from UserPreferences
// Updated: Added data validation and recovery mechanisms
package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.database.StepCountDao
import com.example.myapplication.data.model.StepCount
import com.example.myapplication.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface StepCountRepository {
    fun getTodayStepCount(): Flow<StepCount>
    fun getStepCountForDate(date: LocalDate): Flow<StepCount?>
    suspend fun updateTodaySteps(steps: Int)
    suspend fun setDailyGoal(goal: Int)
    suspend fun resetDailySteps()
    suspend fun getLastSeenDate(): String
    suspend fun updateLastSeenDate(date: String)
    // ENHANCED: Added data validation and recovery methods
    suspend fun validateAndRecoverData()
    suspend fun getDataConsistencyReport(): DataConsistencyReport
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
                // Use dynamic goal from UserPreferences for today only
                val defaultGoal = try {
                    userPreferences.dailyGoal.first()
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting daily goal from preferences, using default", e)
                    10000 // Default goal
                }
                StepCount(today, 0, defaultGoal)
            }
        }
    }

    override fun getStepCountForDate(date: LocalDate): Flow<StepCount?> {
        Log.d(TAG, "getStepCountForDate: Getting step count for $date")
        // Don't create new entries for historical dates - just return what exists
        return stepCountDao.getStepCountForDate(date)
    }

    override suspend fun updateTodaySteps(steps: Int) {
        val today = LocalDate.now()
        val defaultGoal = try {
            userPreferences.dailyGoal.first()
        } catch (e: Exception) {
            Log.w(TAG, "Error getting daily goal from preferences, using default", e)
            10000 // Default goal
        }
        
        val stepCount = StepCount(
            date = today,
            steps = steps,
            goal = defaultGoal
        )
        
        stepCountDao.insertStepCount(stepCount)
        Log.d(TAG, "Updated today's steps: $steps")
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

    // ENHANCED: Data validation and recovery methods
    override suspend fun validateAndRecoverData() {
        try {
            Log.i(TAG, "Validating and recovering step data")
            
            val today = LocalDate.now()
            val todayStepCount = stepCountDao.getStepCountForDate(today).firstOrNull()
            
            if (todayStepCount == null) {
                Log.w(TAG, "No step count found for today, creating default entry")
                val defaultGoal = try {
                    userPreferences.dailyGoal.first()
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting daily goal from preferences, using default", e)
                    10000 // Default goal
                }
                
                val defaultStepCount = StepCount(
                    date = today,
                    steps = 0,
                    goal = defaultGoal
                )
                stepCountDao.insertStepCount(defaultStepCount)
                Log.i(TAG, "Created default step count entry for today")
            } else {
                Log.d(TAG, "Step count validation passed for today")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating and recovering step data", e)
        }
    }

    override suspend fun getDataConsistencyReport(): DataConsistencyReport {
        val today = LocalDate.now()
        val currentStepCount = stepCountDao.getStepCountForDate(today).firstOrNull()
        
        return DataConsistencyReport(
            hasData = currentStepCount != null,
            currentSteps = currentStepCount?.steps ?: 0,
            currentGoal = currentStepCount?.goal ?: 10000,
            isValid = currentStepCount?.let { stepCount -> 
                stepCount.steps >= 0 && stepCount.steps <= 100000 && stepCount.goal > 0 && stepCount.goal <= 100000 
            } ?: false,
            lastUpdated = currentStepCount?.let { "Today" } ?: "Never"
        )
    }
}

// ENHANCED: Data consistency report class
data class DataConsistencyReport(
    val hasData: Boolean,
    val currentSteps: Int,
    val currentGoal: Int,
    val isValid: Boolean,
    val lastUpdated: String
) 