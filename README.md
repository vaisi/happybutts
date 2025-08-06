# Happy Butts - Step Counter & Mood Tracker

## Documentation Index

### User Documentation
- [User Guide](userdoc.md) - How to use the app
- [Features](docs/features.md) - Detailed feature descriptions
- [FAQ](docs/faq.md) - Frequently asked questions

### Technical Documentation
- [Technical Overview](techdoc.md) - High-level technical architecture
- [Step Counting](docs/step-counting.md) - Step counting implementation details
- [Mood Tracking](docs/mood-tracking.md) - Mood tracking system
- [Health Connect Integration](docs/health-connect.md) - Health Connect integration details
- [Database Schema](docs/database.md) - Database structure and relationships
- [Testing](docs/testing.md) - Testing strategy and procedures

### Development
- [Setup Guide](docs/setup.md) - Development environment setup
- [Contributing](docs/contributing.md) - How to contribute to the project
- [Architecture Decisions](docs/architecture-decisions.md) - Key architectural decisions and rationale

## Project Overview
Happy Butts is an Android application that combines step counting with mood tracking to encourage physical activity and mental well-being. The app uses both hardware sensors and Health Connect to track steps, and implements a mood decay system based on activity levels.

### Key Features
- Real-time step counting using hardware sensors
- Health Connect integration for historical data
- Mood tracking with automatic decay
- Calendar view of activity and mood
- Daily reset and data backfilling
- Background processing for continuous tracking

### Technology Stack
- Kotlin
- Android Jetpack
- Health Connect API
- Room Database
- WorkManager
- Hilt for Dependency Injection
- Coroutines for asynchronous operations 