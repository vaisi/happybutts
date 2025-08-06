// Created: DateTimeChangeReceiver to handle manual date/time changes and trigger app resets
package com.example.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myapplication.data.repository.MoodRepository
import com.example.myapplication.data.repository.StepCountRepository
import com.example.myapplication.service.UnifiedStepCounterService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class DateTimeChangeReceiver : BroadcastReceiver() {
    
    private val TAG = "DateTimeChangeReceiver"
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    @Inject
    lateinit var moodRepository: MoodRepository
    
    @Inject
    lateinit var stepCountRepository: StepCountRepository
    
    @Inject
    lateinit var stepCounterService: UnifiedStepCounterService

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "DateTime change detected: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                Log.i(TAG, "=== MANUAL DATE/TIME CHANGE DETECTED ===")
                handleDateTimeChange()
            }
        }
    }

    private fun handleDateTimeChange() {
        receiverScope.launch {
            try {
                val today = LocalDate.now()
                Log.i(TAG, "Handling date/time change for date: $today")
                
                // Get current mood state to check if we need to reset
                val currentMood = moodRepository.getCurrentMood().firstOrNull()
                
                if (currentMood != null && currentMood.date.isBefore(today)) {
                    Log.i(TAG, "Date has changed from ${currentMood.date} to $today, performing reset")
                    
                    // Finalize the previous day's mood
                    val yesterday = currentMood.date
                    Log.d(TAG, "Finalizing mood for date: $yesterday")
                    moodRepository.finalizeDayMood(yesterday)
                    
                    // Reset for the new day
                    Log.d(TAG, "Resetting daily mood and steps for new day")
                    moodRepository.resetDaily()
                    
                    // Reset step baseline for new day
                    stepCounterService.resetDailyBaseline()
                    
                    // Reset step count in repository
                    stepCountRepository.updateTodaySteps(0)
                    
                    Log.i(TAG, "=== DATE/TIME CHANGE RESET COMPLETED ===")
                } else {
                    Log.d(TAG, "No date change detected or already on current date")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling date/time change", e)
            }
        }
    }
} 