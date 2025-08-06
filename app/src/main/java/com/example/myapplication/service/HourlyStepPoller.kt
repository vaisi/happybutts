// Created: HourlyStepPoller - Polling-based step tracking system
// Updated: Fixed polling timing issues and improved step capture accuracy
// Purpose: Provides reliable hourly step tracking by polling step count at regular intervals
// Features: Automatic data recovery, missed hour detection, and enhanced accuracy
package com.example.myapplication.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.data.database.HourlyStepsDao
import com.example.myapplication.data.model.HourlySteps

@Singleton
class HourlyStepPoller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepCountRepository: StepCountRepository,
    private val moodRepository: MoodRepository,
    private val hourlyStepsDao: HourlyStepsDao,
    private val unifiedStepCounterService: UnifiedStepCounterService
) {
    private val TAG = "HourlyStepPoller"
    private val pollScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isPolling = false
    private var lastPollTime = LocalDateTime.now()
    private var lastRecordedTotal = 0

    companion object {
        private const val POLL_INTERVAL_MS = 2 * 60 * 1000L // 2 minutes (reduced from 5)
        private const val MAX_MISSED_HOURS = 6 // Maximum hours to backfill
        private const val HOUR_BOUNDARY_BUFFER_MS = 30 * 1000L // 30 seconds before hour boundary
    }

    /**
     * Start polling for step count updates
     */
    fun startPolling() {
        if (isPolling) {
            Log.d(TAG, "Polling already active")
            return
        }

        Log.i(TAG, "=== STARTING HOURLY STEP POLLING ===")
        isPolling = true

        pollScope.launch {
            try {
                // Initialize last recorded total
                initializeLastRecordedTotal()
                
                // Start periodic polling
                while (isPolling) {
                    pollStepCount()
                    delay(POLL_INTERVAL_MS)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in polling loop", e)
            }
        }
    }

    /**
     * Stop polling for step count updates
     */
    fun stopPolling() {
        Log.i(TAG, "=== STOPPING HOURLY STEP POLLING ===")
        isPolling = false
        pollScope.cancel()
    }

    /**
     * Poll current step count and record hourly data
     */
    private suspend fun pollStepCount() {
        try {
            val now = LocalDateTime.now()
            
            // ENHANCED: Get step count from UnifiedStepCounterService first, then fallback to repository
            val currentTotal = try {
                // Try to get from UnifiedStepCounterService (most accurate)
                val serviceSteps = unifiedStepCounterService.currentStepCount
                if (serviceSteps > 0) {
                    Log.d(TAG, "Polling: Using UnifiedStepCounterService steps: $serviceSteps")
                    serviceSteps
                } else {
                    // Fallback to repository
                    val repoSteps = stepCountRepository.getTodayStepCount().first().steps
                    Log.d(TAG, "Polling: Using StepCountRepository steps: $repoSteps")
                    repoSteps
                }
            } catch (e: Exception) {
                Log.w(TAG, "Polling: Error getting steps from services, using repository fallback", e)
                stepCountRepository.getTodayStepCount().first().steps
            }
            
            val currentHour = now.hour

            Log.d(TAG, "Polling step count: Total=$currentTotal, Hour=$currentHour, LastRecorded=$lastRecordedTotal")

            // ENHANCED: Check if we're near hour boundary and force recording
            val isNearHourBoundary = isNearHourBoundary(now)
            if (isNearHourBoundary) {
                Log.i(TAG, "Near hour boundary detected, forcing hourly recording")
            }

            // Check if we need to record data for the current hour
            var recorded = false
            if (shouldRecordHourlyData(currentHour, currentTotal) || isNearHourBoundary) {
                recordHourlyData(currentHour, currentTotal)
                lastRecordedTotal = currentTotal // CRITICAL: Only update after recording
                Log.d(TAG, "Updated lastRecordedTotal to: $lastRecordedTotal after recording hour $currentHour")
                recorded = true
            }

            // Check for missed hours and recover data
            recoverMissedHourlyData(currentTotal)

            lastPollTime = now
            if (!recorded) {
                Log.d(TAG, "No hourly record needed. lastRecordedTotal remains: $lastRecordedTotal")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error polling step count", e)
        }
    }

    /**
     * Check if we're near an hour boundary (within 30 seconds)
     */
    private fun isNearHourBoundary(now: LocalDateTime): Boolean {
        val secondsInMinute = now.second
        val minutesInHour = now.minute
        
        // Check if we're within 30 seconds of the next hour
        return minutesInHour == 59 && secondsInMinute >= 30
    }

    /**
     * Initialize the last recorded total from database
     */
    private suspend fun initializeLastRecordedTotal() {
        try {
            val today = LocalDate.now()
            val lastRecorded = hourlyStepsDao.getLastRecordedTotalForDate(today)
            lastRecordedTotal = lastRecorded ?: 0
            Log.i(TAG, "Initialized last recorded total: $lastRecordedTotal")
            
            // DEBUG: Log the current state of hourly steps
            val hourlySteps = hourlyStepsDao.getHourlyStepsForDate(today).first()
            Log.d(TAG, "Current hourly steps in database: ${hourlySteps.size} records")
            hourlySteps.forEach { step ->
                Log.d(TAG, "Hour ${step.hour}: steps=${step.steps}, lastRecordedTotal=${step.lastRecordedTotal}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing last recorded total", e)
            lastRecordedTotal = 0
        }
    }

    /**
     * Determine if we should record hourly data
     */
    private fun shouldRecordHourlyData(currentHour: Int, currentTotal: Int): Boolean {
        // Record if we have new steps since last recording
        if (currentTotal > lastRecordedTotal) {
            Log.d(TAG, "Recording because currentTotal ($currentTotal) > lastRecordedTotal ($lastRecordedTotal)")
            return true
        }

        // Record if it's a new hour and we haven't recorded for this hour yet
        val today = LocalDate.now()
        val lastRecordedHour = runBlocking { hourlyStepsDao.getLastRecordedHourForDate(today) }
        
        // CRITICAL FIX: Check if we need to record the current hour
        // We should record if:
        // 1. We haven't recorded any hours yet (lastRecordedHour == null)
        // 2. Current hour is greater than the last recorded hour
        // 3. We have steps to record (currentTotal > 0)
        if (lastRecordedHour == null) {
            // First recording of the day
            Log.d(TAG, "Recording because first recording of the day")
            return currentTotal > 0
        } else if (currentHour > lastRecordedHour) {
            // New hour that hasn't been recorded yet
            Log.d(TAG, "Recording because currentHour ($currentHour) > lastRecordedHour ($lastRecordedHour)")
            return true
        }

        Log.d(TAG, "No recording needed: currentTotal=$currentTotal, lastRecordedTotal=$lastRecordedTotal, currentHour=$currentHour, lastRecordedHour=$lastRecordedHour")
        return false
    }

    /**
     * Record hourly step data
     */
    private suspend fun recordHourlyData(hour: Int, currentTotal: Int) {
        try {
            val today = LocalDate.now()
            
            // FIXED: Get the correct baseline from the previous hour
            val baselineTotal = if (hour == 0) {
                0 // First hour of the day
            } else {
                // Get the last_recorded_total from the previous hour
                hourlyStepsDao.getLastRecordedTotalForHour(today, hour - 1) ?: 0
            }
            
            val stepsInHour = currentTotal - baselineTotal

            Log.i(TAG, "=== RECORDING HOURLY DATA ===")
            Log.i(TAG, "Hour: $hour")
            Log.i(TAG, "Current total steps: $currentTotal")
            Log.i(TAG, "Baseline total (from previous hour): $baselineTotal")
            Log.i(TAG, "Steps in this hour: $stepsInHour")
            Log.i(TAG, "Calculation: $currentTotal - $baselineTotal = $stepsInHour")

            // Create or update hourly steps record
            val hourlySteps = HourlySteps(
                date = today,
                hour = hour,
                steps = stepsInHour,
                lastRecordedTotal = currentTotal
            )

            hourlyStepsDao.insertHourlySteps(hourlySteps)

            Log.i(TAG, "Successfully recorded hourly data for hour $hour: $stepsInHour steps")
            Log.i(TAG, "=== HOURLY DATA RECORDING COMPLETED ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error recording hourly data", e)
        }
    }

    /**
     * Recover missed hourly data
     */
    private suspend fun recoverMissedHourlyData(currentTotal: Int) {
        try {
            val today = LocalDate.now()
            val lastRecordedHour = hourlyStepsDao.getLastRecordedHourForDate(today)
            val currentHour = LocalDateTime.now().hour

            if (lastRecordedHour == null) {
                Log.d(TAG, "No hourly data recorded yet for today")
                return
            }

            // Check for missed hours
            val missedHours = calculateMissedHours(lastRecordedHour, currentHour)
            if (missedHours.isNotEmpty()) {
                Log.i(TAG, "Detected missed hours: $missedHours")
                backfillMissedHours(missedHours, currentTotal)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error recovering missed hourly data", e)
        }
    }

    /**
     * Calculate which hours were missed
     */
    private fun calculateMissedHours(lastRecordedHour: Int, currentHour: Int): List<Int> {
        val missedHours = mutableListOf<Int>()
        
        // Handle hour wrapping (e.g., 23 -> 0)
        if (currentHour >= lastRecordedHour) {
            // Normal case: no day wrapping
            for (hour in (lastRecordedHour + 1)..currentHour) {
                if (hour != currentHour) { // Don't include current hour
                    missedHours.add(hour)
                }
            }
        } else {
            // Day wrapped around (e.g., 23 -> 0)
            // Add hours from lastRecordedHour + 1 to 23
            for (hour in (lastRecordedHour + 1)..23) {
                missedHours.add(hour)
            }
            // Add hours from 0 to currentHour
            for (hour in 0..currentHour) {
                if (hour != currentHour) { // Don't include current hour
                    missedHours.add(hour)
                }
            }
        }

        return missedHours.take(MAX_MISSED_HOURS) // Limit backfill
    }

    /**
     * Backfill missed hours with estimated step data
     */
    private suspend fun backfillMissedHours(missedHours: List<Int>, currentTotal: Int) {
        try {
            val today = LocalDate.now()
            val lastRecorded = hourlyStepsDao.getLastRecordedTotalForDate(today) ?: 0
            val totalStepsToDistribute = currentTotal - lastRecorded

            if (totalStepsToDistribute <= 0 || missedHours.isEmpty()) {
                Log.d(TAG, "No steps to distribute or no missed hours")
                return
            }

            // Distribute steps evenly across missed hours
            val stepsPerHour = totalStepsToDistribute / missedHours.size
            val remainingSteps = totalStepsToDistribute % missedHours.size

            Log.i(TAG, "Backfilling $totalStepsToDistribute steps across ${missedHours.size} missed hours")

            var runningTotal = lastRecorded
            for ((index, hour) in missedHours.withIndex()) {
                val stepsForThisHour = stepsPerHour + if (index < remainingSteps) 1 else 0
                runningTotal += stepsForThisHour

                val hourlySteps = HourlySteps(
                    date = today,
                    hour = hour,
                    steps = stepsForThisHour,
                    lastRecordedTotal = runningTotal
                )

                hourlyStepsDao.insertHourlySteps(hourlySteps)
                Log.d(TAG, "Backfilled hour $hour: $stepsForThisHour steps")
            }

            Log.i(TAG, "Successfully backfilled ${missedHours.size} missed hours")

        } catch (e: Exception) {
            Log.e(TAG, "Error backfilling missed hours", e)
        }
    }

    /**
     * Force a poll now (for testing or manual triggers)
     */
    suspend fun forcePoll() {
        Log.i(TAG, "=== FORCING STEP POLL ===")
        pollStepCount()
    }

    /**
     * Get current polling status
     */
    fun isPollingActive(): Boolean = isPolling

    /**
     * Get last recorded total for debugging
     */
    fun getLastRecordedTotal(): Int = lastRecordedTotal
} 