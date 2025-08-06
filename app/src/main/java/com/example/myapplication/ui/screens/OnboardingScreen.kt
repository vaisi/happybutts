// Updated: Changed onboarding step texts for screens 2, 3, and 4 as per user request.
// Updated: Increased onboarding image size by 20% for better visual impact.
// Updated: NotificationPermissionStep now uses a white background and larger image. All onboarding buttons are now placed at the bottom of the screen for consistent UX.
// Updated: Onboarding step text now uses 16.sp, thin font, and only the word 'Butts' is bolded.
// Updated: Onboarding images are now placed around the middle of the screen, slightly above center. Font size increased to 18.sp and the same style (light font, only 'Butts' bolded) is used for all onboarding screens, including the 7th.
// Refactored: Implements 7-step state machine onboarding flow with data-driven steps, matching new design and requirements. Handles fitness (mandatory) and notification (skippable) permissions, and goal setting. Each step uses the correct image and text.
package com.example.myapplication.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.viewmodel.OnboardingViewModel
import com.example.myapplication.R
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.myapplication.ui.theme.OnboardingButtonColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

// Data model for onboarding steps
private data class OnboardingStepData(
    val imageRes: Int,
    val title: String?,
    val description: String,
    val ctaText: String,
    val type: StepType
)

private enum class StepType {
    INTRO, FITNESS_PERMISSION, GOAL, NOTIFICATION_PERMISSION
}

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    var currentStep by remember { mutableStateOf(0) }
    var fitnessPermissionGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }

    // Permission launchers
    val fitnessPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        fitnessPermissionGranted = isGranted
        if (isGranted) {
            currentStep++
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
        // Complete onboarding regardless of grant/skip
        viewModel.completeOnboarding()
    }

    // Onboarding steps definition (update imageRes as you add onboarding_1.png, etc.)
    val steps = listOf(
        OnboardingStepData(
            imageRes = R.drawable.onboarding_1,
            title = null,
            description = "Meet Butts your walking buddy who feels what you feel!",
            ctaText = "Hey butts",
            type = StepType.INTRO
        ),
        OnboardingStepData(
            imageRes = R.drawable.onboarding_2,
            title = null,
            description = "when you move your Butts glows",
            ctaText = "Next",
            type = StepType.INTRO
        ),
        OnboardingStepData(
            imageRes = R.drawable.onboarding_3,
            title = null,
            description = "sit too long? your Butts gets the blues",
            ctaText = "Next",
            type = StepType.INTRO
        ),
        OnboardingStepData(
            imageRes = R.drawable.onboarding_4,
            title = null,
            description = "a few steps go a long way for your Butts",
            ctaText = "Next",
            type = StepType.INTRO
        ),
        OnboardingStepData(
            imageRes = R.drawable.onboarding_5,
            title = null,
            description = "Butts needs to be able to count your steps",
            ctaText = "Let Butts track steps",
            type = StepType.FITNESS_PERMISSION
        ),
        OnboardingStepData(
            imageRes = R.drawable.onboarding_6,
            title = null,
            description = "Choose how much you plan to walk your butts everyday.",
            ctaText = "Save goal",
            type = StepType.GOAL
        ),
        OnboardingStepData(
            imageRes = R.drawable.onboarding_7,
            title = null,
            description = "Allow your butts to protest when you've been sitting for too long",
            ctaText = "Enable notifications",
            type = StepType.NOTIFICATION_PERMISSION
        )
    )

    // Check if onboarding is already completed
    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted == true) {
            onOnboardingComplete()
        }
    }

    val step = steps.getOrNull(currentStep)
    if (step == null) return

    when (step.type) {
        StepType.INTRO -> {
            OnboardingStepComposable(
                imageRes = step.imageRes,
                description = step.description,
                ctaText = step.ctaText,
                onNext = { currentStep++ }
            )
        }
        StepType.FITNESS_PERMISSION -> {
            OnboardingStepComposable(
                imageRes = step.imageRes,
                description = step.description,
                ctaText = step.ctaText,
                onNext = {
                    fitnessPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                },
                isPermissionStep = true,
                permissionGranted = fitnessPermissionGranted
            )
        }
        StepType.GOAL -> {
            GoalSetupScreen(
                onGoalSet = { currentStep++ },
                onNavigateBack = {},
                isOnboarding = true
            )
        }
        StepType.NOTIFICATION_PERMISSION -> {
            NotificationPermissionStep(
                imageRes = step.imageRes,
                description = step.description,
                ctaText = step.ctaText,
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Complete onboarding if not required
                        viewModel.completeOnboarding()
                    }
                },
                onSkip = {
                    viewModel.completeOnboarding()
                }
            )
        }
    }
}

@Composable
private fun OnboardingStepComposable(
    imageRes: Int,
    description: String,
    ctaText: String,
    onNext: () -> Unit,
    isPermissionStep: Boolean = false,
    permissionGranted: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(264.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                val annotatedText = buildAnnotatedString {
                    val regex = Regex("Butts", RegexOption.IGNORE_CASE)
                    var lastIndex = 0
                    regex.findAll(description).forEach { result ->
                        val start = result.range.first
                        val end = result.range.last + 1
                        append(description.substring(lastIndex, start))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(description.substring(start, end))
                        }
                        lastIndex = end
                    }
                    if (lastIndex < description.length) {
                        append(description.substring(lastIndex))
                    }
                }
                Text(
                    text = annotatedText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
                    .height(56.dp),
                enabled = !isPermissionStep || !permissionGranted,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnboardingButtonColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = ctaText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun NotificationPermissionStep(
    imageRes: Int,
    description: String,
    ctaText: String,
    onRequestPermission: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(264.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                val annotatedText = buildAnnotatedString {
                    val regex = Regex("Butts", RegexOption.IGNORE_CASE)
                    var lastIndex = 0
                    regex.findAll(description).forEach { result ->
                        val start = result.range.first
                        val end = result.range.last + 1
                        append(description.substring(lastIndex, start))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(description.substring(start, end))
                        }
                        lastIndex = end
                    }
                    if (lastIndex < description.length) {
                        append(description.substring(lastIndex))
                    }
                }
                Text(
                    text = annotatedText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnboardingButtonColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = ctaText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Maybe later")
                }
            }
        }
    }
} 