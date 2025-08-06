// Created: MoodLogScreen to display mood change history
package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.viewmodel.StepCounterViewModel
import com.example.myapplication.data.model.MoodState
import java.time.format.DateTimeFormatter
import com.example.myapplication.data.model.MoodLogEntry

@Composable
fun MoodLogScreen(
    onNavigateBack: () -> Unit,
    viewModel: StepCounterViewModel = hiltViewModel()
) {
    val moodHistory by viewModel.moodHistory.collectAsState(initial = emptyList())
    val moodLog by viewModel.todayMoodLog.collectAsState(initial = emptyList())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mood Change Log",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(moodLog) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = String.format("%02d:00", entry.hour), style = MaterialTheme.typography.bodyLarge)
                        Text(text = "${entry.steps} steps", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "${entry.moodState.emoji} (${entry.mood})", style = MaterialTheme.typography.bodyLarge)
                        Text(text = entry.moodState.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Back to Step Counter")
        }
    }
} 