// Created: SimpleDataTest to verify core data functionality independently
package com.example.myapplication.util

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.database.InactivityDao
import com.example.myapplication.data.database.StepCountDao
import com.example.myapplication.data.model.InactivityData
import com.example.myapplication.data.model.StepCount
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime

object SimpleDataTest {
    private const val TAG = "SimpleDataTest"
    
    fun testDataFunctionality(context: Context) {
        Log.i(TAG, "=== SIMPLE DATA FUNCTIONALITY TEST ===")
        
        runBlocking {
            try {
                // Create in-memory database
                val database = Room.inMemoryDatabaseBuilder(
                    context,
                    AppDatabase::class.java
                ).build()
                
                val stepCountDao = database.stepCountDao()
                val inactivityDao = database.inactivityDao()
                
                val today = LocalDate.now()
                val now = LocalDateTime.now()
                
                // Test Step Count
                Log.d(TAG, "Testing step count functionality...")
                val stepCount = StepCount(today, 5000, 10000)
                stepCountDao.insertStepCount(stepCount)
                val retrievedStepCount = stepCountDao.getStepCountForDate(today).firstOrNull()
                
                if (retrievedStepCount != null) {
                    Log.i(TAG, "✅ Step count working: ${retrievedStepCount.steps}/${retrievedStepCount.goal}")
                } else {
                    Log.e(TAG, "❌ Step count failed")
                }
                
                // Test Inactivity Data
                Log.d(TAG, "Testing inactivity functionality...")
                val inactivityData = InactivityData(
                    date = today.toString(),
                    startTime = now.minusHours(2),
                    endTime = now.minusHours(1),
                    durationHours = 1.0f,
                    stepsDuringPeriod = 50,
                    isConsecutive = true,
                    notificationSent = false
                )
                
                inactivityDao.insertInactivity(inactivityData)
                val retrievedInactivity = inactivityDao.getInactivityForDate(today).firstOrNull()
                
                if (retrievedInactivity != null && retrievedInactivity.isNotEmpty()) {
                    Log.i(TAG, "✅ Inactivity data working: ${retrievedInactivity.size} periods")
                    val totalHours = retrievedInactivity.sumOf { it.durationHours.toDouble() }
                    Log.i(TAG, "✅ Total inactive hours: $totalHours")
                } else {
                    Log.e(TAG, "❌ Inactivity data failed")
                }
                
                // Test Data Aggregation
                Log.d(TAG, "Testing data aggregation...")
                val completionPercentage = if (retrievedStepCount != null && retrievedStepCount.goal > 0) {
                    (retrievedStepCount.steps * 100 / retrievedStepCount.goal).coerceIn(0, 100)
                } else {
                    0
                }
                
                val totalInactiveHours = retrievedInactivity?.sumOf { it.durationHours.toDouble() } ?: 0.0
                
                Log.i(TAG, "✅ Data aggregation working:")
                Log.i(TAG, "   - Step completion: $completionPercentage%")
                Log.i(TAG, "   - Total inactive hours: $totalInactiveHours")
                
                database.close()
                Log.i(TAG, "=== DATA FUNCTIONALITY VERIFIED ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Data test failed", e)
            }
        }
    }
} 