// Created: SimpleHourlyAggregator - Simple and reliable hourly step aggregation
// Purpose: Provides offline-friendly hourly step tracking with data validation
// Features: Automatic data recovery, validation, and enhanced reliability
// Fixed: Proper calculation of last_recorded_total to prevent zero values
package com.example.myapplication.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.data.database.HourlyStepsDao
import com.example.myapplication.data.model.HourlySteps

@Singleton
class SimpleHourlyAggregator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepCountRepository: StepCountRepository,
    private val hourlyStepsDao: HourlyStepsDao
) {
    private val TAG = "SimpleHourlyAggregator"
    private val aggregatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isAggregating = false
    private var lastRecordedHour = -1

    companion object {
        private const val AGGREGATION_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val MAX_STEPS_PER_HOUR = 10000 // Reasonable maximum
    }

    /**
     * Start hourly aggregation
     */
    fun startAggregation() {
        if (isAggregating) {
            Log.d(TAG, "Aggregation already active")
            return
        }

        Log.i(TAG, "=== STARTING SIMPLE HOURLY AGGREGATION ===")
        isAggregating = true

        aggregatorScope.launch {
            try {
                // Initialize last recorded hour
                initializeLastRecordedHour()
                
                // Start periodic aggregation
                while (isAggregating) {
                    try {
                        aggregateCurrentHour()
                        delay(AGGREGATION_INTERVAL_MS)
                    } catch (e: CancellationException) {
                        Log.d(TAG, "Aggregation cancelled normally")
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in aggregation loop", e)
                        // Continue aggregation even if there's an error
                        delay(10000) // Wait 10 seconds before retrying
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Aggregation job cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in aggregation loop", e)
            }
        }
    }

    /**
     * Stop hourly aggregation
     */
    fun stopAggregation() {
        Log.i(TAG, "=== STOPPING SIMPLE HOURLY AGGREGATION ===")
        isAggregating = false
        
        // Cancel the aggregator scope
        try {
            aggregatorScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling aggregator scope", e)
        }
    }

    /**
     * Initialize the last recorded hour from database
     */
    private suspend fun initializeLastRecordedHour() {
        try {
            val today = LocalDate.now()
            val lastRecorded = hourlyStepsDao.getLastRecordedHourForDate(today)
            lastRecordedHour = lastRecorded ?: -1
            Log.i(TAG, "Initialized last recorded hour: $lastRecordedHour")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing last recorded hour", e)
            lastRecordedHour = -1
        }
    }

    /**
     * Aggregate steps for the current hour
     */
    private suspend fun aggregateCurrentHour() {
        try {
            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            val currentHour = now.hour
            val currentTotal = stepCountRepository.getTodayStepCount().firstOrNull()?.steps ?: 0

            Log.d(TAG, "Aggregating hour $currentHour with total steps: $currentTotal")

            // Check if we need to record this hour
            if (shouldRecordHour(currentHour, currentTotal)) {
                recordHourlySteps(currentHour, currentTotal)
                lastRecordedHour = currentHour
                Log.d(TAG, "Updated lastRecordedHour to: $lastRecordedHour")
            } else {
                Log.d(TAG, "No recording needed for hour $currentHour")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error aggregating current hour", e)
        }
    }

    /**
     * Determine if we should record the current hour
     */
    private fun shouldRecordHour(currentHour: Int, currentTotal: Int): Boolean {
        // Record if it's a new hour
        if (currentHour > lastRecordedHour) {
            Log.d(TAG, "Recording because currentHour ($currentHour) > lastRecordedHour ($lastRecordedHour)")
            return true
        }

        // Record if we have steps and haven't recorded this hour yet
        if (currentTotal > 0 && lastRecordedHour == -1) {
            Log.d(TAG, "Recording because first recording of the day with steps")
            return true
        }

        return false
    }

    /**
     * FIXED: Record hourly step data with proper last_recorded_total calculation
     */
    private suspend fun recordHourlySteps(hour: Int, currentTotal: Int) {
        try {
            val today = LocalDate.now()
            
            // FIXED: Get the baseline from the previous hour's last_recorded_total
            val baselineTotal = if (hour == 0) {
                0 // First hour of the day
            } else {
                val previousHourRecord = hourlyStepsDao.getHourlyStepsForHour(today, hour - 1)
                previousHourRecord?.lastRecordedTotal ?: 0
            }
            
            val stepsInHour = currentTotal - baselineTotal

            // Validate the calculation
            val validStepsInHour = when {
                stepsInHour < 0 -> {
                    Log.w(TAG, "Invalid step calculation: $currentTotal - $baselineTotal = $stepsInHour, using 0")
                    0
                }
                stepsInHour > MAX_STEPS_PER_HOUR -> {
                    Log.w(TAG, "Unrealistic steps in hour: $stepsInHour, capping at $MAX_STEPS_PER_HOUR")
                    MAX_STEPS_PER_HOUR
                }
                else -> stepsInHour
            }

            Log.i(TAG, "=== RECORDING HOURLY DATA ===")
            Log.i(TAG, "Hour: $hour")
            Log.i(TAG, "Current total steps: $currentTotal")
            Log.i(TAG, "Baseline total (from previous hour): $baselineTotal")
            Log.i(TAG, "Steps in this hour: $validStepsInHour")
            Log.i(TAG, "Calculation: $currentTotal - $baselineTotal = $validStepsInHour")

            // FIXED: Create or update hourly steps record with proper last_recorded_total
            val hourlySteps = HourlySteps(
                date = today,
                hour = hour,
                steps = validStepsInHour,
                lastRecordedTotal = currentTotal // Always use current total as last_recorded_total
            )

            hourlyStepsDao.insertHourlySteps(hourlySteps)

            Log.i(TAG, "Successfully recorded hourly data for hour $hour: $validStepsInHour steps, last_recorded_total: $currentTotal")
            Log.i(TAG, "=== HOURLY DATA RECORDING COMPLETED ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error recording hourly data", e)
        }
    }

    /**
     * Get total steps for a date (works offline)
     */
    suspend fun getTotalStepsForDate(date: LocalDate): Int {
        return try {
            hourlyStepsDao.getTotalStepsForDate(date).firstOrNull() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total steps for date $date", e)
            0
        }
    }

    /**
     * FIXED: Recover missing hourly data with proper last_recorded_total calculation
     */
    suspend fun recoverMissingHourlyData() {
        try {
            val today = LocalDate.now()
            val currentHour = LocalDateTime.now().hour
            val currentTotal = stepCountRepository.getTodayStepCount().firstOrNull()?.steps ?: 0
            
            // Get recorded hours
            val recordedHours = hourlyStepsDao.getHourlyStepsForDate(today).firstOrNull()?.map { hourlyStep -> hourlyStep.hour } ?: emptyList()
            
            // Find missing hours
            val missingHours = (0 until currentHour).filter { hour -> hour !in recordedHours }
            
            if (missingHours.isNotEmpty()) {
                Log.i(TAG, "Recovering data for missing hours: $missingHours")
                backfillMissingHours(missingHours, currentTotal)
            } else {
                Log.d(TAG, "No missing hours to recover")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recovering missing hourly data", e)
        }
    }

    /**
     * FIXED: Backfill missing hours with proper last_recorded_total calculation
     */
    private suspend fun backfillMissingHours(missingHours: List<Int>, currentTotal: Int) {
        try {
            val today = LocalDate.now()
            
            // FIXED: Get the last recorded total from the database
            val lastRecorded = hourlyStepsDao.getLastRecordedTotalForDate(today) ?: 0
            val totalStepsToDistribute = currentTotal - lastRecorded

            if (totalStepsToDistribute <= 0 || missingHours.isEmpty()) {
                Log.d(TAG, "No steps to distribute or no missing hours")
                return
            }

            // Distribute steps evenly across missing hours
            val stepsPerHour = totalStepsToDistribute / missingHours.size
            val remainingSteps = totalStepsToDistribute % missingHours.size

            Log.i(TAG, "Backfilling $totalStepsToDistribute steps across ${missingHours.size} missed hours")

            var runningTotal = lastRecorded
            for ((index, hour) in missingHours.withIndex()) {
                val stepsForThisHour = stepsPerHour + if (index < remainingSteps) 1 else 0
                runningTotal += stepsForThisHour

                val hourlySteps = HourlySteps(
                    date = today,
                    hour = hour,
                    steps = stepsForThisHour,
                    lastRecordedTotal = runningTotal // FIXED: Use running total as last_recorded_total
                )

                hourlyStepsDao.insertHourlySteps(hourlySteps)
                Log.d(TAG, "Backfilled hour $hour: $stepsForThisHour steps, running total: $runningTotal")
            }

            Log.i(TAG, "Successfully backfilled ${missingHours.size} missed hours")

        } catch (e: Exception) {
            Log.e(TAG, "Error backfilling missing hours", e)
        }
    }

    /**
     * Force aggregation now (for testing or manual triggers)
     */
    suspend fun forceAggregation() {
        Log.i(TAG, "=== FORCING HOURLY AGGREGATION ===")
        aggregateCurrentHour()
    }

    /**
     * Get current aggregation status
     */
    fun isAggregatingActive(): Boolean = isAggregating

    /**
     * Get last recorded hour for debugging
     */
    fun getLastRecordedHour(): Int = lastRecordedHour
}
