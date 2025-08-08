// Updated: InactivityDao to use auto-incrementing ID as primary key instead of date
// Fixed: Database constraint issue by using OnConflictStrategy.REPLACE
// Fixed: Update queries to use ID instead of date+startTime composite key
package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.InactivityData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface InactivityDao {
    
    @Query("SELECT * FROM inactivity_data WHERE date = :date ORDER BY startTime DESC")
    fun getInactivityForDate(date: LocalDate): Flow<List<InactivityData>>
    
    @Query("SELECT * FROM inactivity_data WHERE date = :date AND isConsecutive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getCurrentConsecutiveInactivity(date: LocalDate): InactivityData?
    
    @Query("SELECT COUNT(*) FROM inactivity_data WHERE date = :date AND notificationSent = 1")
    suspend fun getNotificationCountForDate(date: LocalDate): Int
    
    @Query("SELECT * FROM inactivity_data WHERE date = :date AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveInactivityPeriod(date: LocalDate): InactivityData?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInactivity(inactivity: InactivityData)
    
    @Update
    suspend fun updateInactivity(inactivity: InactivityData)
    
    // Fixed: Use ID for updates instead of date+startTime composite key
    @Query("UPDATE inactivity_data SET endTime = :endTime, durationHours = :durationHours WHERE id = :id")
    suspend fun endInactivityPeriod(id: Long, endTime: LocalDateTime, durationHours: Float)
    
    // Fixed: Use ID for notification marking instead of date+startTime
    @Query("UPDATE inactivity_data SET notificationSent = 1 WHERE id = :id")
    suspend fun markNotificationSent(id: Long)
    
    @Query("DELETE FROM inactivity_data WHERE date < :date")
    suspend fun deleteOldInactivityData(date: LocalDate)
} 