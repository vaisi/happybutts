// Updated: InactivityData model to match Room's expected schema
// Fixed: Primary key issue by using auto-generated ID instead of date
package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "inactivity_data")
data class InactivityData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD format
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val durationHours: Float,
    val stepsDuringPeriod: Int,
    val isConsecutive: Boolean = true,
    val notificationSent: Boolean = false
)

data class InactivitySettings(
    val enabled: Boolean = true,
    val consecutiveHoursThreshold: Int = 4,
    val maxNotificationsPerDay: Int = 3,
    val quietHoursStart: Int = 23, // 11 PM
    val quietHoursEnd: Int = 7,    // 7 AM
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true
) 