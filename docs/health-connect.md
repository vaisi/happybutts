# Health Connect Integration

## Overview
Health Connect integration provides a reliable fallback for step counting and enables historical data retrieval for missed days. This integration ensures that users don't lose their step data even when the app is closed for extended periods.

## Features

### 1. Real-time Step Counting
- Fallback option when hardware sensors are unavailable
- Seamless switching between data sources
- Real-time updates through Flow

### 2. Historical Data
- Retrieval of past step counts
- Backfilling of missed days (up to 30 days)
- Integration with mood tracking system

### 3. Data Consistency
- Automatic conflict resolution
- Data validation
- Error recovery mechanisms

## Implementation

### Health Connect Client Setup
```kotlin
private val healthConnectClient by lazy {
    try {
        HealthConnectClient.getOrCreate(context)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create HealthConnectClient", e)
        null
    }
}
```

### Required Permissions
```kotlin
val requiredPermissions = setOf(
    HealthPermission.getReadPermission(StepsRecord::class)
)
```

### Step Data Retrieval
```kotlin
suspend fun getStepsForDate(date: LocalDate): Int {
    return try {
        healthConnectClient?.let { client ->
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            val response = client.readRecords(request)
            response.records.sumOf { it.count.toInt() }
        } ?: 0
    } catch (e: Exception) {
        Log.e(TAG, "Error getting steps for date $date", e)
        0
    }
}
```

## Data Backfilling

### Process
1. Detect missed days when app is opened
2. Calculate date range (max 30 days)
3. Query Health Connect for each day
4. Update local database
5. Emit updates through Flow

### Implementation
```kotlin
suspend fun backfillMissedDays(lastProcessedDate: LocalDate) {
    val today = LocalDate.now()
    if (lastProcessedDate >= today) return

    val startDate = if (lastProcessedDate.plusDays(1).isAfter(today.minusDays(MAX_BACKFILL_DAYS.toLong()))) {
        lastProcessedDate.plusDays(1)
    } else {
        today.minusDays(MAX_BACKFILL_DAYS.toLong())
    }
    
    var currentDate = startDate
    while (!currentDate.isAfter(today.minusDays(1))) {
        val steps = getStepsForDate(currentDate)
        _stepUpdates.emit(steps)
        currentDate = currentDate.plusDays(1)
    }
}
```

## Error Handling

### 1. Permission Issues
- Check permissions before accessing data
- Request permissions if missing
- Handle permission denial gracefully

### 2. API Errors
- Rate limit handling
- Network error recovery
- Data validation

### 3. Data Consistency
- Conflict resolution between sources
- Data validation
- Error logging

## Best Practices

### 1. Performance
- Limit historical queries to 30 days
- Batch process when possible
- Cache frequently accessed data

### 2. User Experience
- Clear feedback about data source
- Progress indication during backfilling
- Error messages when appropriate

### 3. Battery Usage
- Minimize API calls
- Use efficient data structures
- Implement proper cleanup

## Future Improvements

### 1. Enhanced Features
- Additional health metrics
- Custom data types
- Advanced analytics

### 2. Performance
- Better caching
- Optimized queries
- Reduced API calls

### 3. User Experience
- Better error messages
- Progress indicators
- Data visualization 