// Updated: MoodUpdateWorker to properly handle hourly updates and ensure accurate mood calculations
// HYBRID FIX: Now uses database lookup first, then falls back to incremental calculation
// CRITICAL FIX: Updates lastPersistedSteps to current total steps for accurate incremental calculations
// This prevents incorrect decay application when steps are above threshold
// Combines accuracy of recorded data with reliability of incremental calculation
package com.example.myapplication.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.data.repository.StepRepository
import com.example.myapplication.service.UnifiedStepCounterService
import com.example.myapplication.data.database.HourlyStepsDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class MoodUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val moodRepository: MoodRepository,
    private val stepRepository: StepRepository,
    private val unifiedStepCounterService: UnifiedStepCounterService,
    private val hourlyStepsDao: HourlyStepsDao
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "MoodUpdateWorker"

    override suspend fun doWork(): Result {
        val workId = id.toString()
        Log.i(TAG, "Starting work execution [ID: $workId]")
        
        return try {
            val now = LocalDateTime.now()
            Log.i(TAG, "Worker running at ${now.hour}:${now.minute} [ID: $workId]")
            
            // Check if it's a new day (between 00:00 and 00:05)
            if (now.hour == 0 && now.minute < 5) {
                Log.i(TAG, "=== MIDNIGHT RESET STARTED ===")
                Log.i(TAG, "Detected midnight, saving previous day's final mood and resetting daily mood and steps [ID: $workId]")
                
                // Calculate and save previous day's final mood
                val yesterday = LocalDate.now().minusDays(1)
                Log.d(TAG, "Finalizing mood for date: $yesterday [ID: $workId]")
                
                // Get the final mood before resetting
                val finalMood = moodRepository.getCurrentMood().firstOrNull()?.mood
                Log.d(TAG, "Final mood before reset: $finalMood [ID: $workId]")
                
                // Finalize the previous day's mood
                moodRepository.finalizeDayMood(yesterday)
                Log.d(TAG, "Previous day's mood finalized [ID: $workId]")
                
                // Now reset for the new day
                Log.d(TAG, "Resetting daily mood and steps [ID: $workId]")
                moodRepository.resetDaily()
                
                // Verify the reset
                val resetMood = moodRepository.getCurrentMood().firstOrNull()?.mood
                Log.d(TAG, "Mood after reset: $resetMood [ID: $workId]")
                
                Log.i(TAG, "=== MIDNIGHT RESET COMPLETED ===")
                return Result.success()
            }
            
            // ENHANCED: Get today's steps from the most accurate source
            val todaySteps = try {
                // Try to get from UnifiedStepCounterService first (most accurate)
                val serviceSteps = unifiedStepCounterService.currentStepCount
                if (serviceSteps > 0) {
                    Log.d(TAG, "Worker: Using UnifiedStepCounterService steps: $serviceSteps [ID: $workId]")
                    serviceSteps
                } else {
                    // Fallback to repository
                    val repoSteps = stepRepository.getTodaySteps().firstOrNull()?.steps ?: 0
                    Log.d(TAG, "Worker: Using repository steps: $repoSteps [ID: $workId]")
                    repoSteps
                }
            } catch (e: Exception) {
                Log.w(TAG, "Worker: Error getting steps, using repository fallback [ID: $workId]", e)
                stepRepository.getTodaySteps().firstOrNull()?.steps ?: 0
            }
            
            Log.i(TAG, "Current step count: $todaySteps [ID: $workId]")
            
            // HYBRID FIX: Try database first, fallback to incremental calculation
            val previousHour = if (now.hour == 0) 23 else now.hour - 1
            Log.d(TAG, "Recording steps for previous hour: $previousHour [ID: $workId]")
            
            // Get last recorded total from polling system for recording purposes
            val lastRecordedTotal = moodRepository.getLastRecordedTotal()
            Log.d(TAG, "Last recorded total: $lastRecordedTotal [ID: $workId]")
            
            // HYBRID FIX: Try database first, fallback to incremental calculation
            val today = LocalDate.now()
            val stepsInPreviousHour = if (previousHour == 0) {
                // First hour of day - use total steps
                Log.d(TAG, "HYBRID FIX: Hour 0 detected, using total steps: $todaySteps [ID: $workId]")
                todaySteps
            } else {
                // Try database first, fallback to incremental calculation
                val recordedSteps = hourlyStepsDao.getHourlyStepsForHour(today, previousHour)?.steps
                if (recordedSteps != null && recordedSteps > 0) {
                    // Database has the data, use it (most accurate)
                    Log.d(TAG, "HYBRID FIX: Using database recorded steps for hour $previousHour: $recordedSteps [ID: $workId]")
                    recordedSteps
                } else {
                    // Database doesn't have it yet, calculate incrementally
                    val previousHourBaseline = hourlyStepsDao.getLastRecordedTotalForHour(today, previousHour - 1) ?: 0
                    val calculatedSteps = todaySteps - previousHourBaseline
                    Log.d(TAG, "HYBRID FIX: Database empty, calculated incrementally for hour $previousHour: $calculatedSteps (total: $todaySteps, baseline: $previousHourBaseline) [ID: $workId]")
                    calculatedSteps
                }
            }
            
            Log.d(TAG, "HYBRID FIX: Final steps for hour $previousHour: $stepsInPreviousHour [ID: $workId]")
            
            // Record the previous hour's steps with total tracking (for future reference)
            moodRepository.recordHourlySteps(previousHour, stepsInPreviousHour, todaySteps)
            
            // Check for and recover any missing hourly data
            if (moodRepository.checkForMissingHourlyData()) {
                Log.i(TAG, "Detected missing hourly data, attempting recovery [ID: $workId]")
                moodRepository.recoverMissingHourlyData(todaySteps)
            }
            
            // Get current mood before update (use clean mood to avoid UI contamination)
            val baseMood = moodRepository.getCleanMoodForWorker()
            Log.d(TAG, "Previous mood value: $baseMood [ID: $workId]")
            
            // CRITICAL FIX: Use clean mood from database (not contaminated by UI)
            // This ensures worker uses stable baseline mood
            Log.i(TAG, "Worker: Using clean mood as base: $baseMood [ID: $workId]")
            
            // Apply mood decay and gain for the previous hour using pre-calculated steps
            Log.d(TAG, "Applying hourly mood decay for hour $previousHour with pre-calculated steps: $stepsInPreviousHour [ID: $workId]")
            moodRepository.applyHourlyMoodDecayWithSteps(todaySteps, stepsInPreviousHour)
            
            // CRITICAL FIX: Update lastPersistedSteps to current total steps for accurate incremental calculations
            Log.d(TAG, "CRITICAL FIX: Updating lastPersistedSteps to current total: $todaySteps [ID: $workId]")
            moodRepository.updateSteps(todaySteps)
            
            // Get and log the updated mood state
            val updatedMoodState = moodRepository.getCurrentMood().firstOrNull()
            val newMood = updatedMoodState?.mood
            Log.i(TAG, "Mood update completed - Previous: $baseMood, Current: $newMood [ID: $workId]")
            
            // IMPROVED: Store timestamp information for UI calculations
            val currentTime = System.currentTimeMillis()
            Log.i(TAG, "Worker: Storing update timestamp: $currentTime for hour: $previousHour [ID: $workId]")
            
            // Store worker update data for UI synchronization
            moodRepository.storeLastWorkerUpdate(previousHour, todaySteps, currentTime)
            
            // ADDITIONAL: Log to regular system logs for easy searching
            android.util.Log.i("MOOD_DEBUG", "=== WORKER EXECUTION (HYBRID FIX APPLIED) ===")
            android.util.Log.i("MOOD_DEBUG", "Worker ID: $workId")
            android.util.Log.i("MOOD_DEBUG", "Current time: ${now.hour}:${now.minute}")
            android.util.Log.i("MOOD_DEBUG", "Previous hour recorded: $previousHour")
            android.util.Log.i("MOOD_DEBUG", "Steps in previous hour (HYBRID CALCULATION): $stepsInPreviousHour")
            android.util.Log.i("MOOD_DEBUG", "Total steps: $todaySteps")
            android.util.Log.i("MOOD_DEBUG", "Previous mood: $baseMood")
            android.util.Log.i("MOOD_DEBUG", "Current mood: $newMood")
            android.util.Log.i("MOOD_DEBUG", "Mood change: ${(newMood ?: 0) - (baseMood ?: 0)} points")
            android.util.Log.i("MOOD_DEBUG", "Update timestamp: $currentTime")
            android.util.Log.i("MOOD_DEBUG", "=== END WORKER EXECUTION ===")
            
            Log.i(TAG, "Work execution completed successfully [ID: $workId]")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed [ID: $workId]", e)
            Result.failure()
        }
    }
} 