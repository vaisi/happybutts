package com.example.myapplication.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.service.StepCountingService
import com.example.myapplication.ui.components.WeeklyChart
import com.example.myapplication.ui.viewmodel.HomeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlin.math.sin
import kotlin.math.PI
import androidx.navigation.compose.*
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val todaySteps by viewModel.todaySteps.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val calories by viewModel.calories.collectAsState()
    val weeklySteps by viewModel.weeklySteps.collectAsState()

    val activityRecognitionPermission = rememberPermissionState(
        permission = Manifest.permission.ACTIVITY_RECOGNITION
    )

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else null

    LaunchedEffect(activityRecognitionPermission.status) {
        Log.i("HomeScreen", "LaunchedEffect(permission) triggered - status: ${activityRecognitionPermission.status}")
        
        if (activityRecognitionPermission.status.isGranted) {
            Log.i("HomeScreen", "Permission granted in original LaunchedEffect, starting service...")
            
            // Start the service with proper action
            val serviceIntent = Intent(context, StepCountingService::class.java).apply {
                action = StepCountingService.ACTION_START_STEP_COUNTING
            }
            
            try {
                context.startForegroundService(serviceIntent)
                Log.i("HomeScreen", "Service started from original LaunchedEffect")
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to start service from original LaunchedEffect", e)
            }
        } else if (!activityRecognitionPermission.status.shouldShowRationale) {
            Log.i("HomeScreen", "Requesting permission...")
            activityRecognitionPermission.launchPermissionRequest()
        } else {
            Log.w("HomeScreen", "Permission denied and should show rationale")
        }
    }
    
    // Always ensure service is running when screen is displayed
    LaunchedEffect(Unit) {
        Log.i("HomeScreen", "LaunchedEffect(Unit) triggered - checking service status")
        Log.i("HomeScreen", "Permission status: ${activityRecognitionPermission.status}")
        
        if (activityRecognitionPermission.status.isGranted) {
            Log.i("HomeScreen", "Permission granted, starting service...")
            
            // Small delay to ensure permission check is complete
            kotlinx.coroutines.delay(500)
            
            // Always try to start the service when HomeScreen is displayed
            val serviceIntent = Intent(context, StepCountingService::class.java).apply {
                action = StepCountingService.ACTION_START_STEP_COUNTING
            }
            
            Log.i("HomeScreen", "Starting service with action: ${serviceIntent.action}")
            Log.i("HomeScreen", "Service class: ${StepCountingService::class.java.name}")
            
            try {
                context.startForegroundService(serviceIntent)
                Log.i("HomeScreen", "Service start command sent successfully")
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to start service", e)
            }
        } else {
            Log.w("HomeScreen", "Permission not granted, cannot start service")
        }
    }

    LaunchedEffect(notificationPermission?.status) {
        notificationPermission?.let {
            if (!it.status.isGranted && !it.status.shouldShowRationale) {
                it.launchPermissionRequest()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Today's Steps",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // DEBUG: Test mood decay button
        Button(
            onClick = { viewModel.debugTriggerMoodDecay() },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("DEBUG: Trigger Mood Decay")
        }

        // DEBUG: Check worker status button
        Button(
            onClick = { viewModel.debugCheckWorkerStatus() },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("DEBUG: Check Worker Status")
        }

        // DEBUG: Test service start button
        Button(
            onClick = { 
                val serviceIntent = Intent(context, StepCountingService::class.java).apply {
                    action = StepCountingService.ACTION_START_STEP_COUNTING
                }
                context.startForegroundService(serviceIntent)
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("DEBUG: Start Service")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Circular Progress Indicator
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(250.dp)
        ) {
            CircularProgressIndicator(
                steps = todaySteps,
                goal = dailyGoal,
                modifier = Modifier.fillMaxSize()
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$todaySteps",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "of $dailyGoal steps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatsCard(
                title = "Calories",
                value = String.format("%.1f", calories),
                unit = "kcal"
            )
            StatsCard(
                title = "Distance",
                value = String.format("%.2f", todaySteps * 0.0008), // Approximate km
                unit = "km"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Weekly Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Weekly Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                WeeklyChart(
                    weeklySteps = weeklySteps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        if (!activityRecognitionPermission.status.isGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Activity recognition permission is needed to count steps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { activityRecognitionPermission.launchPermissionRequest() }
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

@Composable
fun CircularProgressIndicator(
    steps: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val progressColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        drawCircularProgress(
            progress = animatedProgress,
            progressColor = progressColor,
            backgroundColor = backgroundColor
        )
    }
}

fun DrawScope.drawCircularProgress(
    progress: Float,
    progressColor: Color,
    backgroundColor: Color
) {
    val strokeWidth = 20.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)

    // Background arc
    drawArc(
        color = backgroundColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Progress arc
    drawArc(
        color = progressColor,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    unit: String
) {
    Card(
        modifier = Modifier.width(150.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun HappyCharacter(infiniteTransition: InfiniteTransition) {
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(modifier = Modifier.size(150.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-bounce).dp)
        ) {
            // Body
            drawCircle(
                color = Color(0xFFFFD93D),
                radius = 50.dp.toPx()
            )

            // Eyes
            drawCircle(
                color = Color.Black,
                radius = 5.dp.toPx(),
                center = center + Offset(-15.dp.toPx(), -10.dp.toPx())
            )
            drawCircle(
                color = Color.Black,
                radius = 5.dp.toPx(),
                center = center + Offset(15.dp.toPx(), -10.dp.toPx())
            )

            // Smile
            drawArc(
                color = Color.Black,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = center + Offset(-20.dp.toPx(), 0.dp.toPx()),
                size = Size(40.dp.toPx(), 20.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun SleepingCharacter(infiniteTransition: InfiniteTransition) {
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = Modifier.size(150.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Body (slightly squished)
            drawOval(
                color = Color(0xFFFFD93D).copy(alpha = alpha),
                topLeft = Offset(center.x - 50.dp.toPx(), center.y - 40.dp.toPx()),
                size = Size(100.dp.toPx(), 80.dp.toPx())
            )

            // Closed eyes (lines)
            drawLine(
                color = Color.Black,
                start = center + Offset(-20.dp.toPx(), -10.dp.toPx()),
                end = center + Offset(-10.dp.toPx(), -10.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.Black,
                start = center + Offset(10.dp.toPx(), -10.dp.toPx()),
                end = center + Offset(20.dp.toPx(), -10.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Z's for sleeping
            drawText(
                textMeasurer = textMeasurer,
                text = "Z",
                topLeft = center + Offset(40.dp.toPx(), -40.dp.toPx()),
                style = TextStyle(fontSize = 20.sp)
            )
        }
    }
}