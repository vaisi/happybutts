// Created: QuietHoursSetupViewModel for managing quiet hours setup with persistence
package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltViewModel
class QuietHoursSetupViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _quietHours = MutableStateFlow(QuietHours())
    val quietHours: StateFlow<QuietHours> = _quietHours.asStateFlow()

    init {
        viewModelScope.launch {
            loadQuietHours()
        }
    }

    private suspend fun loadQuietHours() {
        // Load from UserPreferences - we'll need to add these methods
        val startHour = userPreferences.quietHoursStart.first()
        val endHour = userPreferences.quietHoursEnd.first()
        _quietHours.value = QuietHours(
            start = LocalTime.of(startHour, 0),
            end = LocalTime.of(endHour, 0)
        )
    }

    fun updateStartTime(time: LocalTime) {
        _quietHours.value = _quietHours.value.copy(start = time)
    }

    fun updateEndTime(time: LocalTime) {
        _quietHours.value = _quietHours.value.copy(end = time)
    }

    fun saveQuietHours() {
        viewModelScope.launch {
            userPreferences.updateQuietHoursStart(_quietHours.value.start.hour)
            userPreferences.updateQuietHoursEnd(_quietHours.value.end.hour)
        }
    }

    data class QuietHours(
        val start: LocalTime = LocalTime.of(22, 0), // 10 PM default
        val end: LocalTime = LocalTime.of(7, 0)     // 7 AM default
    )
} 