// Updated: MoodNotificationRepository - now uses global UserPreferences for quiet hours instead of local settings
package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.myapplication.data.database.MoodNotificationDao
import com.example.myapplication.data.model.MoodNotificationData
import com.example.myapplication.data.model.MoodNotificationMessages
import com.example.myapplication.data.model.MoodNotificationSettings
import com.example.myapplication.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class MoodNotificationRepository @Inject constructor(
    private val moodNotificationDao: MoodNotificationDao,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) {
    private val TAG = "MoodNotificationRepository"
    private val prefs: SharedPreferences = context.getSharedPreferences("mood_notification_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_MOOD_DROP_THRESHOLD = "mood_drop_threshold"
        private const val KEY_MAX_NOTIFICATIONS = "max_notifications"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_MIN_HOURS_BETWEEN = "min_hours_between"
    }
    
    // Get mood notification settings
    fun getMoodNotificationSettings(): MoodNotificationSettings {
        return MoodNotificationSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            moodDropThreshold = prefs.getInt(KEY_MOOD_DROP_THRESHOLD, 2),
            maxNotificationsPerDay = prefs.getInt(KEY_MAX_NOTIFICATIONS, 5),
            quietHoursStart = 22, // Default, will be overridden by global settings
            quietHoursEnd = 7,    // Default, will be overridden by global settings
            notificationSound = prefs.getBoolean(KEY_SOUND, true),
            notificationVibration = prefs.getBoolean(KEY_VIBRATION, true),
            minHoursBetweenNotifications = prefs.getInt(KEY_MIN_HOURS_BETWEEN, 2)
        )
    }
    
    // Update mood notification settings
    suspend fun updateMoodNotificationSettings(settings: MoodNotificationSettings) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.enabled)
            putInt(KEY_MOOD_DROP_THRESHOLD, settings.moodDropThreshold)
            putInt(KEY_MAX_NOTIFICATIONS, settings.maxNotificationsPerDay)
            putBoolean(KEY_SOUND, settings.notificationSound)
            putBoolean(KEY_VIBRATION, settings.notificationVibration)
            putInt(KEY_MIN_HOURS_BETWEEN, settings.minHoursBetweenNotifications)
            apply()
        }
        Log.i(TAG, "Updated mood notification settings: $settings")
    }
    
    // Get mood notifications for a date
    fun getMoodNotificationsForDate(date: LocalDate): Flow<List<MoodNotificationData>> {
        return moodNotificationDao.getMoodNotificationsForDate(date)
    }
    
    // Record a mood drop
    suspend fun recordMoodDrop(
        previousMood: Int,
        currentMood: Int,
        stepsInPeriod: Int,
        periodHours: Float
    ) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        
        val moodDrop = calculateMoodDrop(previousMood, currentMood)
        
        val notification = MoodNotificationData(
            date = today.toString(), // Format: YYYY-MM-DD
            timestamp = now,
            previousMood = previousMood,
            currentMood = currentMood,
            moodDrop = moodDrop,
            stepsInPeriod = stepsInPeriod,
            periodHours = periodHours
        )
        
        try {
            val id = moodNotificationDao.insertMoodNotification(notification)
            Log.i(TAG, "Recorded mood drop: $previousMood -> $currentMood (drop: $moodDrop) with $stepsInPeriod steps in ${periodHours}h")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record mood drop, updating existing record", e)
            // If insert fails, try to update existing record
            val existingNotification = moodNotificationDao.getLastMoodNotification(today)
            existingNotification?.let {
                val updatedNotification = it.copy(
                    timestamp = now,
                    previousMood = previousMood,
                    currentMood = currentMood,
                    moodDrop = moodDrop,
                    stepsInPeriod = stepsInPeriod,
                    periodHours = periodHours
                )
                moodNotificationDao.updateMoodNotification(updatedNotification)
                Log.i(TAG, "Updated existing mood drop record")
            }
        }
    }
    
    // Check if notification should be sent
    suspend fun shouldSendNotification(): Boolean {
        val settings = getMoodNotificationSettings()
        if (!settings.enabled) {
            Log.d(TAG, "Mood notifications disabled")
            return false
        }
        
        val now = LocalDateTime.now()
        val currentHour = now.hour
        
        // Check quiet hours using global UserPreferences
        if (isInQuietHours(currentHour)) {
            Log.d(TAG, "In quiet hours ($currentHour), skipping notification")
            return false
        }
        
        // Check daily notification limit
        val today = now.toLocalDate()
        val notificationCount = moodNotificationDao.getNotificationCountForDate(today)
        if (notificationCount >= settings.maxNotificationsPerDay) {
            Log.d(TAG, "Daily notification limit reached ($notificationCount/${settings.maxNotificationsPerDay})")
            return false
        }
        
        // Check minimum time between notifications
        val recentNotifications = moodNotificationDao.getRecentMoodNotifications(
            now.minusHours(settings.minHoursBetweenNotifications.toLong())
        )
        if (recentNotifications.isNotEmpty()) {
            Log.d(TAG, "Too soon since last notification (${settings.minHoursBetweenNotifications}h minimum)")
            return false
        }
        
        // Check for pending mood drop notification
        val pendingNotification = moodNotificationDao.getPendingMoodNotification(today)
        if (pendingNotification != null && pendingNotification.moodDrop >= settings.moodDropThreshold) {
            Log.d(TAG, "Found pending mood drop notification: ${pendingNotification.moodDrop} levels")
            return true
        }
        
        return false
    }
    
    // Mark notification as sent
    suspend fun markNotificationSent() {
        val today = LocalDate.now()
        val pendingNotification = moodNotificationDao.getPendingMoodNotification(today)
        pendingNotification?.let {
            moodNotificationDao.markNotificationSent(it.date)
            Log.i(TAG, "Marked mood notification as sent: ${it.date}")
        }
    }
    
    // Get notification message for current mood drop
    suspend fun getNotificationMessage(): String {
        val today = LocalDate.now()
        val pendingNotification = moodNotificationDao.getPendingMoodNotification(today)
        
        return if (pendingNotification != null) {
            val message = MoodNotificationMessages.getRandomMessage(
                pendingNotification.currentMood,
                pendingNotification.stepsInPeriod,
                pendingNotification.periodHours
            )
            Log.i(TAG, "Generated notification message: $message")
            message
        } else {
            "Your butts needs some attention! Time to get moving! 🚶‍♂️"
        }
    }
    
    // Calculate mood drop in levels
    private fun calculateMoodDrop(previousMood: Int, currentMood: Int): Int {
        val previousLevel = getMoodLevel(previousMood)
        val currentLevel = getMoodLevel(currentMood)
        return previousLevel - currentLevel
    }
    
    // Get mood level (0-9) based on mood value
    private fun getMoodLevel(mood: Int): Int {
        return when {
            mood in 0..10 -> 0    // COMPLETELY_FLAT
            mood in 11..20 -> 1   // MISERABLE
            mood in 21..30 -> 2   // SAD
            mood in 31..45 -> 3   // ANNOYED
            mood in 46..60 -> 4   // EXHAUSTED
            mood in 61..75 -> 5   // TIRED
            mood in 76..90 -> 6   // NEUTRAL
            mood in 91..110 -> 7  // HAPPY
            mood in 111..130 -> 8 // VERY_HAPPY
            else -> 6             // Default to NEUTRAL
        }
    }
    
    // Check if current time is in quiet hours using global UserPreferences
    private suspend fun isInQuietHours(currentHour: Int): Boolean {
        val quietStart = userPreferences.quietHoursStart.first()
        val quietEnd = userPreferences.quietHoursEnd.first()
        
        return if (quietStart <= quietEnd) {
            // Normal case: start < end (e.g., 22:00 - 07:00)
            currentHour in quietStart..quietEnd
        } else {
            // Wrapping case: start > end (e.g., 23:00 - 06:00)
            currentHour >= quietStart || currentHour <= quietEnd
        }
    }
    
    // Clean up old mood notification data
    suspend fun cleanupOldData() {
        val cutoffDate = LocalDate.now().minusDays(30)
        moodNotificationDao.deleteOldMoodNotifications(cutoffDate)
        Log.i(TAG, "Cleaned up mood notification data older than $cutoffDate")
    }
} 