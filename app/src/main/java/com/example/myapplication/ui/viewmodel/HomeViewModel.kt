// Updated: HomeViewModel - Simplified mood calculation without artificial limits
// Updated: Removed debug methods for cleaner production code
// Updated: Added SimpleHourlyAggregator integration and data validation
package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.data.repository.StepRepository
import com.example.myapplication.service.UnifiedStepCounterService
import com.example.myapplication.service.SimpleHourlyAggregator
import com.example.myapplication.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.CancellationException

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
    private val stepCountRepository: StepCountRepository,
    private val stepRepository: StepRepository,
    private val stepCounterService: UnifiedStepCounterService,
    private val simpleHourlyAggregator: SimpleHourlyAggregator,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val TAG = "HomeViewModel"

    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Step counting state
    private val _isStepCountingActive = MutableStateFlow(false)
    val isStepCountingActive: StateFlow<Boolean> = _isStepCountingActive.asStateFlow()

    // Permission state
    private val _needsPermission = MutableStateFlow(false)
    val needsPermission: StateFlow<Boolean> = _needsPermission.asStateFlow()

    // Current date tracking for daily reset
    private var lastKnownDate = LocalDate.now()

    init {
        Log.i(TAG, "HomeViewModel initialized")
        
        // Start periodic date change check
        startPeriodicDateChangeCheck()
        
        // Initialize step counting if permissions are already granted
        viewModelScope.launch {
            try {
                // Check if onboarding is complete
                val onboardingCompleted = userPreferences.onboardingCompleted.first()
                
                if (onboardingCompleted) {
                    // Start step counting if permissions are already granted
                    val hasActivityRecognition = true // We'll check this in checkPermissions()
                    val hasHealthConnect = stepCounterService.hasRequiredPermissions()
                    
                    if (hasActivityRecognition && hasHealthConnect) {
                        Log.d(TAG, "Permissions already granted, starting step counting")
                        startStepCounting()
                        
                        // ENHANCED: Start simple hourly aggregation
                        startHourlyAggregation()
                        
                        // ENHANCED: Validate data on startup
                        validateDataOnStartup()
                    } else {
                        Log.d(TAG, "Permissions not granted, waiting for user action")
                        _needsPermission.value = true
                    }
                } else {
                    Log.d(TAG, "Onboarding not complete, waiting for user to complete setup")
                }
            } catch (e: Exception) {
                Log.e(TAG, "init: Error during initialization", e)
                // Set default values on error
                _uiState.value = HomeUiState(
                    currentSteps = 0,
                    currentMood = 50,
                    dailyGoal = 10000,
                    moodState = com.example.myapplication.data.model.MoodState.MEH
                )
            }
        }
    }

    /**
     * ENHANCED: Start simple hourly aggregation
     */
    private fun startHourlyAggregation() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Starting simple hourly aggregation")
                simpleHourlyAggregator.startAggregation()
                Log.i(TAG, "Simple hourly aggregation started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting hourly aggregation", e)
            }
        }
    }

    /**
     * ENHANCED: Validate data on startup
     */
    private suspend fun validateDataOnStartup() {
        try {
            Log.i(TAG, "Validating data on startup")
            stepCountRepository.validateAndRecoverData()
            
            // Get data consistency report
            val report = stepCountRepository.getDataConsistencyReport()
            Log.i(TAG, "Data consistency report: $report")
            
            if (!report.isValid) {
                Log.w(TAG, "Data validation found issues, attempting recovery")
                // Force a data recovery
                simpleHourlyAggregator.recoverMissingHourlyData()
            }
            
            // CRITICAL FIX: Check for corrupted hourly data and clear it
            checkAndClearCorruptedHourlyData()
            
            // CRITICAL FIX: Check and reset mood if it's clearly wrong
            checkAndResetMoodIfWrong()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error validating data on startup", e)
        }
    }

    /**
     * CRITICAL FIX: Check for corrupted hourly data and clear it
     */
    private suspend fun checkAndClearCorruptedHourlyData() {
        try {
            val today = LocalDate.now()
            val hourlySteps = stepRepository.getHourlyStepsForDate(today).firstOrNull() ?: emptyList()
            
            // Check if we have corrupted data (unrealistic step counts)
            val hasCorruptedData = hourlySteps.any { it.steps > 10000 } // More than 10k steps in an hour is unrealistic
            
            if (hasCorruptedData) {
                Log.w(TAG, "CRITICAL: Detected corrupted hourly data with unrealistic step counts")
                Log.w(TAG, "Corrupted data: ${hourlySteps.filter { it.steps > 10000 }}")
                
                // Clear the corrupted data
                clearAndRebuildHourlyData()
                
                Log.i(TAG, "CRITICAL: Cleared corrupted hourly data and started rebuilding")
            } else {
                Log.d(TAG, "Hourly data validation passed - no corruption detected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for corrupted hourly data", e)
        }
    }

    /**
     * Clear corrupted hourly data and rebuild it
     */
    fun clearAndRebuildHourlyData() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Clearing corrupted hourly data and rebuilding")
                
                // Clear today's hourly data
                stepRepository.clearTodayHourlyData()
                
                // Restart hourly aggregation to rebuild data
                simpleHourlyAggregator.stopAggregation()
                kotlinx.coroutines.delay(1000) // Brief pause
                simpleHourlyAggregator.startAggregation()
                
                Log.i(TAG, "Hourly data cleared and rebuilding started")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing and rebuilding hourly data", e)
            }
        }
    }

    /**
     * CRITICAL FIX: Check and reset mood if it's clearly wrong
     */
    private suspend fun checkAndResetMoodIfWrong() {
        try {
            val currentMood = moodRepository.getCurrentMood().firstOrNull()
            val currentSteps = stepCountRepository.getTodayStepCount().firstOrNull()?.steps ?: 0
            val goal = try {
                userPreferences.dailyGoal.first()
            } catch (e: Exception) {
                Log.w(TAG, "Error getting daily goal from preferences, using default", e)
                10000 // Default goal
            }
            
            if (currentMood != null && currentSteps > 0) {
                // Calculate what the mood should be (base + gains, no decay consideration)
                val stepsPerMood = (150 * goal / 10000).coerceAtLeast(50)
                val expectedMoodGain = currentSteps / stepsPerMood
                val expectedMoodWithGains = (currentMood.dailyStartMood + expectedMoodGain).coerceIn(0, 130)
                
                // Only reset if mood is WAY off (like 130 with only 5k steps)
                // This accounts for proper decay that might have been applied
                if (currentMood.mood > expectedMoodWithGains + 30) {
                    Log.w(TAG, "CRITICAL: Mood is clearly wrong! Current: ${currentMood.mood}, Expected max: $expectedMoodWithGains, Steps: $currentSteps")
                    
                    // Reset mood to reasonable value (base + gains, decay will be applied by worker)
                    val correctedMood = currentMood.copy(
                        mood = expectedMoodWithGains,
                        lastPersistedSteps = currentSteps
                    )
                    moodRepository.updateMoodState(correctedMood)
                    
                    Log.i(TAG, "CRITICAL: Reset mood from ${currentMood.mood} to $expectedMoodWithGains")
                } else {
                    Log.d(TAG, "Mood validation passed - current: ${currentMood.mood}, expected max: $expectedMoodWithGains")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking and resetting mood", e)
        }
    }

    private fun startPeriodicDateChangeCheck() {
        viewModelScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(60000) // Check every minute
                    checkForDateChange()
                } catch (e: CancellationException) {
                    // This is normal when the ViewModel is cleared or coroutine is cancelled
                    Log.d(TAG, "Periodic date change check cancelled (normal behavior)")
                    break // Exit the loop when cancelled
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic date change check", e)
                }
            }
        }
    }

    private suspend fun checkForDateChange() {
        val currentDate = LocalDate.now()
        
        if (currentDate != lastKnownDate) {
            Log.i(TAG, "=== DATE CHANGE DETECTED IN HOME VIEWMODEL ===")
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
                
                // ENHANCED: Restart hourly aggregation for new day
                simpleHourlyAggregator.stopAggregation()
                kotlinx.coroutines.delay(1000) // Brief pause
                simpleHourlyAggregator.startAggregation()
                
                Log.i(TAG, "=== HOME VIEWMODEL DATE CHANGE RESET COMPLETED ===")
            }
            
            lastKnownDate = currentDate
        }
    }

    fun checkPermissions() {
        Log.d(TAG, "checkPermissions: Checking permissions")
        
        viewModelScope.launch {
            try {
                // Check activity recognition permission
                val hasActivityRecognition = true // We'll check this in the service
                val hasHealthConnect = stepCounterService.hasRequiredPermissions()
                
                Log.d(TAG, "checkPermissions: Activity recognition: $hasActivityRecognition, Health Connect: $hasHealthConnect")
                
                _needsPermission.value = !hasActivityRecognition || !hasHealthConnect
                
                if (!_needsPermission.value) {
                    Log.d(TAG, "checkPermissions: Starting step counting after initialization")
                    startStepCounting()
                } else {
                    Log.d(TAG, "checkPermissions: Permissions granted but waiting for initialization to complete")
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkPermissions: Error checking permissions", e)
                _needsPermission.value = true
            }
        }
    }

    private fun startStepCounting() {
        Log.i(TAG, "startStepCounting: Starting step counting service")
        
        viewModelScope.launch {
            try {
                // Start the step counting service
                stepCounterService.startStepCounting { steps ->
                    Log.d(TAG, "startStepCounting: Received step update: $steps")
                    viewModelScope.launch {
                        handleStepUpdate(steps)
                    }
                }
                
                _isStepCountingActive.value = true
                Log.i(TAG, "startStepCounting: Step counting service started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "startStepCounting: Error starting step counting service", e)
                _isStepCountingActive.value = false
            }
        }
    }

    private suspend fun handleStepUpdate(steps: Int) {
        Log.d(TAG, "handleStepUpdate: Processing step update: $steps")
        
        try {
            // Update step count in repository
            stepCountRepository.updateTodaySteps(steps)
            
            // Update hourly steps tracking
            moodRepository.updateSteps(steps)
            
            // Calculate live mood for UI display
            val liveMood = calculateLiveMood(steps)
            
            // Update UI state
            _uiState.update { currentState ->
                currentState.copy(
                    currentSteps = steps,
                    currentMood = liveMood,
                    moodState = com.example.myapplication.data.model.MoodState.fromMoodValue(liveMood)
                )
            }
            
            Log.d(TAG, "handleStepUpdate: Successfully updated UI with steps: $steps, mood: $liveMood")
            
        } catch (e: Exception) {
            Log.e(TAG, "handleStepUpdate: Error processing step update", e)
        }
    }

    /**
     * Calculate live mood based on current steps and database mood
     * This provides real-time mood updates without artificial limits
     */
    private suspend fun calculateLiveMood(currentSteps: Int): Int {
        try {
            // Get the base mood from database (with decay applied)
            val baseMood = moodRepository.getCurrentMood().firstOrNull()?.mood ?: 50
            
            // Get the last persisted steps from the database (more reliable than UserPreferences)
            val lastPersistedSteps = moodRepository.getCurrentMood().firstOrNull()?.lastPersistedSteps ?: 0
            
            // Calculate steps taken since last persisted update
            val stepsSinceLastUpdate = currentSteps - lastPersistedSteps
            
            if (stepsSinceLastUpdate <= 0) {
                // No new steps, return base mood
                return baseMood
            }
            
            // Calculate mood gain from new steps
            val moodGain = moodRepository.calculateMoodGain(stepsSinceLastUpdate)
            
            // Calculate live mood: base mood + new gains
            val liveMood = (baseMood + moodGain).coerceIn(0, 130)
            
            Log.d(TAG, "calculateLiveMood: Base mood: $baseMood, Steps since update: $stepsSinceLastUpdate, Gain: $moodGain, Live mood: $liveMood")
            
            return liveMood
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in corrected mood calculation", e)
            return 50 // Fallback to neutral mood
        }
    }

    fun onPermissionGranted() {
        Log.d(TAG, "onPermissionGranted: Permissions granted")
        _needsPermission.value = false
        startStepCounting()
    }

    fun refreshMood() {
        viewModelScope.launch {
            try {
                val currentSteps = stepCountRepository.getTodayStepCount().firstOrNull()?.steps ?: 0
                val liveMood = calculateLiveMood(currentSteps)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        currentSteps = currentSteps,
                        currentMood = liveMood,
                        moodState = com.example.myapplication.data.model.MoodState.fromMoodValue(liveMood)
                    )
                }
                
                Log.d(TAG, "refreshMood: Refreshed mood to $liveMood")
            } catch (e: Exception) {
                Log.e(TAG, "refreshMood: Error refreshing mood", e)
            }
        }
    }

    fun stopStepCounting() {
        Log.i(TAG, "stopStepCounting: Stopping step counting")
        stepCounterService.stopStepCounting()
        simpleHourlyAggregator.stopAggregation()
        _isStepCountingActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "HomeViewModel cleared")
        stopStepCounting()
    }
}

data class HomeUiState(
    val currentSteps: Int = 0,
    val currentMood: Int = 50,
    val dailyGoal: Int = 10000,
    val moodState: com.example.myapplication.data.model.MoodState = com.example.myapplication.data.model.MoodState.MEH,
    val needsPermission: Boolean = false
)
