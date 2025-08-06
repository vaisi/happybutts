// Updated: MoodStateDao with consistent method names and return types
package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.MoodStateEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MoodStateDao {
    @Query("SELECT * FROM mood_states WHERE date = :date")
    fun getMoodForDate(date: LocalDate): Flow<MoodStateEntity?>

    @Query("SELECT * FROM mood_states WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getMoodHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<MoodStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(moodState: MoodStateEntity)

    @Update
    suspend fun updateMood(moodState: MoodStateEntity)

    @Query("DELETE FROM mood_states WHERE date < :beforeDate")
    suspend fun deleteOldMoods(beforeDate: LocalDate)
} 