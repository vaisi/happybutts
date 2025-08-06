// Created: QuietHoursSetupScreen for configuring notification quiet hours
package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.viewmodel.QuietHoursSetupViewModel
import com.example.myapplication.ui.viewmodel.QuietHoursSetupViewModel.QuietHours
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuietHoursSetupScreen(
    onQuietHoursSet: () -> Unit,
    onNavigateBack: () -> Unit,
    isOnboarding: Boolean = false,
    viewModel: QuietHoursSetupViewModel = hiltViewModel()
) {
    val quietHours by viewModel.quietHours.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = if (isOnboarding) "Set Quiet Hours" else "Notification Quiet Hours",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = if (isOnboarding) 
                "Choose when you don't want to receive notifications. Perfect for sleep time!" 
            else 
                "Configure when notifications should be silenced. Great for sleep and focus time!",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Visual Time Representation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🌙 Quiet Hours",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${quietHours.start.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${quietHours.end.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2196F3)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${calculateQuietHoursDuration(quietHours.start, quietHours.end)} hours of quiet time",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Time Pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Start Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Start Time",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TimePickerButton(
                    time = quietHours.start,
                    onTimeSelected = { viewModel.updateStartTime(it) },
                    label = "Start"
                )
            }
            
            // End Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "End Time",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TimePickerButton(
                    time = quietHours.end,
                    onTimeSelected = { viewModel.updateEndTime(it) },
                    label = "End"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Quick Preset Buttons
        Text(
            text = "Quick Presets",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickQuietHoursButton(
                startHour = 22,
                endHour = 7,
                label = "Sleep",
                currentQuietHours = quietHours,
                onQuietHoursSelected = { start, end ->
                    viewModel.updateStartTime(LocalTime.of(start, 0))
                    viewModel.updateEndTime(LocalTime.of(end, 0))
                }
            )
            QuickQuietHoursButton(
                startHour = 23,
                endHour = 6,
                label = "Late",
                currentQuietHours = quietHours,
                onQuietHoursSelected = { start, end ->
                    viewModel.updateStartTime(LocalTime.of(start, 0))
                    viewModel.updateEndTime(LocalTime.of(end, 0))
                }
            )
            QuickQuietHoursButton(
                startHour = 21,
                endHour = 8,
                label = "Early",
                currentQuietHours = quietHours,
                onQuietHoursSelected = { start, end ->
                    viewModel.updateStartTime(LocalTime.of(start, 0))
                    viewModel.updateEndTime(LocalTime.of(end, 0))
                }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isOnboarding) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
            
            Button(
                onClick = {
                    viewModel.saveQuietHours()
                    onQuietHoursSet()
                },
                modifier = Modifier.weight(if (isOnboarding) 1f else 1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text(
                    if (isOnboarding) "Continue" else "Save Quiet Hours",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun TimePickerButton(
    time: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    label: String
) {
    var showTimePicker by remember { mutableStateOf(false) }
    
    Button(
        onClick = { showTimePicker = true },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
        modifier = Modifier.size(80.dp)
    ) {
        Text(
            text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
    
    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onTimeSelected = { 
                onTimeSelected(it)
                showTimePicker = false
            },
            initialTime = time
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    initialTime: LocalTime
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            Column {
                // Hour picker
                Text("Hour: $selectedHour")
                Slider(
                    value = selectedHour.toFloat(),
                    onValueChange = { selectedHour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )
                
                // Minute picker
                Text("Minute: $selectedMinute")
                Slider(
                    value = selectedMinute.toFloat(),
                    onValueChange = { selectedMinute = it.toInt() },
                    valueRange = 0f..59f,
                    steps = 58
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuickQuietHoursButton(
    startHour: Int,
    endHour: Int,
    label: String,
    currentQuietHours: QuietHours,
    onQuietHoursSelected: (Int, Int) -> Unit
) {
    val isSelected = currentQuietHours.start.hour == startHour && currentQuietHours.end.hour == endHour
    
    Button(
        onClick = { onQuietHoursSelected(startHour, endHour) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0)
        ),
        modifier = Modifier.size(80.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun calculateQuietHoursDuration(start: LocalTime, end: LocalTime): Int {
    val startMinutes = start.hour * 60 + start.minute
    val endMinutes = end.hour * 60 + end.minute
    
    return if (endMinutes > startMinutes) {
        (endMinutes - startMinutes) / 60
    } else {
        (24 * 60 - startMinutes + endMinutes) / 60
    }
} 