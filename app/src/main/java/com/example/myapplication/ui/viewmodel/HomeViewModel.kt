package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.StepData
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.data.repository.StepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stepRepository: StepRepository,
    private val userPreferences: UserPreferences,
    private val moodRepository: MoodRepository
) : ViewModel() {

    val todaySteps = stepRepository.getTodaySteps()
        .map { it?.steps ?: 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val dailyGoal = userPreferences.dailyGoal
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10000
        )

    val calories = stepRepository.getTodaySteps()
        .map { it?.calories ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val weeklySteps = stepRepository.getWeeklySteps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            userPreferences.updateDailyGoal(newGoal)
        }
    }

    /**
     * DEBUG METHOD: Manually trigger mood decay for testing
     * This should only be used for debugging the mood decay system
     */
    fun debugTriggerMoodDecay() {
        viewModelScope.launch {
            try {
                moodRepository.debugTriggerMoodDecay()
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    /**
     * DEBUG METHOD: Check worker status and system health
     * This should only be used for debugging the mood decay system
     */
    fun debugCheckWorkerStatus() {
        viewModelScope.launch {
            try {
                // Log current mood state
                val currentMood = moodRepository.getCurrentMood().firstOrNull()
                android.util.Log.i("HomeViewModel", "=== DEBUG: CHECKING WORKER STATUS ===")
                android.util.Log.i("HomeViewModel", "Current mood state: $currentMood")
                
                // Log hourly steps
                val hourlySteps = moodRepository.getTodayHourlySteps().firstOrNull()
                android.util.Log.i("HomeViewModel", "Today's hourly steps: $hourlySteps")
                
                // Log user preferences
                val userGoal = dailyGoal.value
                val quietStart = userPreferences.quietHoursStart.first()
                val quietEnd = userPreferences.quietHoursEnd.first()
                android.util.Log.i("HomeViewModel", "User goal: $userGoal, Quiet hours: $quietStart-$quietEnd")
                
                android.util.Log.i("HomeViewModel", "=== DEBUG: WORKER STATUS CHECK COMPLETED ===")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error checking worker status", e)
            }
        }
    }
}