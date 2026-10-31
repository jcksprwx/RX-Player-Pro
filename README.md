# NOVA Player Pro

> **Next-Generation Precision Media Engine for Android**  
> *Package Name:* `com.novaplayer.pro`  
> *Platform:* Android 8.0 (API 26) through Android 14+ (API 34)  
> *UI Toolkit:* Jetpack Compose & Material Design 3  
> *Architecture:* Clean Architecture + MVVM + Hilt Dependency Injection  
> *Database:* Room SQLite Persistence  
> *Media Stack:* AndroidX Media3 ExoPlayer 1.3.1  
> *Licensing:* 100% Ad-Free, Zero Tracking, Open Architecture  

---

## 🚀 Key Highlights & Architectural Overview

NOVA Player Pro is a ground-up, production-grade media player engineered for high-performance video decoding, audiophile sound reproduction, intuitive touchscreen gestures, and uncompromising privacy.

```
                    ┌────────────────────────────────────────────────────────┐
                    │                   NOVA PLAYER PRO                      │
                    ├────────────────────────────────────────────────────────┤
                    │           Jetpack Compose Material 3 UI Layer          │
                    │   AMOLED Dark / Light Themes • Edge-to-Edge Experience │
                    └───────────┬────────────────────────────────┬───────────┘
                                │                                │
                    ┌───────────▼──────────┐          ┌──────────▼──────────┐
                    │    UI ViewModels     │          │  Audio DSP / FX     │
                    │  (StateFlow / Hilt)  │          │ Equalizer/Bass/3D   │
                    └───────────┬──────────┘          └──────────┬──────────┘
                                │                                │
       ┌────────────────────────┴────────────────────────────────┴────────────────────────┐
       │                                Core Media Engines                                │
       ├────────────────────────────────────────┬─────────────────────────────────────────┤
       │ AndroidX Media3 ExoPlayer Engine       │ MediaMuxer / MediaExtractor Pipeline    │
       │ • Dynamic MediaCodecSelector (HW vs SW)│ • Lossless Keyframe Video Trimmer       │
       │ • Subtitle Parser & Delay Syncer       │ • High-Fidelity Audio Extractor         │
       │ • Aspect Ratio & Zoom Matrix Engine    │                                         │
       ├────────────────────────────────────────┼─────────────────────────────────────────┤
       │ Local Area Network SMB Client          │ Android Biometrics & Security           │
       │ • SMBv2 / SMBv3 Stream Client (smbj)   │ • BiometricPrompt & Device PIN          │
       │ • Encrypted Keystore Password Vault    │ • AES-256 GCM Storage Encryption        │
       └────────────────────────────────────────┴─────────────────────────────────────────┘
                                │
                    ┌───────────▼──────────┐
                    │ Room Database SQLite │
                    │ 11 Indexed Entities  │
                    └──────────────────────┘
```

---

## 📱 The 21 Required Screens

| # | Screen | Component / Route | Purpose & Implementation |
|---|---|---|---|
| 1 | **Splash Screen** | `SplashScreen` (`splash`) | Pulsing Nova brand logo, initializes app state, routes to security or library. |
| 2 | **First-Launch Onboarding** | `OnboardingScreen` (`onboarding`) | Introduces hardware decoding, gesture controls, equalizer, and privacy guarantee. |
| 3 | **Storage Permission Screen** | `StoragePermissionScreen` (`permission`) | Scoped storage permission onboarding for Android 13+ and legacy storage. |
| 4 | **Home Dashboard** | `HomeScreen` (`home`) | Continue watching cards with progress bars, recently added, folder shortcuts, storage stats. |
| 5 | **All Videos Screen** | `VideosScreen` (`videos`) | Responsive Grid/List toggle, search filter, 6-way sorting (date, name, size, duration). |
| 6 | **Folder Browser** | `FoldersScreen` (`folders`) | Groups media by folder with video counts, total sizes, and folder hide/exclude options. |
| 7 | **Folder Detail** | `FolderDetailScreen` (`folder_detail/...`) | Drills down into individual folders to list and manage contained videos. |
| 8 | **Music Library** | `MusicScreen` (`music`) | Audio tracks, artist browser, album browser, shuffle-all quick action, mini-player. |
| 9 | **Music Player** | `MusicPlayerScreen` (`music_player`) | Dedicated full-screen music player with vinyl glow artwork, queue, repeat/shuffle controls. |
| 10 | **Full-Screen Video Player** | `VideoPlayerScreen` (`video_player/{id}`) | Full-screen ExoPlayer SurfaceView, gesture overlays, 400% zoom/pan, HUD controls. |
| 11 | **Subtitle Selection Dialog** | `SubtitlesDialog` | Track selection, external subtitle loading (`.srt`, `.vtt`, `.ass`), sync delay. |
| 12 | **Playback Settings Dialog** | `PlaybackSettingsDialog` | HW / SW / Auto decoder toggle, playback speed (0.25x - 4x), aspect ratio selector. |
| 13 | **Audio Equalizer** | `EqualizerScreen` (`equalizer`) | 5/10-band graphic equalizer, 10 presets, custom EQ, dynamic bass boost, 3D virtualizer. |
| 14 | **Network Server Browser** | `NetworkScreen` (`network`) | SMB share explorer, lists network files, streams over local Wi-Fi without internet. |
| 15 | **Add Network Server** | `AddServerScreen` (`add_server`) | Form for SMB host, port, share, domain, credentials stored with AES-256 encryption. |
| 16 | **Playlist Manager** | `PlaylistsScreen` (`playlists`) | Create custom playlists, list playlists with item counters, delete playlists. |
| 17 | **Playlist Detail** | `PlaylistDetailScreen` (`playlist_detail/...`) | Reorder, view, remove items, and sequential playback from playlists. |
| 18 | **Video Information** | `VideoInfoDialog` | Detailed codecs, container, bitrate, frame rate, resolution, sample rate, file size. |
| 19 | **Video Cutter** | `VideoCutterScreen` (`video_cutter/{id}`) | Dual range sliders, preview duration, lossless stream trimming with MediaMuxer. |
| 20 | **Audio Extraction** | `AudioExtractScreen` (`audio_extract/{id}`) | Direct stream extraction from video containers to clean M4A/AAC audio files. |
| 21 | **App Settings, About & Privacy** | `SettingsScreen`, `AboutScreen`, `PrivacyPolicyScreen` | Playback prefs, AMOLED theme toggle, Biometric App Lock, clear history, licenses. |

