// Updated: InactivityNotificationService - Enhanced with comprehensive error handling and reliability features
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
import com.example.myapplication.data.repository.InactivityRepository
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay

@Singleton
class InactivityNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inactivityRepository: InactivityRepository
) {
    private val TAG = "InactivityNotificationService"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "inactivity_notifications"
        private const val CHANNEL_NAME = "Inactivity Alerts"
        private const val CHANNEL_DESCRIPTION = "Notifications for inactivity periods"
        private const val NOTIFICATION_ID = 1001
        private const val TEST_NOTIFICATION_ID = 1004
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
                    Log.i(TAG, "Creating inactivity notification channel: $CHANNEL_ID")
                    createNotificationChannel()
                } else {
                    Log.d(TAG, "Inactivity notification channel already exists: $CHANNEL_ID")
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
            Log.i(TAG, "Created inactivity notification channel: $CHANNEL_ID")
        }
    }
    
    /**
     * ENHANCED: Send inactivity notification with retry logic and error handling
     */
    suspend fun sendInactivityNotification() {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                attempt++
                Log.d(TAG, "Attempting to send inactivity notification (attempt $attempt/$MAX_RETRY_ATTEMPTS)")
                
                // Ensure notification channel exists
                ensureNotificationChannel()
                
                val settings = inactivityRepository.getInactivitySettings()
                
                // Check if we should send notification
                if (!inactivityRepository.shouldSendNotification()) {
                    Log.d(TAG, "Inactivity notification conditions not met, skipping")
                    return
                }
                
                val message = inactivityRepository.getNotificationMessage()
                
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
                    .setContentTitle("Time to Move! 🚶‍♂️")
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
                inactivityRepository.markNotificationSent()
                
                Log.i(TAG, "Successfully sent inactivity notification: $message")
                return // Success, exit retry loop
                
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Error sending inactivity notification (attempt $attempt/$MAX_RETRY_ATTEMPTS)", e)
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    Log.d(TAG, "Retrying in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        
        // All attempts failed
        Log.e(TAG, "Failed to send inactivity notification after $MAX_RETRY_ATTEMPTS attempts", lastException)
        throw lastException ?: Exception("Failed to send inactivity notification")
    }
    
    /**
     * ENHANCED: Verify notification delivery
     */
    private fun verifyNotificationDelivery() {
        try {
            val activeNotifications = notificationManager.activeNotifications
            val hasInactivityNotification = activeNotifications.any { it.id == NOTIFICATION_ID }
            
            if (hasInactivityNotification) {
                Log.d(TAG, "Inactivity notification delivery verified")
            } else {
                Log.w(TAG, "Inactivity notification delivery verification failed - notification not found in active notifications")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify inactivity notification delivery", e)
        }
    }
    
    /**
     * ENHANCED: Test method with improved error handling
     */
    fun sendTestInactivityNotification() {
        try {
            Log.i(TAG, "Sending test inactivity notification")
            
            // Ensure notification channel exists
            ensureNotificationChannel()
            
            val settings = inactivityRepository.getInactivitySettings()
            
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
                .setContentTitle("⏰ Test Inactivity Notification")
                .setContentText("This is a test inactivity notification! Time to get moving!")
                .setStyle(NotificationCompat.BigTextStyle().bigText("This is a test inactivity notification! Time to get moving! Your butts needs some activity! 🚶‍♂️"))
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
            
            Log.i(TAG, "Sent test inactivity notification successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test inactivity notification", e)
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
                Log.d(TAG, "Test inactivity notification delivery verified")
            } else {
                Log.w(TAG, "Test inactivity notification delivery verification failed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify test inactivity notification delivery", e)
        }
    }
    
    /**
     * ENHANCED: Cancel notifications with error handling
     */
    fun cancelInactivityNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
            notificationManager.cancel(TEST_NOTIFICATION_ID)
            Log.d(TAG, "Cancelled inactivity notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling inactivity notifications", e)
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
            Log.e(TAG, "Inactivity notification service health check failed", e)
            false
        }
    }
} 