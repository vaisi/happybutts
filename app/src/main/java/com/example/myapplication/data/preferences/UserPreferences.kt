// Updated: Added quiet hours support and changed default daily goal to 7000
package com.example.myapplication.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

@Singleton
class UserPreferences @Inject constructor(
    val dataStore: DataStore<Preferences>
) {
    companion object {
        val DAILY_GOAL_KEY = intPreferencesKey("daily_goal")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LAST_KNOWN_STEPS_KEY = intPreferencesKey("last_known_steps")
        val LAST_SEEN_DATE_KEY = stringPreferencesKey("last_seen_date")
        val QUIET_HOURS_START_KEY = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END_KEY = intPreferencesKey("quiet_hours_end")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val APP_VERSION_KEY = stringPreferencesKey("app_version")
        // IMPROVED SYSTEM: Worker update tracking
        val LAST_WORKER_UPDATE_KEY = longPreferencesKey("last_worker_update")
        val LAST_WORKER_HOUR_KEY = intPreferencesKey("last_worker_hour")
        val LAST_WORKER_STEPS_KEY = intPreferencesKey("last_worker_steps")
    }

    val dailyGoal: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DAILY_GOAL_KEY] ?: 7000
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "system"
    }

    val lastKnownSteps: Flow<Int> = dataStore.data.map { preferences ->
        preferences[LAST_KNOWN_STEPS_KEY] ?: 0
    }

    val lastSeenDate: Flow<String> = dataStore.data.map { preferences ->
        preferences[LAST_SEEN_DATE_KEY] ?: LocalDate.now().toString()
    }

    val quietHoursStart: Flow<Int> = dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_START_KEY] ?: 22
    }

    val quietHoursEnd: Flow<Int> = dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_END_KEY] ?: 7
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    val appVersion: Flow<String> = dataStore.data.map { preferences ->
        preferences[APP_VERSION_KEY] ?: ""
    }

    // IMPROVED SYSTEM: Worker update tracking
    val lastWorkerUpdate: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_WORKER_UPDATE_KEY] ?: System.currentTimeMillis()
    }

    val lastWorkerHour: Flow<Int> = dataStore.data.map { preferences ->
        preferences[LAST_WORKER_HOUR_KEY] ?: -1
    }

    val lastWorkerSteps: Flow<Int> = dataStore.data.map { preferences ->
        preferences[LAST_WORKER_STEPS_KEY] ?: 0
    }

    suspend fun updateDailyGoal(goal: Int) {
        dataStore.edit { preferences ->
            preferences[DAILY_GOAL_KEY] = goal
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    suspend fun updateLastKnownSteps(steps: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_KNOWN_STEPS_KEY] = steps
        }
    }

    suspend fun updateLastSeenDate(date: String) {
        dataStore.edit { preferences ->
            preferences[LAST_SEEN_DATE_KEY] = date
        }
    }

    suspend fun updateQuietHoursStart(hour: Int) {
        dataStore.edit { preferences ->
            preferences[QUIET_HOURS_START_KEY] = hour
        }
    }

    suspend fun updateQuietHoursEnd(hour: Int) {
        dataStore.edit { preferences ->
            preferences[QUIET_HOURS_END_KEY] = hour
        }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        android.util.Log.i("UserPreferences", "updateOnboardingCompleted called with value: $completed")
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun updateAppVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[APP_VERSION_KEY] = version
        }
    }

    // IMPROVED SYSTEM: Worker update tracking setters
    suspend fun updateLastWorkerUpdate(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_WORKER_UPDATE_KEY] = timestamp
        }
    }

    suspend fun updateLastWorkerHour(hour: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_WORKER_HOUR_KEY] = hour
        }
    }

    suspend fun updateLastWorkerSteps(steps: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_WORKER_STEPS_KEY] = steps
        }
    }

    suspend fun checkAndResetForFreshInstall(currentVersion: String) {
        dataStore.edit { preferences ->
            val savedVersion = preferences[APP_VERSION_KEY]
            if (savedVersion != currentVersion) {
                android.util.Log.i("UserPreferences", "Fresh install detected or version changed. Clearing onboarding status.")
                // Clear onboarding status for fresh install
                preferences[ONBOARDING_COMPLETED_KEY] = false
                preferences[APP_VERSION_KEY] = currentVersion
            }
        }
    }
}