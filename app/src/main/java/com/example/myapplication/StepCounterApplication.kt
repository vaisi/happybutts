// Updated: Cleaned up imports, fixed Configuration.Provider implementation, and ensured only one Application class is used
// Updated: Removed forced database reset logic for better reliability
// Updated: Simplified database initialization
package com.example.myapplication

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.work.*
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import com.example.myapplication.worker.MoodUpdateWorker
import android.util.Log
import javax.inject.Inject
import com.example.myapplication.service.UnifiedStepCounterService
import com.example.myapplication.data.repository.MoodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalDate
import androidx.work.Configuration
import com.example.myapplication.data.repository.StepRepository
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.worker.InactivityCheckWorker
import java.time.Duration
import com.example.myapplication.worker.MoodCheckWorker
import android.content.Context
import com.example.myapplication.worker.ServiceHealthCheckWorker
import com.example.myapplication.util.SimpleDataTest

@HiltAndroidApp
class StepCounterApplication : Application(), Configuration.Provider, Application.ActivityLifecycleCallbacks {
    private val TAG = "StepCounterApplication"

    @Inject
    lateinit var stepCounterService: UnifiedStepCounterService

    @Inject
    lateinit var moodRepository: MoodRepository

    @Inject
    lateinit var stepRepository: StepRepository

    @Inject
    lateinit var stepCountRepository: StepCountRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userPreferences: com.example.myapplication.data.preferences.UserPreferences

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Use property override for WorkManager configuration (required for WorkManager 2.9.0+)
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate started")
        registerActivityLifecycleCallbacks(this)
        
        // Test core data functionality
        Log.i(TAG, "Testing core data functionality")
        SimpleDataTest.testDataFunctionality(this)
        
        // Schedule the mood update worker
        scheduleMoodUpdateWorker()
        
        // Schedule inactivity check worker
        Log.i(TAG, "Scheduling inactivity check worker")
        InactivityCheckWorker.schedulePeriodicWork(this)
        
        // Schedule mood check worker
        Log.i(TAG, "Scheduling mood check worker")
        MoodCheckWorker.schedulePeriodicWork(this)
        
        // Schedule the service health check worker
        ServiceHealthCheckWorker.schedulePeriodicWork(this)
        
        // Check for missed reset when app starts
        checkForMissedReset()
        
        // Log current dynamic values
        logCurrentDynamicValues()
        
