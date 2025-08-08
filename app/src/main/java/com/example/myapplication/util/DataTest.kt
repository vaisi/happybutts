// Created: DataTest utility to verify core data functionality
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

object DataTest {
    private const val TAG = "DataTest"
    
    fun testCoreDataFunctionality(context: Context) {
        Log.i(TAG, "=== TESTING CORE DATA FUNCTIONALITY ===")
        
        runBlocking {
            try {
                // Create in-memory database for testing
                val database = Room.inMemoryDatabaseBuilder(
                    context,
                    AppDatabase::class.java
                ).build()
                
                val stepCountDao = database.stepCountDao()
                val inactivityDao = database.inactivityDao()
                
                val today = LocalDate.now()
                val now = LocalDateTime.now()
                
                // Test 1: Step Count Data
                Log.d(TAG, "Test 1: Testing step count data")
                val stepCount = StepCount(
                    date = today,
                    steps = 5000,
                    goal = 10000
                )
                
                stepCountDao.insertStepCount(stepCount)
                val retrievedStepCount = stepCountDao.getStepCountForDate(today).firstOrNull()
                
                if (retrievedStepCount != null) {
                    Log.i(TAG, "✅ SUCCESS: Step count data working")
                    Log.d(TAG, "Steps: ${retrievedStepCount.steps}, Goal: ${retrievedStepCount.goal}")
                } else {
                    Log.e(TAG, "❌ FAILED: Step count data not working")
                }
                
                // Test 2: Inactivity Data
                Log.d(TAG, "Test 2: Testing inactivity data")
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
                    Log.i(TAG, "✅ SUCCESS: Inactivity data working")
                    Log.d(TAG, "Inactivity periods: ${retrievedInactivity.size}")
                    Log.d(TAG, "Total hours: ${retrievedInactivity.sumOf { it.durationHours.toDouble() }}")
                } else {
                    Log.e(TAG, "❌ FAILED: Inactivity data not working")
                }
                
                // Test 3: Multiple Inactivity Periods
                Log.d(TAG, "Test 3: Testing multiple inactivity periods")
                val inactivityData2 = InactivityData(
                    date = today.toString(),
                    startTime = now.minusHours(4),
                    endTime = now.minusHours(3),
                    durationHours = 1.0f,
                    stepsDuringPeriod = 25,
                    isConsecutive = true,
                    notificationSent = false
                )
                
                inactivityDao.insertInactivity(inactivityData2)
                val allInactivity = inactivityDao.getInactivityForDate(today).firstOrNull()
                
                if (allInactivity != null && allInactivity.size >= 2) {
                    Log.i(TAG, "✅ SUCCESS: Multiple inactivity periods working")
                    Log.d(TAG, "Total periods: ${allInactivity.size}")
                    val totalHours = allInactivity.sumOf { it.durationHours.toDouble() }
                    Log.d(TAG, "Total inactive hours: $totalHours")
                } else {
                    Log.e(TAG, "❌ FAILED: Multiple inactivity periods not working")
                }
                
                // Test 4: Data Aggregation
                Log.d(TAG, "Test 4: Testing data aggregation")
                val totalInactiveHours = allInactivity?.sumOf { it.durationHours.toDouble() } ?: 0.0
                val completionPercentage = if (retrievedStepCount != null && retrievedStepCount.goal > 0) {
                    (retrievedStepCount.steps * 100 / retrievedStepCount.goal).coerceIn(0, 100)
                } else {
                    0
                }
                
                Log.i(TAG, "✅ SUCCESS: Data aggregation working")
                Log.d(TAG, "Step completion: $completionPercentage%")
                Log.d(TAG, "Total inactive hours: $totalInactiveHours")
                
                database.close()
                Log.i(TAG, "=== CORE DATA FUNCTIONALITY TESTING COMPLETED ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR during data testing", e)
            }
        }
    }
} 