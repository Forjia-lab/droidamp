# Droidamp

Open source Android music player for Navidrome/Subsonic — Winamp lineage.

## Stack
- Kotlin + Jetpack Compose + Material3
- Media3/ExoPlayer for playback
- Hilt for DI
- Subsonic API (Navidrome at http://100.122.7.119:4533)
- minSdk 26, targetSdk 35, JVM 17

## Key decisions
- No Material You dynamic color — always use active DroidTheme colors
- Monospace font throughout
- No volume slider on player screen (hardware buttons handle volume)
- Source files in app/src/main/kotlin/com/droidamp/
- Domain models in domain/model/Models.kt

## Architecture
MVVM + Hilt DI + MediaSessionService
