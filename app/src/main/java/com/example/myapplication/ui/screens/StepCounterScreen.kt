// Updated: Changed background color to #f9fafb, added white circle around butts character, made circle smaller (85%), added clip mask to prevent GIF corners from showing outside circle, and added additional white background layer to ensure 100% white coverage during GIF playback
// Updated: Removed permission check and request UI from StepCounterScreen. Permission is now handled only in onboarding.
// Fixed: Removed debug controls (test mood jump, test daily reset) and view mood log feature
// Fixed: Made mood message reactive to mood changes using remember with mood as key
// Updated: Reorganized layout - centered all elements, moved mood value above progress bar, moved mood label below progress bar
// Updated: Removed test buttons from main screen
// Updated: Added tap interaction with bounce/jiggle effect to character
// Updated: Added MP4 video animation playback when character is tapped
package com.example.myapplication.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.viewmodel.StepCounterViewModel
import com.example.myapplication.data.model.MoodState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.example.myapplication.R
import androidx.navigation.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.isGranted
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import com.example.myapplication.util.moodToDrawableAndColor
import com.example.myapplication.data.model.getRandomMoodMainScreenMessage
import com.example.myapplication.ui.components.MoodVideoPlayer
import com.example.myapplication.service.StepCountingService

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StepCounterScreen(
    onNavigateToMoodCalendar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: StepCounterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Always ensure service is running when screen is displayed
    LaunchedEffect(Unit) {
        Log.i("StepCounterScreen", "LaunchedEffect(Unit) triggered - ensuring service is running")
        
        // Small delay to ensure everything is initialized
        kotlinx.coroutines.delay(500)
        
        // Always try to start the service when StepCounterScreen is displayed
        val serviceIntent = Intent(context, StepCountingService::class.java).apply {
            action = StepCountingService.ACTION_START_STEP_COUNTING
        }
        
        Log.i("StepCounterScreen", "Starting service with action: ${serviceIntent.action}")
        Log.i("StepCounterScreen", "Service class: ${StepCountingService::class.java.name}")
        
        try {
            context.startForegroundService(serviceIntent)
            Log.i("StepCounterScreen", "Service start command sent successfully")
        } catch (e: Exception) {
            Log.e("StepCounterScreen", "Failed to start service", e)
        }
    }
    
    // Also ensure service starts when screen becomes visible (for navigation back)
    LaunchedEffect(Unit) {
        // This will run every time the composable is recomposed
        Log.i("StepCounterScreen", "Screen recomposed - checking service status")
        
        // Small delay to ensure everything is initialized
        kotlinx.coroutines.delay(1000)
        
        // Always try to start the service
        val serviceIntent = Intent(context, StepCountingService::class.java).apply {
            action = StepCountingService.ACTION_START_STEP_COUNTING
        }
        
        try {
            context.startForegroundService(serviceIntent)
            Log.i("StepCounterScreen", "Service start command sent from recomposition")
        } catch (e: Exception) {
            Log.e("StepCounterScreen", "Failed to start service from recomposition", e)
        }
    }
    
    // Removed permission check and request UI. Permission is now handled only in onboarding.

    val realTimeMood by viewModel.realTimeMood.collectAsState()
    val moodProgress by viewModel.moodProgress.collectAsState()
    val moodState = MoodState.fromMoodValue(realTimeMood)
    val (characterRes, progressColor) = moodToDrawableAndColor(moodState)
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val topSpacer = (screenHeight * 0.20f)
    val betweenBarAndStepsSpacer = (screenHeight * 0.20f)
    val imageSize = 180.dp * 1.6f // 60% larger

    // Debug logging for UI state
    LaunchedEffect(uiState.currentSteps) {
        Log.d("StepCounterScreen", "UI State updated - Steps: ${uiState.currentSteps}, Mood: ${uiState.currentMood}")
    }

    // Get the actual current goal from the ViewModel
    val currentGoal by viewModel.getCurrentGoal().collectAsState(initial = 10000)
    
    // Calculate actual values based on current goal
    val stepsPerMood by viewModel.getStepsPerMood().collectAsState(initial = 100)
    val decayThreshold by viewModel.getDecayThreshold().collectAsState(initial = 500)
    val dailyGoal = currentGoal // Use the actual current goal

    // Temporary debug button to fix mood
    LaunchedEffect(Unit) {
        // Auto-fix mood calculation on screen load
        viewModel.fixMoodCalculation()
    }

    // Animated progress value
    val animatedProgress by animateFloatAsState(
        targetValue = moodProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    // Animated character scale for mood changes
    var characterScale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = characterScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "character"
    )

    // Animate character when mood changes
    LaunchedEffect(realTimeMood) {
        // Trigger a bounce animation when mood changes
        characterScale = 1.1f
        kotlinx.coroutines.delay(100)
        characterScale = 1f
    }

    // Tap animation state for character interaction
    var isTapped by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (isTapped) 0.9f else 1f,
        animationSpec = tween(
            durationMillis = 150,
            easing = FastOutSlowInEasing
        ),
        label = "tap"
    )

    var showMoodInfo by remember { mutableStateOf(false) }
    var showVideoAnimation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Mood message: deterministic for each mood value
    val moodMessage = remember(realTimeMood) {
        getRandomMoodMainScreenMessage(realTimeMood)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Calendar icon (top right, original size, plain image)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Settings button (top left)
            IconButton(
                onClick = { onNavigateToSettings() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp, start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Calendar icon (top right)
            Image(
                painter = painterResource(id = R.drawable.calendar_icon),
                contentDescription = "Mood History",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(51.dp)
                    .clickable { onNavigateToMoodCalendar() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topSpacer))
            // Mood message above character
            Text(
                text = moodMessage,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            // Character image with GIF animation inside white circle
            Box(
                modifier = Modifier.size(imageSize),
                contentAlignment = Alignment.Center
            ) {
                // White circle background with clip mask
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f) // Make circle smaller (85% of original size)
                        .background(Color.White, CircleShape)
                        .clip(CircleShape) // Clip mask to ensure content stays within circle
                        .scale(animatedScale * tapScale)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isTapped = true
                            showVideoAnimation = true
                            // Reset tap state after animation
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(150)
                                isTapped = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Additional white background layer to ensure 100% white coverage
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        // Static character image (always visible when animation is not playing)
                        Image(
                            painter = painterResource(id = characterRes),
                            contentDescription = "Mood Character",
                            modifier = Modifier
                                .fillMaxSize(0.9f) // Make character slightly smaller to fit in circle
                        )
                        
                        // GIF animation (always present but only visible when playing)
                        MoodVideoPlayer(
                            moodState = moodState,
                            isVisible = showVideoAnimation,
                            onVideoEnd = { showVideoAnimation = false },
                            modifier = Modifier.fillMaxSize(0.9f) // Make animation slightly smaller to fit in circle
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            // Mood value above progress bar
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    text = "$realTimeMood",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 36.sp
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showMoodInfo = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Mood Info",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Progress bar (color based on mood)
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp),
                color = progressColor,
                trackColor = Color(0xFFE0E0E0)
            )
            
            // Mood label below progress bar
            Text(
                text = "mood",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 20.sp
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(betweenBarAndStepsSpacer))
            
            // Step count (centered)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${uiState.currentSteps}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 36.sp
                    )
                )
                Text(
                    text = "steps",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Black,
                        fontSize = 20.sp
                    )
                )
            }
            

        }


    }

    if (showMoodInfo) {
        AlertDialog(
            onDismissRequest = { showMoodInfo = false },
            title = { Text("Mood Score Info") },
            text = {
                Column {
                    Text("Current mood: $realTimeMood", fontWeight = FontWeight.Bold)
                    Text("Current goal: $dailyGoal steps", fontWeight = FontWeight.Bold)
                    Text("Steps for +1 mood: $stepsPerMood", fontWeight = FontWeight.Bold)
                    Text("Butts decay threshold: $decayThreshold steps/hour", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your mood score reflects your activity today. Walk more to improve your mood! Avoid inactivity to prevent mood decay. Each mood stage has a different face and color. Try to keep your mood high for a happy virtual pet!")
                }
            },
            confirmButton = {
                Button(onClick = { showMoodInfo = false }) { Text("OK") }
            }
        )
    }
} 