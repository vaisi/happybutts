// Updated: UI now always shows the latest value from the service for currentSteps; repository is used only for daily goal and persistence
// Updated: Added CharacterState enum, state resolver, and exposed characterState in the ViewModel
// Updated: Added mood state from MoodRepository
// Updated: Consolidated initialization logic to prevent race conditions and step count flashing
// Updated: Single sequential date change detection flow with immediate UI state reset
// Updated: Simplified step update handling to avoid concurrency issues
// Fixed: Removed debug functions (testMoodJump, manualResetDaily)
// Fixed: Implemented hybrid mood calculation - database mood + live step gains for current hour
// Fixed: Live mood calculation now uses current hour's steps from database instead of lastPersistedSteps to prevent mood jumping
// Updated: Removed test methods (testMoodUpdate, checkWorkerStatus) from ViewModel
// Updated: Exposed stepsPerMood and decayThreshold as StateFlow properties for Mood Info modal
// Updated: Added debug mood decay testing functionality
// Updated: Added comprehensive fallback system for mood decay when workers fail
package com.example.myapplication.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.StepCount
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.service.UnifiedStepCounterService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDateTime
import com.example.myapplication.data.database.HourlyStepsDao
import com.example.myapplication.data.model.HourlySteps
import java.time.LocalDate
import com.example.myapplication.data.model.MoodState
import kotlinx.coroutines.delay
import com.example.myapplication.data.preferences.UserPreferences

// --- CharacterState Enum ---
enum class CharacterState {
    COMPLETELY_FLAT, FLAT, EXHAUSTED, TIRED, GRUMPY, NEUTRAL, HAPPY
}

// --- State Resolver ---
fun resolveCharacterState(steps: Int, goal: Int): CharacterState {
    val percent = if (goal > 0) steps * 100 / goal else 0
    return when {
        percent < 10 -> CharacterState.COMPLETELY_FLAT
        percent < 30 -> CharacterState.FLAT
        percent < 50 -> CharacterState.EXHAUSTED
        percent < 70 -> CharacterState.TIRED
        percent < 90 -> CharacterState.GRUMPY
        percent < 100 -> CharacterState.NEUTRAL
        else -> CharacterState.HAPPY
    }
}

