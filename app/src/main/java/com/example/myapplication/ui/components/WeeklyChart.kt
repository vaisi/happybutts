package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.StepData
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun WeeklyChart(
    weeklySteps: List<StepData>,
    modifier: Modifier = Modifier
) {
    val maxSteps = weeklySteps.maxOfOrNull { it.steps } ?: 10000
    val barColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            drawWeeklyBars(weeklySteps, maxSteps, barColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val today = LocalDate.now()
            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    fontSize = 12.sp,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

fun DrawScope.drawWeeklyBars(
    weeklySteps: List<StepData>,
    maxSteps: Int,
    barColor: Color
) {
    val barWidth = size.width / 7f * 0.6f
    val spacing = size.width / 7f
    val maxBarHeight = size.height * 0.9f

    val today = LocalDate.now()
    val stepMap = weeklySteps.associateBy { it.date }

    for (i in 6 downTo 0) {
        val date = today.minusDays(i.toLong())
        val steps = stepMap[date]?.steps ?: 0
        val barHeight = (steps.toFloat() / maxSteps) * maxBarHeight

        val x = (6 - i) * spacing + (spacing - barWidth) / 2
        val y = size.height - barHeight

        drawRect(
            color = barColor.copy(alpha = if (i == 0) 1f else 0.7f),
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight)
        )
    }
}