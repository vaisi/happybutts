// Updated: Added loading spinner while onboarding status is loading from DataStore
// Updated: Using EnhancedMoodCalendarScreen for better UX with weekly and daily views
package com.example.myapplication.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.viewmodel.OnboardingViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

object Screen {
    const val Onboarding = "onboarding"
    const val StepCounter = "step_counter"
    const val MoodCalendar = "mood_calendar"
    const val Settings = "settings"
    const val GoalSetup = "goal_setup"
    const val QuietHoursSetup = "quiet_hours_setup"
}

@Composable
fun AppNavigation(
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsState()
    val permissionGranted by onboardingViewModel.permissionGranted.collectAsState()

    // Debug logs
    LaunchedEffect(onboardingCompleted, permissionGranted) {
        android.util.Log.i("AppNavigation", "onboardingCompleted=$onboardingCompleted, permissionGranted=$permissionGranted")
    }

    // Show loading until we have loaded the onboarding status from DataStore
    if (onboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Determine the correct initial screen based on loaded data
    val shouldShowOnboarding = !onboardingCompleted!! || !permissionGranted

    NavHost(
        navController = navController, 
        startDestination = if (shouldShowOnboarding) Screen.Onboarding else Screen.StepCounter
    ) {
        // Onboarding flow
        composable(Screen.Onboarding) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.StepCounter) {
                        popUpTo(Screen.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        // Main app screens
        composable(Screen.StepCounter) {
            StepCounterScreen(
                onNavigateToMoodCalendar = {
                    navController.navigate(Screen.MoodCalendar)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
                }
            )
        }
        composable(Screen.MoodCalendar) {
            EnhancedMoodCalendarScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings) {
            SettingsScreen(
                onNavigateToMoodCalendar = {
                    navController.navigate(Screen.MoodCalendar)
                },
                onNavigateToGoalSetup = {
                    navController.navigate(Screen.GoalSetup)
                },
                onNavigateToQuietHoursSetup = {
                    navController.navigate(Screen.QuietHoursSetup)
                },
                onResetOnboarding = {
                    onboardingViewModel.resetOnboarding()
                    navController.navigate(Screen.Onboarding) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.GoalSetup) {
            GoalSetupScreen(
                onGoalSet = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isOnboarding = true // Show onboarding-style goal setup from settings
            )
        }
        composable(Screen.QuietHoursSetup) {
            QuietHoursSetupScreen(
                onQuietHoursSet = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isOnboarding = false
            )
        }
    }
} 