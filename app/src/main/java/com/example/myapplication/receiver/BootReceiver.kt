// Updated: Only start StepCountingService on boot if onboarding is complete and permission is granted.
package com.example.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myapplication.service.StepCountingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import androidx.core.content.ContextCompat
import android.Manifest

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"
    private val bootScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Boot completed, scheduling step counting service start")
            
            // Start the service with retry logic
            bootScope.launch {
                startStepCountingServiceWithRetry(context)
            }
        }
    }
    
    private fun canStartStepCountingService(context: Context): Boolean {
        // For boot, we'll be conservative and only start if permission is granted
        // Onboarding completion will be checked by the service itself
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return hasPermission
    }
    
    private suspend fun startStepCountingServiceWithRetry(context: Context) {
        val maxRetries = 3
        var retryCount = 0
        
        while (retryCount < maxRetries) {
            try {
                // Wait for system to be fully booted (increasing delay with each retry)
                val delayMs = (5000L + (retryCount * 3000L)) // 5s, 8s, 11s
                Log.d(TAG, "Waiting ${delayMs}ms before attempt ${retryCount + 1}")
                delay(delayMs)
                
                val serviceIntent = Intent(context, StepCountingService::class.java).apply {
                    action = StepCountingService.ACTION_START_STEP_COUNTING
                }
                context.startForegroundService(serviceIntent)
                
                Log.i(TAG, "Successfully started step counting service on boot (attempt ${retryCount + 1})")
                return
                
            } catch (e: Exception) {
                retryCount++
                Log.e(TAG, "Failed to start step counting service on boot (attempt $retryCount)", e)
                
                if (retryCount >= maxRetries) {
                    Log.e(TAG, "Failed to start step counting service after $maxRetries attempts")
                }
            }
        }
    }
}