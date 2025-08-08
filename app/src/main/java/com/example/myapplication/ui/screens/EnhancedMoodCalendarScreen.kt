// Created: EnhancedMoodCalendarScreen with weekly and daily views in cute character style
// Updated: Better header navigation and view toggle functionality
package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.R
import com.example.myapplication.data.model.MoodState
import com.example.myapplication.ui.viewmodel.StepCounterViewModel
import com.example.myapplication.util.moodToDrawableAndColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues

enum class CalendarViewType {
    MONTHLY, WEEKLY, DAILY
}

@Composable
fun EnhancedMoodCalendarScreen(
    viewModel: StepCounterViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var viewType by remember { mutableStateOf(CalendarViewType.MONTHLY) }
    val moodHistory by viewModel.moodHistory.collectAsState()
    
    // Log for debugging
    LaunchedEffect(Unit) {
        android.util.Log.d("EnhancedMoodCalendarScreen", "Screen initialized with ${moodHistory.size} mood entries")
    }
    
    // Colors and styling
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFF03DAC6)
    val backgroundColor = Color(0xFFFAFAFA)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val lightTextColor = Color(0xFF666666)
    val borderColor = Color(0xFFE0E0E0)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Enhanced Header with View Toggle
        EnhancedHeader(
            selectedDate = selectedDate,
            viewType = viewType,
            onViewTypeChange = { viewType = it },
            onNavigateBack = onNavigateBack,
            onDateChange = { selectedDate = it },
            primaryColor = primaryColor,
            textColor = textColor
        )
        
        // Content based on view type
        when (viewType) {
            CalendarViewType.MONTHLY -> MonthlyView(
                selectedDate = selectedDate,
                moodHistory = moodHistory,
                onDateSelected = { selectedDate = it },
                cardColor = cardColor,
                borderColor = borderColor,
                textColor = textColor,
                lightTextColor = lightTextColor
            )
            CalendarViewType.WEEKLY -> WeeklyView(
                selectedDate = selectedDate,
                moodHistory = moodHistory,
                onDateSelected = { selectedDate = it },
                cardColor = cardColor,
                borderColor = borderColor,
                textColor = textColor,
                lightTextColor = lightTextColor,
                viewModel = viewModel
            )
            CalendarViewType.DAILY -> DailyView(
                selectedDate = selectedDate,
                moodHistory = moodHistory,
                cardColor = cardColor,
                borderColor = borderColor,
                textColor = textColor,
                lightTextColor = lightTextColor,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun EnhancedHeader(
    selectedDate: LocalDate,
    viewType: CalendarViewType,
    onViewTypeChange: (CalendarViewType) -> Unit,
    onNavigateBack: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    primaryColor: Color,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        // Top bar with back button and view toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            OutlinedButton(
                onClick = onNavigateBack,
                border = ButtonDefaults.outlinedBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = primaryColor
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // View toggle buttons
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewToggleButton(
                    icon = Icons.Default.CalendarMonth,
                    label = "Month",
                    isSelected = viewType == CalendarViewType.MONTHLY,
                    onClick = { onViewTypeChange(CalendarViewType.MONTHLY) },
                    primaryColor = primaryColor,
                    textColor = textColor
                )
                ViewToggleButton(
                    icon = Icons.Default.ViewWeek,
                    label = "Week",
                    isSelected = viewType == CalendarViewType.WEEKLY,
                    onClick = { onViewTypeChange(CalendarViewType.WEEKLY) },
                    primaryColor = primaryColor,
                    textColor = textColor
                )
                ViewToggleButton(
                    icon = Icons.Default.Today,
                    label = "Day",
                    isSelected = viewType == CalendarViewType.DAILY,
                    onClick = { onViewTypeChange(CalendarViewType.DAILY) },
                    primaryColor = primaryColor,
                    textColor = textColor
                )
            }
        }
        
        // Date navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    val newDate = when (viewType) {
                        CalendarViewType.MONTHLY -> selectedDate.minusMonths(1)
                        CalendarViewType.WEEKLY -> selectedDate.minusWeeks(1)
                        CalendarViewType.DAILY -> selectedDate.minusDays(1)
                    }
                    onDateChange(newDate)
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = when (viewType) {
                    CalendarViewType.MONTHLY -> selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    CalendarViewType.WEEKLY -> "Week of ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
                    CalendarViewType.DAILY -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            IconButton(
                onClick = {
                    val newDate = when (viewType) {
                        CalendarViewType.MONTHLY -> selectedDate.plusMonths(1)
                        CalendarViewType.WEEKLY -> selectedDate.plusWeeks(1)
                        CalendarViewType.DAILY -> selectedDate.plusDays(1)
                    }
                    onDateChange(newDate)
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ViewToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (isSelected) primaryColor else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else textColor
            )
        }
    }
}