        Log.i(TAG, "Application onCreate completed")
    }

    private fun scheduleMoodUpdateWorker() {
        Log.i(TAG, "Scheduling mood update worker")
        
        // Enhanced constraints to ensure worker runs more reliably
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()

        // CRITICAL FIX: Calculate initial delay to reach next hour boundary
        val now = LocalDateTime.now()
        val nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
        val initialDelay = Duration.between(now, nextHour)
        
        Log.i(TAG, "Current time: ${now.hour}:${now.minute}")
        Log.i(TAG, "Next hour boundary: ${nextHour.hour}:${nextHour.minute}")
        Log.i(TAG, "Initial delay: ${initialDelay.toMinutes()} minutes")

        // Schedule worker to run every hour with exact hour boundary alignment
        val moodUpdateRequest = PeriodicWorkRequestBuilder<MoodUpdateWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(5))
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES) // Align to hour boundary
            .addTag("mood_update")
            .build()

        WorkManager.getInstance(this).apply {
            // Cancel any existing work with this tag
            cancelAllWorkByTag("mood_update")
            
            // Enqueue the new work
            enqueueUniquePeriodicWork(
                "mood_update_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                moodUpdateRequest
            )
        }
        
        Log.i(TAG, "Mood update worker scheduled successfully for hour boundaries")
        
        // Log worker status for debugging
        WorkManager.getInstance(this).getWorkInfosByTagLiveData("mood_update").observeForever { workInfos ->
            Log.i(TAG, "=== WORKER STATUS UPDATE ===")
            workInfos.forEach { workInfo ->
                Log.i(TAG, "Worker ID: ${workInfo.id}, State: ${workInfo.state}, Tags: ${workInfo.tags}")
            }
            Log.i(TAG, "=== END WORKER STATUS ===")
        }
    }

    private fun checkForMissedReset() {
        Log.i(TAG, "Checking for missed reset")
        applicationScope.launch {
            try {
                val currentMoodWithSteps = moodRepository.getCurrentMood().first()
                val today = LocalDate.now()
                
                // Check if this is a fresh install (no mood history)
                val hasMoodHistory = moodRepository.getMoodHistory(1).first().isNotEmpty()
                
                if (!hasMoodHistory) {
                    Log.i(TAG, "Fresh install detected - mood will be initialized by MoodRepository.getCurrentMood()")
                    // FIXED: Don't call applyHourlyMoodDecay here
                    // MoodRepository.getCurrentMood() already handles fresh install initialization
                    // This prevents mood from being set to 0 incorrectly
                }
                
                // If we have a mood entry and it's from before today
                if (currentMoodWithSteps != null && currentMoodWithSteps.date.isBefore(today)) {
                    Log.i(TAG, "Detected missed reset - last mood date was before today")
                    // Finalize the previous day's mood
                    val yesterday = today.minusDays(1)
                    Log.d(TAG, "Finalizing mood for date: $yesterday")
                    moodRepository.finalizeDayMood(yesterday)
                    // Reset for the new day
                    Log.d(TAG, "Resetting daily mood and steps")
                    moodRepository.resetDaily()
                    // Clear step detector data to force new baseline
                    stepCounterService.clearStepDetectorData()
                    // Also reset the step count in the repository
                    stepCountRepository.updateTodaySteps(0)
                    Log.i(TAG, "Missed reset handled successfully")
                } else {
                    Log.d(TAG, "No missed reset detected")
                    // Also check if the app is opened after midnight and the date has changed
                    val lastProcessedDate = stepCounterService.javaClass.getDeclaredField("lastProcessedDate").apply { isAccessible = true }.get(stepCounterService) as? LocalDate
                    if (lastProcessedDate != null && lastProcessedDate.isBefore(today)) {
                        Log.i(TAG, "App opened after midnight, clearing step detector data for new day")
                        stepCounterService.clearStepDetectorData()
                        // Also reset the step count in the repository
                        stepCountRepository.updateTodaySteps(0)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for missed reset", e)
            }
        }
    }

    // Log the current dynamic values based on user's goal
    private fun logCurrentDynamicValues() {
        applicationScope.launch {
            try {
                val userGoal = try {
                    userPreferences.dailyGoal.first()
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting daily goal from preferences, using default", e)
                    10000 // Default goal
                }
                val BASE_GOAL_STEPS = 10000
                val BASE_STEPS_PER_MOOD = 150
                val BASE_DECAY_THRESHOLD = 250
                val BASE_MOOD_DECAY = 5
                
                val stepsPerMood = (BASE_STEPS_PER_MOOD * userGoal / BASE_GOAL_STEPS).coerceAtLeast(50)
                val decayThreshold = (BASE_DECAY_THRESHOLD * userGoal / BASE_GOAL_STEPS).coerceAtLeast(50)
                val moodDecayPerHour = (BASE_MOOD_DECAY * userGoal / BASE_GOAL_STEPS).coerceAtLeast(1)
                
                Log.i(TAG, "=== APP START - CURRENT DYNAMIC VALUES ===")
                Log.i(TAG, "User Goal: $userGoal steps")
                Log.i(TAG, "Steps per +1 mood: $stepsPerMood (base: $BASE_STEPS_PER_MOOD)")
                Log.i(TAG, "Decay threshold: $decayThreshold steps/hour (base: $BASE_DECAY_THRESHOLD)")
                Log.i(TAG, "Mood decay per hour: $moodDecayPerHour points (base: $BASE_MOOD_DECAY)")
                Log.i(TAG, "================================================")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging dynamic values", e)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed: Activity resumed")
        // Let the ViewModel control when step counting starts after date change detection
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
} 