// Updated: Changed default goal to 7000 to match new user preferences
package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "step_counts")
data class StepCount(
    @PrimaryKey
    val date: LocalDate,
    val steps: Int,
    val goal: Int = 7000
) 