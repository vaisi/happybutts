// Updated: Modern calendar with visible month, rounded square day cells, small number top-left, large centered character, and smooth light grays
// Updated: Removed premium gate - calendar is now freely accessible
package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues


@Composable
fun MoodCalendarScreen(
    viewModel: StepCounterViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {

    var selectedMonth by remember { mutableStateOf(LocalDate.now()) }
    val moodHistory by viewModel.moodHistory.collectAsState()
    val shape = RoundedCornerShape(16.dp)
    val dayCellShape = RoundedCornerShape(12.dp)
    val dayCellBg = Color(0xFFF7F7F7)
    val dayCellBorder = Color(0xFFEEEEEE)
    val dayNumberColor = Color(0xFFB0B0B0)
    val dayOfWeekColor = Color(0xFF666666)
    val monthTextColor = Color(0xFF444444)
    val arrowColor = Color(0xFF888888)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top bar with Go Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                border = ButtonDefaults.outlinedBorder,
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    tint = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Go Back",
                    color = MaterialTheme.colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        // Month/year header with navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { selectedMonth = selectedMonth.minusMonths(1) }
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", tint = arrowColor)
            }
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = monthTextColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = { selectedMonth = selectedMonth.plusMonths(1) }
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", tint = arrowColor)
            }
        }
        // Days of week header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, start = 8.dp, end = 8.dp),
        ) {
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        // Calendar grid
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val firstDayOfMonth = selectedMonth.withDayOfMonth(1)
                // Add empty cells for days before the first day of the month
                items(firstDayOfMonth.dayOfWeek.value % 7) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                    )
                }
                // Add cells for each day of the month
                items(selectedMonth.lengthOfMonth()) { day ->
                    val date = selectedMonth.withDayOfMonth(day + 1)
                    val moodForDay = moodHistory.find { it.date == date }
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(dayCellShape)
                            .background(dayCellBg)
                            .border(1.dp, dayCellBorder, dayCellShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Day number top-left
                            Text(
                                text = (day + 1).toString(),
                                fontSize = 11.sp,
                                color = dayNumberColor,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 6.dp, top = 4.dp)
                            )
                            // Character image centered and as large as possible
                            if (moodForDay != null) {
                                val moodState = MoodState.fromMoodValue(moodForDay.mood)
                                val (drawableRes, _) = moodToDrawableAndColor(moodState)
                                Image(
                                    painter = painterResource(id = drawableRes),
                                    contentDescription = "Mood for ${date.format(DateTimeFormatter.ISO_DATE)}",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(44.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 