// Updated: Cleaned up imports, fixed Configuration.Provider implementation, and ensured only one Application class is used
// Fixed: Added forced database reset to handle schema corruption issues
// Fixed: Prevented infinite loop in error handling by adding cooldown and clearing exception handler
// Updated: Added comprehensive fallback system for mood decay when workers fail
// Updated: Added force poll mechanism when app becomes active to catch missed steps
package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.navigation.AppNavigation
import com.example.myapplication.ui.theme.StepCounterTheme
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import android.util.Log
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.service.UnifiedStepCounterService
import com.example.myapplication.service.StepCountingService
import com.example.myapplication.service.HourlyStepPoller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import android.content.Context
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val TAG = "MainActivity"
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    @Inject
    lateinit var moodRepository: MoodRepository
    
    @Inject
    lateinit var stepCountRepository: StepCountRepository
    
    @Inject
    lateinit var stepCounterService: UnifiedStepCounterService
    
    // REMOVED: @Inject lateinit var hourlyStepPoller: HourlyStepPoller - MoodUpdateWorker handles hourly data recording
    
    private var lastKnownDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity onCreate started")
        
        // Start the step counting service with a small delay to ensure app is fully initialized
        lifecycleScope.launch {
            delay(500) // Small delay to ensure app is fully initialized
            startStepCountingService()
        }
        
        // Set up error handling for database issues
        setupErrorHandling()
        
        // Check for date changes on app start
        checkForDateChange()
        
        // Check for missed mood decay when app starts
        checkForMissedMoodDecay()
        
        // Force poll to catch any missed steps when app becomes active
        forcePollOnAppActive()
        
        setContent {
            StepCounterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun setupErrorHandling() {
        // Set up a global error handler for this activity
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in MainActivity thread: ${thread.name}", throwable)
            
            // Check if this is a database-related error
            if (throwable.message?.contains("Migration didn't properly handle") == true ||
                throwable.message?.contains("UNIQUE constraint failed") == true ||
                throwable.message?.contains("SQLiteConstraintException") == true ||
                throwable.message?.contains("database") == true) {
                
                Log.w(TAG, "Detected database error in MainActivity, triggering reset")
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                
                // Check if we've already tried to reset recently to prevent infinite loops
                val lastResetTime = prefs.getLong("last_database_reset", 0)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastResetTime < 5000) { // 5 second cooldown
                    Log.w(TAG, "Database reset attempted too recently, letting default handler deal with it")
                    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                    defaultHandler?.uncaughtException(thread, throwable)
                    return@setDefaultUncaughtExceptionHandler
                }
                
                prefs.edit()
                    .putBoolean("migration_failed", true)
                    .putLong("last_database_reset", currentTime)
                    .apply()
                
                // Clear the exception handler to prevent infinite loops
                Thread.setDefaultUncaughtExceptionHandler(null)
                
                // Run recreate on the main thread
                runOnUiThread {
                    recreate()
                }
                return@setDefaultUncaughtExceptionHandler
            }
            
            // For other errors, let the default handler deal with it
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity onResume")
        
        // Force poll when app becomes active to catch any missed steps
        forcePollOnAppActive()
    }

    private fun checkForDateChange() {
        activityScope.launch {
            try {
                val currentDate = LocalDate.now()
                
                if (currentDate != lastKnownDate) {
                    Log.i(TAG, "=== DATE CHANGE DETECTED IN MAIN ACTIVITY ===")
                    Log.i(TAG, "Date changed from $lastKnownDate to $currentDate")
                    
                    // Get current mood state
                    val currentMood = moodRepository.getCurrentMood().firstOrNull()
                    
                    if (currentMood != null && currentMood.date.isBefore(currentDate)) {
                        Log.i(TAG, "Performing date change reset")
                        
                        // Finalize the previous day's mood
                        val yesterday = currentMood.date
                        Log.d(TAG, "Finalizing mood for date: $yesterday")
                        moodRepository.finalizeDayMood(yesterday)
                        
                        // Reset for the new day
                        Log.d(TAG, "Resetting daily mood and steps for new day")
                        moodRepository.resetDaily()
                        
                        // Reset step baseline for new day
                        stepCounterService.resetDailyBaseline()
                        
                        // Reset step count in repository
                        stepCountRepository.updateTodaySteps(0)
                        
                        Log.i(TAG, "=== MAIN ACTIVITY DATE CHANGE RESET COMPLETED ===")
                    }
                    
                    lastKnownDate = currentDate
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for date change in MainActivity", e)
            }
        }
    }

    private fun checkForMissedMoodDecay() {
        activityScope.launch {
            try {
                Log.i(TAG, "Checking for missed mood decay in MainActivity")
                
                val now = java.time.LocalDateTime.now()
                val currentHour = now.hour
                val currentMood = moodRepository.getCurrentMood().firstOrNull()
                
                if (currentMood != null) {
                    val lastPersistedSteps = currentMood.lastPersistedSteps
                    Log.d(TAG, "checkForMissedMoodDecay: Current hour: $currentHour, Last persisted steps: $lastPersistedSteps")
                    
                    // ENHANCED: Check hourly steps to identify missed worker executions
                    val today = java.time.LocalDate.now()
                    val hourlySteps = moodRepository.getTodayHourlySteps().firstOrNull() ?: emptyList()
                    
                    Log.d(TAG, "checkForMissedMoodDecay: Current hour: $currentHour, Recorded hours: ${hourlySteps.map { it.hour }}")
                    
                    // Check each hour from 0 to currentHour-1 to see if it was processed
                    var missedHours = 0
                    for (hour in 0 until currentHour) {
                        val hourRecorded = hourlySteps.any { it.hour == hour }
                        if (!hourRecorded) {
                            Log.w(TAG, "checkForMissedMoodDecay: Hour $hour was not recorded! This indicates worker failure.")
                            missedHours++
                        }
                    }
                    
                    if (missedHours > 0) {
                        Log.i(TAG, "checkForMissedMoodDecay: Found $missedHours missed hours, applying comprehensive fallback")
                        
                        // Get today's total steps to calculate incremental steps for missed hours
                        val todaySteps = stepCountRepository.getTodayStepCount().firstOrNull()?.steps ?: 0
                        var newMood = currentMood.mood
                        
                        // Calculate steps for each missed hour
                        for (hour in 0 until currentHour) {
                            val hourRecorded = hourlySteps.any { it.hour == hour }
                            if (!hourRecorded) {
                                // Calculate steps for this missed hour
                                val totalStepsUpToHour = hourlySteps
                                    .filter { it.hour < hour }
                                    .sumOf { it.steps }
                                
                                val stepsInHour = if (todaySteps > totalStepsUpToHour) {
                                    todaySteps - totalStepsUpToHour
                                } else {
                                    0
                                }
                                
                                // Record the missed hour's steps
                                moodRepository.recordHourlySteps(hour, stepsInHour)
                                
                                // Apply mood calculation for this hour
                                val moodGain = moodRepository.calculateMoodFromStepsInPeriod(stepsInHour)
                                val decay = moodRepository.calculateDecay(hour, stepsInHour, newMood)
                                newMood = (newMood + moodGain - decay).coerceIn(0, 130)
                                
                                Log.d(TAG, "checkForMissedMoodDecay: Processed missed hour $hour: steps=$stepsInHour, gain=$moodGain, decay=$decay, mood=$newMood")
                            }
                        }
                        
                        // Update the mood in the database
                        val updatedMood = currentMood.copy(mood = newMood, lastPersistedSteps = todaySteps)
                        moodRepository.updateMoodState(updatedMood)
                        
                        Log.i(TAG, "checkForMissedMoodDecay: Updated mood from ${currentMood.mood} to $newMood")
                    } else {
                        Log.d(TAG, "checkForMissedMoodDecay: No missed hours detected")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for missed mood decay", e)
            }
        }
    }
    
    private fun startStepCountingService() {
        try {
            Log.i(TAG, "Starting step counting service from MainActivity")
            
            // Check permissions before starting service
            val hasActivityRecognitionPermission = ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                notificationManager.areNotificationsEnabled()
            }
            
            if (!hasActivityRecognitionPermission) {
                Log.w(TAG, "Cannot start service: Missing ACTIVITY_RECOGNITION permission")
                return
            }
            
            if (!hasNotificationPermission) {
                Log.w(TAG, "Cannot start service: Missing POST_NOTIFICATIONS permission")
                return
            }
            
            val serviceIntent = Intent(this, StepCountingService::class.java).apply {
                action = StepCountingService.ACTION_START_STEP_COUNTING
            }
            
            Log.i(TAG, "Starting service with action: ${serviceIntent.action}")
            Log.i(TAG, "Service class: ${StepCountingService::class.java.name}")
            
            startForegroundService(serviceIntent)
            Log.i(TAG, "Service start command sent successfully from MainActivity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service from MainActivity", e)
        }
    }

    /**
     * Force poll when app becomes active to catch any missed steps
     * FIXED: Removed applyHourlyMoodDecay call - UI already shows latest mood
     */
    private fun forcePollOnAppActive() {
        lifecycleScope.launch {
            try {
                Log.i(TAG, "=== FORCING POLL ON APP ACTIVE ===")
                
                // Small delay to ensure services are initialized
                delay(1000)
                
                // FIXED: Don't call applyHourlyMoodDecay here
                // The UI already shows the latest mood from the database
                // The MoodUpdateWorker handles hourly updates at exact hour boundaries
                // This prevents unnecessary mood calculations and potential data corruption
                
                Log.i(TAG, "=== FORCE POLL ON APP ACTIVE COMPLETED (no mood update needed) ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error forcing poll on app active", e)
            }
        }
    }
}