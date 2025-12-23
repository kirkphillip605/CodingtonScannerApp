# CodingtonScannerApp

A native Android app that streams audio from https://uranus.kevys.net:8034/scanner

## Features

- **Audio Streaming**: Streams audio using ExoPlayer (Media3) with HTTPS support
- **Standard Media Controls**: Play/pause button and seekbar for position control
- **Clean UI**: Material Design with header and bottom control panel
- **Exit Functionality**: Exit button that fully stops the stream and terminates the app
- **Foreground Service**: Continuous playback with notification support

## Requirements

- Android Studio Arctic Fox or later
- Android SDK 34 (Android 14)
- Minimum Android version: 7.0 (API 24)

## Building

To build the project:

```bash
./gradlew build
```

To build and install debug APK on a connected device:

```bash
./gradlew installDebug
```

To build release APK:

```bash
./gradlew assembleRelease
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/codington/scannerapp/
│   │   ├── MainActivity.kt          # Main UI activity
│   │   └── AudioStreamService.kt    # Audio streaming service
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml    # Main UI layout
│   │   ├── values/
│   │   │   ├── strings.xml          # String resources
│   │   │   └── colors.xml           # Color resources
│   │   └── drawable/                # App icons
│   └── AndroidManifest.xml          # App configuration
└── build.gradle.kts                 # App build configuration
```

## Usage

1. Launch the app
2. Press the **Play** button to start streaming
3. Use the **Pause** button to pause playback
4. The seekbar displays current position (disabled for live streams)
5. Press the **Exit** button to stop streaming and close the app

## Technical Details

- **Streaming Protocol**: HTTPS
- **Audio Engine**: ExoPlayer (Media3)
- **Architecture**: Service-based with activity binding
- **Permissions**: INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK

## License

This project is open source and available under standard licensing terms.