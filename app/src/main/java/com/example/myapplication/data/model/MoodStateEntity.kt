// Updated: Reverted to LocalDate to match existing codebase
package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "mood_states")
data class MoodStateEntity(
    @PrimaryKey
    val date: LocalDate,
    val mood: Int,
    val dailyStartMood: Int,
    val previousDayEndMood: Int,
    val lastPersistedSteps: Int = 0 // New field for real-time mood calculation
) 