@HiltViewModel
class StepCounterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StepCountRepository,
    private val moodRepository: MoodRepository,
    private val stepCounterService: UnifiedStepCounterService,
    private val hourlyStepsDao: HourlyStepsDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val TAG = "StepUI"
    private val _uiState = MutableStateFlow(StepCounterUiState())
    val uiState: StateFlow<StepCounterUiState> = _uiState.asStateFlow()

    // --- Expose characterState as a StateFlow ---
    private val _characterState = MutableStateFlow(CharacterState.COMPLETELY_FLAT)
    val characterState: StateFlow<CharacterState> = _characterState.asStateFlow()

    // --- Expose mood state ---
    val currentMood = moodRepository.getCurrentMood()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val moodHistory = moodRepository.getMoodHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Expose hourly mood log ---
    val todayMoodLog = moodRepository.getTodayMoodLog()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Expose real-time mood state ---
    private val _realTimeMood = MutableStateFlow(50)
    val realTimeMood: StateFlow<Int> = _realTimeMood.asStateFlow()

    // --- Expose mood progress ---
    private val _moodProgress = MutableStateFlow(0f)
    val moodProgress: StateFlow<Float> = _moodProgress.asStateFlow()

    // --- Expose daily goal ---
    val dailyGoal = userPreferences.dailyGoal
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 7000
        )

    // --- Expose steps per mood and decay threshold for info modal ---
    private val _stepsPerMood = dailyGoal.map { calculateStepsPerMood(it) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = calculateStepsPerMood(dailyGoal.value)
    )
    val stepsPerMood: StateFlow<Int> = _stepsPerMood

    private val _decayThreshold = dailyGoal.map { calculateDecayThreshold(it) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = calculateDecayThreshold(dailyGoal.value)
    )
    val decayThreshold: StateFlow<Int> = _decayThreshold

    private var lastDate: LocalDate = LocalDate.now()
    private var todaySteps: Int = 0
    private var isInitialized = false
    private var shouldStartStepCounting = false

    init {
        Log.i(TAG, "=== STEP COUNTER VIEWMODEL INITIALIZATION STARTED ===")
        
        // Single consolidated initialization flow
        viewModelScope.launch {
            try {
                // Step 1: Check for date change and perform daily reset if needed
                val currentDate = LocalDate.now()
                val lastMood = moodRepository.getCurrentMood().firstOrNull()
                
                Log.i(TAG, "init: Current date: $currentDate, lastMood date: ${lastMood?.date}")
                
                val dateChanged = lastMood != null && lastMood.date != currentDate
                
                if (dateChanged) {
                    Log.i(TAG, "=== DATE CHANGE DETECTED DURING INITIALIZATION ===")
                    
                    // Immediately reset UI state to prevent flashing
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentSteps = 0,
                            isHappy = false
                        )
                    }
                    _characterState.value = CharacterState.COMPLETELY_FLAT
                    
                    // Clear step detector data FIRST to prevent old baseline loading
                    Log.i(TAG, "init: Clearing step detector data before any step counting starts")
                    stepCounterService.clearStepDetectorData()
                    
                    // Finalize previous day's mood
                    lastMood?.let { mood ->
                        val yesterday = mood.date
                        Log.d(TAG, "init: Finalizing mood for date: $yesterday")
                        moodRepository.finalizeDayMood(yesterday)
                    }
                    
                    // Reset for new day
                    Log.d(TAG, "init: Resetting daily mood and steps for new day")
                    moodRepository.resetDaily()
                    
                    // Reset step count in repository
                    repository.updateTodaySteps(0)
                    
                    Log.i(TAG, "=== DATE CHANGE RESET COMPLETED ===")
                } else if (lastMood == null) {
                    // No mood entry exists, create one for today
                    Log.i(TAG, "init: No mood entry found, creating new entry for today")
                    moodRepository.resetDaily()
                }
                
                // Step 2: Initialize mood state
                val currentMood = moodRepository.getCurrentMood().firstOrNull()
                if (currentMood != null) {
                    Log.i(TAG, "init: Initializing mood with current mood: ${currentMood.mood}")
                    _realTimeMood.value = currentMood.mood
                    _moodProgress.value = calculateMoodProgress(currentMood.mood)
                } else {
                    Log.i(TAG, "init: No current mood found, initializing with default mood: 50")
                    _realTimeMood.value = 50
                    _moodProgress.value = calculateMoodProgress(50)
                }
                
                // Step 3: Update lastDate and mark as ready to start step counting
                lastDate = currentDate
                isInitialized = true
                
                // Step 4: Check for missed mood decay (fallback system)
                Log.i(TAG, "init: Checking for missed mood decay")
                checkAndApplyMissedMoodDecay()
                shouldStartStepCounting = true
                
                // Start step counting if permissions are already granted
                val hasActivityRecognition = context.checkSelfPermission(
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasActivityRecognition) {
                    Log.d(TAG, "init: Starting step counting after initialization")
                    startStepCounting()
                }
                
                Log.i(TAG, "=== STEP COUNTER VIEWMODEL INITIALIZATION COMPLETED ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "init: Error during initialization", e)
                // Set default values on error
                _realTimeMood.value = 50
                _moodProgress.value = calculateMoodProgress(50)
                isInitialized = true
                shouldStartStepCounting = true
            }
        }

        // Periodic date change detection (every 30 seconds)
        viewModelScope.launch {
            while (true) {
                try {
                    if (isInitialized) {
                        val currentDate = LocalDate.now()
                        val lastMood = moodRepository.getCurrentMood().firstOrNull()
                        
                        val dateChanged = currentDate != lastDate
                        val moodDateMismatch = lastMood != null && lastMood.date != currentDate
                        
                        if (dateChanged || moodDateMismatch) {
                            Log.i(TAG, "=== PERIODIC DATE CHANGE DETECTED ===")
                            
                            // Immediately reset UI state
                            _uiState.update { currentState ->
                                currentState.copy(
                                    currentSteps = 0,
                                    isHappy = false
                                )
                            }
                            _characterState.value = CharacterState.COMPLETELY_FLAT
                            
                            // Finalize previous day's mood
                            if (lastMood != null && lastMood.date.isBefore(currentDate)) {
                                val yesterday = lastMood.date
                                Log.d(TAG, "Finalizing mood for date: $yesterday")
                                moodRepository.finalizeDayMood(yesterday)
                            }
                            
                            // Reset for new day
                            Log.d(TAG, "Resetting daily mood and steps for new day")
                            moodRepository.resetDaily()
                            
                            // Reset step count in repository
                            repository.updateTodaySteps(0)
                            
                            // Clear step detector data
                            stepCounterService.clearStepDetectorData()
                            
                            lastDate = currentDate
                            Log.i(TAG, "=== PERIODIC DATE CHANGE RESET COMPLETED ===")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic date change check", e)
                }
                
                delay(30000) // Check every 30 seconds
            }
        }

        // Collect step updates from the service
        viewModelScope.launch {
            stepCounterService.stepUpdates.collect { steps ->
                Log.d(TAG, "Step update from SharedFlow: $steps")
                handleStepUpdate(steps)
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        Log.d(TAG, "checkPermissions: Checking permissions")
        viewModelScope.launch {
            val hasActivityRecognition = context.checkSelfPermission(
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            val hasHealthConnect = stepCounterService.hasRequiredPermissions()

            _uiState.update { currentState ->
                currentState.copy(
                    needsPermission = !hasActivityRecognition || !hasHealthConnect
                )
            }

            // Only start step counting after initialization is complete
            if (hasActivityRecognition && shouldStartStepCounting) {
                Log.d(TAG, "checkPermissions: Starting step counting after initialization")
                startStepCounting()
            } else if (hasActivityRecognition) {
                Log.d(TAG, "checkPermissions: Permissions granted but waiting for initialization to complete")
            }
        }
    }

    private fun calculateMoodProgress(mood: Int): Float {
        // Progress is now global: 0 (min mood) to 130 (max mood)
        return (mood.coerceIn(0, 130)) / 130f
    }

    private fun calculateMoodFromSteps(currentSteps: Int, previousSteps: Int): Int {
        var start = previousSteps
        var end = currentSteps
        if (end <= start) return 0
        
        // Use the same dynamic calculation as MoodRepository
        val userGoal = dailyGoal.value
        val stepsPerMood = calculateStepsPerMood(userGoal)
        
        val stepsInPeriod = end - start
        val gain = stepsInPeriod / stepsPerMood
        
        return gain
    }

    // Calculate steps per mood point based on user goal (same as MoodRepository)
    private fun calculateStepsPerMood(userGoal: Int): Int {
        val BASE_GOAL_STEPS = 10000
        val BASE_STEPS_PER_MOOD = 150
        val stepsPerMood = (BASE_STEPS_PER_MOOD * userGoal / BASE_GOAL_STEPS).coerceAtLeast(50)
        Log.i(TAG, "StepCounterViewModel - Dynamic calculation - Goal: $userGoal steps, Steps per mood: $stepsPerMood (base: $BASE_STEPS_PER_MOOD)")
        return stepsPerMood
    }

    // Calculate decay threshold based on user goal (same as MoodRepository)
    private fun calculateDecayThreshold(userGoal: Int): Int {
        val BASE_GOAL_STEPS = 10000
        val BASE_DECAY_THRESHOLD = 250
        val threshold = (BASE_DECAY_THRESHOLD * userGoal / BASE_GOAL_STEPS).coerceAtLeast(50)
        Log.i(TAG, "StepCounterViewModel - Dynamic calculation - Goal: $userGoal steps, Decay threshold: $threshold steps/hour (base: $BASE_DECAY_THRESHOLD)")
        return threshold
    }

    // Calculate mood decay per hour based on user goal (same as MoodRepository)
    private fun calculateMoodDecayPerHour(userGoal: Int): Int {
        val BASE_GOAL_STEPS = 10000
        val BASE_MOOD_DECAY = 5
        val decay = (BASE_MOOD_DECAY * userGoal / BASE_GOAL_STEPS).coerceAtLeast(1)
        Log.i(TAG, "StepCounterViewModel - Dynamic calculation - Goal: $userGoal steps, Mood decay per hour: $decay points (base: $BASE_MOOD_DECAY)")
        return decay
    }

    // IMPROVED SYSTEM: Helper methods to get last worker update data
    private suspend fun getLastWorkerHour(): Int {
        return try {
            // Use the stored worker hour from preferences
            val lastWorkerHour = moodRepository.getLastWorkerUpdateHour()
            Log.d(TAG, "getLastWorkerHour: Last worker hour: $lastWorkerHour")
            lastWorkerHour
        } catch (e: Exception) {
            Log.w(TAG, "getLastWorkerHour: Error getting last worker hour, using current hour - 1", e)
            val currentHour = LocalDateTime.now().hour
            if (currentHour == 0) 23 else currentHour - 1
        }
    }

    private suspend fun getLastWorkerSteps(): Int {
        return try {
            // Use the stored worker steps from preferences
            val lastWorkerSteps = moodRepository.getLastWorkerUpdateSteps()
            Log.d(TAG, "getLastWorkerSteps: Last worker steps: $lastWorkerSteps")
            lastWorkerSteps
        } catch (e: Exception) {
            Log.w(TAG, "getLastWorkerSteps: Error getting last worker steps, using 0", e)
            0
        }
    }

    private suspend fun getLastWorkerUpdateTime(): Long {
        return try {
            // Use the stored worker timestamp from preferences
            val lastWorkerTime = moodRepository.getLastWorkerUpdateTimestamp()
            Log.d(TAG, "getLastWorkerUpdateTime: Last worker update time: $lastWorkerTime")
            lastWorkerTime
        } catch (e: Exception) {
            Log.w(TAG, "getLastWorkerUpdateTime: Error getting last worker update time, using current time - 1 hour", e)
            System.currentTimeMillis() - (60 * 60 * 1000) // 1 hour ago
        }
    }

    private suspend fun calculateDecay(currentHour: Int, stepsInLastHour: Int, currentMood: Int): Int {
        // Check if current hour is within user's quiet hours
        if (isInQuietHours(currentHour)) {
            Log.d(TAG, "StepCounterViewModel - Decay calculation - Hour $currentHour is in quiet hours, no decay applied")
            return 0
        }
        
        val userGoal = dailyGoal.value
        val decayThreshold = calculateDecayThreshold(userGoal)
        val moodDecayPerHour = calculateMoodDecayPerHour(userGoal)
        
        val decay = when {
            currentMood > 100 -> 8 // OVEREXERTION_DECAY
            stepsInLastHour < decayThreshold -> moodDecayPerHour
            else -> 0
        }
        
        Log.d(TAG, "StepCounterViewModel - Decay calculation - Hour: $currentHour, Steps: $stepsInLastHour, Threshold: $decayThreshold, Current mood: $currentMood, Decay applied: $decay")
        return decay
    }

    // Check if the given hour is within user's quiet hours
    private suspend fun isInQuietHours(hour: Int): Boolean {
        val quietStart = userPreferences.quietHoursStart.first()
        val quietEnd = userPreferences.quietHoursEnd.first()
        
        return if (quietStart <= quietEnd) {
            // Normal case: start < end (e.g., 22:00 - 07:00)
            hour in quietStart..quietEnd
        } else {
            // Wrapping case: start > end (e.g., 23:00 - 06:00)
            hour >= quietStart || hour <= quietEnd
        }
    }

    private fun startStepCounting() {
        Log.d(TAG, "startStepCounting: Starting step counting")
        stepCounterService.startStepCounting { /* No-op: updates come from SharedFlow */ }
    }

    private suspend fun handleStepUpdate(steps: Int) {
        Log.i(TAG, "=== HANDLE STEP UPDATE STARTED ===")
        Log.d(TAG, "handleStepUpdate: Processing step update: $steps")
        
        try {
            // Update steps for current day
            Log.d(TAG, "handleStepUpdate: Updating steps to $steps")
            todaySteps = steps
            repository.updateTodaySteps(steps)
            
            // Note: Hourly step tracking is handled by the foreground service
            // The UI should only display data, not write to hourly database
            // REMOVED: moodRepository.updateSteps(steps) - this is now handled by the service only
            Log.d(TAG, "handleStepUpdate: Step update processed (hourly tracking handled by service)")
            
            val currentGoal = dailyGoal.value
            _uiState.update { currentState ->
                currentState.copy(
                    currentSteps = steps,
                    isHappy = steps >= currentGoal
                )
            }
            _characterState.value = resolveCharacterState(steps, currentGoal)
            
            // Update mood in real-time
            Log.d(TAG, "handleStepUpdate: Calling refreshLiveMood()")
            refreshLiveMood()
            
            Log.i(TAG, "=== HANDLE STEP UPDATE COMPLETED ===")
        } catch (e: Exception) {
            Log.e(TAG, "handleStepUpdate: Error processing step update", e)
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            repository.setDailyGoal(goal)
        }
    }

    fun onPermissionGranted() {
        Log.d(TAG, "onPermissionGranted: Permissions granted")
        _uiState.update { it.copy(needsPermission = false) }
        startStepCounting()
    }

    /**
     * Calculate live mood based on current steps and database mood
     * FIXED: No additional decay - database mood already includes worker's decay
     */
    fun refreshLiveMood() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "=== REFRESH LIVE MOOD STARTED ===")
                
                // Get base mood from database (updated by worker - already includes decay)
                val currentMood = moodRepository.getCurrentMood().first()
                val baseMood = currentMood.mood
                Log.d(TAG, "refreshLiveMood: Base mood from database: $baseMood")
                
                val currentHour = LocalDateTime.now().hour
                val currentSteps = _uiState.value.currentSteps
                
                // Get the last worker update time and data
                val lastWorkerHour = getLastWorkerHour()
                val lastWorkerSteps = getLastWorkerSteps()
                val lastWorkerTime = getLastWorkerUpdateTime()
                
                Log.d(TAG, "refreshLiveMood: Current hour: $currentHour, Last worker hour: $lastWorkerHour")
                Log.d(TAG, "refreshLiveMood: Current steps: $currentSteps, Last worker steps: $lastWorkerSteps")
                
                // DEBUG: Add detailed calculation logging
                Log.i(TAG, "=== CORRECTED MOOD CALCULATION DEBUG ===")
                Log.i(TAG, "Base mood (from worker): $baseMood")
                Log.i(TAG, "Current hour: $currentHour")
                Log.i(TAG, "Last worker hour: $lastWorkerHour")
                Log.i(TAG, "Current steps: $currentSteps")
                Log.i(TAG, "Last worker steps: $lastWorkerSteps")
                
                // Calculate steps since last worker update
                val stepsSinceLastWorker = if (currentSteps > lastWorkerSteps) {
                    currentSteps - lastWorkerSteps
                } else {
                    0
                }
                
                Log.i(TAG, "Steps since last worker update: $stepsSinceLastWorker")
                
                // Calculate mood gain from new steps (no artificial limits)
                val stepsPerMoodPoint = calculateStepsPerMood(dailyGoal.value)
                val moodGain = if (stepsSinceLastWorker > 0) {
                    stepsSinceLastWorker / stepsPerMoodPoint
                } else {
                    0
                }
                
                Log.i(TAG, "Steps per mood point: $stepsPerMoodPoint")
                Log.i(TAG, "Calculated mood gain: $moodGain")
                
                // FIXED: NO ADDITIONAL DECAY - database mood already includes worker's decay
                // The worker already applied decay at hour boundaries
                // We only need to add mood gain from new steps
                val totalDecay = 0
                
                Log.i(TAG, "CORRECTED: No additional decay applied - worker already handled it")
                Log.i(TAG, "Database mood already includes: previous decay + gain - decay")
                
                // CORRECTED: Only add mood gain to the worker's calculated mood
                val finalMood = (baseMood + moodGain).coerceIn(0, 130)
                
                Log.i(TAG, "Base mood (from worker): $baseMood")
                Log.i(TAG, "Mood gain (new steps): $moodGain")
                Log.i(TAG, "Final mood: $finalMood")
                Log.i(TAG, "=== END CORRECTED MOOD CALCULATION DEBUG ===")
                
                Log.i(TAG, "refreshLiveMood: Corrected calculation - Base: $baseMood, Gain: $moodGain, Final: $finalMood")
                Log.i(TAG, "refreshLiveMood: Previous realTimeMood: ${_realTimeMood.value} -> New: $finalMood")
                
                // CORRECTED: Don't update database with live mood - only use for UI display
                // The database should only be updated by the worker at hour boundaries
                Log.i(TAG, "refreshLiveMood: Using live mood for UI display only (not updating database)")
                
                _realTimeMood.value = finalMood
                _moodProgress.value = calculateMoodProgress(finalMood)
                
                Log.i(TAG, "=== REFRESH LIVE MOOD COMPLETED ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error in corrected mood calculation", e)
                // Fallback to database mood only
                val databaseMood = moodRepository.getCurrentMood().first().mood
                Log.i(TAG, "refreshLiveMood: Fallback to database mood only: $databaseMood")
                _realTimeMood.value = databaseMood
                _moodProgress.value = calculateMoodProgress(databaseMood)
            }
        }
    }

    /**
     * FALLBACK SYSTEM: Check and apply missed mood decay when app becomes active
     * This ensures mood decay works even if the worker fails
     */
    fun checkAndApplyMissedMoodDecay() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "=== CHECKING FOR MISSED MOOD DECAY ===")
                
                val now = LocalDateTime.now()
                val currentHour = now.hour
                val currentMood = moodRepository.getCurrentMood().first()
                val lastPersistedSteps = currentMood.lastPersistedSteps
                
                Log.d(TAG, "checkAndApplyMissedMoodDecay: Current hour: $currentHour, Last persisted steps: $lastPersistedSteps")
                
                // Check if we need to apply decay for any missed hours
                val missedHours = calculateMissedHours(currentHour, lastPersistedSteps)
                
                if (missedHours > 0) {
                    Log.i(TAG, "checkAndApplyMissedMoodDecay: Found $missedHours missed hours, applying decay")
                    applyMissedMoodDecay(missedHours, currentMood)
                } else {
                    Log.d(TAG, "checkAndApplyMissedMoodDecay: No missed hours detected")
                }
                
                // Refresh the UI to show updated mood
                refreshLiveMood()
                
                Log.i(TAG, "=== MISSED MOOD DECAY CHECK COMPLETED ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for missed mood decay", e)
            }
        }
    }

        /**
     * Calculate how many hours of mood decay we missed
     * This improved version only checks hours that the worker should have processed but didn't
     */
    private suspend fun calculateMissedHours(currentHour: Int, lastPersistedSteps: Int): Int {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        
        // Get all hourly steps for today to see which hours had activity
        val hourlySteps = moodRepository.getTodayHourlySteps().firstOrNull() ?: emptyList()
        val hoursWithSteps = hourlySteps.map { it.hour }.toSet()
        
        Log.d(TAG, "calculateMissedHours: Hours with steps: $hoursWithSteps")
        
        // The worker should have processed hours 0 to (currentHour - 1)
        // We only need to check if the worker missed any of these hours
        var missedHours = 0
        val workerShouldHaveProcessedUntil = currentHour - 1
        
        Log.d(TAG, "calculateMissedHours: Worker should have processed hours 0 to $workerShouldHaveProcessedUntil")
        
        for (hour in 0..workerShouldHaveProcessedUntil) {
            // Check if this hour was in quiet hours
            if (isInQuietHours(hour)) {
                Log.d(TAG, "calculateMissedHours: Hour $hour was in quiet hours, no decay needed")
                continue
            }
            
            // Check if this hour had steps recorded
            if (hour in hoursWithSteps) {
                val stepsInHour = hourlySteps.find { it.hour == hour }?.steps ?: 0
                Log.d(TAG, "calculateMissedHours: Hour $hour had $stepsInHour steps, no decay needed")
                continue
            }
            
            // This hour had no steps and wasn't in quiet hours - worker should have applied decay but didn't
            Log.d(TAG, "calculateMissedHours: Hour $hour had 0 steps, worker should have applied decay but didn't")
            missedHours++
        }
        
        Log.i(TAG, "calculateMissedHours: Found $missedHours hours that worker missed processing")
        
        // ADDITIONAL: Log to regular system logs for easy searching
        android.util.Log.i("MOOD_DEBUG", "=== MISSED HOURS CALCULATION ===")
        android.util.Log.i("MOOD_DEBUG", "Current hour: $currentHour")
        android.util.Log.i("MOOD_DEBUG", "Hours with steps: $hoursWithSteps")
        android.util.Log.i("MOOD_DEBUG", "Missed hours count: $missedHours")
        android.util.Log.i("MOOD_DEBUG", "=== END MISSED HOURS CALCULATION ===")
        
        return missedHours
    }

    /**
     * Apply mood decay for missed hours
     */
    private suspend fun applyMissedMoodDecay(missedHours: Int, currentMood: com.example.myapplication.data.model.MoodStateEntity) {
        Log.i(TAG, "applyMissedMoodDecay: Applying decay for $missedHours missed hours")
        
        var newMood = currentMood.mood
        val userGoal = dailyGoal.value
        val moodDecayPerHour = calculateMoodDecayPerHour(userGoal)
        
        // Get all hourly steps to identify which specific hours need decay
        val hourlySteps = moodRepository.getTodayHourlySteps().firstOrNull() ?: emptyList()
        val hoursWithSteps = hourlySteps.map { it.hour }.toSet()
        val now = LocalDateTime.now()
        val currentHour = now.hour
        
        // Find the specific hours that the worker missed processing
        val hoursNeedingDecay = mutableListOf<Int>()
        Log.i(TAG, "=== APPLYING MISSED DECAY - HOUR ANALYSIS ===")
        
        // Only check hours that the worker should have processed (0 to currentHour - 1)
        val workerShouldHaveProcessedUntil = currentHour - 1
        
        Log.d(TAG, "applyMissedMoodDecay: Worker should have processed hours 0 to $workerShouldHaveProcessedUntil")
        
        for (hour in 0..workerShouldHaveProcessedUntil) {
            // Check if this hour was in quiet hours
            if (isInQuietHours(hour)) {
                Log.d(TAG, "applyMissedMoodDecay: Hour $hour was in quiet hours, no decay needed")
                continue
            }
            
            // Check if this hour had steps
            if (hour in hoursWithSteps) {
                val stepsInHour = hourlySteps.find { it.hour == hour }?.steps ?: 0
                Log.d(TAG, "applyMissedMoodDecay: Hour $hour had $stepsInHour steps, no decay needed")
                continue
            }
            
            // This hour had no steps and wasn't in quiet hours - worker should have applied decay but didn't
            Log.i(TAG, "applyMissedMoodDecay: Hour $hour had 0 steps, worker missed applying decay")
            hoursNeedingDecay.add(hour)
        }
        
        Log.i(TAG, "applyMissedMoodDecay: Hours needing decay: $hoursNeedingDecay")
        
        // Apply decay for each specific hour that needs it
        for (hour in hoursNeedingDecay) {
            val decay = when {
                newMood > 100 -> 8 // OVEREXERTION_DECAY
                else -> moodDecayPerHour
            }
            
            newMood = (newMood - decay).coerceIn(0, 130)
            Log.i(TAG, "applyMissedMoodDecay: Applied $decay decay for hour $hour, new mood: $newMood")
        }
        
        // Update the mood in the database
        val updatedMood = currentMood.copy(mood = newMood)
        moodRepository.updateMoodState(updatedMood)
        
        Log.i(TAG, "applyMissedMoodDecay: Updated mood from ${currentMood.mood} to $newMood")
        
        // ADDITIONAL: Log to regular system logs for easy searching
        android.util.Log.i("MOOD_DEBUG", "=== MISSED DECAY APPLICATION ===")
        android.util.Log.i("MOOD_DEBUG", "Hours needing decay: $hoursNeedingDecay")
        android.util.Log.i("MOOD_DEBUG", "Original mood: ${currentMood.mood}")
        android.util.Log.i("MOOD_DEBUG", "Final mood: $newMood")
        android.util.Log.i("MOOD_DEBUG", "Mood change: ${currentMood.mood - newMood} points")
        android.util.Log.i("MOOD_DEBUG", "=== END MISSED DECAY APPLICATION ===")
    }

    /**
     * DEBUG METHOD: Manually trigger mood decay for testing
     * This should only be used for debugging the mood decay system
     */
    fun debugTriggerMoodDecay() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "=== DEBUG: MANUALLY TRIGGERING MOOD DECAY ===")
                moodRepository.debugTriggerMoodDecay()
                
                // Refresh the UI to show the updated mood
                refreshLiveMood()
                
                Log.i(TAG, "=== DEBUG: MOOD DECAY TRIGGERED AND UI REFRESHED ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error in debug mood decay", e)
            }
        }
    }

    /**
     * DEBUG METHOD: Check worker status and system health
     */
    fun debugCheckWorkerStatus() {
        viewModelScope.launch {
            try {
                Log.i(TAG, "=== DEBUG: CHECKING WORKER STATUS ===")
                
                // Check current mood state
                val currentMood = moodRepository.getCurrentMood().firstOrNull()
                Log.i(TAG, "Current mood state: $currentMood")
                
                // Check hourly steps
                val hourlySteps = moodRepository.getTodayHourlySteps().firstOrNull()
                Log.i(TAG, "Today's hourly steps: $hourlySteps")
                
                // Check step count
                val currentSteps = _uiState.value.currentSteps
                Log.i(TAG, "Current UI steps: $currentSteps")
                
                // Check if we're in quiet hours
                val currentHour = java.time.LocalDateTime.now().hour
                val isQuietHour = isInQuietHours(currentHour)
                Log.i(TAG, "Current hour: $currentHour, Is quiet hour: $isQuietHour")
                
                // Check user preferences
                val userGoal = dailyGoal.value
                val quietStart = userPreferences.quietHoursStart.first()
                val quietEnd = userPreferences.quietHoursEnd.first()
                Log.i(TAG, "User goal: $userGoal, Quiet hours: $quietStart-$quietEnd")
                
                // NEW: Check which hours should have had decay applied
                val now = LocalDateTime.now()
                val today = LocalDate.now()
                val hoursWithSteps = hourlySteps?.map { it.hour }?.toSet() ?: emptySet()
                
                Log.i(TAG, "=== DEBUG: DECAY ANALYSIS ===")
                Log.i(TAG, "Hours with steps: $hoursWithSteps")
                
                var totalDecayHours = 0
                var totalStepsToday = 0
                
                Log.i(TAG, "=== HOUR-BY-HOUR BREAKDOWN ===")
                for (hour in 0..currentHour) {
                    val isQuiet = isInQuietHours(hour)
                    val hadSteps = hour in hoursWithSteps
                    val stepsInHour = if (hadSteps) {
                        hourlySteps?.find { it.hour == hour }?.steps ?: 0
                    } else {
                        0
                    }
                    val shouldHaveDecay = !isQuiet && !hadSteps
                    
                    Log.i(TAG, "Hour $hour: Quiet=$isQuiet, Steps=$stepsInHour, ShouldDecay=$shouldHaveDecay")
                    
                    if (shouldHaveDecay) {
                        totalDecayHours++
                    }
                    totalStepsToday += stepsInHour
                }
                
                Log.i(TAG, "=== SUMMARY ===")
                Log.i(TAG, "Total steps today: $totalStepsToday")
                Log.i(TAG, "Total hours that should have had decay: $totalDecayHours")
                Log.i(TAG, "Expected total decay: ${totalDecayHours * 4} points (4 per hour)")
                Log.i(TAG, "=== DEBUG: DECAY ANALYSIS COMPLETED ===")
                
                Log.i(TAG, "=== DEBUG: WORKER STATUS CHECK COMPLETED ===")
                
                // ADDITIONAL: Log to regular system logs for easy searching
                android.util.Log.i("MOOD_DEBUG", "=== MOOD SYSTEM DEBUG REPORT ===")
                android.util.Log.i("MOOD_DEBUG", "Current mood: ${currentMood?.mood ?: "null"}")
                android.util.Log.i("MOOD_DEBUG", "Current steps: $currentSteps")
                android.util.Log.i("MOOD_DEBUG", "Current hour: $currentHour")
                android.util.Log.i("MOOD_DEBUG", "Is quiet hour: $isQuietHour")
                android.util.Log.i("MOOD_DEBUG", "User goal: $userGoal")
                android.util.Log.i("MOOD_DEBUG", "Quiet hours: $quietStart-$quietEnd")
                android.util.Log.i("MOOD_DEBUG", "Hours with steps: $hoursWithSteps")
                android.util.Log.i("MOOD_DEBUG", "Total steps today: $totalStepsToday")
                android.util.Log.i("MOOD_DEBUG", "Total hours that should have had decay: $totalDecayHours")
                android.util.Log.i("MOOD_DEBUG", "Expected total decay: ${totalDecayHours * 4} points")
                android.util.Log.i("MOOD_DEBUG", "=== END MOOD SYSTEM DEBUG REPORT ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking worker status", e)
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        stepCounterService.stopStepCounting()
    }
}

data class StepCounterUiState(
    val stepCount: StepCount? = null,
    val currentSteps: Int = 0,
    val isHappy: Boolean = false,
    val needsPermission: Boolean = false
) 