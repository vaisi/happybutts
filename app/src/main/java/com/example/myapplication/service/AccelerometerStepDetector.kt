package com.example.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.sqrt

class AccelerometerStepDetector @Inject constructor(
    @ApplicationContext private val context: Context
) : StepDetector, SensorEventListener {

    companion object {
        private const val TAG = "AccelerometerStepDetector"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var onStepDetected: ((Int) -> Unit)? = null

    // Step detection algorithm parameters
    private var stepCount = 0
    private val stepThreshold = 10.5f
    private val stepTimeoutNs = 250_000_000L // 250ms
    private var lastStepTimeNs = 0L

    // Low-pass filter
    private val alpha = 0.8f
    private val gravity = FloatArray(3)

    // Peak detection
    private var lastMagnitude = 0f
    private var lastDiff = 0f
    private var peakCount = 0

    override fun startDetection(onStepDetected: (Int) -> Unit) {
        Log.d(TAG, "startDetection: Starting accelerometer step detection")
        this.onStepDetected = onStepDetected
        stepCount = 0

        accelerometer?.let {
            val registered = sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "startDetection: Accelerometer sensor registered: $registered")
        } ?: Log.e(TAG, "startDetection: No accelerometer sensor available")
    }

    override fun stopDetection() {
        sensorManager.unregisterListener(this)
        onStepDetected = null
    }

    override fun isAvailable(): Boolean {
        return accelerometer != null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Apply low-pass filter to remove gravity
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        // Calculate linear acceleration
        val linearAcceleration = FloatArray(3)
        linearAcceleration[0] = event.values[0] - gravity[0]
        linearAcceleration[1] = event.values[1] - gravity[1]
        linearAcceleration[2] = event.values[2] - gravity[2]

        // Calculate magnitude
        val magnitude = sqrt(
            linearAcceleration[0] * linearAcceleration[0] +
                    linearAcceleration[1] * linearAcceleration[1] +
                    linearAcceleration[2] * linearAcceleration[2]
        )

        // Detect peaks
        val diff = magnitude - lastMagnitude

        if (lastDiff > 0 && diff <= 0 && lastMagnitude > stepThreshold) {
            val currentTimeNs = event.timestamp
            if (currentTimeNs - lastStepTimeNs > stepTimeoutNs) {
                stepCount++
                Log.d(TAG, "onSensorChanged: Step detected via accelerometer, total: $stepCount")
                onStepDetected?.invoke(stepCount)
                lastStepTimeNs = currentTimeNs
            }
        }

        lastMagnitude = magnitude
        lastDiff = diff
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    override fun resetDailyBaseline() {
        // No-op: Accelerometer step detection is session-based, not cumulative.
    }

    fun clearPersistedData() {
        Log.i(TAG, "clearPersistedData: Clearing accelerometer step data")
        stepCount = 0
        lastStepTimeNs = 0L
        lastMagnitude = 0f
        lastDiff = 0f
        peakCount = 0
        Log.i(TAG, "clearPersistedData: Accelerometer step data cleared")
    }
}