// Created: OnboardingViewModel for managing onboarding flow and permissions
package com.example.myapplication.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "OnboardingViewModel"
    
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    // Use nullable Boolean to indicate loading state
    private val _onboardingCompleted = MutableStateFlow<Boolean?>(null)
    val onboardingCompleted: StateFlow<Boolean?> = _onboardingCompleted.asStateFlow()

    init {
        Log.i(TAG, "OnboardingViewModel init started")
        checkPermissionStatus()
        checkAndResetForFreshInstall()
        checkOnboardingStatus()
    }

    private fun checkPermissionStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
        
        _permissionGranted.value = hasPermission
        Log.i(TAG, "Permission status checked: $hasPermission")
    }

    private fun checkAndResetForFreshInstall() {
        viewModelScope.launch {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = packageInfo.versionName ?: "1.0.0"
                Log.i(TAG, "Current app version: $currentVersion")
                userPreferences.checkAndResetForFreshInstall(currentVersion)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for fresh install", e)
            }
        }
    }

    private fun checkOnboardingStatus() {
        Log.i(TAG, "Starting to check onboarding status from DataStore")
        viewModelScope.launch {
            // Small delay to ensure DataStore is properly initialized
            kotlinx.coroutines.delay(100)
            userPreferences.onboardingCompleted.collect { completed ->
                Log.i(TAG, "DataStore returned onboarding status: $completed")
                _onboardingCompleted.value = completed
                Log.i(TAG, "Updated _onboardingCompleted to: $completed")
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
        Log.i(TAG, "Permission result: $granted")
        
        if (granted) {
            // Permission granted, can proceed to goal setup
            Log.i(TAG, "Permission granted, ready for goal setup")
        } else {
            // Permission denied, show message or handle accordingly
            Log.w(TAG, "Permission denied by user")
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            Log.i(TAG, "Completing onboarding")
            userPreferences.updateOnboardingCompleted(true)
            _onboardingCompleted.value = true
            Log.i(TAG, "Onboarding completed")
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            Log.i(TAG, "Resetting onboarding")
            userPreferences.updateOnboardingCompleted(false)
            _onboardingCompleted.value = false
            Log.i(TAG, "Onboarding reset")
        }
    }
} 