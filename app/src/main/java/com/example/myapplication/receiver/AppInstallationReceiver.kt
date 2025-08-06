// Updated: Only start StepCountingService after app install if onboarding is complete and permission is granted.
// Created: AppInstallationReceiver to start step counting service when app is installed or updated
package com.example.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myapplication.service.StepCountingService
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.content.ContextCompat
import android.Manifest

@AndroidEntryPoint
class AppInstallationReceiver : BroadcastReceiver() {
    private val TAG = "AppInstallationReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val packageName = intent.data?.schemeSpecificPart
                if (packageName == context.packageName) {
                    Log.i(TAG, "App installed, starting step counting service")
                    startStepCountingService(context)
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                val packageName = intent.data?.schemeSpecificPart
                if (packageName == context.packageName) {
                    Log.i(TAG, "App updated, ensuring step counting service is running")
                    startStepCountingService(context)
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "App replaced, starting step counting service")
                startStepCountingService(context)
            }
        }
    }
    
    private fun canStartStepCountingService(context: Context): Boolean {
        // For app installation/update, we'll be conservative and only start if permission is granted
        // Onboarding completion will be checked by the service itself
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return hasPermission
    }

    private fun startStepCountingService(context: Context) {
        if (!canStartStepCountingService(context)) {
            Log.i(TAG, "Not starting StepCountingService: permission not granted")
            return
        }
        try {
            // Add a small delay to ensure the system is ready
            Thread.sleep(1000)
            
            val serviceIntent = Intent(context, StepCountingService::class.java).apply {
                action = StepCountingService.ACTION_START_STEP_COUNTING
            }
            context.startForegroundService(serviceIntent)
            Log.i(TAG, "Successfully started step counting service after app installation")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting step counting service after app installation", e)
        }
    }
} 