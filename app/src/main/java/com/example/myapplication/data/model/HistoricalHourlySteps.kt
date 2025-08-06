// Created: HistoricalHourlySteps entity for preserving hourly data before midnight reset
package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "historical_hourly_steps",
    primaryKeys = ["date", "hour"]
)
data class HistoricalHourlySteps(
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "hour")
    val hour: Int, // 0-23
    @ColumnInfo(name = "steps")
    val steps: Int,
    @ColumnInfo(name = "archived_at")
    val archivedAt: LocalDateTime
) 