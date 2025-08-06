// Updated: InactivityDao to use date as primary key instead of id
// Fixed: Database constraint issue by using OnConflictStrategy.REPLACE
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
    
    @Query("UPDATE inactivity_data SET endTime = :endTime, durationHours = :durationHours WHERE date = :date AND startTime = :startTime")
    suspend fun endInactivityPeriod(date: String, startTime: LocalDateTime, endTime: LocalDateTime, durationHours: Float)
    
    @Query("UPDATE inactivity_data SET notificationSent = 1 WHERE date = :date AND startTime = :startTime")
    suspend fun markNotificationSent(date: String, startTime: LocalDateTime)
    
    @Query("DELETE FROM inactivity_data WHERE date < :date")
    suspend fun deleteOldInactivityData(date: LocalDate)
} 