// Updated: MoodNotificationData - changed primary key from id to date to fix Room migration
package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "mood_notifications")
data class MoodNotificationData(
    @PrimaryKey val date: String, // YYYY-MM-DD format - primary key
    val timestamp: LocalDateTime,
    val previousMood: Int,
    val currentMood: Int,
    val moodDrop: Int,
    val stepsInPeriod: Int,
    val periodHours: Float,
    val notificationSent: Boolean = false
)

data class MoodNotificationSettings(
    val enabled: Boolean = true,
    val moodDropThreshold: Int = 2, // Notify when mood drops by 2 levels
    val maxNotificationsPerDay: Int = 5,
    val quietHoursStart: Int = 23, // 11 PM
    val quietHoursEnd: Int = 7,    // 7 AM
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val minHoursBetweenNotifications: Int = 2
)

// Fun notification messages for each mood level
object MoodNotificationMessages {
    
    // Messages for when mood drops to ANNOYED (31-45)
    val ANNOYED_MESSAGES = listOf(
        "Your butts is feeling annoyed! Only {steps} steps in {hours} hours 😤",
        "Your character is getting grumpy! {steps} steps won't cut it! 😠",
        "Your butts needs more action! {steps} steps in {hours} hours is making it cranky! 😤",
        "Your character is tapping its foot impatiently! Time to move! 🚶‍♂️",
        "Your butts is giving you the side-eye! Only {steps} steps? Really? 😒"
    )
    
    // Messages for when mood drops to SAD (21-30)
    val SAD_MESSAGES = listOf(
        "Your butts is feeling sad! Only {steps} steps in {hours} hours 😢",
        "Your character is getting depressed! {steps} steps is not enough! 😭",
        "Your butts is looking downcast! It needs more movement! 😔",
        "Your character is feeling blue! Time to cheer it up with some steps! 💙",
        "Your butts is getting gloomy! {steps} steps won't lift its spirits! 😞"
    )
    
    // Messages for when mood drops to MISERABLE (11-20)
    val MISERABLE_MESSAGES = listOf(
        "Your butts is miserable! Only {steps} steps in {hours} hours 😩",
        "Your character is in despair! {steps} steps is making it suffer! 😫",
        "Your butts is at rock bottom! It needs a step intervention! 😖",
        "Your character is completely flat! Time for some serious movement! 📉",
        "Your butts is in a dark place! {steps} steps won't save it! 😰"
    )
    
    // Messages for when mood drops to COMPLETELY_FLAT (0-10)
    val COMPLETELY_FLAT_MESSAGES = listOf(
        "Your butts is completely flat! Only {steps} steps in {hours} hours 📉",
        "Your character has given up! {steps} steps is not cutting it! 😵",
        "Your butts is lifeless! It needs immediate step resuscitation! 💀",
        "Your character is beyond help! Emergency steps needed! 🚨",
        "Your butts is in a coma! {steps} steps won't wake it up! 😴"
    )
    
    // Messages for when mood drops to EXHAUSTED (46-60)
    val EXHAUSTED_MESSAGES = listOf(
        "Your butts is exhausted! Only {steps} steps in {hours} hours 😮‍💨",
        "Your character is running on empty! {steps} steps won't refuel it! ⛽",
        "Your butts is barely hanging on! It needs more energy! 🔋",
        "Your character is tired and cranky! Time to energize! ⚡",
        "Your butts is running out of steam! {steps} steps won't power it up! 🔌"
    )
    
    // Messages for when mood drops to TIRED (61-75)
    val TIRED_MESSAGES = listOf(
        "Your butts is getting tired! Only {steps} steps in {hours} hours 😴",
        "Your character is losing energy! {steps} steps won't keep it going! 💤",
        "Your butts is starting to fade! It needs a boost! 🚀",
        "Your character is getting sluggish! Time to wake it up! ⏰",
        "Your butts is losing its spark! {steps} steps won't reignite it! 🔥"
    )
    
    // Messages for when mood drops to NEUTRAL (76-90)
    val NEUTRAL_MESSAGES = listOf(
        "Your butts is feeling meh! Only {steps} steps in {hours} hours 😐",
        "Your character is getting bored! {steps} steps won't entertain it! 🎭",
        "Your butts is losing interest! It needs some excitement! 🎢",
        "Your character is getting indifferent! Time to engage it! 🎯",
        "Your butts is getting complacent! {steps} steps won't motivate it! 📊"
    )
    
    // Messages for when mood drops to HAPPY (91-110)
    val HAPPY_MESSAGES = listOf(
        "Your butts is getting less happy! Only {steps} steps in {hours} hours 😊",
        "Your character is losing its joy! {steps} steps won't maintain it! 😄",
        "Your butts is getting less cheerful! It needs more positivity! 🌟",
        "Your character is losing its sparkle! Time to brighten it up! ✨",
        "Your butts is getting less excited! {steps} steps won't thrill it! 🎉"
    )
    
    // Messages for when mood drops to VERY_HAPPY (111-130)
    val VERY_HAPPY_MESSAGES = listOf(
        "Your butts is losing its ecstasy! Only {steps} steps in {hours} hours 😍",
        "Your character is getting less euphoric! {steps} steps won't maintain it! 🤩",
        "Your butts is losing its bliss! It needs more ecstasy! 🌈",
        "Your character is getting less overjoyed! Time to elevate it! 🚀",
        "Your butts is losing its rapture! {steps} steps won't maintain it! 💫"
    )
    
    // Get messages for a specific mood level
    fun getMessagesForMood(mood: Int): List<String> {
        return when {
            mood in 0..10 -> COMPLETELY_FLAT_MESSAGES
            mood in 11..20 -> MISERABLE_MESSAGES
            mood in 21..30 -> SAD_MESSAGES
            mood in 31..45 -> ANNOYED_MESSAGES
            mood in 46..60 -> EXHAUSTED_MESSAGES
            mood in 61..75 -> TIRED_MESSAGES
            mood in 76..90 -> NEUTRAL_MESSAGES
            mood in 91..110 -> HAPPY_MESSAGES
            mood in 111..130 -> VERY_HAPPY_MESSAGES
            else -> NEUTRAL_MESSAGES
        }
    }
    
    // Get a random message for a mood level with step and hour placeholders
    fun getRandomMessage(mood: Int, steps: Int, hours: Float): String {
        val messages = getMessagesForMood(mood)
        val randomMessage = messages.random()
        return randomMessage
            .replace("{steps}", steps.toString())
            .replace("{hours}", hours.toInt().toString())
    }
} 