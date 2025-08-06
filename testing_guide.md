# Mood Decay System Testing Guide

## How to Verify the System is Working

### 1. Check Worker Status
**Command**: `adb logcat -s StepCounterApplication -d`
**What to Look For**:
- "Mood update worker scheduled successfully"
- "=== WORKER STATUS UPDATE ==="
- Worker states like "ENQUEUED", "RUNNING", "SUCCEEDED"

### 2. Check Mood Repository Activity
**Command**: `adb logcat -s MoodRepository -d`
**What to Look For**:
- "=== APPLYING HOURLY MOOD DECAY ==="
- "Mood gain calculation"
- "Decay calculation"
- "Applied hourly mood update"

### 3. Check StepCounterViewModel Activity
**Command**: `adb logcat -s StepUI -d`
**What to Look For**:
- "=== REFRESH LIVE MOOD STARTED ==="
- "Hybrid mood calculation"
- "=== CHECKING FOR MISSED MOOD DECAY ==="

### 4. Check MainActivity Fallback
**Command**: `adb logcat -s MainActivity -d`
**What to Look For**:
- "Checking for missed mood decay in MainActivity"
- "Applied X decay for hour Y"

## Manual Testing Steps

### Step 1: Basic Functionality Test
1. Open the app
2. Click "DEBUG: Check Worker Status" button
3. Check logs for system health information
4. Verify current mood value is displayed

### Step 2: Manual Decay Test
1. Note your current mood value
2. Click "DEBUG: Trigger Mood Decay" button
3. Check logs for decay application
4. Verify mood value decreased
5. Check logs for "=== DEBUG: MOOD DECAY COMPLETED ==="

### Step 3: Fallback System Test
1. Close the app completely
2. Wait for at least 1 hour (or simulate by changing system time)
3. Reopen the app
4. Check logs for "Checking for missed mood decay"
5. Verify mood has decreased due to missed decay

### Step 4: Real-time Updates Test
1. Take some steps (walk around)
2. Check logs for "handleStepUpdate" and "refreshLiveMood"
3. Verify mood increases as you take steps
4. Check logs for "Hybrid mood calculation"

## Expected Log Patterns

### Normal Operation
```
StepCounterApplication: Mood update worker scheduled successfully
StepCounterApplication: === WORKER STATUS UPDATE ===
StepCounterApplication: Worker ID: xxx, State: ENQUEUED, Tags: [mood_update]
MoodUpdateWorker: Starting work execution [ID: xxx]
MoodRepository: === APPLYING HOURLY MOOD DECAY ===
MoodRepository: Applied hourly mood update for hour X: Y -> Z
```

### Fallback System
```
MainActivity: Checking for missed mood decay in MainActivity
MainActivity: X hours since last update, applying missed decay
MainActivity: Applied Y decay for hour Z, new mood: W
StepUI: === CHECKING FOR MISSED MOOD DECAY ===
StepUI: Found X missed hours, applying decay
```

### Debug Operations
```
StepUI: === DEBUG: MANUALLY TRIGGERING MOOD DECAY ===
MoodRepository: === DEBUG: MANUALLY TRIGGERING MOOD DECAY ===
MoodRepository: Decay applied: X, New mood: Y
StepUI: === DEBUG: MOOD DECAY TRIGGERED AND UI REFRESHED ===
```

## Troubleshooting

### If Workers Aren't Running
1. Check if app has battery optimization disabled
2. Verify app has necessary permissions
3. Check if device has aggressive battery saving enabled
4. Look for "Worker failed" or "Worker cancelled" in logs

### If Mood Isn't Decaying
1. Check if you're in quiet hours (22:00-07:00 by default)
2. Verify you have less than 250 steps per hour (or your goal's threshold)
3. Check if mood is already at minimum (0)
4. Look for "Hour X is in quiet hours, no decay applied" in logs

### If Real-time Updates Aren't Working
1. Check step counting service is running
2. Verify activity recognition permission is granted
3. Look for "handleStepUpdate" logs
4. Check if "refreshLiveMood" is being called

## Key Metrics to Monitor

### Worker Health
- Worker scheduling success
- Worker execution frequency
- Worker failure rates
- Worker completion status

### Mood System Health
- Mood decay application
- Mood gain calculation
- Fallback system activation
- Real-time updates

### Data Integrity
- Hourly steps tracking
- Mood state persistence
- Step count accuracy
- Date change handling

## Expected Behavior

### Normal Day (Active User)
- Mood increases with steps
- No decay during active hours
- Real-time updates visible
- Worker runs every hour

### Inactive Period
- Mood decays every hour of inactivity
- Decay respects quiet hours
- Fallback system catches missed decay
- UI shows updated mood when app opens

### Debug Mode
- Manual decay triggers work
- System status is logged
- All components are accessible
- Error handling works properly

## Success Criteria

✅ **Worker Status**: Workers are scheduled and running
✅ **Manual Testing**: Debug buttons work and show expected results
✅ **Fallback System**: Mood decays when app reopens after inactivity
✅ **Real-time Updates**: Mood changes as user takes steps
✅ **Quiet Hours**: No decay during configured quiet hours
✅ **Data Persistence**: Mood state survives app restarts
✅ **Error Handling**: System gracefully handles failures 