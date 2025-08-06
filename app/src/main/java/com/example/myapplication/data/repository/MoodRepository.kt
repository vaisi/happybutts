// Updated: MoodRepository - now uses dynamic formulas based on user goals instead of hardcoded values
// Fixed: Enhanced applyHourlyMoodDecay to store hourly steps in database for hybrid mood calculation
// Fixed: Critical bug in hourly steps tracking - now stores incremental steps per hour instead of total steps
package com.example.myapplication.data.repository

import com.example.myapplication.data.database.DailyStatisticsDao
import com.example.myapplication.data.database.MoodStateDao
import com.example.myapplication.data.database.HourlyStepsDao
import com.example.myapplication.data.database.HistoricalHourlyStepsDao
import com.example.myapplication.data.model.MoodStateEntity
import com.example.myapplication.data.model.HourlySteps
import com.example.myapplication.data.model.MoodState
import com.example.myapplication.data.model.MoodLogEntry
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.myapplication.data.preferences.UserPreferences

@Singleton
class MoodRepository @Inject constructor(
    private val moodStateDao: MoodStateDao,
    private val hourlyStepsDao: HourlyStepsDao,
    private val historicalHourlyStepsDao: HistoricalHourlyStepsDao,
    private val dailyStatisticsDao: DailyStatisticsDao,
    private val moodNotificationRepository: MoodNotificationRepository,
    private val userPreferences: UserPreferences
) {
    private val TAG = "MoodRepository"

    companion object {
        const val MOOD_MIN = 0
        const val MOOD_MAX = 130
        const val OVEREXERTION_DECAY = 8
        const val BASE_GOAL_STEPS = 10000 // Base goal for calculations
        const val BASE_DECAY_THRESHOLD = 250 // Steps per hour for 10k goal
        const val BASE_MOOD_DECAY = 5 // Mood decay per hour for 10k goal
        const val BASE_STEPS_PER_MOOD = 150 // Steps per +1 mood for 10k goal
    }

    // Get current mood state
    fun getCurrentMood(): Flow<MoodStateEntity> {
        val today = LocalDate.now()
        return moodStateDao.getMoodForDate(today)
            .map { state ->
                if (state == null) {
                    // No mood state for today, check if we need to reset
                    val yesterday = today.minusDays(1)
                    val yesterdayMood = moodStateDao.getMoodForDate(yesterday).firstOrNull()
                    val previousDayEndMood = yesterdayMood?.mood ?: 50
                    val startMood = calculateStartMood(previousDayEndMood)
                    Log.i(TAG, "First launch of new day, resetting mood to $startMood (previous day end: $previousDayEndMood)")
                    
                    // For fresh installs, use a more appropriate default
                    val finalStartMood = if (yesterdayMood == null) {
                        Log.i(TAG, "Fresh install detected, using default mood of 50")
                        50 // Neutral starting mood for fresh installs
                    } else {
                        startMood
                    }
                    
                    // Create new mood state for today
                    MoodStateEntity(
                        date = today,
                        mood = finalStartMood,
                        dailyStartMood = finalStartMood,
                        previousDayEndMood = previousDayEndMood,
                        lastPersistedSteps = 0
                    ).also { newState ->
                        // Insert the new state
                        moodStateDao.insertMood(newState)
                        Log.i(TAG, "Created new mood state for fresh install: $finalStartMood")
                    }
                } else {
                    state
                }
            }
    }

    // Get mood history for calendar view
    fun getMoodHistory(months: Int = 12): Flow<List<MoodStateEntity>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(months.toLong())
        return moodStateDao.getMoodHistory(startDate, endDate)
    }

    // Get hourly steps for today
    fun getTodayHourlySteps(): Flow<List<HourlySteps>> {
        return hourlyStepsDao.getHourlyStepsForDate(LocalDate.now())
    }

    // Calculate mood gain from steps using dynamic formulas based on user goal
    private suspend fun calculateMoodFromSteps(currentSteps: Int, previousSteps: Int): Int {
        val userGoal = userPreferences.dailyGoal.first()
        val stepsPerMood = calculateStepsPerMood(userGoal)
        
        var start = previousSteps
        var end = currentSteps
        if (end <= start) return 0

        // Simplified bracket system: single bracket based on user goal
        val stepsInPeriod = end - start
        val gain = stepsInPeriod / stepsPerMood
        
        Log.d(TAG, "Mood gain calculation: $stepsInPeriod steps / $stepsPerMood steps per mood = $gain mood points (goal: $userGoal)")
        return gain
    }

    // Calculate mood gain from steps in a specific period (for hourly calculations)
    suspend fun calculateMoodFromStepsInPeriod(stepsInPeriod: Int): Int {
        val userGoal = userPreferences.dailyGoal.first()
        val stepsPerMood = calculateStepsPerMood(userGoal)
        
        if (stepsInPeriod <= 0) return 0

        // Simplified bracket system: single bracket based on user goal
        val gain = stepsInPeriod / stepsPerMood
        
        Log.d(TAG, "Mood gain calculation for period: $stepsInPeriod steps / $stepsPerMood steps per mood = $gain mood points (goal: $userGoal)")
        return gain
    }

    // Calculate steps per mood point based on user goal
    private fun calculateStepsPerMood(userGoal: Int): Int {
        // Base: 150 steps per mood for 10,000 goal
        // Scale proportionally: stepsPerMood = BASE_STEPS_PER_MOOD * (userGoal / BASE_GOAL_STEPS)
        val stepsPerMood = (BASE_STEPS_PER_MOOD * userGoal / BASE_GOAL_STEPS).coerceAtLeast(50)
        Log.i(TAG, "Dynamic calculation - Goal: $userGoal steps, Steps per mood: $stepsPerMood (base: $BASE_STEPS_PER_MOOD)")
        return stepsPerMood
    }

    // Calculate decay threshold based on user goal
    private suspend fun calculateDecayThreshold(): Int {
        val userGoal = userPreferences.dailyGoal.first()
        // Base: 250 steps per hour for 10,000 goal (2.5% of daily goal)
        // Scale proportionally: threshold = BASE_DECAY_THRESHOLD * (userGoal / BASE_GOAL_STEPS)
        val threshold = (BASE_DECAY_THRESHOLD * userGoal / BASE_GOAL_STEPS).coerceAtLeast(50)
        Log.i(TAG, "Dynamic calculation - Goal: $userGoal steps, Decay threshold: $threshold steps/hour (base: $BASE_DECAY_THRESHOLD)")
        return threshold
    }

    // Calculate mood decay per hour based on user goal
    private suspend fun calculateMoodDecayPerHour(): Int {
        val userGoal = userPreferences.dailyGoal.first()
        // Base: 5 mood decay per hour for 10,000 goal
        // Scale proportionally: decay = BASE_MOOD_DECAY * (userGoal / BASE_GOAL_STEPS)
        val decay = (BASE_MOOD_DECAY * userGoal / BASE_GOAL_STEPS).coerceAtLeast(1)
        Log.i(TAG, "Dynamic calculation - Goal: $userGoal steps, Mood decay per hour: $decay points (base: $BASE_MOOD_DECAY)")
        return decay
    }

    // Calculate decay based on steps and current mood using dynamic thresholds
    suspend fun calculateDecay(
        currentHour: Int,
        stepsInLastHour: Int,
        currentMood: Int
    ): Int {
        // Check if current hour is within user's quiet hours
        if (isInQuietHours(currentHour)) {
            Log.d(TAG, "Decay calculation - Hour $currentHour is in quiet hours, no decay applied")
            return 0
        }

        val decayThreshold = calculateDecayThreshold()
        val moodDecayPerHour = calculateMoodDecayPerHour()

        val decay = when {
            // Overexertion penalty
            currentMood > 100 -> OVEREXERTION_DECAY
            // Normal decay if inactive (using dynamic threshold)
            stepsInLastHour < decayThreshold -> moodDecayPerHour
            // No decay if active
            else -> 0
        }
        
        Log.d(TAG, "Decay calculation - Hour: $currentHour, Steps: $stepsInLastHour, Threshold: $decayThreshold, Current mood: $currentMood, Decay applied: $decay")
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

    // Calculate start mood for a new day
    private fun calculateStartMood(previousDayEndMood: Int): Int {
        return when {
            previousDayEndMood > 115 -> 60       // After extreme overexertion
            previousDayEndMood < 20 -> 30        // After terrible day
            else -> 50                           // Normal day
        }
    }

    /**
     * Record steps for a specific hour. Used by the worker to record the previous hour's steps.
     * Enhanced: Now supports polling-based system with last recorded total tracking.
     */
    suspend fun recordHourlySteps(hour: Int, stepsInHour: Int, lastRecordedTotal: Int = 0) {
        val today = LocalDate.now()
        
        Log.d(TAG, "recordHourlySteps: Recording $stepsInHour steps for hour $hour on $today (total: $lastRecordedTotal)")
        
        // Create or update the hourly steps entry
        val hourlySteps = hourlyStepsDao.getHourlyStepsForHour(today, hour)
        if (hourlySteps != null) {
            Log.d(TAG, "recordHourlySteps: Updating existing hourly steps for hour $hour")
            hourlyStepsDao.updateHourlyStepsWithTotal(today, hour, stepsInHour, lastRecordedTotal)
        } else {
            Log.d(TAG, "recordHourlySteps: Creating new hourly steps for hour $hour")
            hourlyStepsDao.insertHourlySteps(
                HourlySteps(
                    date = today,
                    hour = hour,
                    steps = stepsInHour,
                    lastRecordedTotal = lastRecordedTotal
                )
            )
        }
        
        Log.i(TAG, "recordHourlySteps: DATABASE WRITE - Hour: $hour, Steps recorded: $stepsInHour, Total: $lastRecordedTotal")
        Log.d(TAG, "recordHourlySteps: Successfully recorded $stepsInHour steps for hour $hour")
    }

    /**
     * Call this frequently to log steps for the current hour. Does NOT apply mood decay.
     */
    suspend fun updateSteps(steps: Int) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val currentHour = now.hour
        
        Log.d(TAG, "updateSteps: Updating steps for hour $currentHour: $steps")
        
        // Get the last persisted steps to calculate incremental steps for this hour
        val currentMood = moodStateDao.getMoodForDate(today).firstOrNull()
        val lastPersistedSteps = currentMood?.lastPersistedSteps ?: 0
        
        // Calculate steps taken in the current hour
        val stepsInCurrentHour = if (steps > lastPersistedSteps) {
            steps - lastPersistedSteps
        } else {
            0
        }
        
        Log.i(TAG, "updateSteps: HOURLY TRACKING - Total steps: $steps, Last persisted: $lastPersistedSteps, Steps in current hour: $stepsInCurrentHour")
        
        // Log the hourly steps that will be recorded
        Log.i(TAG, "updateSteps: RECORDING - Hour: $currentHour, Steps to record: $stepsInCurrentHour")
        
        // Update hourly steps with steps taken in the current hour (not total steps)
        val hourlySteps = hourlyStepsDao.getHourlyStepsForHour(today, currentHour)
        if (hourlySteps != null) {
            Log.d(TAG, "updateSteps: Updating existing hourly steps for hour $currentHour")
            hourlyStepsDao.updateHourlySteps(today, currentHour, stepsInCurrentHour)
        } else {
            Log.d(TAG, "updateSteps: Creating new hourly steps for hour $currentHour")
            hourlyStepsDao.insertHourlySteps(
                HourlySteps(
                    date = today,
                    hour = currentHour,
                    steps = stepsInCurrentHour
                )
            )
        }
        
        // Update lastPersistedSteps to the current total steps so next calculation is incremental
        val existingMood = moodStateDao.getMoodForDate(today).firstOrNull()
        if (existingMood != null) {
            val updatedMood = existingMood.copy(lastPersistedSteps = steps)
            moodStateDao.updateMood(updatedMood)
            Log.i(TAG, "updateSteps: Updated lastPersistedSteps from ${existingMood.lastPersistedSteps} to $steps")
        } else {
            // Create a new mood state for today if none exists
            Log.w(TAG, "updateSteps: No mood state found for today, creating new one")
            val newMoodState = MoodStateEntity(
                date = today,
                mood = 50, // Default mood
                dailyStartMood = 50,
                previousDayEndMood = 50,
                lastPersistedSteps = steps
            )
            moodStateDao.insertMood(newMoodState)
            Log.i(TAG, "updateSteps: Created new mood state with lastPersistedSteps: $steps")
        }
        
        Log.d(TAG, "updateSteps: Hourly steps updated, lastPersistedSteps updated to $steps")
    }

    /**
     * Call this ONCE at the end of each hour to apply mood decay and gain for that hour.
     * This should be called by the hourly worker.
     */
    suspend fun applyHourlyMoodDecay(steps: Int) {
        Log.i(TAG, "=== APPLYING HOURLY MOOD DECAY ===")
        
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val currentHour = now.hour
        val previousHour = if (currentHour == 0) 23 else currentHour - 1
        
        Log.d(TAG, "applyHourlyMoodDecay: Current hour: $currentHour, Previous hour: $previousHour, Total steps: $steps")
        
        // Get steps for the previous hour (now correctly calculated by worker using hybrid approach)
        val stepsInPreviousHour = hourlyStepsDao.getHourlyStepsForHour(today, previousHour)?.steps ?: 0

        Log.d(TAG, "applyHourlyMoodDecay: Steps in previous hour: $stepsInPreviousHour")

        // Get current mood state
        val currentMood = moodStateDao.getMoodForDate(today).firstOrNull()
        val previousMood = currentMood?.mood ?: 50

        Log.d(TAG, "applyHourlyMoodDecay: Previous mood: $previousMood")

        // Calculate mood gain and decay for the PREVIOUS hour that just ended
        val moodGain = calculateMoodFromStepsInPeriod(stepsInPreviousHour)
        val decay = calculateDecay(previousHour, stepsInPreviousHour, previousMood)  // Fixed: use stepsInPreviousHour
        val newMood = (previousMood + moodGain - decay).coerceIn(MOOD_MIN, MOOD_MAX)

        Log.d(TAG, "applyHourlyMoodDecay: Mood gain: $moodGain, Decay: $decay, New mood: $newMood")

        // Check for mood drop and record it for notifications
        if (newMood < previousMood) {
            val moodDrop = previousMood - newMood
            val periodHours = 1f // 1 hour period
            
            Log.i(TAG, "Mood drop detected: $previousMood -> $newMood (drop: $moodDrop, steps in hour: $stepsInPreviousHour)")
            moodNotificationRepository.recordMoodDrop(previousMood, newMood, stepsInPreviousHour, periodHours)
        }

        // Update mood state
        val updatedMood = if (currentMood != null) {
            currentMood.copy(mood = newMood, lastPersistedSteps = steps)
        } else {
            MoodStateEntity(
                date = today,
                mood = newMood,
                dailyStartMood = newMood,
                previousDayEndMood = 50,
                lastPersistedSteps = steps
            )
        }
        moodStateDao.insertMood(updatedMood)
        
        Log.i(TAG, "Applied hourly mood update for hour $previousHour: $previousMood -> $newMood (gain: $moodGain, decay: $decay, steps in hour: $stepsInPreviousHour, total steps: $steps)")
        Log.i(TAG, "=== HOURLY MOOD DECAY COMPLETED ===")
    }

    /**
     * HYBRID FIX: Apply mood decay and gain using pre-calculated steps for the previous hour.
     * This avoids database lookup issues and uses the worker's hybrid calculation.
     */
    suspend fun applyHourlyMoodDecayWithSteps(steps: Int, stepsInPreviousHour: Int) {
        Log.i(TAG, "=== APPLYING HOURLY MOOD DECAY WITH PRE-CALCULATED STEPS ===")
        
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val currentHour = now.hour
        val previousHour = if (currentHour == 0) 23 else currentHour - 1
        
        Log.d(TAG, "applyHourlyMoodDecayWithSteps: Current hour: $currentHour, Previous hour: $previousHour")
        Log.d(TAG, "applyHourlyMoodDecayWithSteps: Total steps: $steps, Steps in previous hour: $stepsInPreviousHour")

        // Get current mood state
        val currentMood = moodStateDao.getMoodForDate(today).firstOrNull()
        val previousMood = currentMood?.mood ?: 50

        Log.d(TAG, "applyHourlyMoodDecayWithSteps: Previous mood: $previousMood")

        // Calculate mood gain and decay for the PREVIOUS hour that just ended
        val moodGain = calculateMoodFromStepsInPeriod(stepsInPreviousHour)
        val decay = calculateDecay(previousHour, stepsInPreviousHour, previousMood)
        val newMood = (previousMood + moodGain - decay).coerceIn(MOOD_MIN, MOOD_MAX)

        Log.d(TAG, "applyHourlyMoodDecayWithSteps: Mood gain: $moodGain, Decay: $decay, New mood: $newMood")

        // Check for mood drop and record it for notifications
        if (newMood < previousMood) {
            val moodDrop = previousMood - newMood
            val periodHours = 1f // 1 hour period
            
            Log.i(TAG, "Mood drop detected: $previousMood -> $newMood (drop: $moodDrop, steps in hour: $stepsInPreviousHour)")
            moodNotificationRepository.recordMoodDrop(previousMood, newMood, stepsInPreviousHour, periodHours)
        }

        // CRITICAL FIX: Update mood state with current total steps as lastPersistedSteps
        val updatedMood = if (currentMood != null) {
            currentMood.copy(mood = newMood, lastPersistedSteps = steps)
        } else {
            MoodStateEntity(
                date = today,
                mood = newMood,
                dailyStartMood = newMood,
                previousDayEndMood = 50,
                lastPersistedSteps = steps
            )
        }
        moodStateDao.insertMood(updatedMood)
        
        Log.i(TAG, "CRITICAL FIX: Updated lastPersistedSteps to current total: $steps")
        Log.i(TAG, "Applied hourly mood update for hour $previousHour: $previousMood -> $newMood (gain: $moodGain, decay: $decay, steps in hour: $stepsInPreviousHour, total steps: $steps)")
        Log.i(TAG, "=== HOURLY MOOD DECAY COMPLETED ===")
    }

    /**
     * DEBUG METHOD: Manually trigger mood decay for testing
     * This should only be used for debugging the mood decay system
     */
    suspend fun debugTriggerMoodDecay() {
        Log.i(TAG, "=== DEBUG: MANUALLY TRIGGERING MOOD DECAY ===")
        
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val currentHour = now.hour
        val previousHour = if (currentHour == 0) 23 else currentHour - 1
        
        Log.d(TAG, "debugTriggerMoodDecay: Current hour: $currentHour, Previous hour: $previousHour")
        
        // Get current mood state
        val currentMood = moodStateDao.getMoodForDate(today).firstOrNull()
        val previousMood = currentMood?.mood ?: 50
        
        Log.d(TAG, "debugTriggerMoodDecay: Current mood: $previousMood")
        
        // Simulate 0 steps in the previous hour to trigger decay
        val stepsInPreviousHour = 0
        val decay = calculateDecay(previousHour, stepsInPreviousHour, previousMood)
        val newMood = (previousMood - decay).coerceIn(MOOD_MIN, MOOD_MAX)
        
        Log.i(TAG, "debugTriggerMoodDecay: Decay applied: $decay, New mood: $newMood")
        
        // Update mood state
        val updatedMood = if (currentMood != null) {
            currentMood.copy(mood = newMood)
        } else {
            MoodStateEntity(
                date = today,
                mood = newMood,
                dailyStartMood = newMood,
                previousDayEndMood = 50,
                lastPersistedSteps = 0
            )
        }
        moodStateDao.insertMood(updatedMood)
        
        Log.i(TAG, "debugTriggerMoodDecay: Mood updated: $previousMood -> $newMood")
        Log.i(TAG, "=== DEBUG: MOOD DECAY COMPLETED ===")
    }

    /**
     * Get the last time mood was updated (for fallback system)
     */
    suspend fun getLastMoodUpdateTime(): LocalDateTime? {
        val today = LocalDate.now()
        val currentMood = moodStateDao.getMoodForDate(today).firstOrNull()
        
        // Check if we have hourly steps for the current hour (indicates worker ran recently)
        val currentHour = LocalDateTime.now().hour
        val currentHourSteps = hourlyStepsDao.getHourlyStepsForHour(today, currentHour)
        
        return if (currentMood != null && currentHourSteps != null) {
            // Worker has run for current hour, so last update was very recent
            LocalDateTime.now().minusMinutes(5) // Very recent
        } else if (currentMood != null) {
            // No hourly steps for current hour, but mood exists - worker hasn't run yet
            LocalDateTime.now().minusHours(1) // Rough estimate
        } else {
            null
        }
    }

    /**
     * Enhanced: Get last recorded total steps for polling system
     */
    suspend fun getLastRecordedTotal(): Int {
        val today = LocalDate.now()
        return hourlyStepsDao.getLastRecordedTotalForDate(today) ?: 0
    }

    /**
     * Enhanced: Check if hourly data is missing for the current day
     */
    suspend fun checkForMissingHourlyData(): Boolean {
        val today = LocalDate.now()
        val currentHour = LocalDateTime.now().hour
        val recordedHours = hourlyStepsDao.getHourlyStepsForDate(today).firstOrNull()?.map { it.hour } ?: emptyList()
        
        // Check if we have data for all hours up to current hour
        val expectedHours = (0 until currentHour).toSet()
        val missingHours = expectedHours - recordedHours.toSet()
        
        return missingHours.isNotEmpty()
    }

    /**
     * Enhanced: Recover missing hourly data using polling system
     */
    suspend fun recoverMissingHourlyData(currentTotalSteps: Int) {
        val today = LocalDate.now()
        val currentHour = LocalDateTime.now().hour
        val lastRecorded = getLastRecordedTotal()
        
        if (currentTotalSteps <= lastRecorded) {
            Log.d(TAG, "No new steps to recover")
            return
        }
        
        val stepsToRecover = currentTotalSteps - lastRecorded
        Log.i(TAG, "Recovering $stepsToRecover steps for missing hourly data")
        
        // Get missing hours
        val recordedHours = hourlyStepsDao.getHourlyStepsForDate(today).firstOrNull()?.map { it.hour } ?: emptyList()
        val expectedHours = (0 until currentHour).toSet()
        val missingHours = (expectedHours - recordedHours.toSet()).toList().sorted()
        
        if (missingHours.isEmpty()) {
            Log.d(TAG, "No missing hours to recover")
            return
        }
        
        // Distribute steps across missing hours
        val stepsPerHour = stepsToRecover / missingHours.size
        val remainingSteps = stepsToRecover % missingHours.size
        
        var currentTotal = lastRecorded
        for ((index, hour) in missingHours.withIndex()) {
            val stepsForThisHour = stepsPerHour + if (index < remainingSteps) 1 else 0
            currentTotal += stepsForThisHour
            
            recordHourlySteps(hour, stepsForThisHour, currentTotal)
            Log.d(TAG, "Recovered hour $hour: $stepsForThisHour steps")
        }
        
        Log.i(TAG, "Successfully recovered ${missingHours.size} missing hours")
    }

    /**
     * Update mood state (for fallback system)
     */
    suspend fun updateMoodState(updatedMood: MoodStateEntity) {
        moodStateDao.insertMood(updatedMood)
        Log.i(TAG, "updateMoodState: Updated mood to ${updatedMood.mood}")
    }

    // Reset daily mood state
    suspend fun resetDaily() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        // CRITICAL: Archive yesterday's hourly data before clearing
        Log.i(TAG, "=== ARCHIVING YESTERDAY'S DATA ===")
        archiveDailyData(yesterday)
        Log.i(TAG, "=== ARCHIVING COMPLETED ===")
        
        // Get yesterday's end mood
        val yesterdayMood = moodStateDao.getMoodForDate(yesterday).firstOrNull()
        val previousDayEndMood = yesterdayMood?.mood ?: 50
        // Calculate start mood for today
        val startMood = calculateStartMood(previousDayEndMood)
        // Create new mood state for today
        val newMoodState = MoodStateEntity(
            date = today,
            mood = startMood,
            dailyStartMood = startMood,
            previousDayEndMood = previousDayEndMood,
            lastPersistedSteps = 0
        )
        moodStateDao.insertMood(newMoodState)
        
        // Clear any existing hourly steps for today to ensure fresh start
        try {
            hourlyStepsDao.deleteHourlyStepsForDate(today)
            Log.i(TAG, "Cleared hourly steps for $today to ensure fresh start")
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear hourly steps for $today: ${e.message}")
        }
        
        Log.i(TAG, "Reset daily mood: $startMood (previous day end: $previousDayEndMood)")
    }

    /**
     * Get historical hourly steps for a specific date range
     */
    fun getHistoricalHourlySteps(startDate: LocalDate, endDate: LocalDate): Flow<List<com.example.myapplication.data.model.HistoricalHourlySteps>> {
        return historicalHourlyStepsDao.getHistoricalHourlyStepsForDateRange(startDate, endDate)
    }

    /**
     * Get historical hourly steps for a specific date
     */
    fun getHistoricalHourlyStepsForDate(date: LocalDate): Flow<List<com.example.myapplication.data.model.HistoricalHourlySteps>> {
        return historicalHourlyStepsDao.getHistoricalHourlyStepsForDate(date)
    }

    /**
     * Get daily statistics for a specific date range
     */
    fun getDailyStatistics(startDate: LocalDate, endDate: LocalDate): Flow<List<com.example.myapplication.data.model.DailyStatistics>> {
        return dailyStatisticsDao.getDailyStatisticsForDateRange(startDate, endDate)
    }

    /**
     * Get recent daily statistics
     */
    fun getRecentDailyStatistics(limit: Int = 30): Flow<List<com.example.myapplication.data.model.DailyStatistics>> {
        return dailyStatisticsDao.getRecentDailyStatistics(limit)
    }

    /**
     * Get average mood for a date range
     */
    fun getAverageMoodForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Double?> {
        return dailyStatisticsDao.getAverageMoodForDateRange(startDate, endDate)
    }

    /**
     * Get average steps for a date range
     */
    fun getAverageStepsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Double?> {
        return dailyStatisticsDao.getAverageStepsForDateRange(startDate, endDate)
    }

    /**
     * Get goal achievement count for a date range
     */
    fun getGoalAchievementCount(startDate: LocalDate, endDate: LocalDate): Flow<Int?> {
        return dailyStatisticsDao.getGoalAchievementCount(startDate, endDate)
    }

    /**
     * Archive all hourly steps data for a specific date before clearing it
     */
    private suspend fun archiveDailyData(date: LocalDate) {
        try {
            Log.i(TAG, "Archiving daily data for date: $date")
            
            // Get all hourly steps for the date
            val hourlySteps = hourlyStepsDao.getHourlyStepsForDate(date).firstOrNull() ?: emptyList()
            
            if (hourlySteps.isEmpty()) {
                Log.i(TAG, "No hourly data to archive for $date")
                return
            }
            
            val archiveTime = LocalDateTime.now()
            
            // Archive hourly steps
            val historicalData = hourlySteps.map { hourlyStep ->
                com.example.myapplication.data.model.HistoricalHourlySteps(
                    date = hourlyStep.date,
                    hour = hourlyStep.hour,
                    steps = hourlyStep.steps,
                    archivedAt = archiveTime
                )
            }
            
            // Insert into historical table
            historicalHourlyStepsDao.insertHistoricalHourlyStepsList(historicalData)
            
            Log.i(TAG, "Successfully archived ${historicalData.size} hourly records for $date")
            
            // Archive daily statistics
            val moodState = moodStateDao.getMoodForDate(date).firstOrNull()
            val finalMood = moodState?.mood ?: 50
            val dailyGoal = userPreferences.dailyGoal.first()
            val totalSteps = hourlySteps.sumOf { it.steps }
            
            val dailyStatistics = com.example.myapplication.data.model.DailyStatistics(
                date = date,
                dailyGoal = dailyGoal,
                finalMood = finalMood,
                totalSteps = totalSteps,
                archivedAt = archiveTime
            )
            
            dailyStatisticsDao.insertDailyStatistics(dailyStatistics)
            
            Log.i(TAG, "Successfully archived daily statistics for $date: Goal=$dailyGoal, FinalMood=$finalMood, TotalSteps=$totalSteps")
            
            // Log the archived data for debugging
            historicalData.forEach { record ->
                Log.d(TAG, "Archived: ${record.date} Hour ${record.hour}: ${record.steps} steps")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to archive daily data for $date", e)
        }
    }

    // Get current hour's steps from database
    suspend fun getCurrentHourSteps(): Int {
        val today = LocalDate.now()
        val currentHour = LocalDateTime.now().hour
        return hourlyStepsDao.getHourlyStepsForHour(today, currentHour)?.steps ?: 0
    }

    fun getTodayMoodLog(): Flow<List<MoodLogEntry>> {
        val today = LocalDate.now()
        return hourlyStepsDao.getHourlyStepsForDate(today).map { hourlyStepsList ->
            val log = mutableListOf<MoodLogEntry>()
            var mood = 50 // Start mood for the day
            var prevSteps = 0
            for (entry in hourlyStepsList.sortedBy { it.hour }) {
                val moodGain = calculateMoodFromSteps(entry.steps, prevSteps)
                val decay = calculateDecay(entry.hour, entry.steps - prevSteps, mood)
                mood = (mood + moodGain - decay).coerceIn(MOOD_MIN, MOOD_MAX)
                log.add(MoodLogEntry(
                    hour = entry.hour,
                    steps = entry.steps - prevSteps,
                    mood = mood,
                    moodState = MoodState.fromMoodValue(mood)
                ))
                prevSteps = entry.steps
            }
            log
        }
    }

    /**
     * Finalize the mood for a given date by calculating the end-of-day mood and updating the MoodStateEntity.
     */
    suspend fun finalizeDayMood(date: LocalDate) {
        // Get all hourly steps for the date
        val hourlyStepsList = hourlyStepsDao.getHourlyStepsForDate(date).firstOrNull()?.sortedBy { it.hour } ?: emptyList()
        val moodState = moodStateDao.getMoodForDate(date).firstOrNull()
        if (moodState == null) return
        var mood = moodState.dailyStartMood
        var prevSteps = 0
        var lastHour = -1
        // Replay the day's mood changes
        for (entry in hourlyStepsList) {
            // If there are missing hours, apply decay for each missing hour
            if (lastHour != -1 && entry.hour > lastHour + 1) {
                for (h in (lastHour + 1) until entry.hour) {
                    val decay = calculateDecay(h, 0, mood)
                    mood = (mood - decay).coerceIn(MOOD_MIN, MOOD_MAX)
                }
            }
            val moodGain = calculateMoodFromSteps(entry.steps, prevSteps)
            val decay = calculateDecay(entry.hour, entry.steps - prevSteps, mood)
            mood = (mood + moodGain - decay).coerceIn(MOOD_MIN, MOOD_MAX)
            prevSteps = entry.steps
            lastHour = entry.hour
        }
        // Apply decay for any missing hours until 23
        if (lastHour < 23) {
            for (h in (lastHour + 1)..23) {
                val decay = calculateDecay(h, 0, mood)
                mood = (mood - decay).coerceIn(MOOD_MIN, MOOD_MAX)
            }
        }
        // Update the MoodStateEntity for the date with the final mood
        val updatedMood = moodState.copy(mood = mood)
        moodStateDao.updateMood(updatedMood)
        Log.i(TAG, "Finalized mood for $date: $mood")
    }

    /**
     * Get the clean mood state for worker calculations (without UI contamination)
     * This should be used by the worker to get the stable baseline mood
     */
    suspend fun getCleanMoodForWorker(): Int {
        val currentMood = moodStateDao.getMoodForDate(LocalDate.now()).firstOrNull()?.mood ?: 50
        
        Log.d(TAG, "getCleanMoodForWorker: Clean mood for worker: $currentMood")
        return currentMood
    }

    /**
     * Store the last worker update timestamp for UI calculations
     */
    suspend fun storeLastWorkerUpdate(hour: Int, steps: Int, timestamp: Long) {
        val today = LocalDate.now()
        Log.d(TAG, "storeLastWorkerUpdate: Hour: $hour, Steps: $steps, Timestamp: $timestamp")
        
        // Store in UserPreferences for now (could be moved to database later)
        userPreferences.updateLastWorkerUpdate(timestamp)
        userPreferences.updateLastWorkerHour(hour)
        userPreferences.updateLastWorkerSteps(steps)
        
        Log.i(TAG, "storeLastWorkerUpdate: Stored worker update data for UI synchronization")
    }

    /**
     * Get the last worker update timestamp for UI calculations
     */
    suspend fun getLastWorkerUpdateTimestamp(): Long {
        return userPreferences.lastWorkerUpdate.first()
    }

    /**
     * Get the last worker update hour for UI calculations
     */
    suspend fun getLastWorkerUpdateHour(): Int {
        return userPreferences.lastWorkerHour.first()
    }

    /**
     * Get the last worker update steps for UI calculations
     */
    suspend fun getLastWorkerUpdateSteps(): Int {
        return userPreferences.lastWorkerSteps.first()
    }
} 