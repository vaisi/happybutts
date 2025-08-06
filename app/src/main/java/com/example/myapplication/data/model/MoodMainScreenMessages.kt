// Created: MoodMainScreenMessages - provides random mood messages for the main screen based on mood value
package com.example.myapplication.data.model

data class MoodMessageRange(val min: Int, val max: Int, val messages: List<String>)

val moodMainScreenMessages = listOf(
    MoodMessageRange(0, 15, listOf(
        "Your Butts has given up",
        "Butts is completely drained",
        "Your Butts needs serious help",
        "Your Butts hit rock bottom"
    )),
    MoodMessageRange(15, 30, listOf(
        "Your Butts is feeling low",
        "Butts could use some love",
        "Your Butts is having a rough day",
        "Your Butts is wallowing"
    )),
    MoodMessageRange(30, 45, listOf(
        "Your Butts is getting anxious",
        "Butts is starting to worry",
        "Your Butts needs some attention"
    )),
    MoodMessageRange(45, 55, listOf(
        "Your Butts is getting cranky",
        "Butts is NOT having it",
        "Your Butts is fed up with sitting"
    )),
    MoodMessageRange(55, 65, listOf(
        "Your Butts found their comfortable groove",
        "Your Butts achieved solid okayness",
        "Your Butts is pleasantly existing"
    )),
    MoodMessageRange(65, 75, listOf(
        "Your Butts is cruising along nicely",
        "Butts is feeling cautiously optimistic",
        "Your Butts feels there is potential"
    )),
    MoodMessageRange(75, 85, listOf(
        "Your Butts is vibing",
        "Your Butts is zen",
        "Butts is in the zone",
        "Your Butts found inner peace"
    )),
    MoodMessageRange(85, 100, listOf(
        "Your Butts is pumped",
        "Your Butts is on a roll",
        "Your Butts is feeling amazing"
    )),
    MoodMessageRange(100, 115, listOf(
        "Your Butts is FIRED UP!",
        "Butts is absolutely blazing!",
        "Your Butts is unstoppable!"
    )),
    MoodMessageRange(115, 130, listOf(
        "Your Butts crushed it!",
        "Your Butts is in champion mode",
        "Your butts earned those stripes"
    ))
)

/**
 * Returns a random message for the given mood value.
 * @param mood The current mood value.
 * @param seed Optional seed for deterministic selection (e.g., for testing).
 */
fun getRandomMoodMainScreenMessage(mood: Int, seed: Long? = null): String {
    // Use exclusive upper bound to avoid overlapping ranges
    val range = moodMainScreenMessages.find { mood >= it.min && mood < it.max }
    val messages = range?.messages ?: listOf("Your Butts is... confused?")
    return if (seed != null) {
        messages.shuffled(java.util.Random(seed)).first()
    } else {
        messages.random()
    }
}