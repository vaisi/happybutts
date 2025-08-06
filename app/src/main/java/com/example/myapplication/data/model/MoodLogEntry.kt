// Created: MoodLogEntry data class for representing hourly mood and step log entries across the app.
package com.example.myapplication.data.model

import com.example.myapplication.data.model.MoodState

/**
 * Represents a log entry for a specific hour, including steps taken, mood value, and mood state.
 */
data class MoodLogEntry(
    val hour: Int,
    val steps: Int,
    val mood: Int,
    val moodState: MoodState
) 