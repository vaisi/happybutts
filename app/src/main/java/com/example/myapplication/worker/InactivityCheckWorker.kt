// Created: InactivityCheckWorker for background inactivity monitoring
package com.example.myapplication.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.myapplication.service.InactivityNotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.util.concurrent.TimeUnit

@HiltWorker
class InactivityCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val inactivityNotificationService: InactivityNotificationService
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val TAG = "InactivityCheckWorker"
        private const val WORK_NAME = "inactivity_check_worker"
        private const val CHECK_INTERVAL_HOURS = 1L // Check every hour
        
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()
            
            val request = PeriodicWorkRequestBuilder<InactivityCheckWorker>(
                CHECK_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofMinutes(15))
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            
            Log.i(TAG, "Scheduled periodic inactivity check every $CHECK_INTERVAL_HOURS hours")
        }
        
        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled inactivity check work")
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting inactivity check")
            
            // Attempt to send notification if conditions are met
            inactivityNotificationService.sendInactivityNotification()
            
            Log.d(TAG, "Inactivity check completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during inactivity check", e)
            Result.retry()
        }
    }
} 