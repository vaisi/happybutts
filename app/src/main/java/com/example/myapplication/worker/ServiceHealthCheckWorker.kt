// Updated: ServiceHealthCheckWorker - Enhanced with comprehensive error handling and reliability features
// Added: Enhanced service restart mechanisms
// Added: Better error recovery and logging
// Added: Service health validation
// Added: Notification service health checks
package com.example.myapplication.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.myapplication.service.StepCountingService
import com.example.myapplication.service.UnifiedStepCounterService
import com.example.myapplication.service.MoodNotificationService
import com.example.myapplication.service.InactivityNotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import com.example.myapplication.data.preferences.UserPreferences
import androidx.core.content.ContextCompat
import android.Manifest

@HiltWorker
class ServiceHealthCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val unifiedStepCounterService: UnifiedStepCounterService,
    private val moodNotificationService: MoodNotificationService,
    private val inactivityNotificationService: InactivityNotificationService,
    private val userPreferences: UserPreferences
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val TAG = "ServiceHealthCheckWorker"
        private const val WORK_NAME = "service_health_check_worker"
        private const val CHECK_INTERVAL_MINUTES = 15L // Check every 15 minutes
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 2000L
        
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build()
            
            val request = PeriodicWorkRequestBuilder<ServiceHealthCheckWorker>(
                CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(5))
                .addTag("service_health_check")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            
            Log.i(TAG, "Scheduled periodic service health check every $CHECK_INTERVAL_MINUTES minutes")
        }
        
        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelAllWorkByTag("service_health_check")
            Log.i(TAG, "Cancelled service health check work")
        }
    }
    
    override suspend fun doWork(): Result {
        val workId = id.toString()
        Log.i(TAG, "Starting service health check [ID: $workId]")
        
        return try {
            // ENHANCED: Check all services with retry logic
            var attempt = 0
            var lastException: Exception? = null
            
            while (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    attempt++
                    Log.d(TAG, "Attempting service health check (attempt $attempt/$MAX_RETRY_ATTEMPTS) [ID: $workId]")
                    
                    // Check step counting service
                    checkStepCountingService()
                    
                    // Check notification services
                    checkNotificationServices()
                    
                    // Attempt to recover any missed step data
                    attemptStepDataRecovery()
                    
                    Log.i(TAG, "Service health check completed successfully [ID: $workId]")
                    return Result.success()
                    
                } catch (e: Exception) {
                    lastException = e
                    Log.e(TAG, "Error during service health check (attempt $attempt/$MAX_RETRY_ATTEMPTS) [ID: $workId]", e)
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val delayMs = BASE_RETRY_DELAY_MS * (1 shl (attempt - 1)) // Exponential backoff
                        Log.d(TAG, "Retrying in ${delayMs}ms... [ID: $workId]")
                        delay(delayMs)
                    }
                }
            }
            
            // All attempts failed
            Log.e(TAG, "Failed service health check after $MAX_RETRY_ATTEMPTS attempts [ID: $workId]", lastException)
            return Result.retry()
            
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during service health check [ID: $workId]", e)
            return Result.retry()
        }
    }
    
    /**
     * ENHANCED: Check step counting service health
     */
    private suspend fun checkStepCountingService() {
        try {
            // Check if the step counting service is running
            val isServiceRunning = isStepCountingServiceRunning()
            
            if (!isServiceRunning) {
                Log.w(TAG, "Step counting service is not running, attempting to start it")
                startStepCountingService()
                
                // Wait a moment for the service to start
                delay(2000)
                
                // Verify the service started successfully
                val serviceStarted = isStepCountingServiceRunning()
                if (serviceStarted) {
                    Log.i(TAG, "Successfully started step counting service")
                } else {
                    Log.e(TAG, "Failed to start step counting service")
                    throw Exception("Failed to start step counting service")
                }
            } else {
                Log.d(TAG, "Step counting service is running normally")
            }
            
            // ENHANCED: Check UnifiedStepCounterService by attempting data recovery
            // This will help identify if the service is working properly
            try {
                unifiedStepCounterService.attemptStepDataRecovery()
                Log.d(TAG, "UnifiedStepCounterService data recovery check passed")
            } catch (e: Exception) {
                Log.w(TAG, "UnifiedStepCounterService data recovery check failed, but continuing", e)
                // Don't throw here as this is not critical
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking step counting service", e)
            throw e
        }
    }
    
    /**
     * ENHANCED: Check notification services health
     */
    private suspend fun checkNotificationServices() {
        try {
            // Check mood notification service
            if (!moodNotificationService.isNotificationServiceHealthy()) {
                Log.w(TAG, "Mood notification service is not healthy, attempting recovery")
                try {
                    moodNotificationService.sendTestMoodNotification()
                    Log.i(TAG, "Successfully recovered mood notification service")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to recover mood notification service", e)
                    throw e
                }
            } else {
                Log.d(TAG, "Mood notification service is healthy")
            }
            
            // Check inactivity notification service
            if (!inactivityNotificationService.isNotificationServiceHealthy()) {
                Log.w(TAG, "Inactivity notification service is not healthy, attempting recovery")
                try {
                    inactivityNotificationService.sendTestInactivityNotification()
                    Log.i(TAG, "Successfully recovered inactivity notification service")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to recover inactivity notification service", e)
                    throw e
                }
            } else {
                Log.d(TAG, "Inactivity notification service is healthy")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification services", e)
            throw e
        }
    }
    
    /**
     * ENHANCED: Check if step counting service is running
     */
    private fun isStepCountingServiceRunning(): Boolean {
        return try {
            val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
            runningServices.any { it.service.className == "com.example.myapplication.service.StepCountingService" }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if step counting service is running", e)
            false
        }
    }
    
    /**
     * ENHANCED: Start step counting service
     */
    private fun startStepCountingService() {
        try {
            val intent = Intent(applicationContext, StepCountingService::class.java).apply {
                action = StepCountingService.ACTION_START_STEP_COUNTING
            }
            
            // Check if we have the required permissions
            val hasActivityRecognition = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (hasActivityRecognition) {
                applicationContext.startService(intent)
                Log.i(TAG, "Started step counting service")
            } else {
                Log.w(TAG, "Cannot start step counting service - missing ACTIVITY_RECOGNITION permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting step counting service", e)
            throw e
        }
    }
    
    /**
     * ENHANCED: Attempt to recover any missed step data
     */
    private suspend fun attemptStepDataRecovery() {
        try {
            Log.d(TAG, "Attempting step data recovery")
            
            // Check if we need to recover data
            val onboardingCompleted = userPreferences.onboardingCompleted.first()
            if (!onboardingCompleted) {
                Log.d(TAG, "Onboarding not completed, skipping data recovery")
                return
            }
            
            // Force a step count update to catch any missed steps
            val currentSteps = unifiedStepCounterService.currentStepCount
            Log.d(TAG, "Current step count: $currentSteps")
            
            // Validate step count is reasonable
            if (currentSteps < 0) {
                Log.w(TAG, "Invalid step count detected: $currentSteps")
            } else if (currentSteps > 100000) {
                Log.w(TAG, "Unrealistic step count detected: $currentSteps")
            } else {
                Log.d(TAG, "Step count validation passed: $currentSteps")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during step data recovery", e)
            // Don't throw here as this is not critical
        }
    }
} 