package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "step_data")
data class StepData(
    @PrimaryKey
    val date: LocalDate,
    val steps: Int,
    val calories: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)