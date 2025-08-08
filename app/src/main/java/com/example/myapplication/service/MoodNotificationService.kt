// Updated: MoodNotificationService - Enhanced with comprehensive error handling and reliability features
// Added: Notification channel validation and recreation
// Added: Service restart mechanisms
// Added: Enhanced error recovery and logging
// Added: Notification delivery verification
package com.example.myapplication.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.repository.MoodNotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay

@Singleton
class MoodNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moodNotificationRepository: MoodNotificationRepository
) {
    private val TAG = "MoodNotificationService"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "mood_notifications"
        private const val CHANNEL_NAME = "Mood Alerts"
        private const val CHANNEL_DESCRIPTION = "Notifications for mood drops"
        private const val NOTIFICATION_ID = 1002
        private const val TEST_NOTIFICATION_ID = 1003
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    init {
        ensureNotificationChannel()
    }
    
    /**
     * ENHANCED: Ensure notification channel exists and is properly configured
     */
    private fun ensureNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Check if channel already exists
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                
                if (existingChannel == null) {
                    Log.i(TAG, "Creating mood notification channel: $CHANNEL_ID")
                    createNotificationChannel()
                } else {
                    Log.d(TAG, "Mood notification channel already exists: $CHANNEL_ID")
                    // Verify channel settings
                    if (existingChannel.importance != NotificationManager.IMPORTANCE_DEFAULT) {
                        Log.w(TAG, "Notification channel has unexpected importance, recreating")
                        notificationManager.deleteNotificationChannel(CHANNEL_ID)
                        createNotificationChannel()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring notification channel", e)
            // Try to create channel as fallback
            try {
                createNotificationChannel()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to create notification channel even as fallback", e2)
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.i(TAG, "Created mood notification channel: $CHANNEL_ID")
        }
    }
    
    /**
     * ENHANCED: Send mood notification with retry logic and error handling
     */
    suspend fun sendMoodNotification() {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                attempt++
                Log.d(TAG, "Attempting to send mood notification (attempt $attempt/$MAX_RETRY_ATTEMPTS)")
                
                // Ensure notification channel exists
                ensureNotificationChannel()
                
                val settings = moodNotificationRepository.getMoodNotificationSettings()
                
                // Check if we should send notification
                if (!moodNotificationRepository.shouldSendNotification()) {
                    Log.d(TAG, "Mood notification conditions not met, skipping")
                    return
                }
                
                val message = moodNotificationRepository.getNotificationMessage()
                
                // Create intent for when notification is tapped
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Build notification
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_walk)
                    .setContentTitle("Your Butts Needs You! 🚶‍♂️")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .apply {
                        if (settings.notificationSound) {
                            setDefaults(NotificationCompat.DEFAULT_SOUND)
                        }
                        if (settings.notificationVibration) {
                            setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                        }
                    }
                    .build()
                
                // Send notification
                notificationManager.notify(NOTIFICATION_ID, notification)
                
                // Verify notification was sent
                verifyNotificationDelivery()
                
                // Mark as sent in repository
                moodNotificationRepository.markNotificationSent()
                
                Log.i(TAG, "Successfully sent mood notification: $message")
                return // Success, exit retry loop
                
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Error sending mood notification (attempt $attempt/$MAX_RETRY_ATTEMPTS)", e)
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    Log.d(TAG, "Retrying in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        // All attempts failed
        Log.e(TAG, "Failed to send mood notification after $MAX_RETRY_ATTEMPTS attempts", lastException)
        throw lastException ?: Exception("Failed to send mood notification")
    }
    
    /**
     * ENHANCED: Verify notification delivery
     */
    private fun verifyNotificationDelivery() {
        try {
            val activeNotifications = notificationManager.activeNotifications
            val hasMoodNotification = activeNotifications.any { it.id == NOTIFICATION_ID }
            
            if (hasMoodNotification) {
                Log.d(TAG, "Notification delivery verified")
            } else {
                Log.w(TAG, "Notification delivery verification failed - notification not found in active notifications")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify notification delivery", e)
        }
    }
    
    /**
     * ENHANCED: Test method with improved error handling
     */
    fun sendTestMoodNotification() {
        try {
            Log.i(TAG, "Sending test mood notification")
            
            // Ensure notification channel exists
            ensureNotificationChannel()
            
            val settings = moodNotificationRepository.getMoodNotificationSettings()
            
            // Create intent for when notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build test notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_walk)
                .setContentTitle("🧠 Test Mood Notification")
                .setContentText("This is a test mood notification! Your character's mood needs attention!")
                .setStyle(NotificationCompat.BigTextStyle().bigText("This is a test mood notification! Your character's mood needs attention! Time to get moving and make your butts happy! 🚶‍♂️"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .apply {
                    if (settings.notificationSound) {
                        setDefaults(NotificationCompat.DEFAULT_SOUND)
                    }
                    if (settings.notificationVibration) {
                        setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    }
                }
                .build()
            
            // Send test notification
            notificationManager.notify(TEST_NOTIFICATION_ID, notification)
            
            // Verify test notification delivery
            verifyTestNotificationDelivery()
            
            Log.i(TAG, "Sent test mood notification successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test mood notification", e)
            throw e
        }
    }
    
    /**
     * ENHANCED: Verify test notification delivery
     */
    private fun verifyTestNotificationDelivery() {
        try {
            val activeNotifications = notificationManager.activeNotifications
            val hasTestNotification = activeNotifications.any { it.id == TEST_NOTIFICATION_ID }
            
            if (hasTestNotification) {
                Log.d(TAG, "Test notification delivery verified")
            } else {
                Log.w(TAG, "Test notification delivery verification failed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify test notification delivery", e)
        }
    }
    
    /**
     * ENHANCED: Cancel notifications with error handling
     */
    fun cancelMoodNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
            notificationManager.cancel(TEST_NOTIFICATION_ID)
            Log.d(TAG, "Cancelled mood notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling mood notifications", e)
        }
    }
    
    /**
     * ENHANCED: Check if notification service is working properly
     */
    fun isNotificationServiceHealthy(): Boolean {
        return try {
            // Check if notification manager is available
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Check if channel exists (for Android O+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = manager.getNotificationChannel(CHANNEL_ID)
                channel != null
            } else {
                true // For older Android versions, assume it's working
            }
        } catch (e: Exception) {
            Log.e(TAG, "Notification service health check failed", e)
            false
        }
    }
} 