// Created: InactivityTest utility to verify inactivity data functionality
package com.example.myapplication.util

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.database.InactivityDao
import com.example.myapplication.data.model.InactivityData
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime

object InactivityTest {
    private const val TAG = "InactivityTest"
    
    fun testInactivityDataFunctionality(context: Context) {
        Log.i(TAG, "=== TESTING INACTIVITY DATA FUNCTIONALITY ===")
        
        runBlocking {
            try {
                // Create in-memory database for testing
                val database = Room.inMemoryDatabaseBuilder(
                    context,
                    AppDatabase::class.java
                ).build()
                
                val inactivityDao = database.inactivityDao()
                val today = LocalDate.now()
                val now = LocalDateTime.now()
                
                // Test 1: Insert single inactivity period
                Log.d(TAG, "Test 1: Inserting single inactivity period")
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
                    Log.i(TAG, "✅ SUCCESS: Single inactivity period inserted and retrieved")
                    Log.d(TAG, "Retrieved ${retrievedInactivity.size} periods")
                    Log.d(TAG, "First period duration: ${retrievedInactivity.first().durationHours} hours")
                } else {
                    Log.e(TAG, "❌ FAILED: Could not retrieve inserted inactivity data")
                }
                
                // Test 2: Insert multiple periods for same day
                Log.d(TAG, "Test 2: Inserting multiple inactivity periods")
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
                
                // Test 3: Update inactivity period
                Log.d(TAG, "Test 3: Testing inactivity period update")
                val firstInactivity = allInactivity?.firstOrNull()
                if (firstInactivity != null) {
                    val updatedDuration = 1.5f
                    inactivityDao.endInactivityPeriod(firstInactivity.id, now, updatedDuration)
                    
                    val updatedInactivity = inactivityDao.getInactivityForDate(today).firstOrNull()
                    val updatedPeriod = updatedInactivity?.find { it.id == firstInactivity.id }
                    
                    if (updatedPeriod != null && updatedPeriod.durationHours == updatedDuration) {
                        Log.i(TAG, "✅ SUCCESS: Inactivity period update working")
                        Log.d(TAG, "Updated duration: ${updatedPeriod.durationHours} hours")
                    } else {
                        Log.e(TAG, "❌ FAILED: Inactivity period update not working")
                    }
                }
                
                database.close()
                Log.i(TAG, "=== INACTIVITY DATA FUNCTIONALITY TESTING COMPLETED ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR during inactivity testing", e)
            }
        }
    }
} 