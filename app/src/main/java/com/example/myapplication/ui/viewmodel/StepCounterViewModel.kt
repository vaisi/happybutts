// Updated: StepCounterViewModel - Simplified mood calculation system
// Fixed: Uses consolidated mood calculation methods from MoodRepository
// Updated: Removed redundant calculation logic while preserving live mood updates
// Updated: Simplified hourly data handling and mood display
package com.example.myapplication.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.data.repository.StepRepository
import com.example.myapplication.service.UnifiedStepCounterService
import com.example.myapplication.service.SimpleHourlyAggregator
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.InactivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.supervisorScope

@HiltViewModel
class StepCounterViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
    private val stepCountRepository: StepCountRepository,
    private val stepRepository: StepRepository,
    private val stepCounterService: UnifiedStepCounterService,
    private val simpleHourlyAggregator: SimpleHourlyAggregator,
    private val userPreferences: UserPreferences,
    private val inactivityRepository: InactivityRepository
) : ViewModel() {

    private val TAG = "StepCounterViewModel"

    // UI State
    private val _uiState = MutableStateFlow(StepCounterUiState())
    val uiState: StateFlow<StepCounterUiState> = _uiState.asStateFlow()

    // Step counting state
    private val _isStepCountingActive = MutableStateFlow(false)
    val isStepCountingActive: StateFlow<Boolean> = _isStepCountingActive.asStateFlow()

    // Permission state
    private val _needsPermission = MutableStateFlow(false)
    val needsPermission: StateFlow<Boolean> = _needsPermission.asStateFlow()

    // Mood history for calendar
    val moodHistory: StateFlow<List<com.example.myapplication.data.model.MoodStateEntity>> = moodRepository.getMoodHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Real-time mood for UI
    val realTimeMood: StateFlow<Int> = uiState.map { it.currentMood }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        50
    )

    // Mood progress for progress bar (0.0 to 1.0)
    val moodProgress: StateFlow<Float> = realTimeMood.map { mood ->
        (mood / 130f).coerceIn(0f, 1f) // 130 is max mood
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0.38f // 50/130
    )

    // Current date tracking for daily reset
    private var lastKnownDate = LocalDate.now()

    init {
        Log.i(TAG, "StepCounterViewModel initialized")
        
        // Start periodic date change check
        startPeriodicDateChangeCheck()
        
        // Initialize step counting if permissions are already granted
        viewModelScope.launch {
            try {
                // Check if onboarding is complete
                val onboardingCompleted = userPreferences.onboardingCompleted.first()
                
                if (onboardingCompleted) {
                    // Always try to start step counting since we can use hardware detector
                    Log.i(TAG, "Onboarding completed, starting step counting")
                    startStepCounting()
                } else {
                    Log.i(TAG, "Onboarding not completed yet")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking onboarding status", e)
            }
        }
    }

    // Start periodic date change check
    private fun startPeriodicDateChangeCheck() {
        viewModelScope.launch {
            while (true) {
                try {
                    checkForDateChange()
                    delay(60000) // Check every minute
                } catch (e: CancellationException) {
                    Log.d(TAG, "Date change check cancelled")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in date change check", e)
                    delay(60000) // Wait before retrying
                }
            }
        }
    }

    // Check for date changes and handle daily reset
    private suspend fun checkForDateChange() {
        val currentDate = LocalDate.now()
        
        if (currentDate != lastKnownDate) {
            Log.i(TAG, "=== DATE CHANGE DETECTED IN VIEWMODEL ===")
            Log.i(TAG, "Date changed from $lastKnownDate to $currentDate")
            
            // Get current mood state
            val currentMood = moodRepository.getCurrentMood().firstOrNull()
            
            if (currentMood != null && currentMood.date.isBefore(currentDate)) {
                Log.i(TAG, "Performing date change reset")
                
                // Finalize the previous day's mood
                val yesterday = currentMood.date
                Log.d(TAG, "Finalizing mood for date: $yesterday")
                moodRepository.finalizeDayMood(yesterday)
                
                // Reset for the new day
                Log.d(TAG, "Resetting daily mood and steps for new day")
                moodRepository.resetDaily()
                
                // Reset step baseline for new day
                stepCounterService.resetDailyBaseline()
                
                // Reset step count in repository
                stepCountRepository.updateTodaySteps(0)
                
                Log.i(TAG, "=== VIEWMODEL DATE CHANGE RESET COMPLETED ===")
            }
            
            lastKnownDate = currentDate
        }
    }

    // SIMPLIFIED: Start step counting
    private suspend fun startStepCounting() {
        try {
            Log.i(TAG, "Starting step counting")
            
            // Check permissions - we'll use a default context for now
            // In a real app, you'd get the context from the ViewModel's context
            val hasActivityRecognitionPermission = true // Assume permission is granted for now
            
            if (!hasActivityRecognitionPermission) {
                Log.w(TAG, "Cannot start step counting: Missing ACTIVITY_RECOGNITION permission")
                _needsPermission.value = true
                return
            }
            
            // Start the step counting service
            stepCounterService.startStepCounting { steps ->
                // Default step detection callback
                Log.d(TAG, "Step detected: $steps")
            }
            _isStepCountingActive.value = true
            
            // Start monitoring step updates
            startStepMonitoring()
            
            Log.i(TAG, "Step counting started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start step counting", e)
        }
    }

    // SIMPLIFIED: Monitor step updates and calculate live mood
    private fun startStepMonitoring() {
        viewModelScope.launch {
            try {
                // Monitor step count updates more frequently
                stepCountRepository.getTodayStepCount()
                    .collect { stepCount ->
                        Log.d(TAG, "Step monitoring: Received step update: ${stepCount.steps}")
                        handleStepUpdate(stepCount.steps)
                        
                        // Also trigger live mood calculation
                        calculateLiveMood()
                    }
            } catch (e: CancellationException) {
                Log.d(TAG, "Step monitoring cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error in step monitoring", e)
            }
        }
    }

    // SIMPLIFIED: Handle step updates and calculate live mood
    private suspend fun handleStepUpdate(currentSteps: Int) {
        try {
            Log.d(TAG, "handleStepUpdate: Current steps: $currentSteps")
            
            // Update steps in repository for real-time tracking
            moodRepository.updateSteps(currentSteps)
            
            // Calculate mood based on TOTAL progress toward goal, not incremental steps
            val currentGoal = try {
                userPreferences.dailyGoal.first()
            } catch (e: Exception) {
                Log.w(TAG, "Error getting current goal, using default", e)
                10000 // Default goal
            }
            
            // Calculate progress percentage
            val progressPercentage = if (currentGoal > 0) {
                (currentSteps.toFloat() / currentGoal.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            
            // Calculate expected mood based on progress
            // Mood should be 0-130 based on progress toward goal
            val expectedMood = (progressPercentage * 130).toInt().coerceIn(0, 130)
            
            // Get current mood from repository
            val currentMood = moodRepository.getCurrentMood().firstOrNull()?.mood ?: 50
            
            // Only update if there's a significant difference
            if (kotlin.math.abs(expectedMood - currentMood) >= 1) {
                _uiState.value = _uiState.value.copy(
                    currentMood = expectedMood,
                    lastPersistedSteps = currentSteps
                )
                
                Log.d(TAG, "Live mood update: $currentMood -> $expectedMood (${(progressPercentage * 100).toInt()}% of goal)")
            }
            
            // Update step count in UI state
            _uiState.value = _uiState.value.copy(
                currentSteps = currentSteps,
                lastPersistedSteps = currentSteps
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling step update", e)
        }
    }

    // SIMPLIFIED: Calculate live mood for display
    private suspend fun calculateLiveMood() {
        try {
            val currentMood = moodRepository.getCurrentMood().firstOrNull()
            if (currentMood != null) {
                _uiState.value = _uiState.value.copy(
                    currentMood = currentMood.mood,
                    lastPersistedSteps = currentMood.lastPersistedSteps
                )
                Log.d(TAG, "Live mood calculated: ${currentMood.mood}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating live mood", e)
        }
    }

    // SIMPLIFIED: Check for missed mood decay
    private suspend fun checkForMissedMoodDecay() {
        try {
            Log.i(TAG, "Checking for missed mood decay")
            
            val now = LocalDateTime.now()
            val currentHour = now.hour
            val currentMood = moodRepository.getCurrentMood().firstOrNull()
            
            if (currentMood != null) {
                Log.d(TAG, "checkForMissedMoodDecay: Current hour: $currentHour, Current mood: ${currentMood.mood}")
                
                // Get hourly steps to check for missed worker executions
                val today = LocalDate.now()
                val hourlySteps = moodRepository.getTodayHourlySteps().firstOrNull() ?: emptyList()
                
                Log.d(TAG, "checkForMissedMoodDecay: Current hour: $currentHour, Recorded hours: ${hourlySteps.map { it.hour }}")
                
                // Check each hour from 0 to currentHour-1 to see if it was processed
                var missedHours = 0
                for (hour in 0 until currentHour) {
                    val hourRecorded = hourlySteps.any { it.hour == hour }
                    if (!hourRecorded) {
                        Log.w(TAG, "checkForMissedMoodDecay: Hour $hour was not recorded! This indicates worker failure.")
                        missedHours++
                    }
                }
                
                if (missedHours > 0) {
                    Log.i(TAG, "checkForMissedMoodDecay: Found $missedHours missed hours, applying fallback")
                    
                    // Get today's total steps to calculate incremental steps for missed hours
                    val todaySteps = stepCountRepository.getTodayStepCount().firstOrNull()?.steps ?: 0
                    var newMood = currentMood.mood
                    
                    // Calculate steps for each missed hour
                    for (hour in 0 until currentHour) {
                        val hourRecorded = hourlySteps.any { it.hour == hour }
                        if (!hourRecorded) {
                            // Calculate steps for this missed hour
                            val totalStepsUpToHour = hourlySteps
                                .filter { it.hour < hour }
                                .sumOf { it.steps }
                            
                            val stepsInHour = if (todaySteps > totalStepsUpToHour) {
                                todaySteps - totalStepsUpToHour
                } else {
                    0
                }
                
                            // Record the missed hour's steps
                            moodRepository.recordHourlySteps(hour, stepsInHour)
                            
                            // Apply mood calculation for this hour
                            val moodGain = moodRepository.calculateMoodGain(stepsInHour)
                            val decay = moodRepository.calculateMoodDecay(hour, stepsInHour, newMood)
                            newMood = (newMood + moodGain - decay).coerceIn(0, 130)
                            
                            Log.d(TAG, "checkForMissedMoodDecay: Processed missed hour $hour: steps=$stepsInHour, gain=$moodGain, decay=$decay, mood=$newMood")
                        }
                    }
                    
                    // Update the mood in the database
                    val updatedMood = currentMood.copy(mood = newMood, lastPersistedSteps = todaySteps)
                    moodRepository.updateMoodState(updatedMood)
                    
                    Log.i(TAG, "checkForMissedMoodDecay: Updated mood from ${currentMood.mood} to $newMood")
                } else {
                    Log.d(TAG, "checkForMissedMoodDecay: No missed hours detected")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for missed mood decay", e)
        }
    }

    // SIMPLIFIED: Refresh mood from database
    private suspend fun refreshMoodFromDatabase() {
        try {
            calculateLiveMood()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing mood", e)
        }
    }

    // SIMPLIFIED: Stop step counting
    private suspend fun stopStepCountingInternal() {
        try {
            Log.i(TAG, "Stopping step counting")
            stepCounterService.stopStepCounting()
            _isStepCountingActive.value = false
            Log.i(TAG, "Step counting stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping step counting", e)
        }
    }

    // Public method to handle permission granted
    fun onPermissionGranted() {
        viewModelScope.launch {
            try {
                _needsPermission.value = false
                startStepCounting()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling permission granted", e)
            }
        }
    }

    // Public method to refresh mood
    fun refreshMood() {
        viewModelScope.launch {
            refreshMoodFromDatabase()
        }
    }

    // Public method to stop step counting
    fun stopStepCounting() {
        viewModelScope.launch {
            stopStepCountingInternal()
        }
    }

    // Fix mood calculation based on current progress
    fun fixMoodCalculation() {
        viewModelScope.launch {
            try {
                moodRepository.calculateAndSetCorrectMood()
                // Refresh the UI state
                calculateLiveMood()
                Log.i(TAG, "Mood calculation fixed")
            } catch (e: Exception) {
                Log.e(TAG, "Error fixing mood calculation", e)
            }
        }
    }

    // Get current goal from user preferences
    fun getCurrentGoal(): Flow<Int> {
        return flow {
            try {
                val goal = userPreferences.dailyGoal.first()
                Log.d(TAG, "getCurrentGoal: Retrieved goal from preferences: $goal")
                emit(goal)
            } catch (e: Exception) {
                Log.w(TAG, "Error getting current goal, using default", e)
                emit(10000) // Default goal
            }
        }
    }

    // Get steps per mood based on current goal
    fun getStepsPerMood(): Flow<Int> {
        return flow {
            try {
                val goal = userPreferences.dailyGoal.first()
                val stepsPerMood = (150 * goal / 10000).coerceAtLeast(50)
                emit(stepsPerMood)
            } catch (e: Exception) {
                Log.w(TAG, "Error calculating steps per mood, using default", e)
                emit(100) // Default value
            }
        }
    }

    // Get decay threshold based on current goal
    fun getDecayThreshold(): Flow<Int> {
        return flow {
            try {
                val goal = userPreferences.dailyGoal.first()
                val threshold = (250 * goal / 10000).coerceAtLeast(50)
                emit(threshold)
            } catch (e: Exception) {
                Log.w(TAG, "Error calculating decay threshold, using default", e)
                emit(500) // Default value
            }
        }
    }

    // Get daily stats for a specific date
    fun getDailyStats(date: LocalDate): Flow<DailyStats?> {
        return flow {
            try {
                val stepCount = stepCountRepository.getStepCountForDate(date).firstOrNull()
                
                if (stepCount != null) {
                    // Use the actual goal that was stored for this specific date
                    emit(DailyStats(
                        date = date,
                        steps = stepCount.steps,
                        goal = stepCount.goal
                    ))
                    } else {
                    // For dates with no data, return null instead of creating fake data
                    emit(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting daily stats for $date", e)
                emit(null)
            }
        }
    }

    // Get inactivity data for a specific date
    fun getInactivityForDate(date: LocalDate): Flow<List<com.example.myapplication.data.model.InactivityData>> {
        return inactivityRepository.getInactivityForDate(date)
    }

    // Get hourly step data for a specific date
    fun getHourlyStepsForDate(date: LocalDate): Flow<List<com.example.myapplication.data.model.HourlySteps>> {
        return stepRepository.getHourlyStepsForDate(date)
    }

    // Data class for daily stats
    data class DailyStats(
        val date: LocalDate,
        val steps: Int,
        val goal: Int
    )

    // Data class for UI state
data class StepCounterUiState(
        val currentMood: Int = 50,
    val currentSteps: Int = 0,
        val lastPersistedSteps: Int = 0
) 
} 