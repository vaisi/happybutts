// Updated: MoodNotificationService - added test notification method that bypasses conditions
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
            Log.i(TAG, "Created mood notification channel: $CHANNEL_ID")
        }
    }
    
    suspend fun sendMoodNotification() {
        try {
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
            
            // Mark as sent in repository
            moodNotificationRepository.markNotificationSent()
            
            Log.i(TAG, "Sent mood notification: $message")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending mood notification", e)
        }
    }

    // Test method that bypasses all conditions
    fun sendTestMoodNotification() {
        try {
            Log.i(TAG, "Sending test mood notification")
            
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
            
            Log.i(TAG, "Sent test mood notification successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test mood notification", e)
        }
    }
    
    fun cancelMoodNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(TEST_NOTIFICATION_ID)
        Log.d(TAG, "Cancelled mood notifications")
    }
} 