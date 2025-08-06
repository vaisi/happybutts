package com.example.myapplication.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.service.StepCountingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.myapplication.data.preferences.UserPreferences
import androidx.core.content.ContextCompat
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StepCounterViewModel @Inject constructor(
    application: Application,
    private val userPreferences: UserPreferences
) : AndroidViewModel(application) {

    private val TAG = "StepCounterViewModel"

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState: StateFlow<PermissionState> = _permissionState

    init {
        Log.d(TAG, "init: ViewModel initialized")
        checkPermissions()
    }

    fun checkPermissions() {
        Log.d(TAG, "checkPermissions: Checking permissions")
        viewModelScope.launch {
            val hasPermissions = hasRequiredPermissions()
            _permissionState.value = if (hasPermissions) {
                Log.d(TAG, "checkPermissions: All permissions granted")
                PermissionState.Granted
            } else {
                Log.d(TAG, "checkPermissions: Some permissions are missing")
                PermissionState.Denied
            }
        }
    }

    fun startStepCountingService() {
        Log.d(TAG, "startStepCountingService: Attempting to start service")
        if (!hasRequiredPermissions()) {
            Log.d(TAG, "startStepCountingService: Missing required permissions")
            _permissionState.value = PermissionState.Denied
            return
        }
        viewModelScope.launch {
            if (!canStartStepCountingService(getApplication(), userPreferences)) {
                Log.d(TAG, "startStepCountingService: Onboarding not complete or permission not granted")
                return@launch
            }
            try {
                Log.d(TAG, "startStepCountingService: All permissions granted, starting service")
                val serviceIntent = Intent(getApplication(), StepCountingService::class.java).apply {
                    action = StepCountingService.ACTION_START_STEP_COUNTING
                }
                getApplication<Application>().startForegroundService(serviceIntent)
                Log.d(TAG, "startStepCountingService: Service start intent sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "startStepCountingService: Error starting service", e)
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        Log.d(TAG, "hasRequiredPermissions: Checking permissions")
        val activityRecognitionGranted = getApplication<Application>().checkSelfPermission(
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "hasRequiredPermissions: Activity recognition: $activityRecognitionGranted")
        return activityRecognitionGranted
    }

    private suspend fun canStartStepCountingService(context: Context, userPreferences: UserPreferences): Boolean {
        val onboardingCompleted = userPreferences.onboardingCompleted.first()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return onboardingCompleted && hasPermission
    }

    sealed class PermissionState {
        object Unknown : PermissionState()
        object Granted : PermissionState()
        object Denied : PermissionState()
    }
} 