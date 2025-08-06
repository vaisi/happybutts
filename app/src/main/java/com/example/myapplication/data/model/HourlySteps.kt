// Updated: HourlySteps entity with composite key and proper column names
// Added: lastRecordedTotal field for polling-based step tracking
package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.time.LocalDate

@Entity(
    tableName = "hourly_steps",
    primaryKeys = ["date", "hour"]
)
data class HourlySteps(
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "hour")
    val hour: Int, // 0-23
    @ColumnInfo(name = "steps")
    val steps: Int,
    @ColumnInfo(name = "last_recorded_total")
    val lastRecordedTotal: Int = 0 // Total steps recorded at the end of this hour
) 