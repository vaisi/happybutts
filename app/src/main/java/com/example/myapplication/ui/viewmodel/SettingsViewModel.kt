package com.example.myapplication.ui.viewmodel

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.MoodNotificationSettings
import com.example.myapplication.data.repository.MoodNotificationRepository
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.service.MoodNotificationService
import com.example.myapplication.service.InactivityNotificationService
import com.example.myapplication.data.repository.MoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.edit

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val moodNotificationRepository: MoodNotificationRepository,
    private val moodNotificationService: MoodNotificationService,
    private val inactivityNotificationService: InactivityNotificationService,
    private val moodRepository: MoodRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "SettingsViewModel"

    private val _dailyGoal = MutableStateFlow(10000)
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _moodNotificationSettings = MutableStateFlow(MoodNotificationSettings())
    val moodNotificationSettings: StateFlow<MoodNotificationSettings> = _moodNotificationSettings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.dailyGoal.collect { goal ->
                _dailyGoal.value = goal
            }
            userPreferences.notificationsEnabled.collect { enabled ->
                _notificationsEnabled.value = enabled
            }
            userPreferences.themeMode.collect { mode ->
                _themeMode.value = mode
            }
            _moodNotificationSettings.value = moodNotificationRepository.getMoodNotificationSettings()
        }
    }

    fun updateDailyGoal(goal: Int) {
        viewModelScope.launch {
            userPreferences.updateDailyGoal(goal)
            _dailyGoal.value = goal
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateNotificationsEnabled(enabled)
            _notificationsEnabled.value = enabled
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.updateThemeMode(mode)
            _themeMode.value = mode
        }
    }

    fun updateMoodNotificationSettings(settings: MoodNotificationSettings) {
        viewModelScope.launch {
            moodNotificationRepository.updateMoodNotificationSettings(settings)
            _moodNotificationSettings.value = settings
        }
    }

    // Check notification permissions and log status
    private fun checkNotificationPermissions() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Check if notifications are enabled
            val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            Log.i(TAG, "Notifications enabled: $areNotificationsEnabled")
            
            // Check notification channels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val moodChannel = notificationManager.getNotificationChannel("mood_notifications")
                val inactivityChannel = notificationManager.getNotificationChannel("inactivity_notifications")
                
                Log.i(TAG, "Mood notification channel exists: ${moodChannel != null}")
                Log.i(TAG, "Inactivity notification channel exists: ${inactivityChannel != null}")
                
                moodChannel?.let {
                    Log.i(TAG, "Mood channel importance: ${it.importance}")
                    Log.i(TAG, "Mood channel enabled: ${it.importance != NotificationManager.IMPORTANCE_NONE}")
                }
                
                inactivityChannel?.let {
                    Log.i(TAG, "Inactivity channel importance: ${it.importance}")
                    Log.i(TAG, "Inactivity channel enabled: ${it.importance != NotificationManager.IMPORTANCE_NONE}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification permissions", e)
        }
    }

    // Test notification methods - now using bypass methods with better logging
    fun testMoodNotification() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Testing mood notification...")
                checkNotificationPermissions()
                moodNotificationService.sendTestMoodNotification()
                Log.i(TAG, "Mood notification test completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error testing mood notification", e)
            }
        }
    }

    fun testInactivityNotification() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Testing inactivity notification...")
                checkNotificationPermissions()
                inactivityNotificationService.sendTestInactivityNotification()
                Log.i(TAG, "Inactivity notification test completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error testing inactivity notification", e)
            }
        }
    }

    // Test mood decay method
    fun testMoodDecay() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Testing mood decay...")
                
                // Get current mood before decay
                val currentMood = moodRepository.getCurrentMood().first()
                val previousMood = currentMood.mood
                Log.i(TAG, "Current mood before decay: $previousMood")
                
                // Apply hourly mood decay for testing
                moodRepository.applyHourlyMoodUpdate(0, 0)
                
                // Get mood after decay
                val newMood = moodRepository.getCurrentMood().first()
                val updatedMood = newMood.mood
                Log.i(TAG, "Mood after decay: $updatedMood")
                
                val moodChange = previousMood - updatedMood
                Log.i(TAG, "Mood decay test completed - Change: $moodChange points")
                
                // Show toast with result
                val message = if (moodChange > 0) {
                    "Mood decay applied: $previousMood → $updatedMood (-$moodChange points). Go back to main screen to see the change!"
                } else {
                    "No mood decay applied (sleep hours or other conditions). Current mood: $updatedMood"
                }
                
                // Note: We can't show toast from ViewModel directly, but we can log it
                Log.i(TAG, "TOAST MESSAGE: $message")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error testing mood decay", e)
            }
        }
    }

    fun cancelAllNotifications() {
        moodNotificationService.cancelMoodNotification()
        inactivityNotificationService.cancelInactivityNotification()
        Log.i(TAG, "Cancelled all notifications")
    }

    fun clearDataStoreForTesting() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Clearing DataStore for testing...")
                // Clear all DataStore preferences
                userPreferences.dataStore.edit { preferences ->
                    preferences.clear()
                }
                Log.i(TAG, "DataStore cleared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing DataStore", e)
            }
        }
    }
}