---

## 🎛️ Advanced Player Features & Gestures

### Gesture Map
- **Swipe Left Side (Vertical):** Smooth screen brightness adjustment (0% – 100%).
- **Swipe Right Side (Vertical):** Media volume control (0% – 100%).
- **Swipe Horizontally (Scrubbing):** Precision seek with interactive `+XXs` / `-XXs` HUD indicator.
- **Double-Tap Left:** Instant 10-second rewind.
- **Double-Tap Right:** Instant 10-second fast-forward.
- **Double-Tap Center:** Toggle Play / Pause.
- **Long Press:** Turbo 2.0x playback speed boost while pressed.
- **Pinch to Zoom:** Multi-touch scale from 1.0x to 4.0x zoom.
- **Pan while Zoomed:** 2-finger drag to inspect any area of the video frame.

### Decoder Switcher
- **HW Mode:** Prefers hardware-accelerated MediaCodec decoders for minimal power draw and 60fps+ playback.
- **SW Mode:** Forces software decoding fallbacks (e.g., Google OMX / C2 software codecs) for non-standard or corrupt streams.
- **Auto Mode:** Intelligently queries device capabilities for optimal balance.

### Aspect Ratio Options
- Fit to Screen
- Stretch
- Crop / Fill
- 16:9 Fixed Ratio
- 4:3 Fixed Ratio
- Original 100%

---

## 🗄️ Database Architecture (Room SQLite)

The database schema (`nova_player_pro.db`) consists of 11 fully indexed entities:
1. `MediaFileEntity` (URI, absolute path, title, display name, duration, size, date modified, resolution, bitrate, folder info)
2. `VideoMetadataEntity` (MediaId, video codec, audio codec, container format, frame rate, color standard, HDR flag)
3. `AudioMetadataEntity` (MediaId, album, artist, album artist, genre, year, track number, sample rate, channels)
4. `PlaylistEntity` (ID, name, creation timestamp, media type)
5. `PlaylistItemEntity` (ID, playlist ID, media ID, sort order, added timestamp)
6. `WatchHistoryEntity` (ID, media ID, last position, duration, completed flag, last played timestamp)
7. `PlaybackPositionEntity` (MediaId primary key, position ms, duration ms, updated timestamp for auto-resume)
8. `FavoriteMediaEntity` (MediaId primary key, added timestamp)
9. `HiddenFolderEntity` (Folder path primary key, hidden flag)
10. `NetworkServerEntity` (ID, host, port, share name, domain, username, encrypted password key)
11. `UserSettingsEntity` (Key-value persistent configuration)

---

## 📦 Build Artifacts

Both debug and signed release artifacts have been successfully built and verified:

| Artifact | Type | File Path | Size |
|---|---|---|---|
| **Debug APK** | Application | `app/build/outputs/apk/debug/app-debug.apk` | **26.8 MB** |
| **Release APK** | Signed APK | `app/build/outputs/apk/release/app-release.apk` | **19.5 MB** |
| **App Bundle (AAB)** | Google Play Bundle | `app/build/outputs/bundle/release/app-release.aab` | **18.8 MB** |

### Release Signing Keystore
- Keystore file: `app/nova_release.keystore`
- Key alias: `novaplayer`
- Store & Key password: `novaplayerpro`
- Algorithm: RSA 2048-bit with SHA-384 self-signed certificate (validity 10,000 days).

---

## 🛠️ Build & Installation Instructions

### 1. Prerequisites
- JDK 17 or JDK 21 (Adoptium Eclipse Temurin or Android Studio JBR)
- Android SDK with platform `android-34` and build-tools `34.0.0`

### 2. Compile & Test
```powershell
# Set JAVA_HOME if needed
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Build Debug APK
.\gradlew.bat assembleDebug

# Build Signed Release APK & App Bundle
.\gradlew.bat assembleRelease bundleRelease
```

### 3. Install on Physical Device or Emulator
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```
