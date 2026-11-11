# Ruler for 8 Ball

**Ruler for 8 Ball** is a utility Android application designed to assist players in 8-ball pool and billiard games. It renders a highly customizable, floating aim-ruler, ghost ball prediction guide, and protractor overlay directly on top of active game applications using Android's WindowManager and Foreground Service APIs.

---

## Key Features

- **Floating Overlay Service**: Runs smoothly over other apps using `TYPE_APPLICATION_OVERLAY` and a foreground service (`specialUse`).
- **Multiple Ruler Styles**:
  - **Aim Line**: Straight aiming line with ghost ball projection and distance ticks.
  - **Protractor**: Angular arc guidance for bank shots and cut shots.
  - **Crosshair**: Precise positioning crosshairs.
- **Customization Options**:
  - **Colors**: Crimson, Cyan, Lime, Ivory, and Gold.
  - **Adjustable Parameters**: Line thickness, opacity, length fraction, ghost ball sizing, and angle snap degrees.
  - **Angle & Ticks Display**: Real-time angle readout and measurement markers.
- **Interactive Gestures**:
  - Drag to reposition the pivot point.
  - Rotate smoothly using touch gestures.
  - Lock/unlock overlay position to prevent accidental touches during gameplay.
- **Jetpack Compose Control Panel**: Modern Material 3 UI for configuring settings, toggling the overlay, and managing permissions.

---

## Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3.
- **Android APIs**:
  - `WindowManager` for custom overlay views.
  - `ForegroundService` for persistent background execution.
  - `SharedPreferences` for reactive settings persistence.
- **Target SDK**: API 37 (Minimum SDK 30 / Android 11+).

---

## Project Structure

```
app/
├── src/main/
    ├── AndroidManifest.xml            # App manifest with overlay & FGS permissions
    ├── java/com/gem/bruller/
    │   ├── MainActivity.kt            # Entry point activity with edge-to-edge support
    │   ├── HomeScreen.kt              # Compose control dashboard & settings UI
    │   └── overlay/
    │       ├── OverlayService.kt      # Foreground service managing the window overlay
    │       ├── OverlaySettings.kt     # Data model and SharedPreferences persistence
    │       └── RulerOverlayView.kt    # Custom Canvas view rendering aim lines & touch gestures
    └── res/                           # Drawables, themes, strings, and app icons
```

---

## Getting Started & Installation

1. **Prerequisites**:
   - Android Studio (Jellyfish or newer recommended).
   - JDK 11 or higher.
   - An Android device or emulator running **Android 11 (API 30)** or higher.

2. **Building the Project**:
   - Clone or open the project folder in Android Studio.
   - Sync project with Gradle files.
   - Run the `app` module configuration on your physical device or emulator.

3. **Usage**:
   - Open **Ruler for 8 Ball**.
   - Grant the **"Display over other apps"** permission when prompted.
   - Configure your desired style, color, and settings in the dashboard.
   - Tap **"Start Overlay"** to launch the floating ruler over your pool game.
   - Use the lock button on the overlay to lock position when aiming.
