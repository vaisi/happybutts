package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.viewmodel.HomeViewModel
import com.example.myapplication.ui.components.CharacterAnimation
import com.example.myapplication.ui.components.WeeklyChart

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToStepCounter: () -> Unit,
    onNavigateToMoodLog: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isStepCountingActive by viewModel.isStepCountingActive.collectAsState()
    val needsPermission by viewModel.needsPermission.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Happy Butts",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Character Animation
        CharacterAnimation(
            isHappy = uiState.currentMood > 50,
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        )

        // Current Mood Display
        Text(
            text = "Mood: ${uiState.currentMood}",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Step Count Display
        Text(
            text = "${uiState.currentSteps} steps today",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Progress towards goal
        val progress = if (uiState.dailyGoal > 0) {
            (uiState.currentSteps.toFloat() / uiState.dailyGoal.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            text = "Goal: ${uiState.dailyGoal} steps",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onNavigateToStepCounter,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Step Counter")
            }

            Button(
                onClick = onNavigateToMoodLog,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("Mood Log")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Button
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Settings")
        }

        // Status Information
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (isStepCountingActive) "Step counting active" else "Step counting inactive",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (needsPermission) {
                    Text(
                        text = "Permissions needed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Weekly Chart
        Spacer(modifier = Modifier.height(16.dp))
        
        // WeeklyChart temporarily removed due to compilation issues
        // WeeklyChart(
        //     modifier = Modifier
        //         .fillMaxWidth()
        //         .height(200.dp)
        // )
    }
}