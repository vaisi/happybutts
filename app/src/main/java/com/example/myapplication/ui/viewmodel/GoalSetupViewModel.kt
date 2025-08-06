// Updated: GoalSetupViewModel - added logging for goal changes and dynamic value calculations
package com.example.myapplication.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalSetupViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val TAG = "GoalSetupViewModel"
    private val _currentGoal = MutableStateFlow(DEFAULT_GOAL) // Default goal is now 7000
    val currentGoal: StateFlow<Int> = _currentGoal.asStateFlow()

    init {
        loadCurrentGoal()
    }

    private fun loadCurrentGoal() {
        viewModelScope.launch {
            userPreferences.dailyGoal.collect { goal ->
                // Clamp loaded goal to new min/max
                _currentGoal.value = goal.coerceIn(MIN_GOAL, MAX_GOAL)
                Log.i(TAG, "Loaded current goal: ${_currentGoal.value} steps")
                logDynamicValues(_currentGoal.value)
            }
        }
    }

    fun updateGoal(newGoal: Int) {
        val validatedGoal = newGoal.coerceIn(MIN_GOAL, MAX_GOAL)
        val previousGoal = _currentGoal.value
        _currentGoal.value = validatedGoal
        
        if (validatedGoal != previousGoal) {
            Log.i(TAG, "Goal updated: $previousGoal -> $validatedGoal steps")
            logDynamicValues(validatedGoal)
        }
    }

    fun saveGoal() {
        viewModelScope.launch {
            val goalToSave = _currentGoal.value.coerceIn(MIN_GOAL, MAX_GOAL)
            userPreferences.updateDailyGoal(goalToSave)
            Log.i(TAG, "Goal saved to preferences: $goalToSave steps")
            logDynamicValues(goalToSave)
        }
    }

    // Log the dynamic values that will be used with this goal
    private fun logDynamicValues(userGoal: Int) {
        val BASE_GOAL_STEPS = 10000
        val BASE_STEPS_PER_MOOD = 150
        val BASE_DECAY_THRESHOLD = 250
        val BASE_MOOD_DECAY = 5
        
        val stepsPerMood = (BASE_STEPS_PER_MOOD * userGoal / BASE_GOAL_STEPS).coerceAtLeast(50)
        val decayThreshold = (BASE_DECAY_THRESHOLD * userGoal / BASE_GOAL_STEPS).coerceAtLeast(50)
        val moodDecayPerHour = (BASE_MOOD_DECAY * userGoal / BASE_GOAL_STEPS).coerceAtLeast(1)
        
        Log.i(TAG, "=== DYNAMIC VALUES FOR GOAL: $userGoal STEPS ===")
        Log.i(TAG, "Steps per +1 mood: $stepsPerMood (base: $BASE_STEPS_PER_MOOD)")
        Log.i(TAG, "Decay threshold: $decayThreshold steps/hour (base: $BASE_DECAY_THRESHOLD)")
        Log.i(TAG, "Mood decay per hour: $moodDecayPerHour points (base: $BASE_MOOD_DECAY)")
        Log.i(TAG, "================================================")
    }

    companion object {
        const val MIN_GOAL = 3000
        const val MAX_GOAL = 20000
        const val DEFAULT_GOAL = 7000
    }
} 