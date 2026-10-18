# Smart Alarm Calendar (ChronoAlarm)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android_8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.1-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-blue.svg)](https://developer.android.com/jetpack/compose)

**Smart Alarm Calendar** is a modern, privacy-first Android application designed for individuals with flexible schedules, variable shift work, or unconventional daily routines.

Unlike traditional alarm clocks based on days of the week (Monday-Friday), **Smart Alarm Calendar** allows you to create **Alarm Sets (Presets)** and assign them to **any calendar date** with a single tap or through multi-date selection.

---

## Key Features

- **Interactive Calendar**:
  - Quick assignment of Presets to single dates or multi-day blocks.
  - Instant 120 FPS pre-loading of previous and next months.
  - Colored indicators on dates to quickly identify assigned shifts and presets.

- **Customizable Alarm Sets (Presets)**:
  - Create templates with multiple alarms (e.g., *Morning Shift*, *Afternoon Shift*, *Day Off*).
  - Customizable colors and immediate testing options (+1 minute).

- **Home Screen Widget (Proton Calendar Style)**:
  - List of scheduled alarms for **Today** and **Tomorrow** separated by clean headers.
  - **Side toggle switches** to disable or reactivate alarms directly from the widget without opening the app.
  - Native support for system **Light and Dark Theme**.

- **Full-Screen Lock Screen Alarm**:
  - Works on locked screens with automatic screen turn-on and full Xiaomi/MIUI support.
  - **Swipe-Up** gesture to dismiss the alarm and prevent accidental turn-offs.
  - Continuous **looping sound** and vibration tied to alarm frequency.

- **Pre-Alarm Notification with Action Button**:
  - Silent notification 1 hour prior with a one-tap **"Disable"** button in compact view.

- **Advanced Settings Panel**:
  - Light Theme, Dark Theme, and System Default support.
  - Customizable alarm screen background color.
  - Custom full-screen alarm message.
  - Ringtone selection and Snooze duration (5, 10, 15 min).

---

## Architecture and Technologies

The application is developed following modern Android Development Best Practices:

- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose with Material 3 Design
- **Database**: Room Database v2 (with KSP code generation)
- **System Alarms**: `AlarmManager.setAlarmClock` + `PowerManager.WakeLock`
- **Widget**: `AppWidgetProvider` with adaptive `RemoteViews`
- **Injection & Concurrency**: Kotlin Coroutines & `StateFlow`

---

## Build and Installation (Build from Source)

### Prerequisites
- Android Studio Ladybug (2024.2+) or Android Studio 2026.2+
- JDK 17 or higher
- Android SDK 35 (Android 15)

### Gradle Commands
```bash
# Clone the repository
git clone [https://github.com/your-username/SmartAlarmCalendar.git](https://github.com/your-username/SmartAlarmCalendar.git)
cd SmartAlarmCalendar

# Build the Debug APK
./gradlew app:assembleDebug

# Run unit tests
./gradlew app:testDebugUnitTest
```

---

## F-Droid Publishing

The app is **100% Open Source**, free of tracking, analytics, or proprietary SDKs (no mandatory Google Play Services).

### F-Droid Metadata (`fastlane/metadata/android/`)
Information for inclusion in the F-Droid catalog follows the standard Fastlane structure:
- `title.txt`: Smart Alarm Calendar
- `short_description.txt`: Flexible calendar-based alarm clock for shift work and dynamic schedules.
- `full_description.txt`: Complete description in plain text format.

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
See the [LICENSE](LICENSE) file for details.