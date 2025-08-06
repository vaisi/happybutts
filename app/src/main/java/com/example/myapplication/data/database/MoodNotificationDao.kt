// Updated: MoodNotificationDao - changed markNotificationSent to use date instead of id
package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.MoodNotificationData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface MoodNotificationDao {
    
    @Query("SELECT * FROM mood_notifications WHERE date = :date ORDER BY timestamp DESC")
    fun getMoodNotificationsForDate(date: LocalDate): Flow<List<MoodNotificationData>>
    
    @Query("SELECT COUNT(*) FROM mood_notifications WHERE date = :date AND notificationSent = 1")
    suspend fun getNotificationCountForDate(date: LocalDate): Int
    
    @Query("SELECT * FROM mood_notifications WHERE date = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMoodNotification(date: LocalDate): MoodNotificationData?
    
    @Query("SELECT * FROM mood_notifications WHERE date = :date AND notificationSent = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getPendingMoodNotification(date: LocalDate): MoodNotificationData?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodNotification(notification: MoodNotificationData): Long
    
    @Update
    suspend fun updateMoodNotification(notification: MoodNotificationData)
    
    @Query("UPDATE mood_notifications SET notificationSent = 1 WHERE date = :date")
    suspend fun markNotificationSent(date: String)
    
    @Query("DELETE FROM mood_notifications WHERE date < :date")
    suspend fun deleteOldMoodNotifications(date: LocalDate)
    
    @Query("SELECT * FROM mood_notifications WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getRecentMoodNotifications(since: LocalDateTime): List<MoodNotificationData>
} 