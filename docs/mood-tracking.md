# Mood Tracking System

## Overview
The mood tracking system in Happy Butts is designed to encourage physical activity by implementing a mood decay mechanism that can only be improved through step activity. The system maintains mood state across app restarts and handles daily resets.

## Core Concepts

### 1. Mood States
- Represented by emoji characters
- Decay over time
- Can be improved through step activity
- Persisted in local database

### 2. Mood Decay
- Automatic decay when step goals aren't met
- Decay rate based on time
- Minimum mood threshold
- Maximum mood cap

### 3. Mood Improvement
- Based on step count achievements
- Step thresholds for mood boosts
- Gradual improvement system

## Implementation

### Mood Repository
```kotlin
@Singleton
class MoodRepository @Inject constructor(
    private val moodStateDao: MoodStateDao,
    private val stepCountRepository: StepCountRepository
) {
    // Core mood management functions
    suspend fun getCurrentMood(): Flow<MoodState>
    suspend fun updateMood(steps: Int)
    suspend fun resetDaily()
    suspend fun finalizeDayMood(date: LocalDate)
}
```

### Mood State Entity
```kotlin
@Entity(tableName = "mood_states")
data class MoodState(
    @PrimaryKey val date: LocalDate,
    val mood: String,  // Emoji character
    val steps: Int,
    val lastUpdated: Instant
)
```

## Mood Management

### 1. Daily Reset
- Occurs at midnight or when app is opened after midnight
- Finalizes previous day's mood
- Initializes new day's mood state
- Integrates with step counting reset

### 2. Mood Updates
- Triggered by step count changes
- Calculates mood based on step thresholds
- Persists changes to database
- Emits updates through Flow

### 3. Historical Data
- Maintains mood history
- Supports calendar view
- Enables trend analysis

## Mood Calculation

### Step Brackets and Mood Gain
```kotlin
val brackets = listOf(
    Triple(0, 5000, 150),    // 0-5000 steps: 1 mood point per 150 steps
    Triple(5000, 10000, 200), // 5000-10000 steps: 1 mood point per 200 steps
    Triple(10000, 15000, 500), // 10000-15000 steps: 1 mood point per 500 steps
    Triple(15000, Int.MAX_VALUE, 1000) // 15000+ steps: 1 mood point per 1000 steps
)
```

The mood system uses a numerical scale (0-100) with diminishing returns:
- Each step bracket has a different divisor
- Steps are counted in their respective brackets
- Mood gain is calculated by dividing steps in each bracket by its divisor
- Total mood gain is the sum across all brackets

### Mood Decay
```kotlin
private fun calculateDecay(currentHour: Int, stepsInLastHour: Int, currentMood: Int): Int {
    // No decay during sleep hours (11 PM to 6 AM)
    if (currentHour in 23..5) return 0

    return when {
        // Overexertion penalty
        currentMood > 100 -> OVEREXERTION_DECAY
        // Normal decay if inactive
        stepsInLastHour < MIN_STEPS_FOR_NO_DECAY -> MOOD_DECAY_PER_HOUR
        // No decay if active
        else -> 0
    }
}
```

### Daily Reset
```kotlin
private fun calculateStartMood(previousDayEndMood: Int): Int {
    return when {
        previousDayEndMood > 115 -> 60       // After extreme overexertion
        previousDayEndMood < 20 -> 30        // After terrible day
        else -> 50                           // Normal day
    }
}
```

## Data Flow

1. **Step Updates**
   ```
   Step Counter → Mood Repository → Database → UI
   ```

2. **Daily Reset**
   ```
   Midnight/App Open → Finalize Previous Day → Reset Current Day
   ```

3. **Mood Decay**
   ```
   Background Check → Calculate Decay → Update Database → UI
   ```

## Error Handling

### 1. Data Consistency
- Validation of mood states
- Conflict resolution
- Recovery from crashes

### 2. Edge Cases
- First-time user initialization
- Missing historical data
- Invalid mood states

### 3. Background Processing
- WorkManager integration
- Battery optimization
- Error recovery

## Integration Points

### 1. Step Counting
- Real-time step updates
- Historical step data
- Step goal tracking

### 2. UI Components
- Calendar view
- Mood indicators
- Progress tracking

### 3. Background Processing
- Daily reset worker
- Mood decay calculations
- Data persistence

## Best Practices

### 1. Performance
- Efficient database queries
- Caching strategies
- Background processing

### 2. User Experience
- Smooth mood transitions
- Clear feedback
- Intuitive UI

### 3. Data Management
- Regular backups
- Data validation
- Error logging

## Future Improvements

### 1. Enhanced Features
- Custom mood thresholds
- More mood states
- Advanced analytics

### 2. Personalization
- User-defined goals
- Custom decay rates
- Personalized feedback

### 3. Social Features
- Mood sharing
- Friend challenges
- Community features

## Testing Strategy

### 1. Unit Tests
- Mood calculation logic
- Decay mechanisms
- Data persistence

### 2. Integration Tests
- Step-mood interaction
- Background processing
- Database operations

### 3. UI Tests
- Mood display
- Calendar updates
- User interactions 