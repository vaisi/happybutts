package com.example.myapplication.ui.screens

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.model.MoodNotificationSettings
import com.example.myapplication.ui.viewmodel.SettingsViewModel
import com.example.myapplication.service.StepCountingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToMoodCalendar: () -> Unit,
    onNavigateToGoalSetup: () -> Unit,
    onNavigateToQuietHoursSetup: () -> Unit,
    onResetOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.moodNotificationSettings.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Goal and Quiet Hours Configuration Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "App Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Configure your daily goals and notification preferences",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Daily Goal Configuration
                Button(
                    onClick = onNavigateToGoalSetup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("🎯 Configure Daily Step Goal", color = Color.White)
                }
                
                // Quiet Hours Configuration
                Button(
                    onClick = onNavigateToQuietHoursSetup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text("🌙 Configure Quiet Hours", color = Color.White)
                }
            }
        }
        
        // Mood Notifications Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Mood Notifications",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Get notified when your character's mood drops!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Enable/Disable notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable mood alerts")
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateMoodNotificationSettings(settings.copy(enabled = enabled))
                        }
                    )
                }
                
                if (settings.enabled) {
                    // Mood drop threshold
                    Column {
                        Text("Alert when mood drops by ${settings.moodDropThreshold} levels")
                        Slider(
                            value = settings.moodDropThreshold.toFloat(),
                            onValueChange = { value ->
                                viewModel.updateMoodNotificationSettings(settings.copy(moodDropThreshold = value.toInt()))
                            },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }
                    
                    // Max notifications per day
                    Column {
                        Text("Max notifications per day: ${settings.maxNotificationsPerDay}")
                        Slider(
                            value = settings.maxNotificationsPerDay.toFloat(),
                            onValueChange = { value ->
                                viewModel.updateMoodNotificationSettings(settings.copy(maxNotificationsPerDay = value.toInt()))
                            },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }
                    
                    // Minimum hours between notifications
                    Column {
                        Text("Minimum ${settings.minHoursBetweenNotifications} hours between notifications")
                        Slider(
                            value = settings.minHoursBetweenNotifications.toFloat(),
                            onValueChange = { value ->
                                viewModel.updateMoodNotificationSettings(settings.copy(minHoursBetweenNotifications = value.toInt()))
                            },
                            valueRange = 1f..6f,
                            steps = 4
                        )
                    }
                    
                    // Notification preferences
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound")
                        Switch(
                            checked = settings.notificationSound,
                            onCheckedChange = { sound ->
                                viewModel.updateMoodNotificationSettings(settings.copy(notificationSound = sound))
                            }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration")
                        Switch(
                            checked = settings.notificationVibration,
                            onCheckedChange = { vibration ->
                                viewModel.updateMoodNotificationSettings(settings.copy(notificationVibration = vibration))
                            }
                        )
                    }
                }
            }
        }

        // Test Notifications Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Test Notifications",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Test the notification system to make sure it's working properly!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Test Mood Notification Button
                Button(
                    onClick = {
                        viewModel.testMoodNotification()
                        Toast.makeText(context, "Testing mood notification...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("🧠 Test Mood Notification", color = Color.White)
                }
                
                // Test Inactivity Notification Button
                Button(
                    onClick = {
                        viewModel.testInactivityNotification()
                        Toast.makeText(context, "Testing inactivity notification...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("⏰ Test Inactivity Notification", color = Color.White)
                }
                
                // Test Mood Decay Button
                Button(
                    onClick = {
                        viewModel.testMoodDecay()
                        Toast.makeText(context, "Testing mood decay...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text("📉 Test Mood Decay (1 Hour)", color = Color.White)
                }
                
                // Test Service Start Button
                Button(
                    onClick = {
                        try {
                            Log.i("SettingsScreen", "Testing service start...")
                            
                            // Start the service with proper action
                            val serviceIntent = Intent(context, StepCountingService::class.java).apply {
                                action = StepCountingService.ACTION_START_STEP_COUNTING
                            }
                            
                            // Log the intent details
                            Log.i("SettingsScreen", "Starting service with action: ${serviceIntent.action}")
                            Log.i("SettingsScreen", "Service class: ${StepCountingService::class.java.name}")
                            
                            // Start the service
                            context.startForegroundService(serviceIntent)
                            
                            // Show success toast
                            Toast.makeText(context, "✅ Service start test initiated! Check logcat for details.", Toast.LENGTH_LONG).show()
                            
                            Log.i("SettingsScreen", "Service start command sent successfully")
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to start service", e)
                            Toast.makeText(context, "❌ Service start failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B))
                ) {
                    Text("🔧 Test Foreground Service", color = Color.White)
                }
                
                // Cancel All Notifications Button
                Button(
                    onClick = {
                        viewModel.cancelAllNotifications()
                        Toast.makeText(context, "Cancelled all notifications", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("❌ Cancel All Notifications", color = Color.White)
                }
            }
        }
        
        // Navigation Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Navigation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Button(
                    onClick = onNavigateToMoodCalendar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mood Calendar")
                }
                

            }
        }
        
        // App Info Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "App Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text("Version: 1.0.0")
                Text("HappyButts - Step Counter & Mood Tracker")
                Text("Stay active and happy! 🚶‍♂️😊")
            }
        }
        
        // Reset Onboarding Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Advanced Options",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = "Reset the onboarding flow to see the intro screens again",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Button(
                    onClick = onResetOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("🔄 Reset Onboarding", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
        
        // Add database reset button for testing
        Button(
            onClick = {
                // Force database reset
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("migration_failed", true).apply()
                
                // Show toast
                Toast.makeText(context, "Database reset triggered. Restart app to apply.", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("🔧 FORCE DATABASE RESET (Testing Only)", color = Color.White)
        }
        
        // Add DataStore clear button for testing
        Button(
            onClick = {
                // Clear DataStore data for testing
                viewModel.clearDataStoreForTesting()
                Toast.makeText(context, "DataStore cleared. Restart app to see onboarding.", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
        ) {
            Text("🗑️ CLEAR DATASTORE (Testing Only)", color = Color.White)
        }
    }
}