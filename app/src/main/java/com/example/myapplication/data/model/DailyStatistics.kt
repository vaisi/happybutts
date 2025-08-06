// Created: DailyStatistics entity for storing comprehensive daily metrics for historical analysis
package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "daily_statistics",
    primaryKeys = ["date"]
)
data class DailyStatistics(
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "daily_goal")
    val dailyGoal: Int,
    @ColumnInfo(name = "final_mood")
    val finalMood: Int,
    @ColumnInfo(name = "total_steps")
    val totalSteps: Int,
    @ColumnInfo(name = "archived_at")
    val archivedAt: LocalDateTime
) 