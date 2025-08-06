# User Documentation

## Overview
This app tracks your daily steps and calculates your mood based on your activity level. The system now uses a simplified architecture that provides accurate step tracking and mood calculation without unnecessary battery drain.

## How It Works

### Step Tracking
- **Real-time tracking**: Uses your phone's built-in step sensors
- **Background service**: Continues tracking even when the app is closed
- **Notification**: Shows your current step count and progress toward your goal
- **Accurate data**: Direct access to hardware sensors for reliable step counting

### Mood Calculation
- **Hourly updates**: Mood is calculated and updated every hour
- **Live display**: Shows real-time mood based on your current activity
- **No artificial limits**: Can handle any number of steps without restrictions
- **Smart decay**: Applies mood decay only when appropriate (not during quiet hours)

### Key Features

#### 1. **Simplified Architecture**
- **Eliminated redundant systems**: Removed unnecessary polling that drained battery
- **Direct calculation**: Uses your actual step count without artificial limits
- **Better performance**: No more 2-minute polling cycles
- **More accurate**: Direct access to live step data

#### 2. **Real-time Updates**
- **Live step count**: Updates in real-time as you walk
- **Live mood display**: Shows current mood based on your activity
- **Notification updates**: Always shows your current progress
- **No delays**: Immediate feedback on your activity

#### 3. **Smart Mood System**
- **Database storage**: Permanent record of your mood with decay applied
- **Live calculation**: UI shows real-time mood based on current steps
- **No artificial limits**: Can handle 1000+ steps without restrictions
- **Accurate tracking**: Properly accounts for time passed and step gains

### System Components

#### Core Services:
- **UnifiedStepCounterService**: Provides real-time step data
- **MoodUpdateWorker**: Handles hourly mood calculations and data recording
- **StepCountingService**: Manages background service and notifications

#### Data Flow:
```
Hardware Sensors → Real-time Step Data → Live Mood Calculation → UI Display
MoodUpdateWorker → Hourly Data Recording → Database Storage
```

### Troubleshooting

#### If steps aren't being counted:
1. Check that the app has permission to access activity recognition
2. Ensure the notification is showing (indicates service is running)
3. Try restarting the app

#### If mood seems incorrect:
1. The system calculates mood based on your step count and time passed
2. Mood decay is applied hourly for inactivity
3. Mood gain is calculated based on your actual steps taken
4. No artificial limits prevent realistic mood gains

#### If the app was closed for hours:
1. The system will calculate the correct mood when you reopen
2. It accounts for steps taken while the app was closed
3. It applies appropriate decay for the time passed
4. The notification should show your current step count

### Database Queries (for debugging)

To check your step data:
```sql
SELECT * FROM hourly_steps WHERE date = '2025-08-04' ORDER BY hour;
```

To check your mood data:
```sql
SELECT * FROM mood_states WHERE date = '2025-08-04';
```

### Performance Benefits

#### Battery Life:
- **No unnecessary polling**: Eliminated 2-minute polling cycles
- **Efficient background processing**: Only runs when needed
- **Smart scheduling**: Workers run at optimal times

#### Accuracy:
- **Direct sensor access**: Uses hardware step sensors directly
- **No artificial limits**: Can handle any realistic step count
- **Proper time tracking**: Accounts for hours passed correctly

#### Reliability:
- **Simplified architecture**: Fewer moving parts means fewer failures
- **Better error handling**: Graceful recovery from interruptions
- **Data consistency**: Single source of truth for step data 