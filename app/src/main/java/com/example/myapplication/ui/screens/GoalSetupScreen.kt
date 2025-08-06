// Redesigned: Onboarding goal step now matches the provided screenshot and requirements. Only one text, dynamic character image, custom progress bar (3k-20k, 100 step increments), and Save button at the bottom. All extra info and quick buttons removed for onboarding mode.
package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.viewmodel.GoalSetupViewModel
import com.example.myapplication.R
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.example.myapplication.ui.theme.OnboardingButtonColor
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSetupScreen(
    onGoalSet: () -> Unit,
    onNavigateBack: () -> Unit,
    isOnboarding: Boolean = false,
    viewModel: GoalSetupViewModel = hiltViewModel()
) {
    val goal by viewModel.currentGoal.collectAsState()
    val context = LocalContext.current
    
    if (isOnboarding) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dynamic character image with bounce effect
                    val characterRes = when {
                        goal < 5000 -> R.drawable.goal_1
                        goal < 7000 -> R.drawable.goal_2
                        goal < 12000 -> R.drawable.goal_3
                        goal < 15000 -> R.drawable.goal_4
                        else -> R.drawable.goal_5
                    }
                    var lastCharacterRes by remember { mutableStateOf(characterRes) }
                    var bounceTrigger by remember { mutableStateOf(0) }
                    if (characterRes != lastCharacterRes) {
                        lastCharacterRes = characterRes
                        bounceTrigger++
                    }
                    val bounceScale by animateFloatAsState(
                        targetValue = if (bounceTrigger > 0) 1.15f else 1f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
                        label = "characterBounce"
                    )
                    LaunchedEffect(bounceTrigger) {
                        if (bounceTrigger > 0) {
                            kotlinx.coroutines.delay(180)
                            bounceTrigger = 0
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Image(
                        painter = painterResource(id = characterRes),
                        contentDescription = null,
                        modifier = Modifier.size(220.dp).graphicsLayer(scaleX = bounceScale, scaleY = bounceScale)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Only one text, styled as before
                    val annotatedText = buildAnnotatedString {
                        val text = "Choose how much you plan to walk your butts everyday."
                        val regex = Regex("Butts", RegexOption.IGNORE_CASE)
                        var lastIndex = 0
                        regex.findAll(text).forEach { result ->
                            val start = result.range.first
                            val end = result.range.last + 1
                            append(text.substring(lastIndex, start))
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(text.substring(start, end))
                            }
                            lastIndex = end
                        }
                        if (lastIndex < text.length) {
                            append(text.substring(lastIndex))
                        }
                    }
                    Text(
                        text = annotatedText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Counter for selected goal
                    Text(
                        text = "${goal} steps",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Material3 Slider styled to match the design, with custom thumb
                    Slider(
                        value = goal.toFloat(),
                        onValueChange = { viewModel.updateGoal(it.roundToInt().coerceIn(3000, 20000)) },
                        valueRange = 3000f..20000f,
                        steps = ((20000 - 3000) / 100) - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF83DBC7),
                            activeTrackColor = Color(0xFF83DBC7),
                            inactiveTrackColor = Color(0xFFE5E5E5),
                            disabledThumbColor = Color(0xFF83DBC7),
                            disabledActiveTrackColor = Color(0xFF83DBC7),
                            disabledInactiveTrackColor = Color(0xFFE5E5E5)
                        ),
                        thumb = {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = Color.White,
                                            radius = size.minDimension / 2f,
                                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    .background(Color(0xFF83DBC7), shape = CircleShape)
                            )
                        },
                        modifier = Modifier
                            .width(244.dp)
                            .height(48.dp)
                    )
                }
                Button(
                    onClick = {
                        viewModel.saveGoal()
                        onGoalSet()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnboardingButtonColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Save goal",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Daily Step Goal",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Adjust your daily step target. Your character adapts to your goals!",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Character Preview Section
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
                    // Character preview based on goal
                    val characterRes = when {
                        goal < 3000 -> R.drawable.sad_character
                        goal < 7000 -> R.drawable.happy_character
                        goal < 12000 -> R.drawable.happy_character
                        goal < 20000 -> R.drawable.happy_character
                        else -> R.drawable.happy_character
                    }
                    
                    Image(
                        painter = painterResource(id = characterRes),
                        contentDescription = "Goal Preview Character",
                        modifier = Modifier.size(80.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when {
                            goal < 3000 -> "Easy Goal"
                            goal < 7000 -> "Moderate Goal"
                            goal < 12000 -> "Active Goal"
                            goal < 20000 -> "Challenging Goal"
                            else -> "Extreme Goal"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    
                    Text(
                        text = when {
                            goal < 3000 -> "Perfect for beginners!"
                            goal < 7000 -> "Great for regular activity!"
                            goal < 12000 -> "Excellent for fitness!"
                            goal < 20000 -> "Ambitious and challenging!"
                            else -> "Ultimate fitness challenge!"
                        },
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Goal Display
            Text(
                text = "$goal",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Text(
                text = "steps per day",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Draggable Progress Bar
            DraggableGoalSlider(
                currentGoal = goal,
                onGoalChanged = { viewModel.updateGoal(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quick Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickGoalButton(
                    goal = 5000,
                    label = "5K",
                    currentGoal = goal,
                    onGoalSelected = { viewModel.updateGoal(it) }
                )
                QuickGoalButton(
                    goal = 10000,
                    label = "10K",
                    currentGoal = goal,
                    onGoalSelected = { viewModel.updateGoal(it) }
                )
                QuickGoalButton(
                    goal = 15000,
                    label = "15K",
                    currentGoal = goal,
                    onGoalSelected = { viewModel.updateGoal(it) }
                )
                QuickGoalButton(
                    goal = 20000,
                    label = "20K",
                    currentGoal = goal,
                    onGoalSelected = { viewModel.updateGoal(it) }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        viewModel.saveGoal()
                        onGoalSet()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text(
                        "Save Goal",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DraggableGoalSlider(
    currentGoal: Int,
    onGoalChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val minGoal = 1000
    val maxGoal = 50000
    val progress = (currentGoal - minGoal).toFloat() / (maxGoal - minGoal).toFloat()

    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFFE0E0E0), RoundedCornerShape(40.dp))
    ) {
        val sliderWidth = constraints.maxWidth.toFloat()
        val thumbPx = with(LocalDensity.current) { 24.dp.toPx() }
        val thumbOffset = (progress * (sliderWidth - thumbPx)).coerceIn(0f, sliderWidth - thumbPx)

        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(Color(0xFF2196F3), RoundedCornerShape(40.dp))
        )

        // Goal markers
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoalMarker(goal = 5000, currentGoal = currentGoal, onGoalChanged = onGoalChanged)
            GoalMarker(goal = 10000, currentGoal = currentGoal, onGoalChanged = onGoalChanged)
            GoalMarker(goal = 15000, currentGoal = currentGoal, onGoalChanged = onGoalChanged)
            GoalMarker(goal = 20000, currentGoal = currentGoal, onGoalChanged = onGoalChanged)
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToInt(), 0) }
                .size(24.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val dragX = change.position.x.coerceIn(0f, sliderWidth)
                        val dragProgress = (dragX / sliderWidth).coerceIn(0f, 1f)
                        val newGoal = (minGoal + (maxGoal - minGoal) * dragProgress).roundToInt()
                        onGoalChanged(newGoal)
                    }
                }
        )
    }
}

@Composable
fun GoalMarker(
    goal: Int,
    currentGoal: Int,
    onGoalChanged: (Int) -> Unit
) {
    val minGoal = 1000
    val maxGoal = 50000
    val progress = (goal - minGoal).toFloat() / (maxGoal - minGoal).toFloat()
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                if (currentGoal >= goal) Color.White else Color.Gray,
                RoundedCornerShape(4.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { _, _ ->
                    onGoalChanged(goal)
                }
            }
    )
}

@Composable
fun QuickGoalButton(
    goal: Int,
    label: String,
    currentGoal: Int,
    onGoalSelected: (Int) -> Unit
) {
    val isSelected = currentGoal == goal
    
    Button(
        onClick = { onGoalSelected(goal) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0)
        ),
        modifier = Modifier.size(48.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GoalProgressBar(
    currentGoal: Int,
    onGoalChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val minGoal = 3000
    val maxGoal = 20000
    val progress = (currentGoal - minGoal).toFloat() / (maxGoal - minGoal).toFloat()
    val displayGoal = (currentGoal / 100) * 100 // Snap to nearest 100

    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFFE0E0E0), RoundedCornerShape(40.dp))
    ) {
        val sliderWidth = constraints.maxWidth.toFloat()
        val thumbPx = with(LocalDensity.current) { 32.dp.toPx() }
        val thumbOffset = (progress * (sliderWidth - thumbPx)).coerceIn(0f, sliderWidth - thumbPx)

        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(Color(0xFF6EDFC6), RoundedCornerShape(40.dp))
        )

        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToInt(), 0) }
                .size(32.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(3.dp, Color(0xFF6EDFC6), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val dragX = change.position.x.coerceIn(0f, sliderWidth)
                        val dragProgress = (dragX / sliderWidth).coerceIn(0f, 1f)
                        val newGoal = (minGoal + (maxGoal - minGoal) * dragProgress).toInt()
                        val snappedGoal = (newGoal / 100) * 100
                        onGoalChanged(snappedGoal.coerceIn(minGoal, maxGoal))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$displayGoal",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6EDFC6),
                fontSize = 18.sp
            )
        }
    }
} 