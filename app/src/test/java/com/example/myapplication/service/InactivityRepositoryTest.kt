// Created: InactivityRepositoryTest to verify inactivity data functionality after schema fix
package com.example.myapplication.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.database.InactivityDao
import com.example.myapplication.data.model.InactivityData
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.InactivityRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class InactivityRepositoryTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var database: AppDatabase
    private lateinit var inactivityDao: InactivityDao
    private lateinit var inactivityRepository: InactivityRepository

    @Mock
    private lateinit var mockUserPreferences: UserPreferences

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        
        inactivityDao = database.inactivityDao()
        inactivityRepository = InactivityRepository(
            inactivityDao = inactivityDao,
            userPreferences = mockUserPreferences,
            context = context
        )
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun testInactivityDataInsertion() = runTest {
        // Test that we can insert inactivity data
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        
        val inactivityData = InactivityData(
            date = today.toString(),
            startTime = now,
            durationHours = 0f,
            stepsDuringPeriod = 100,
            isConsecutive = true,
            notificationSent = false
        )
        
        // Insert the data
        inactivityDao.insertInactivity(inactivityData)
        
        // Verify it was inserted
        val retrievedData = inactivityDao.getInactivityForDate(today).first()
        assertNotNull("Inactivity data should be retrieved", retrievedData)
        assertTrue("Should have at least one record", retrievedData.isNotEmpty())
        
        val firstRecord = retrievedData.first()
        assertEquals("Date should match", today.toString(), firstRecord.date)
        assertEquals("Steps should match", 100, firstRecord.stepsDuringPeriod)
        assertTrue("Should be consecutive", firstRecord.isConsecutive)
        assertFalse("Should not be notification sent", firstRecord.notificationSent)
    }

    @Test
    fun testMultipleInactivityRecordsPerDay() = runTest {
        // Test that we can have multiple inactivity records per day
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        
        val inactivityData1 = InactivityData(
            date = today.toString(),
            startTime = now.minusHours(4),
            endTime = now.minusHours(2),
            durationHours = 2f,
            stepsDuringPeriod = 50,
            isConsecutive = true,
            notificationSent = false
        )
        
        val inactivityData2 = InactivityData(
            date = today.toString(),
            startTime = now.minusHours(1),
            durationHours = 0f,
            stepsDuringPeriod = 25,
            isConsecutive = true,
            notificationSent = false
        )
        
        // Insert both records
        inactivityDao.insertInactivity(inactivityData1)
        inactivityDao.insertInactivity(inactivityData2)
        
        // Verify both records exist
        val retrievedData = inactivityDao.getInactivityForDate(today).first()
        assertEquals("Should have 2 records for the same day", 2, retrievedData.size)
        
        // Verify they have different IDs (auto-generated)
        val ids = retrievedData.map { it.id }
        assertTrue("Should have unique IDs", ids.distinct().size == 2)
    }

    @Test
    fun testInactivityPeriodEnding() = runTest {
        // Test ending an inactivity period
        val today = LocalDate.now()
        val startTime = LocalDateTime.now().minusHours(2)
        
        val inactivityData = InactivityData(
            date = today.toString(),
            startTime = startTime,
            durationHours = 0f,
            stepsDuringPeriod = 100,
            isConsecutive = true,
            notificationSent = false
        )
        
        // Insert the data
        inactivityDao.insertInactivity(inactivityData)
        
        // Get the inserted record to get its ID
        val insertedRecord = inactivityDao.getInactivityForDate(today).first().first()
        
        // End the inactivity period
        val endTime = LocalDateTime.now()
        val durationHours = 2.5f
        inactivityDao.endInactivityPeriod(insertedRecord.id, endTime, durationHours)
        
        // Verify the record was updated
        val updatedRecord = inactivityDao.getInactivityForDate(today).first().first()
        assertNotNull("End time should be set", updatedRecord.endTime)
        assertEquals("Duration should be updated", durationHours, updatedRecord.durationHours, 0.01f)
    }

    @Test
    fun testRepositoryStartAndEndInactivity() = runTest {
        // Test the repository's start and end inactivity methods
        val initialSteps = 1000
        
        // Start inactivity period
        inactivityRepository.startInactivityPeriod(initialSteps)
        
        // Verify active period exists
        val today = LocalDate.now()
        val activePeriod = inactivityDao.getActiveInactivityPeriod(today)
        assertNotNull("Should have an active inactivity period", activePeriod)
        assertEquals("Steps should match", initialSteps, activePeriod.stepsDuringPeriod)
        assertNull("End time should be null for active period", activePeriod.endTime)
        
        // End inactivity period
        val finalSteps = 1500
        inactivityRepository.endInactivityPeriod(finalSteps)
        
        // Verify period was ended
        val endedPeriod = inactivityDao.getInactivityForDate(today).first().first()
        assertNotNull("End time should be set", endedPeriod.endTime)
        assertTrue("Duration should be greater than 0", endedPeriod.durationHours > 0)
    }
} 