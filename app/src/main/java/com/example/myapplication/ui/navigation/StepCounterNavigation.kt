package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.SettingsScreen

@Composable
fun StepCounterNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                    onClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { 
                HomeScreen(
                    onNavigateToStepCounter = { /* Not used in main flow */ },
                    onNavigateToMoodLog = { /* Not used in main flow */ },
                    onNavigateToSettings = { navController.navigate("settings") }
                ) 
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateToMoodCalendar = { navController.navigate("mood_calendar") },
                    onNavigateToGoalSetup = { navController.navigate("goal_setup") },
                    onNavigateToQuietHoursSetup = { navController.navigate("quiet_hours_setup") },
                    onResetOnboarding = {
                        // This navigation doesn't have onboarding reset functionality
                        // The main AppNavigation handles this
                    }
                )
            }
        }
    }
}