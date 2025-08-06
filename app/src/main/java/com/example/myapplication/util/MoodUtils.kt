package com.example.myapplication.util

import androidx.compose.ui.graphics.Color
import com.example.myapplication.R
import com.example.myapplication.data.model.MoodState

/**
 * Utility functions for handling mood-related operations
 */

fun moodToDrawableAndColor(moodState: MoodState): Pair<Int, Color> {
    return when (moodState) {
        MoodState.DEPRESSED -> Pair(R.drawable.depressed_mood, Color(0xFF9C27B0))
        MoodState.MISERABLE -> Pair(R.drawable.miserable_mood, Color(0xFFE91E63))
        MoodState.ANNOYED -> Pair(R.drawable.annoyed_mood, Color(0xFFFF5722))
        MoodState.MEH -> Pair(R.drawable.meh_mood, Color(0xFFFFC107))
        MoodState.CONTENT -> Pair(R.drawable.content_mood, Color(0xFF4CAF50))
        MoodState.HAPPY -> Pair(R.drawable.happy_mood, Color(0xFFFFEB3B))
        MoodState.PUMPED -> Pair(R.drawable.pumped_mood, Color(0xFF2196F3))
        MoodState.UNSTOPPABLE -> Pair(R.drawable.unstoppable_mood, Color(0xFF3F51B5))
        MoodState.TIRED -> Pair(R.drawable.tired_mood, Color(0xFF795548))
        MoodState.EXHAUSTED -> Pair(R.drawable.exhausted_mood, Color(0xFF607D8B))
    }
} 