@Composable
fun MonthlyView(
    selectedDate: LocalDate,
    moodHistory: List<com.example.myapplication.data.model.MoodStateEntity>,
    onDateSelected: (LocalDate) -> Unit,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    lightTextColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = lightTextColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            val firstDayOfMonth = selectedDate.withDayOfMonth(1)
            
            // Empty cells for days before the first day of the month
            items(firstDayOfMonth.dayOfWeek.value % 7) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                )
            }
            
            // Cells for each day of the month
            items(selectedDate.lengthOfMonth()) { day ->
                val date = selectedDate.withDayOfMonth(day + 1)
                val moodForDay = moodHistory.find { it.date == date }
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardColor)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Day number top-left
                        Text(
                            text = (day + 1).toString(),
                            fontSize = 10.sp,
                            color = lightTextColor,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 4.dp, top = 2.dp)
                        )
                        
                        // Character image centered
                        if (moodForDay != null) {
                            val moodState = MoodState.fromMoodValue(moodForDay.mood)
                            val (drawableRes, _) = moodToDrawableAndColor(moodState)
                            Image(
                                painter = painterResource(id = drawableRes),
                                contentDescription = "Mood for ${date.format(DateTimeFormatter.ISO_DATE)}",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyView(
    selectedDate: LocalDate,
    moodHistory: List<com.example.myapplication.data.model.MoodStateEntity>,
    onDateSelected: (LocalDate) -> Unit,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    lightTextColor: Color,
    viewModel: StepCounterViewModel
) {
    val weekStart = selectedDate.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sticky Weekly progress chart with bars and characters
        WeeklyProgressChart(
            weekDays = weekDays,
            moodHistory = moodHistory,
            onDateSelected = onDateSelected,
            viewModel = viewModel,
            textColor = textColor,
            lightTextColor = lightTextColor
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Scrollable daily details below the chart
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(weekDays) { date ->
                val moodForDay = moodHistory.find { it.date == date }
                val isToday = date == LocalDate.now()
                val isSelected = date == selectedDate
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFFE8F5E8) else cardColor,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) Color(0xFF4CAF50) else borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDateSelected(date) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("EEEE")),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("MMM d")),
                                fontSize = 14.sp,
                                color = lightTextColor
                            )
                            if (isToday) {
                                Text(
                                    text = "Today",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Character mood
                        if (moodForDay != null) {
                            val moodState = MoodState.fromMoodValue(moodForDay.mood)
                            val (drawableRes, _) = moodToDrawableAndColor(moodState)
                            Image(
                                painter = painterResource(id = drawableRes),
                                contentDescription = "Mood for ${date.format(DateTimeFormatter.ISO_DATE)}",
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?",
                                    fontSize = 20.sp,
                                    color = lightTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyProgressChart(
    weekDays: List<LocalDate>,
    moodHistory: List<com.example.myapplication.data.model.MoodStateEntity>,
    onDateSelected: (LocalDate) -> Unit,
    viewModel: StepCounterViewModel,
    textColor: Color,
    lightTextColor: Color
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = LocalDate.now()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Progress bars with characters (no title)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weekDays.forEachIndexed { index, date ->
                val moodForDay = moodHistory.find { it.date == date }
                val isToday = date == today
                val isFuture = date.isAfter(today)
                
                // Get daily stats for progress calculation
                val dailyStats by viewModel.getDailyStats(date).collectAsState(initial = null)
                val progress = if (dailyStats?.goal != null && dailyStats?.goal != 0) {
                    (dailyStats?.steps ?: 0).toFloat() / dailyStats?.goal!!.toFloat()
                } else {
                    0f
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDateSelected(date) }
                ) {
                    // Day label
                    Text(
                        text = dayLabels[index],
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isToday) Color(0xFF4CAF50) else lightTextColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Progress bar container
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(180.dp)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Progress bar background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(
                                    if (isFuture) Color(0xFFF5F5F5) else Color(0xFFE8E8E8),
                                    RoundedCornerShape(8.dp)
                                )
                        )
                        
                        // Progress bar fill with softer colors
                        if (!isFuture) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((140 * progress).coerceIn(0f, 140f).dp)
                                    .background(
                                        when {
                                            progress >= 1.0f -> Color(0xFF66BB6A) // Softer green for completed
                                            progress >= 0.6f -> Color(0xFF42A5F5) // Softer blue for good progress
                                            progress >= 0.3f -> Color(0xFFFFB74D) // Softer orange for moderate
                                            else -> Color(0xFFEF5350) // Softer red for low progress
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .align(Alignment.BottomCenter)
                            )
                        }
                        
                        // Character mood on top of the bar (larger size)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        ) {
                            if (moodForDay != null) {
                                val moodState = MoodState.fromMoodValue(moodForDay.mood)
                                val (drawableRes, _) = moodToDrawableAndColor(moodState)
                                Image(
                                    painter = painterResource(id = drawableRes),
                                    contentDescription = "Mood for ${date.format(DateTimeFormatter.ISO_DATE)}",
                                    modifier = Modifier.size(64.dp) // Increased from 48dp
                                )
                            } else if (isFuture) {
                                // Question mark for future days
                                Box(
                                    modifier = Modifier
                                        .size(64.dp) // Increased from 48dp
                                        .background(Color(0xFFF0F0F0), RoundedCornerShape(32.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "?",
                                        fontSize = 24.sp, // Increased from 18sp
                                        color = lightTextColor
                                    )
                                }
                            } else {
                                // No data indicator
                                Box(
                                    modifier = Modifier
                                        .size(64.dp) // Increased from 48dp
                                        .background(Color(0xFFE0E0E0), RoundedCornerShape(32.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "-",
                                        fontSize = 24.sp, // Increased from 18sp
                                        color = lightTextColor
                                    )
                                }
                            }
                        }
                        
                        // Today indicator
                        if (isToday) {
                            Text(
                                text = "LIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 76.dp) // Adjusted for larger character
                            )
                        }
                    }
                    
                    // Progress percentage
                    if (!isFuture && dailyStats != null) {
                        val percentage = (progress * 100).toInt()
                        Text(
                            text = "$percentage%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isToday) Color(0xFF4CAF50) else lightTextColor,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (isFuture) {
                        Text(
                            text = "?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = lightTextColor,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Text(
                            text = "0%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = lightTextColor,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun DailyView(
    selectedDate: LocalDate,
    moodHistory: List<com.example.myapplication.data.model.MoodStateEntity>,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    lightTextColor: Color,
    viewModel: StepCounterViewModel
) {
    val moodForDay = moodHistory.find { it.date == selectedDate }
    val isToday = selectedDate == LocalDate.now()
    
    // Get daily stats with error handling
    val dailyStats by viewModel.getDailyStats(selectedDate).collectAsState(initial = null)
    val inactivityData by viewModel.getInactivityForDate(selectedDate).collectAsState(initial = emptyList())
    
    // Log for debugging
    LaunchedEffect(selectedDate) {
        try {
            android.util.Log.d("DailyView", "Selected date: $selectedDate, moodForDay: ${moodForDay != null}, dailyStats: $dailyStats, inactivityData: ${inactivityData.size} periods")
        } catch (e: Exception) {
            android.util.Log.e("DailyView", "Error in DailyView", e)
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            // Day header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    fontSize = 16.sp,
                    color = lightTextColor
                )
                if (isToday) {
                    Text(
                        text = "Today",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        item {
            // Mood card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardColor)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mood",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (moodForDay != null) {
                        val moodState = MoodState.fromMoodValue(moodForDay.mood)
                        val (drawableRes, moodColor) = moodToDrawableAndColor(moodState)
                        
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = "Mood for ${selectedDate.format(DateTimeFormatter.ISO_DATE)}",
                            modifier = Modifier.size(80.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = moodState.description,
                            fontSize = 16.sp,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Mood Score: ${moodForDay.mood}",
                            fontSize = 14.sp,
                            color = lightTextColor
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                fontSize = 32.sp,
                                color = lightTextColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No mood data for this day",
                            fontSize = 16.sp,
                            color = lightTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Daily Stats Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardColor)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Daily Stats",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Goal and Steps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "Goal",
                            value = dailyStats?.goal?.toString() ?: "0",
                            color = textColor
                        )
                        StatItem(
                            label = "Steps",
                            value = dailyStats?.steps?.toString() ?: "0",
                            color = textColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Completion Percentage
                    val completionPercentage = if (dailyStats?.goal != null && dailyStats?.goal != 0) {
                        ((dailyStats?.steps ?: 0) * 100 / dailyStats?.goal!!).coerceIn(0, 100)
                    } else {
                        0
                    }
                    
                    StatItem(
                        label = "Completion",
                        value = "$completionPercentage%",
                        color = if (completionPercentage >= 100) Color(0xFF4CAF50) else textColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = completionPercentage / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        backgroundColor = Color(0xFFE0E0E0),
                        color = if (completionPercentage >= 100) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Inactive Hours
                    val totalInactiveHours = inactivityData.sumOf { it.durationHours.toDouble() }.toFloat()
                    StatItem(
                        label = "Inactive Hours",
                        value = String.format("%.1f", totalInactiveHours),
                        color = if (totalInactiveHours > 8) Color(0xFFFF5722) else textColor
                    )
                }
            }
        }
        
        // Hourly Step Chart
        item {
            HourlyStepChart(
                selectedDate = selectedDate,
                viewModel = viewModel,
                cardColor = cardColor,
                borderColor = borderColor,
                textColor = textColor,
                lightTextColor = lightTextColor
            )
        }
        
        // Inactivity Details
        if (inactivityData.isNotEmpty()) {
            item {
                Text(
                    text = "Inactivity Periods",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            items(inactivityData) { inactivity ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Period",
                                fontSize = 12.sp,
                                color = lightTextColor
                            )
                            Text(
                                text = "${inactivity.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
                                    inactivity.endTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Active"
                                }",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Duration",
                                fontSize = 12.sp,
                                color = lightTextColor
                            )
                            Text(
                                text = String.format("%.1f hours", inactivity.durationHours),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
} 

@Composable
fun HourlyStepChart(
    selectedDate: LocalDate,
    viewModel: StepCounterViewModel,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    lightTextColor: Color
) {
    val hourlySteps by viewModel.getHourlyStepsForDate(selectedDate).collectAsState(initial = emptyList())
    val isToday = selectedDate == LocalDate.now()
    
    // Add error handling for the chart
    LaunchedEffect(selectedDate) {
        try {
            android.util.Log.d("HourlyStepChart", "Loading hourly data for $selectedDate")
        } catch (e: Exception) {
            android.util.Log.e("HourlyStepChart", "Error loading hourly data", e)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Chart header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today, ${selectedDate.format(DateTimeFormatter.ofPattern("MMMM d"))}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                if (isToday) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = lightTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Chart area with grid
            if (hourlySteps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    val maxSteps = hourlySteps.maxOfOrNull { it.steps } ?: 1
                    val maxValue = if (maxSteps > 0) maxSteps else 1
                    
                    // Y-axis labels (right side)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(maxValue * 0.75).toInt()}",
                            fontSize = 10.sp,
                            color = lightTextColor
                        )
                        Text(
                            text = "${(maxValue * 0.5).toInt()}",
                            fontSize = 10.sp,
                            color = lightTextColor
                        )
                        Text(
                            text = "${(maxValue * 0.25).toInt()}",
                            fontSize = 10.sp,
                            color = lightTextColor
                        )
                        Text(
                            text = "0",
                            fontSize = 10.sp,
                            color = lightTextColor
                        )
                    }
                    
                    // Horizontal grid lines
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 30.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFF0F0F0))
                            )
                        }
                    }
                    
                    // Bars
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 30.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Generate 24 hours (0-23)
                        for (hour in 0..23) {
                            val hourData = hourlySteps.find { it.hour == hour }
                            val steps = hourData?.steps ?: 0
                            val progress = if (maxValue > 0) steps.toFloat() / maxValue.toFloat() else 0f
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp)
                                        .height((160 * progress).coerceIn(0f, 160f).dp)
                                        .background(
                                            when {
                                                progress >= 0.8f -> Color(0xFF4CAF50) // High activity
                                                progress >= 0.5f -> Color(0xFF2196F3) // Medium activity
                                                progress >= 0.2f -> Color(0xFFFF9800) // Low activity
                                                else -> Color(0xFFFF5722) // Very low activity
                                            },
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                
                                // Hour label (only show every 4 hours to avoid clutter)
                                if (hour % 4 == 0) {
                                    Text(
                                        text = hour.toString(),
                                        fontSize = 10.sp,
                                        color = lightTextColor,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // FIXED: Use last recorded total instead of summing hourly steps
                    val totalSteps = hourlySteps.maxByOrNull { it.hour }?.lastRecordedTotal ?: hourlySteps.sumOf { it.steps }
                    val activeHours = hourlySteps.count { it.steps > 0 }
                    val avgStepsPerActiveHour = if (activeHours > 0) totalSteps / activeHours else 0
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = totalSteps.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Total Steps",
                            fontSize = 10.sp,
                            color = lightTextColor
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeHours.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Active Hours",
                            fontSize = 10.sp,
                            color = lightTextColor
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = avgStepsPerActiveHour.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Avg/Hour",
                            fontSize = 10.sp,
                            color = lightTextColor
                        )
                    }
                }
            } else {
                // Fallback UI when no data available
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No hourly data available",
                                fontSize = 14.sp,
                                color = lightTextColor
                            )
                            Text(
                                text = "Check back later for activity details",
                                fontSize = 12.sp,
                                color = lightTextColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
} 