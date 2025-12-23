# CodingtonScannerApp

A native Android app that streams audio from https://uranus.kevys.net:8034/scanner

## Features

- Audio streaming with ExoPlayer
- Standard media player controls (play/pause, scrubber)
- Exit button that fully stops the stream and terminates the app
- Foreground service for continuous playback

## Building

To build the project:

```bash
./gradlew build
```

To install on a device:

```bash
./gradlew installDebug
```