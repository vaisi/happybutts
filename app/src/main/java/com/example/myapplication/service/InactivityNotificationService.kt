// Updated: InactivityNotificationService - added test notification method that bypasses conditions
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
    }
    
    init {
        createNotificationChannel()
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
            }
            notificationManager.createNotificationChannel(channel)
            Log.i(TAG, "Created notification channel: $CHANNEL_ID")
        }
    }
    
    suspend fun sendInactivityNotification() {
        try {
            val settings = inactivityRepository.getInactivitySettings()
            
            // Check if we should send notification
            if (!inactivityRepository.shouldSendNotification()) {
                Log.d(TAG, "Notification conditions not met, skipping")
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
            
            // Mark as sent in repository
            inactivityRepository.markNotificationSent()
            
            Log.i(TAG, "Sent inactivity notification: $message")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending inactivity notification", e)
        }
    }

    // Test method that bypasses all conditions
    fun sendTestInactivityNotification() {
        try {
            Log.i(TAG, "Sending test inactivity notification")
            
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
                .setStyle(NotificationCompat.BigTextStyle().bigText("This is a test inactivity notification! You've been inactive for too long. Time to get up and move around! Your butts needs some exercise! 🚶‍♂️"))
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
            
            Log.i(TAG, "Sent test inactivity notification successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test inactivity notification", e)
        }
    }
    
    fun cancelInactivityNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(TEST_NOTIFICATION_ID)
        Log.d(TAG, "Cancelled inactivity notifications")
    }
} 