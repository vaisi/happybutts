# Technical Documentation

## System Architecture

### Core Components

#### 1. **StepCountingService**
- Foreground service for continuous step counting
- Updates notification with current step count
- Handles service lifecycle and permissions
- **REMOVED: HourlyStepPoller dependency** - MoodUpdateWorker handles hourly data recording

#### 2. **UnifiedStepCounterService**
- Provides real-time step data from hardware sensors
- Manages step counting lifecycle
- Updates StepCountRepository with current totals

#### 3. **MoodUpdateWorker** (Enhanced)
- **NEW: Primary system for hourly data recording and mood calculation**
- Runs hourly to record step data and calculate mood changes
- Handles all hourly step tracking previously done by HourlyStepPoller
- Records incremental steps per hour with total tracking
- Applies mood decay and gain calculations
- Stores worker update data for UI synchronization

#### 4. **StepCounterViewModel**
- **UPDATED: Simplified mood calculation without artificial limits**
- Calculates live mood based on current steps and database mood
- Uses direct calculation instead of complex incremental gain
- Removes 500-step artificial limit for realistic mood gains
- Displays live mood updates in UI

### Data Flow

#### Simplified Architecture:
```
UnifiedStepCounterService (real-time) → StepCountRepository (current total)
MoodUpdateWorker (hourly) → Records hourly data + Calculates mood
UI (when opened) → Gets live steps + Calculates live mood
```

#### Removed Redundant System:
- **ELIMINATED: HourlyStepPoller** - redundant with MoodUpdateWorker
- **ELIMINATED: 2-minute polling** - unnecessary battery drain
- **ELIMINATED: Artificial 500-step limit** - prevents realistic mood gains

### Database Schema

#### Core Tables:
- `step_count`: Daily step totals
- `mood_state`: Current mood with decay applied
- `hourly_steps`: Hourly step data with `lastRecordedTotal`
- `historical_hourly_steps`: Archived hourly data

### Key Improvements

1. **Eliminated Redundancy**: Removed HourlyStepPoller in favor of MoodUpdateWorker
2. **Simplified Mood Calculation**: Direct calculation without artificial limits
3. **Better Battery Life**: No more 2-minute polling
4. **More Accurate**: Direct access to live step data
5. **Cleaner Architecture**: Single responsibility per component

### Mood Calculation

#### Database Storage (Permanent):
- MoodUpdateWorker stores mood with decay in database
- Permanent record of mood state

#### UI Display (Live Calculation):
- StepCounterViewModel calculates live mood for display
- Based on database mood + current step gains
- No artificial limits on step gains

### Testing

#### Key Test Scenarios:
1. **Hourly mood decay** - verify worker runs correctly
2. **Live mood updates** - verify UI shows accurate mood
3. **Step tracking accuracy** - verify no missed steps
4. **Battery efficiency** - verify no unnecessary polling
5. **Data recovery** - verify system recovers from interruptions