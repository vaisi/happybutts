// Updated: InactivityRepository - now uses global UserPreferences for quiet hours instead of local settings
package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.myapplication.data.database.InactivityDao
import com.example.myapplication.data.model.InactivityData
import com.example.myapplication.data.model.InactivitySettings
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
class InactivityRepository @Inject constructor(
    private val inactivityDao: InactivityDao,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) {
    private val TAG = "InactivityRepository"
    private val prefs: SharedPreferences = context.getSharedPreferences("inactivity_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_THRESHOLD = "threshold"
        private const val KEY_MAX_NOTIFICATIONS = "max_notifications"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATION = "vibration"
    }
    
    // Get inactivity settings
    fun getInactivitySettings(): InactivitySettings {
        return InactivitySettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            consecutiveHoursThreshold = prefs.getInt(KEY_THRESHOLD, 4),
            maxNotificationsPerDay = prefs.getInt(KEY_MAX_NOTIFICATIONS, 3),
            quietHoursStart = 22, // Default, will be overridden by global settings
            quietHoursEnd = 7,    // Default, will be overridden by global settings
            notificationSound = prefs.getBoolean(KEY_SOUND, true),
            notificationVibration = prefs.getBoolean(KEY_VIBRATION, true)
        )
    }
    
    // Update inactivity settings
    suspend fun updateInactivitySettings(settings: InactivitySettings) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.enabled)
            putInt(KEY_THRESHOLD, settings.consecutiveHoursThreshold)
            putInt(KEY_MAX_NOTIFICATIONS, settings.maxNotificationsPerDay)
            putBoolean(KEY_SOUND, settings.notificationSound)
            putBoolean(KEY_VIBRATION, settings.notificationVibration)
            apply()
        }
        Log.i(TAG, "Updated inactivity settings: $settings")
    }
    
    // Get inactivity data for a date
    fun getInactivityForDate(date: LocalDate): Flow<List<InactivityData>> {
        return inactivityDao.getInactivityForDate(date)
    }
    
    // Start tracking inactivity period
    suspend fun startInactivityPeriod(steps: Int) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        
        // Check if there's already an active inactivity period
        val activePeriod = inactivityDao.getActiveInactivityPeriod(today)
        if (activePeriod != null) {
            Log.d(TAG, "Inactivity period already active, continuing...")
            return
        }
        
        // Create new inactivity period
        val inactivity = InactivityData(
            date = today.toString(),
            startTime = now,
            durationHours = 0f,
            stepsDuringPeriod = steps,
            isConsecutive = true
        )
        
        inactivityDao.insertInactivity(inactivity)
        Log.i(TAG, "Started inactivity period at $now with $steps steps")
    }
    
    // End current inactivity period
    suspend fun endInactivityPeriod(steps: Int) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        
        val activePeriod = inactivityDao.getActiveInactivityPeriod(today)
        if (activePeriod != null) {
            val durationHours = ChronoUnit.MINUTES.between(activePeriod.startTime, now) / 60f
            inactivityDao.endInactivityPeriod(activePeriod.date, activePeriod.startTime, now, durationHours)
            Log.i(TAG, "Ended inactivity period, duration: ${durationHours}h, steps: $steps")
        }
    }
    
    // Check if notification should be sent
    suspend fun shouldSendNotification(): Boolean {
        val settings = getInactivitySettings()
        if (!settings.enabled) {
            Log.d(TAG, "Inactivity notifications disabled")
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
        val notificationCount = inactivityDao.getNotificationCountForDate(today)
        if (notificationCount >= settings.maxNotificationsPerDay) {
            Log.d(TAG, "Daily notification limit reached ($notificationCount/${settings.maxNotificationsPerDay})")
            return false
        }
        
        // Check current inactivity duration
        val currentInactivity = inactivityDao.getCurrentConsecutiveInactivity(today)
        if (currentInactivity != null && currentInactivity.endTime == null) {
            val durationHours = ChronoUnit.MINUTES.between(currentInactivity.startTime, now) / 60f
            val shouldNotify = durationHours >= settings.consecutiveHoursThreshold
            
            Log.d(TAG, "Current inactivity: ${durationHours}h (threshold: ${settings.consecutiveHoursThreshold}h), should notify: $shouldNotify")
            return shouldNotify
        }
        
        return false
    }
    
    // Mark notification as sent
    suspend fun markNotificationSent() {
        val today = LocalDate.now()
        val currentInactivity = inactivityDao.getCurrentConsecutiveInactivity(today)
        currentInactivity?.let {
            inactivityDao.markNotificationSent(it.date, it.startTime)
            Log.i(TAG, "Marked notification as sent for inactivity period")
        }
    }
    
    // Get notification message based on inactivity duration
    suspend fun getNotificationMessage(): String {
        val today = LocalDate.now()
        val currentInactivity = inactivityDao.getCurrentConsecutiveInactivity(today)
        
        if (currentInactivity != null) {
            val now = LocalDateTime.now()
            val durationHours = ChronoUnit.MINUTES.between(currentInactivity.startTime, now) / 60f
            val steps = currentInactivity.stepsDuringPeriod
            
            return when {
                durationHours >= 8 -> "You've been inactive for over 8 hours! Time for a walk? 🚶‍♂️"
                durationHours >= 6 -> "6+ hours of inactivity detected. Your character needs some steps! 😊"
                durationHours >= 4 -> "4 hours without activity. A short walk could boost your mood! 🌟"
                else -> "Time to get moving! Your step count is waiting for you! 🎯"
            }
        }
        
        return "Time to get active! Your character is waiting for some steps! 🚶‍♂️"
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
    
    // Clean up old inactivity data
    suspend fun cleanupOldData() {
        val cutoffDate = LocalDate.now().minusDays(30)
        inactivityDao.deleteOldInactivityData(cutoffDate)
        Log.i(TAG, "Cleaned up inactivity data older than $cutoffDate")
    }
} 