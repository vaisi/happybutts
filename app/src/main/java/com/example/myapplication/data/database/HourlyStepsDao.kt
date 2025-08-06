// Updated: HourlyStepsDao with queries matching the updated entity structure
// Added: Polling-based queries for enhanced step tracking
package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.HourlySteps
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HourlyStepsDao {
    @Query("SELECT * FROM hourly_steps WHERE date = :date ORDER BY hour ASC")
    fun getHourlyStepsForDate(date: LocalDate): Flow<List<HourlySteps>>

    @Query("SELECT * FROM hourly_steps WHERE date = :date AND hour = :hour")
    suspend fun getHourlyStepsForHour(date: LocalDate, hour: Int): HourlySteps?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlySteps(hourlySteps: HourlySteps)

    @Query("UPDATE hourly_steps SET steps = steps + :steps WHERE date = :date AND hour = :hour")
    suspend fun updateHourlySteps(date: LocalDate, hour: Int, steps: Int)

    @Query("UPDATE hourly_steps SET steps = :steps, last_recorded_total = :lastRecordedTotal WHERE date = :date AND hour = :hour")
    suspend fun updateHourlyStepsWithTotal(date: LocalDate, hour: Int, steps: Int, lastRecordedTotal: Int)

    @Query("SELECT SUM(steps) FROM hourly_steps WHERE date = :date")
    fun getTotalStepsForDate(date: LocalDate): Flow<Int?>

    @Query("DELETE FROM hourly_steps WHERE date < :beforeDate")
    suspend fun deleteOldSteps(beforeDate: LocalDate)

    @Query("DELETE FROM hourly_steps WHERE date = :date")
    suspend fun deleteHourlyStepsForDate(date: LocalDate)

    // New queries for polling-based system
    @Query("SELECT last_recorded_total FROM hourly_steps WHERE date = :date ORDER BY hour DESC LIMIT 1")
    suspend fun getLastRecordedTotalForDate(date: LocalDate): Int?

    @Query("SELECT last_recorded_total FROM hourly_steps WHERE date = :date AND hour = :hour")
    suspend fun getLastRecordedTotalForHour(date: LocalDate, hour: Int): Int?

    @Query("SELECT hour FROM hourly_steps WHERE date = :date ORDER BY hour DESC LIMIT 1")
    suspend fun getLastRecordedHourForDate(date: LocalDate): Int?

    @Query("SELECT COUNT(*) FROM hourly_steps WHERE date = :date")
    suspend fun getHourlyRecordsCountForDate(date: LocalDate): Int
} 