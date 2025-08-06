# Mood Calculation System Flowchart

## Overview
The mood system operates through multiple pathways to ensure reliability even when background workers fail.

## 1. Normal Hourly Worker Path (Ideal Scenario)
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ MoodUpdateWorker│───▶│ applyHourlyMood  │───▶│ Update Database │
│ (Every Hour)    │    │ Decay()          │    │ & UI            │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Get Current     │    │ Calculate Steps  │    │ Store New Mood  │
│ Hour & Steps    │    │ in Previous Hour │    │ State           │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Calculate Mood  │    │ Apply Decay if   │    │ Update UI with  │
│ Gain from Steps │    │ Steps < Threshold│    │ New Mood Value  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 2. Fallback System Path (When Worker Fails)
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ App Becomes     │───▶│ Check for Missed │───▶│ Calculate Hours │
│ Active          │    │ Mood Decay       │    │ Since Last      │
└─────────────────┘    └──────────────────┘    │ Update          │
         │                       │              └─────────────────┘
         ▼                       ▼                       │
┌─────────────────┐    ┌──────────────────┐              ▼
│ MainActivity    │    │ StepCounter      │    ┌─────────────────┐
│ onCreate()      │    │ ViewModel init   │───▶│ Apply Decay for │
│ onResume()      │    │                  │    │ Each Missed Hour│
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Check Last      │    │ Respect Quiet    │    │ Update Database │
│ Update Time     │    │ Hours            │    │ & Refresh UI    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 3. Real-Time Mood Display Path
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ User Takes      │───▶│ StepCounter      │───▶│ refreshLiveMood │
│ Steps           │    │ Service Updates  │    │ ()              │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Update Step     │    │ Update Hourly    │    │ Get Database    │
│ Count           │    │ Steps Tracking   │    │ Mood + Live     │
└─────────────────┘    └──────────────────┘    │ Gain            │
         │                       │              └─────────────────┘
         ▼                       ▼                       │
┌─────────────────┐    ┌──────────────────┐              ▼
│ Update UI with  │    │ Calculate Live   │    ┌─────────────────┐
│ New Step Count  │    │ Step Gain Since  │───▶│ Combine Database │
└─────────────────┘    │ Last Update      │    │ Mood + Live     │
                       └──────────────────┘    │ Gain for Final  │
                                               │ Mood Display    │
                                               └─────────────────┘
```

## 4. Debug/Manual Testing Path
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ User Clicks     │───▶│ debugTriggerMood │───▶│ Apply Decay     │
│ Debug Button    │    │ Decay()          │    │ Manually        │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ HomeScreen      │    │ MoodRepository   │    │ Update Database │
│ Button Handler  │    │ debugTriggerMood │    │ & Refresh UI    │
└─────────────────┘    │ Decay()          │    └─────────────────┘
                       └──────────────────┘
```

## 5. Mood Calculation Logic
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Input: Steps    │───▶│ Calculate Mood   │───▶│ Apply Decay     │
│ in Period       │    │ Gain:            │    │ Rules:          │
└─────────────────┘    │ Steps / Steps    │    │ • Quiet Hours   │
         │              │ Per Mood Point   │    │ • Activity      │
         ▼              └──────────────────┘    │   Threshold     │
┌─────────────────┐              │              │ • Overexertion  │
│ Dynamic         │              ▼              └─────────────────┘
│ Calculation:    │    ┌──────────────────┐              │
│ • User Goal     │    │ Calculate Decay: │              ▼
│ • Base Values   │    │ • 0 if Quiet    │    ┌─────────────────┐
│ • Scaling       │    │ • 8 if Mood>100 │    │ Final Mood =    │
└─────────────────┘    │ • 5 if Inactive │    │ Previous + Gain │
                       └──────────────────┘    │ - Decay         │
                                               └─────────────────┘
```

## 6. Fallback System Triggers
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ App Startup     │    │ App Resume       │    │ ViewModel Init  │
│ (MainActivity   │    │ (MainActivity    │    │ (StepCounter    │
│ onCreate)       │    │ onResume)        │    │ ViewModel)      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Check Hours     │    │ Check Hours      │    │ Check Hours     │
│ Since Last      │    │ Since Last       │    │ Since Last      │
│ Update          │    │ Update           │    │ Update          │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ If > 1 Hour:    │    │ If > 1 Hour:     │    │ If > 1 Hour:    │
│ Apply Missed    │    │ Apply Missed     │    │ Apply Missed    │
│ Decay           │    │ Decay            │    │ Decay           │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 7. Data Flow Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ StepCounter     │───▶│ MoodRepository   │───▶│ Database        │
│ Service         │    │                  │    │ (Room)          │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Real-time Step  │    │ Mood Calculation │    │ Persistent      │
│ Updates         │    │ & Decay Logic    │    │ Storage         │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ StepCounter     │    │ StepCounter      │    │ UI Components   │
│ ViewModel       │    │ ViewModel        │    │ (Compose)       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ UI State        │    │ Mood State       │    │ User Interface  │
│ Management      │    │ Management       │    │ Display         │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Key Points:

1. **Multiple Redundancy**: 4 different triggers ensure mood decay happens
2. **Real-time Updates**: UI shows live mood changes as user takes steps
3. **Fallback System**: Works even when background workers fail
4. **Debug Tools**: Manual testing available for verification
5. **Dynamic Scaling**: Mood calculations adapt to user's goal
6. **Quiet Hours**: Respects user's sleep/focus periods

## How to Verify It's Working:

1. **Check Logs**: Look for "MoodUpdateWorker", "StepCounterApplication", "MoodRepository" tags
2. **Use Debug Button**: Click debug button to manually trigger decay
3. **Monitor UI**: Watch mood value change in real-time
4. **Test Fallback**: Close app for hours, reopen to see decay applied
5. **Check Database**: Verify mood state is being updated correctly 