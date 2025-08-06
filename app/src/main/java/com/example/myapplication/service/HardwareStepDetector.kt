package com.example.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HardwareStepDetector @Inject constructor(
    @ApplicationContext private val context: Context
) : StepDetector, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var onStepDetected: ((Int) -> Unit)? = null
    private var initialSteps = -1
    private var lastStepCount = 0
    private var lastTotalSteps = 0
    private var shouldLoadPersistedData = true

    companion object {
        private const val TAG = "HardwareStepDetector"
        private const val PREF_NAME = "step_detector_prefs"
        private const val KEY_INITIAL_STEPS = "initial_steps"
        private const val KEY_LAST_TOTAL_STEPS = "last_total_steps"
        private const val KEY_LAST_DATE = "last_date"
    }

    init {
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        
        Log.d(TAG, "init: Step counter sensor available: ${stepCounterSensor != null}")
        if (stepCounterSensor != null) {
            Log.d(TAG, "init: Step counter sensor name: ${stepCounterSensor?.name}")
            Log.d(TAG, "init: Step counter sensor vendor: ${stepCounterSensor?.vendor}")
            Log.d(TAG, "init: Step counter sensor version: ${stepCounterSensor?.version}")
            Log.d(TAG, "init: Step counter sensor power: ${stepCounterSensor?.power} mA")
        }
        
        Log.d(TAG, "init: Step detector sensor available: ${stepDetectorSensor != null}")
        if (stepDetectorSensor != null) {
            Log.d(TAG, "init: Step detector sensor name: ${stepDetectorSensor?.name}")
            Log.d(TAG, "init: Step detector sensor vendor: ${stepDetectorSensor?.vendor}")
            Log.d(TAG, "init: Step detector sensor version: ${stepDetectorSensor?.version}")
            Log.d(TAG, "init: Step detector sensor power: ${stepDetectorSensor?.power} mA")
        }
    }

    override fun startDetection(onStepDetected: (Int) -> Unit) {
        Log.d(TAG, "startDetection: Starting hardware step detection")
        this.onStepDetected = onStepDetected
        
        // Only load persisted data if we haven't been told to start fresh
        if (shouldLoadPersistedData) {
            loadPersistedSteps()
        } else {
            Log.i(TAG, "startDetection: Skipping persisted data load - starting fresh")
            shouldLoadPersistedData = true // Reset flag for next time
        }
        
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        Log.d(TAG, "startDetection: Step counter sensor: ${stepCounterSensor?.name}")
        Log.d(TAG, "startDetection: Step detector sensor: ${stepDetectorSensor?.name}")

        stepCounterSensor?.let {
            val registered = sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "startDetection: Step counter sensor registered: $registered")
            
            val currentSteps = getCurrentSteps()
            Log.d(TAG, "startDetection: Initial step count: $currentSteps (total: $lastTotalSteps, initial: $initialSteps)")
            onStepDetected?.invoke(currentSteps)
        } ?: Log.e(TAG, "startDetection: No step counter sensor available")

        stepDetectorSensor?.let {
            val registered = sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "startDetection: Step detector sensor registered: $registered")
        } ?: Log.e(TAG, "startDetection: No step detector sensor available")
    }

    override fun stopDetection() {
        Log.d(TAG, "stopDetection: Stopping step detection")
        sensorManager.unregisterListener(this)
        onStepDetected = null
        savePersistedSteps()
        Log.d(TAG, "stopDetection: Step detection stopped")
    }

    override fun isAvailable(): Boolean {
        val available = stepCounterSensor != null || stepDetectorSensor != null
        Log.d(TAG, "isAvailable: Step sensors available: $available")
        return available
    }

    private fun loadPersistedSteps() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        initialSteps = prefs.getInt(KEY_INITIAL_STEPS, -1)
        lastTotalSteps = prefs.getInt(KEY_LAST_TOTAL_STEPS, 0)
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        
        Log.d(TAG, "loadPersistedSteps: Loaded initialSteps=$initialSteps, lastTotalSteps=$lastTotalSteps, lastDate=$lastDate")
        
        // Check if date has changed
        val currentDate = java.time.LocalDate.now().toString()
        if (lastDate != null && lastDate != currentDate) {
            Log.i(TAG, "loadPersistedSteps: Date changed from $lastDate to $currentDate - resetting baseline")
            clearPersistedData()
            shouldLoadPersistedData = false // Prevent reloading on next startDetection
            return
        }
        
        // Save current date
        prefs.edit().putString(KEY_LAST_DATE, currentDate).apply()
    }

    private fun savePersistedSteps() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentDate = java.time.LocalDate.now().toString()
        prefs.edit().apply {
            putInt(KEY_INITIAL_STEPS, initialSteps)
            putInt(KEY_LAST_TOTAL_STEPS, lastTotalSteps)
            putString(KEY_LAST_DATE, currentDate)
            apply()
        }
        Log.d(TAG, "savePersistedSteps: Saved initialSteps=$initialSteps, lastTotalSteps=$lastTotalSteps, date=$currentDate")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values[0].toInt()
                Log.d(TAG, "onSensorChanged: Step counter - Raw total steps: $totalSteps")
                Log.d(TAG, "onSensorChanged: Step counter - Current initialSteps: $initialSteps")
                Log.d(TAG, "onSensorChanged: Step counter - Current lastTotalSteps: $lastTotalSteps")
                
                // Check if sensor has reset (totalSteps is less than lastTotalSteps)
                if (lastTotalSteps > 0 && totalSteps < lastTotalSteps) {
                    Log.w(TAG, "onSensorChanged: Step counter - Sensor reset detected! totalSteps=$totalSteps, lastTotalSteps=$lastTotalSteps")
                    Log.w(TAG, "onSensorChanged: Step counter - Updating initialSteps to new baseline: $totalSteps")
                    initialSteps = totalSteps
                    lastStepCount = 0
                    val todaySteps = 0
                    Log.d(TAG, "onSensorChanged: Step counter - After reset: Today's steps: $todaySteps")
                    onStepDetected?.invoke(todaySteps)
                    lastTotalSteps = totalSteps
                    savePersistedSteps()
                    return
                }
                
                if (initialSteps == -1) {
                    initialSteps = totalSteps
                    Log.d(TAG, "onSensorChanged: Step counter - Initial steps set to $initialSteps")
                    Log.d(TAG, "onSensorChanged: Step counter - This will be the new daily baseline")
                }
                
                val todaySteps = totalSteps - initialSteps
                lastTotalSteps = totalSteps
                Log.d(TAG, "onSensorChanged: Step counter - Total steps: $totalSteps, Today's steps: $todaySteps")
                Log.d(TAG, "onSensorChanged: Step counter - Calculation: $totalSteps - $initialSteps = $todaySteps")
                onStepDetected?.invoke(todaySteps)
                lastStepCount = todaySteps
                savePersistedSteps()
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                lastStepCount++
                Log.d(TAG, "onSensorChanged: Step detector - Step detected, total: $lastStepCount")
                onStepDetected?.invoke(lastStepCount)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged: Sensor: ${sensor?.name}, Accuracy: $accuracy")
    }

    fun getCurrentSteps(): Int {
        stepCounterSensor?.let {
            if (lastTotalSteps > 0 && initialSteps != -1) {
                // Check if sensor has reset (this can happen if the sensor reading is much lower than expected)
                if (lastTotalSteps < initialSteps) {
                    Log.w(TAG, "getCurrentSteps: Sensor reset detected in getCurrentSteps! lastTotalSteps=$lastTotalSteps, initialSteps=$initialSteps")
                    Log.w(TAG, "getCurrentSteps: Updating initialSteps to current total: $lastTotalSteps")
                    initialSteps = lastTotalSteps
                    savePersistedSteps()
                    return 0
                }
                
                val currentSteps = lastTotalSteps - initialSteps
                Log.d(TAG, "getCurrentSteps: Returning calculated steps: $currentSteps (total: $lastTotalSteps, initial: $initialSteps)")
                Log.d(TAG, "getCurrentSteps: Calculation: $lastTotalSteps - $initialSteps = $currentSteps")
                return currentSteps
            }
        }
        Log.d(TAG, "getCurrentSteps: Returning last step count: $lastStepCount")
        return lastStepCount
    }

    override fun resetDailyBaseline() {
        Log.i(TAG, "=== HARDWARE STEP DETECTOR BASELINE RESET STARTED ===")
        Log.i(TAG, "resetDailyBaseline: Before reset - initialSteps: $initialSteps, lastTotalSteps: $lastTotalSteps")
        
        stepCounterSensor?.let {
            // Clear the persisted values to force reinitialization
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.i(TAG, "resetDailyBaseline: Cleared persisted values")
            
            // Reset the in-memory values
            initialSteps = -1
            lastTotalSteps = 0
            lastStepCount = 0
            
            Log.i(TAG, "resetDailyBaseline: Reset in-memory values - initialSteps: $initialSteps, lastTotalSteps: $lastTotalSteps")
            Log.i(TAG, "resetDailyBaseline: Next sensor reading will set new baseline")
        } ?: run {
            Log.w(TAG, "resetDailyBaseline: No step counter sensor available")
        }
        
        Log.i(TAG, "=== HARDWARE STEP DETECTOR BASELINE RESET COMPLETED ===")
    }

    // Add this method to clear all persisted data when needed
    fun clearPersistedData() {
        Log.i(TAG, "clearPersistedData: Clearing all persisted step data")
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // Reset in-memory values
        initialSteps = -1
        lastTotalSteps = 0
        lastStepCount = 0
        
        // Set flag to prevent loading persisted data on next startDetection
        shouldLoadPersistedData = false
        
        Log.i(TAG, "clearPersistedData: All step data cleared, will start fresh on next detection")
    }
}