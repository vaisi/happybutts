// Updated: Added daily reset functionality and improved logging
package com.example.myapplication.service

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis
import kotlin.math.max
import com.example.myapplication.data.repository.InactivityRepository
import com.example.myapplication.data.repository.StepCountRepository

@Singleton
class UnifiedStepCounterService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hardwareStepDetector: HardwareStepDetector,
    private val accelerometerStepDetector: AccelerometerStepDetector,
    private val stepCountRepository: StepCountRepository,
    private val inactivityRepository: InactivityRepository
) {
    private val TAG = "StepSource"
    private var activeDetector: StepDetector? = null
    private var onStepDetected: ((Int) -> Unit)? = null
    private var currentSteps = 0
    
    // Getter for current steps to allow external access
    val currentStepCount: Int
        get() = currentSteps
    private var lastEmittedSource: String = ""
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastEmitTime = AtomicLong(0)
    private val EMIT_INTERVAL_MS = 10_000L // 10 seconds
    private var lastProcessedDate = LocalDate.now()
    private val _stepUpdates = MutableSharedFlow<Int>(replay = 1)
    val stepUpdates = _stepUpdates.asSharedFlow()
    private var lastEmittedStepCount = 0
    private var lastEmitTimestamp = 0L
    private var lastStepCount = 0
    private var isInactivityTracking = false

    companion object {
        private const val EMIT_STEP_THRESHOLD = 1
        private const val EMIT_TIME_THRESHOLD_MS = 5_000L // 5 seconds
        private const val MAX_BACKFILL_DAYS = 30
        private const val INACTIVITY_THRESHOLD_STEPS = 50 // Consider inactive if less than 50 steps in an hour
    }

    private val healthConnectClient by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create HealthConnectClient", e)
            null
        }
    }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun startStepCounting(onStepDetected: (Int) -> Unit) {
        Log.i(TAG, "=== START STEP COUNTING ===")
        Log.d(TAG, "startStepCounting: Starting step counting")
        this.onStepDetected = onStepDetected
        val today = LocalDate.now()
        
        Log.d(TAG, "startStepCounting: Today: $today, lastProcessedDate: $lastProcessedDate")
        
        // Backfill missed days if needed
        if (today != lastProcessedDate) {
            Log.i(TAG, "Date changed from $lastProcessedDate to $today, backfilling missed days")
            serviceScope.launch {
                backfillMissedDays(lastProcessedDate)
            }
            currentSteps = 0
            lastProcessedDate = today
        }

        // First, try to get steps from hardware detector if available
        if (hardwareStepDetector.isAvailable()) {
            Log.i(TAG, "startStepCounting: Hardware detector available, using it")
            selectBestDetector()
            // Add a small delay before emitting the initial step count
            serviceScope.launch {
                delay(500) // 0.5 second delay
                val initialSteps = hardwareStepDetector.getCurrentSteps()
                Log.i(TAG, "startStepCounting: Emitting delayed initial hardware steps: $initialSteps")
                currentSteps = initialSteps
                lastEmittedStepCount = initialSteps
                lastEmitTimestamp = System.currentTimeMillis()
                _stepUpdates.emit(initialSteps)
                onStepDetected?.invoke(initialSteps)
                
                // Start inactivity tracking
                startInactivityTracking(initialSteps)
            }
            return
        }

        // If no hardware detector, fall back to Health Connect
        Log.i(TAG, "startStepCounting: No hardware detector, using Health Connect")
        serviceScope.launch {
            try {
                // Query the latest available step count from Health Connect
                val latestSteps = getLatestStepCount()
                currentSteps = latestSteps
                Log.i(TAG, "startStepCounting: Emitting Health Connect steps: $latestSteps")
                lastEmittedStepCount = latestSteps
                lastEmitTimestamp = System.currentTimeMillis()
                _stepUpdates.emit(latestSteps)
                onStepDetected?.invoke(latestSteps)
                
                // Start inactivity tracking
                startInactivityTracking(latestSteps)
            } catch (e: Exception) {
                Log.e(TAG, "startStepCounting: Error getting Health Connect steps", e)
            }
        }

        // Start periodic Health Connect updates if no hardware detector is available
        if (activeDetector == null) {
            Log.i(TAG, "startStepCounting: No hardware detector available, starting Health Connect updates")
            startHealthConnectUpdates()
        } else {
            Log.i(TAG, "startStepCounting: Using ${lastEmittedSource} detector")
        }
        
        Log.i(TAG, "=== START STEP COUNTING COMPLETED ===")
    }

    private fun startInactivityTracking(initialSteps: Int) {
        if (!isInactivityTracking) {
            isInactivityTracking = true
            serviceScope.launch {
                inactivityRepository.startInactivityPeriod(initialSteps)
                Log.i(TAG, "Started inactivity tracking with $initialSteps steps")
            }
        }
    }

    private fun startHealthConnectUpdates() {
        serviceScope.launch {
            try {
                getStepsForToday().collect { steps ->
                    currentSteps = steps
                    Log.i(TAG, "Health Connect update: Emitting steps: $steps")
                    onStepDetected?.invoke(steps)
                    _stepUpdates.emit(steps)
                    Log.d(TAG, "Emitted steps to SharedFlow: $steps")
                    
                    // Update inactivity tracking
                    updateInactivityTracking(steps)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Health Connect updates", e)
            }
        }
    }

    fun stopStepCounting() {
        Log.d(TAG, "stopStepCounting: Stopping step counting")
        activeDetector?.stopDetection()
        activeDetector = null
        onStepDetected = null
        
        // End inactivity tracking
        serviceScope.launch {
            inactivityRepository.endInactivityPeriod(currentSteps)
            isInactivityTracking = false
            Log.i(TAG, "Stopped inactivity tracking")
        }
    }

    /**
     * Selects the best available detector and starts detection. Logs the source.
     */
    private fun selectBestDetector() {
        Log.i(TAG, "=== SELECTING BEST DETECTOR ===")
        
        // Try hardware step detector first
        if (hardwareStepDetector.isAvailable()) {
            Log.i(TAG, "selectBestDetector: Hardware step detector available")
            activeDetector = hardwareStepDetector
            lastEmittedSource = "Hardware"
            activeDetector?.startDetection { steps ->
                Log.d(TAG, "selectBestDetector: Hardware step update: $steps")
                processStepUpdate(steps)
            }
            return
        }
        
        // Try accelerometer step detector
        if (accelerometerStepDetector.isAvailable()) {
            Log.i(TAG, "selectBestDetector: Accelerometer step detector available")
            activeDetector = accelerometerStepDetector
            lastEmittedSource = "Accelerometer"
            activeDetector?.startDetection { steps ->
                Log.d(TAG, "selectBestDetector: Accelerometer step update: $steps")
                processStepUpdate(steps)
            }
            return
        }
        
        Log.i(TAG, "selectBestDetector: No hardware detectors available, will use Health Connect")
        activeDetector = null
        lastEmittedSource = "Health Connect"
    }

    private fun processStepUpdate(steps: Int) {
        Log.d(TAG, "processStepUpdate: Received step update: $steps from $lastEmittedSource")
        
        // Check for date change
        val currentDate = LocalDate.now()
        if (currentDate != lastProcessedDate) {
            Log.i(TAG, "processStepUpdate: Date changed from $lastProcessedDate to $currentDate, resetting baseline")
            lastProcessedDate = currentDate
            currentSteps = 0
            lastEmittedStepCount = 0
            lastEmitTimestamp = 0
            
            // Reset inactivity tracking for new day
            serviceScope.launch {
                inactivityRepository.endInactivityPeriod(lastStepCount)
                inactivityRepository.startInactivityPeriod(0)
                Log.i(TAG, "Reset inactivity tracking for new day")
            }
        }
        
        Log.d(TAG, "processStepUpdate: Updating currentSteps from $currentSteps to $steps")
        currentSteps = steps
        
        // CRITICAL FIX: Update StepCountRepository to ensure data consistency
        serviceScope.launch {
            try {
                stepCountRepository.updateTodaySteps(steps)
                Log.i(TAG, "processStepUpdate: Updated StepCountRepository with $steps steps")
            } catch (e: Exception) {
                Log.e(TAG, "processStepUpdate: Error updating StepCountRepository", e)
            }
        }
        
        // Update inactivity tracking
        updateInactivityTracking(steps)
        
        val now = System.currentTimeMillis()
        val stepDelta = kotlin.math.abs(steps - lastEmittedStepCount)
        val timeDelta = now - lastEmitTimestamp
        val shouldEmit = stepDelta >= EMIT_STEP_THRESHOLD || timeDelta >= EMIT_TIME_THRESHOLD_MS
        Log.d(TAG, "processStepUpdate: stepDelta=$stepDelta, timeDelta=$timeDelta, shouldEmit=$shouldEmit")
        
        if (shouldEmit) {
            lastEmittedStepCount = steps
            lastEmitTimestamp = now
            Log.i(TAG, "processStepUpdate: Step update from $lastEmittedSource: $steps (emitted)")
            onStepDetected?.invoke(steps)
            serviceScope.launch { 
                Log.d(TAG, "processStepUpdate: Emitting to SharedFlow: $steps")
                _stepUpdates.emit(steps) 
            }
            Log.d(TAG, "processStepUpdate: Emitted steps to SharedFlow: $steps")
        } else {
            Log.d(TAG, "processStepUpdate: Step update throttled: $steps (not emitted)")
        }
    }

    private fun updateInactivityTracking(steps: Int) {
        val stepDelta = steps - lastStepCount
        
        serviceScope.launch {
            if (stepDelta > INACTIVITY_THRESHOLD_STEPS) {
                // User is active, end current inactivity period
                inactivityRepository.endInactivityPeriod(steps)
                // Start new inactivity period
                inactivityRepository.startInactivityPeriod(steps)
                Log.d(TAG, "User active: ended inactivity period, started new one")
            }
            // If stepDelta is small, inactivity period continues
        }
        
        lastStepCount = steps
    }

    /**
     * Emits only the best available step count. If hardware/accelerometer is available, emits that value only.
     * Only falls back to Health Connect if no hardware/accelerometer is available.
     */
    fun getStepsForToday(): Flow<Int> = flow {
        try {
            if (activeDetector != null) {
                Log.i(TAG, "getStepsForToday: Emitting hardware/accelerometer value: $currentSteps")
                emit(currentSteps)
                return@flow
            }
            
            healthConnectClient?.let { client ->
                val today = LocalDate.now()
                val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
                val response = client.readRecords(request)
                val totalSteps = response.records.sumOf { it.count.toInt() }
                Log.i(TAG, "getStepsForToday: Emitting Health Connect value: $totalSteps")
                emit(totalSteps)
            } ?: run {
                Log.i(TAG, "getStepsForToday: No available step source, emitting 0")
                emit(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting steps", e)
            emit(0)
        }
    }

    suspend fun hasRequiredPermissions(): Boolean {
        return try {
            healthConnectClient?.let { client ->
                client.permissionController.getGrantedPermissions().containsAll(requiredPermissions)
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }

    // Helper to get the latest available step count
    private suspend fun getLatestStepCount(): Int {
        // Prefer hardware detector if available
        if (hardwareStepDetector.isAvailable()) {
            val steps = hardwareStepDetector.getCurrentSteps()
            Log.d(TAG, "getLatestStepCount: Hardware steps = $steps")
            return steps
        }
        // Fallback to Health Connect
        healthConnectClient?.let { client ->
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            val response = client.readRecords(request)
            val totalSteps = response.records.sumOf { it.count.toInt() }
            Log.d(TAG, "getLatestStepCount: Health Connect steps = $totalSteps")
            return totalSteps
        }
        Log.d(TAG, "getLatestStepCount: No available step source, returning 0")
        return 0
    }

    fun resetDailyBaseline() {
        Log.i(TAG, "=== UNIFIED STEP COUNTER SERVICE BASELINE RESET STARTED ===")
        Log.i(TAG, "resetDailyBaseline: Current steps before reset: $currentSteps")
        
        // Reset the current steps to 0
        currentSteps = 0
        lastEmittedStepCount = 0
        
        // Reset all detectors
        hardwareStepDetector.resetDailyBaseline()
        accelerometerStepDetector.resetDailyBaseline()
        
        // Force emit 0 steps to ensure UI is updated immediately
        Log.i(TAG, "resetDailyBaseline: Force emitting 0 steps to SharedFlow")
        serviceScope.launch { 
            _stepUpdates.emit(0)
            Log.d(TAG, "resetDailyBaseline: Emitted 0 steps to SharedFlow")
        }
        
        Log.i(TAG, "resetDailyBaseline: Current steps after reset: $currentSteps")
        Log.i(TAG, "=== UNIFIED STEP COUNTER SERVICE BASELINE RESET COMPLETED ===")
    }

    /**
     * Clears all persisted data from step detectors to force new baseline establishment
     */
    fun clearStepDetectorData() {
        Log.i(TAG, "=== CLEARING STEP DETECTOR PERSISTED DATA ===")
        
        // Clear data from both detectors
        hardwareStepDetector.clearPersistedData()
        accelerometerStepDetector.clearPersistedData()
        
        // Reset service state
        currentSteps = 0
        lastEmittedStepCount = 0
        lastEmitTimestamp = 0
        
        Log.i(TAG, "=== STEP DETECTOR PERSISTED DATA CLEARED ===")
    }

    /**
     * Gets steps for a specific date from Health Connect
     */
    suspend fun getStepsForDate(date: LocalDate): Int {
        return try {
            healthConnectClient?.let { client ->
                val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
                val response = client.readRecords(request)
                val totalSteps = response.records.sumOf { it.count.toInt() }
                Log.i(TAG, "getStepsForDate: Got $totalSteps steps for $date")
                totalSteps
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting steps for date $date", e)
            0
        }
    }

    /**
     * Backfills step data for missed days, limited to MAX_BACKFILL_DAYS
     */
    suspend fun backfillMissedDays(lastProcessedDate: LocalDate) {
        val today = LocalDate.now()
        if (lastProcessedDate >= today) return

        // Calculate the start date for backfilling
        val startDate = if (lastProcessedDate.plusDays(1).isAfter(today.minusDays(MAX_BACKFILL_DAYS.toLong()))) {
            lastProcessedDate.plusDays(1)
        } else {
            today.minusDays(MAX_BACKFILL_DAYS.toLong())
        }
        
        Log.i(TAG, "Backfilling steps from $startDate to ${today.minusDays(1)}")
        
        var currentDate = startDate
        while (!currentDate.isAfter(today.minusDays(1))) {
            val steps = getStepsForDate(currentDate)
            Log.i(TAG, "Backfilled $steps steps for $currentDate")
            _stepUpdates.emit(steps)
            currentDate = currentDate.plusDays(1)
        }
    }

    /**
     * Called by the ServiceHealthCheckWorker to recover missed step data if the service was down.
     */
    suspend fun attemptStepDataRecovery() {
        try {
            val today = LocalDate.now()
            val recoveredSteps = getLatestStepCount()
            Log.i(TAG, "attemptStepDataRecovery: Recovered $recoveredSteps steps for today")
            stepCountRepository.updateTodaySteps(recoveredSteps)
            // Optionally emit to SharedFlow to update UI
            _stepUpdates.emit(recoveredSteps)
        } catch (e: Exception) {
            Log.e(TAG, "attemptStepDataRecovery: Error recovering step data", e)
        }
    }
} 