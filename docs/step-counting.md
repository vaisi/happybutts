# Step Counting System

## Overview
The step counting system in Happy Butts is designed to be robust and accurate, using multiple data sources and implementing automatic data backfilling for missed days.

## Components

### 1. UnifiedStepCounterService
The main service that coordinates step counting from multiple sources:
- Hardware step detector
- Accelerometer step detector
- Health Connect integration

#### Key Features
- Automatic source selection based on availability
- Real-time step updates
- Historical data backfilling
- Daily reset functionality

### 2. Step Detectors

#### HardwareStepDetector
- Uses the device's built-in step counter sensor
- Maintains a baseline for daily step counting
- Persists step count across app restarts

#### AccelerometerStepDetector
- Fallback option when hardware sensor is unavailable
- Uses accelerometer data to detect steps
- Session-based counting (resets when app restarts)

### 3. Health Connect Integration
- Provides historical step data
- Backfills missed days (up to 30 days)
- Used as a fallback when hardware sensors are unavailable

## Data Flow

1. **Real-time Counting**
   ```
   Hardware Sensor → HardwareStepDetector → UnifiedStepCounterService → UI
   ```

2. **Historical Data**
   ```
   Health Connect → UnifiedStepCounterService → Database → UI
   ```

3. **Daily Reset**
   ```
   Midnight/App Open → Reset Baseline → Start New Day
   ```

## Key Features

### Automatic Source Selection
The system automatically selects the best available step counting source:
1. Hardware step detector (preferred)
2. Accelerometer (if hardware unavailable)
3. Health Connect (as fallback)

### Historical Data Backfilling
- Detects missed days when app is opened
- Queries Health Connect for historical data
- Limited to last 30 days for performance
- Updates both step count and mood data

### Daily Reset
- Occurs at midnight or when app is opened after midnight
- Resets step baseline
- Finalizes previous day's data
- Starts new day's tracking

## Implementation Details

### Step Counting Logic
```kotlin
// In UnifiedStepCounterService
fun startStepCounting(onStepDetected: (Int) -> Unit) {
    // 1. Check for missed days and backfill
    // 2. Select best available detector
    // 3. Start real-time counting
    // 4. Emit updates through Flow
}
```

### Historical Data Retrieval
```kotlin
suspend fun getStepsForDate(date: LocalDate): Int {
    // Query Health Connect for specific date
    // Return total steps for that day
}
```

### Backfilling Process
```kotlin
suspend fun backfillMissedDays(lastProcessedDate: LocalDate) {
    // 1. Calculate date range (max 30 days)
    // 2. Query Health Connect for each day
    // 3. Update database and UI
}
```

## Error Handling

1. **Sensor Unavailability**
   - Graceful fallback to alternative sources
   - Clear user feedback about source changes

2. **Health Connect Issues**
   - Permission handling
   - Rate limit management
   - Error recovery

3. **Data Consistency**
   - Validation of step counts
   - Conflict resolution between sources
   - Data persistence across app restarts

## Future Improvements

1. **Performance Optimization**
   - Batch processing for historical data
   - Caching strategies
   - Reduced API calls

2. **Accuracy Enhancements**
   - Machine learning for step detection
   - Better filtering of false positives
   - Calibration options

3. **User Experience**
   - More detailed step statistics
   - Customizable goals
   - Better visualization of historical data 