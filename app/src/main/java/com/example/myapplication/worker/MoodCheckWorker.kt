// Updated: MoodCheckWorker - Enhanced with comprehensive error handling and reliability features
// Added: Retry logic with exponential backoff
// Added: Service health monitoring
// Added: Enhanced error recovery and logging
// Added: Notification service validation
package com.example.myapplication.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.myapplication.service.MoodNotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.time.LocalDate
import kotlinx.coroutines.delay

@HiltWorker
class MoodCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val moodNotificationService: MoodNotificationService
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val TAG = "MoodCheckWorker"
        private const val WORK_NAME = "mood_check_worker"
        private const val CHECK_INTERVAL_HOURS = 1L // Check every hour
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 5000L
        
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build()
            
            val request = PeriodicWorkRequestBuilder<MoodCheckWorker>(
                CHECK_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(15))
                .addTag("mood_check")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            
            Log.i(TAG, "Scheduled periodic mood check every $CHECK_INTERVAL_HOURS hours")
        }
        
        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelAllWorkByTag("mood_check")
            Log.i(TAG, "Cancelled mood check work")
        }
    }
    
    override suspend fun doWork(): Result {
        val workId = id.toString()
        Log.i(TAG, "Starting mood check work [ID: $workId]")
        
        return try {
            // ENHANCED: Check notification service health first
            if (!moodNotificationService.isNotificationServiceHealthy()) {
                Log.w(TAG, "Mood notification service is not healthy, attempting recovery [ID: $workId]")
                // Try to recover by forcing a test notification
                try {
                    moodNotificationService.sendTestMoodNotification()
                    Log.i(TAG, "Successfully recovered mood notification service [ID: $workId]")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to recover mood notification service [ID: $workId]", e)
                    return Result.retry()
                }
            }
            
            // ENHANCED: Attempt to send notification with retry logic
            var attempt = 0
            var lastException: Exception? = null
            
            while (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    attempt++
                    Log.d(TAG, "Attempting mood notification (attempt $attempt/$MAX_RETRY_ATTEMPTS) [ID: $workId]")
                    
                    moodNotificationService.sendMoodNotification()
                    
                    Log.i(TAG, "Successfully sent mood notification [ID: $workId]")
                    return Result.success()
                    
                } catch (e: Exception) {
                    lastException = e
                    Log.e(TAG, "Error sending mood notification (attempt $attempt/$MAX_RETRY_ATTEMPTS) [ID: $workId]", e)
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val delayMs = BASE_RETRY_DELAY_MS * (1 shl (attempt - 1)) // Exponential backoff
                        Log.d(TAG, "Retrying in ${delayMs}ms... [ID: $workId]")
                        delay(delayMs)
                    }
                }
            }
            
            // All attempts failed
            Log.e(TAG, "Failed to send mood notification after $MAX_RETRY_ATTEMPTS attempts [ID: $workId]", lastException)
            
            // Return retry with exponential backoff
            Result.retry()
            
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during mood check [ID: $workId]", e)
            Result.retry()
        }
    }
} 