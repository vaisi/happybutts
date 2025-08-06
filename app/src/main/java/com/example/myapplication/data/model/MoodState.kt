// Created: MoodState enum to represent different mood states
package com.example.myapplication.data.model

enum class MoodState(
    val emoji: String,
    val description: String,
    val range: IntRange
) {
    DEPRESSED("💀", "Butts is barely moving, dark cloud overhead", 0..15),
    MISERABLE("😭", "Tears flowing, dragging feet", 16..30),
    ANNOYED("😤", "Frowning, arms crossed, tapping foot impatiently", 31..45),
    MEH("😑", "Neutral face, shoulders shrugged, just existing", 46..55),
    CONTENT("🙂", "Slight smile, relaxed posture, gentle bounce", 56..65),
    HAPPY("😃", "Big smile, arms up, happy bounce", 66..75),
    PUMPED("😄", "Big grin, energetic movements, little hops", 76..85),
    UNSTOPPABLE("🔥", "Glowing, jumping, fist-pumping, on fire!", 86..100),
    TIRED("😮‍💨", "Sweating, heavy breathing, needs a break", 101..115),
    EXHAUSTED("🥵", "Collapsed, tongue out, can barely move", 116..130);

    companion object {
        fun fromMoodValue(mood: Int): MoodState {
            return values().find { mood in it.range } ?: MEH
        }
    }
} 