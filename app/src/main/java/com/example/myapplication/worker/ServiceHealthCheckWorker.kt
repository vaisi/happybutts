// Created: ServiceHealthCheckWorker for ensuring step counting service is always running
package com.example.myapplication.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.myapplication.service.StepCountingService
import com.example.myapplication.service.UnifiedStepCounterService
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
    private val userPreferences: UserPreferences
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val TAG = "ServiceHealthCheckWorker"
        private const val WORK_NAME = "service_health_check_worker"
        private const val CHECK_INTERVAL_MINUTES = 15L // Check every 15 minutes
        
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()
            
            val request = PeriodicWorkRequestBuilder<ServiceHealthCheckWorker>(
                CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(5))
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
            Log.i(TAG, "Cancelled service health check work")
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting service health check")
            
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
                    return Result.retry()
                }
            } else {
                Log.d(TAG, "Step counting service is running normally")
            }
            
            // Attempt to recover any missed step data
            attemptStepDataRecovery()
            
            Log.d(TAG, "Service health check completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during service health check", e)
            Result.retry()
        }
    }
    
    private fun isStepCountingServiceRunning(): Boolean {
        return try {
            val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
            
            val isRunning = runningServices.any { service ->
                service.service.className == StepCountingService::class.java.name
            }
            
            Log.d(TAG, "Service running check: $isRunning")
            isRunning
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if service is running", e)
            false
        }
    }
    
    private suspend fun canStartStepCountingService(context: Context, userPreferences: UserPreferences): Boolean {
        val onboardingCompleted = userPreferences.onboardingCompleted.first()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return onboardingCompleted && hasPermission
    }
    
    private suspend fun startStepCountingService() {
        if (!canStartStepCountingService(applicationContext, userPreferences)) {
            Log.i(TAG, "Not starting StepCountingService: onboarding not complete or permission not granted")
            return
        }
        try {
            val serviceIntent = Intent(applicationContext, StepCountingService::class.java).apply {
                action = StepCountingService.ACTION_START_STEP_COUNTING
            }
            applicationContext.startForegroundService(serviceIntent)
            Log.i(TAG, "Sent start intent to StepCountingService")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting step counting service", e)
            throw e
        }
    }
    
    private suspend fun attemptStepDataRecovery() {
        try {
            Log.d(TAG, "Attempting step data recovery")
            
            // Trigger step data recovery in the unified service
            unifiedStepCounterService.attemptStepDataRecovery()
            
            Log.d(TAG, "Step data recovery attempt completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during step data recovery", e)
        }
    }
} 