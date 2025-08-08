// Updated: MoodUpdateWorker - Simplified to use consolidated mood calculation methods
// Fixed: Removed complex hybrid calculation logic
// Updated: Uses single authoritative mood calculation methods
// Updated: Removed excessive debug logging for cleaner production code
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
            
            // Get today's steps from the most accurate source
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
            
            // SIMPLIFIED: Calculate steps for the previous hour
            val previousHour = if (now.hour == 0) 23 else now.hour - 1
            Log.d(TAG, "Recording steps for previous hour: $previousHour [ID: $workId]")
            
            // Get last recorded total for recording purposes
            val lastRecordedTotal = moodRepository.getLastRecordedTotal()
            Log.d(TAG, "Last recorded total: $lastRecordedTotal [ID: $workId]")
            
            // SIMPLIFIED: Calculate steps for the previous hour
            val stepsInPreviousHour = if (previousHour == 0) {
                // First hour of day - use total steps
                Log.d(TAG, "Hour 0 detected, using total steps: $todaySteps [ID: $workId]")
                todaySteps
            } else {
                // Calculate incremental steps for the previous hour
                val previousHourBaseline = hourlyStepsDao.getLastRecordedTotalForHour(LocalDate.now(), previousHour - 1) ?: 0
                val calculatedSteps = todaySteps - previousHourBaseline
                Log.d(TAG, "Calculated steps for hour $previousHour: $calculatedSteps (total: $todaySteps, baseline: $previousHourBaseline) [ID: $workId]")
                calculatedSteps
            }
            
            Log.d(TAG, "Final steps for hour $previousHour: $stepsInPreviousHour [ID: $workId]")
            
            // Record the previous hour's steps
            moodRepository.recordHourlySteps(previousHour, stepsInPreviousHour, todaySteps)
            
            // SIMPLIFIED: Apply hourly mood update using consolidated method
            moodRepository.applyHourlyMoodUpdate(stepsInPreviousHour, todaySteps)
            
            // Store worker update data for UI synchronization
            val currentTime = System.currentTimeMillis()
            Log.i(TAG, "Worker: Storing update timestamp: $currentTime for hour: $previousHour [ID: $workId]")
            moodRepository.storeLastWorkerUpdate(previousHour, todaySteps, currentTime)
            
            Log.i(TAG, "Work execution completed successfully [ID: $workId]")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed [ID: $workId]", e)
            Result.failure()
        }
    }
} 