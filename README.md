# LuxNote
### Studio-Grade Audiophile Local Music Player for Android with Native C++20 DSP & Obsidian Aesthetics

[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Architecture](https://img.shields.io/badge/Architecture-Kotlin_+_C++20_NEON-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Build & Verify](https://github.com/vivekvijayan7227-cmd/LuxNote/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/vivekvijayan7227-cmd/LuxNote/actions)
[![Offline](https://img.shields.io/badge/Security-100%25_Offline_(Zero_Internet)-success)](https://github.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

**LuxNote** is an ultra-premium, privacy-first local music player for Android phones, tablets, and dedicated Digital Audio Players (DAPs: FiiO, Sony Walkman, Astell&Kern, HiBy). Engineered with a zero-copy local storage engine, a real-time `MediaStore` filesystem observer, a native 64-bit C++20 DSP core with ARM NEON SIMD intrinsics, and the signature **Obsidian Studio** design system.

---

## 💎 Design System: Obsidian Studio

Built using Jetpack Compose following strict studio-grade design principles:

### Core Palette
| Token | Hex | Role |
| :--- | :--- | :--- |
| **Obsidian Black** | `#07090E` | Structural background, visual breathing room (70%) |
| **Deep Navy** | `#101521` | Navigation bars, elevated panels |
| **Studio Slate** | `#182033` | Elevated surfaces, cards, dialogs, sheets (20%) |
| **Champagne Gold** | `#F5C76A` | Primary actions, play/pause FAB, active highlights (10%) |
| **Electric Blue** | `#67B7FF` | Audio visualization, format badges, technical metadata |
| **Pearl White** | `#E8ECF4` | Crisp, high-contrast primary typography |
| **Secondary Text** | `#A0A9BA` | Artists, metadata, album details |
| **Muted Text** | `#737F93` | Durations, disabled labels, timestamps |

---

## ⚡ Core Engineering Benchmarks

### 1. Direct Local Storage Playback (Zero-Copy)
* **Zero Disk Duplication:** Unlike players that copy audio files into private sandboxes, LuxNote plays directly from device storage paths and native Android `content://` URIs.
* **Instant Indexing:** Pre-indexed SQLite cursor queries read metadata directly from Android's MediaStore in **< 30 milliseconds** for 1,000+ tracks.
* **Universal Format Support:** Plays FLAC, WAV, ALAC, MP3, AAC, M4A, OGG, OPUS, and DSD-in-PCM.

### 2. Real-Time Storage Discovery
* **MediaStore ContentObserver:** Automatically observes `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
* **Instant Sync:** When you download a new song, move a folder, or delete a track, LuxNote detects the change immediately and refreshes the library in real time without requiring manual scans.
* **Direct Storage Fallback:** High-speed scanner traverses standard directories (`/storage/emulated/0/Music`, `Download`, SD cards) for non-indexed audio files.

### 3. Continuous Background Playback
* **Transient Audio Focus Resilience:** Intelligently handles transient focus changes. UI transitions and launcher minimize animations do not pause music playback.
* **Foreground MediaStyle Service:** System notification with standard Android lock-screen transport controls and `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
* **Dual Partial WakeLocks:** Service and playback engine prevent CPU sleep while audio is streaming.

### 4. Real Compiled Native C++20 DSP Core (`libhires_audio.so`)
* Compiled directly into APK via Android NDK (`26.1.10909125`) and CMake (`3.22.1`) with `-O3 -ffast-math -fvectorize` and ARM NEON intrinsics on `arm64-v8a`.
* **Canonical Wait-Free Triple Buffer:** Single-producer single-consumer parameter state packing guaranteeing lock-free, zero-allocation updates without reader/writer races.
* **Direct Form II Transposed Biquads:** 10-band ISO equalizer filter with 100% mathematical parity against the managed Kotlin fallback.
* **Phase-Coherent Mid/Side Processor:** Real-time acoustic center-channel extraction and field balance.
* **1024-Point Radix-2 FFT Analyzer:** Real-time 60 FPS spectrum analyzer.
* **Smooth Soft Limiter:** Tanh saturation knee at 0.95 preventing digital clipping.

### 5. Hardware USB DAC & Audio Routing Inspector
* Queries USB Audio Class (`USB_CLASS_AUDIO` 0x01), AudioStreaming subclass (`USB_SUBCLASS_AUDIOSTREAMING` 0x02 - UAC2), and isochronous endpoints.
* Truthfully reports Android AudioServer shared routing versus hardware descriptor capabilities without unsupported bit-perfect guarantees.

### 6. In-App System & Audio Diagnostics Inspector
* **Storage & Permission Audit:** Inspects `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, and `POST_NOTIFICATIONS`.
* **Audio Sink Hardware Audit:** Queries native sample rates, optimal buffer frames, Float 32-bit sink support, and active audio routing (Speaker, Headset, Bluetooth, USB DAC).
* **Live Playback Self-Test Probe:** Tests file descriptor opening, track extraction, and buffer feeds in real time.
* **Live Event Console:** Full timestamped log with one-tap report copying.

### 7. 100% Offline & Privacy First
* Strict absence of `android.permission.INTERNET` in `AndroidManifest.xml`.
* Zero analytics, zero cloud dependencies, zero telemetry.

---

## 📁 Repository Structure

```
├── HiResPlayer-Android/               # Complete Android Studio project
│   ├── app/
│   │   ├── src/main/cpp/             # Native C++20 DSP engine (SIMD biquad, FFT, M/S)
│   │   ├── src/main/java/com/audioplayer/hires/
│   │   │   ├── audio/                # AudioTrackPlayer, DSP pipeline, USB DAC inspector
│   │   │   ├── data/                 # AudioLibraryRepository, MediaStoreObserver, Diagnostics
│   │   │   ├── service/              # HiResAudioService (MediaStyle foreground service)
│   │   │   └── ui/                   # Jetpack Compose UI (Obsidian Studio theme & screens)
│   │   └── src/test/                 # Unit test suite (26 tests verifying DSP & storage)
│   ├── build.gradle.kts
│   └── gradlew.bat
├── Build-AndroidApp.bat               # Windows 1-click test & release build tool
├── LuxNote.apk                        # Production release APK (multi-pass verified)
├── LuxNote.apk.sha256                 # Cryptographic verification checksum
└── README.md                          # Documentation
```

---

## 🛠️ Building from Source

### Prerequisites
* JDK 21 (`JAVA_HOME`)
* Android SDK (API 34, Build Tools 34.0.0)
* Android NDK (`26.1.10909125`)
* CMake (`3.22.1`)

### Build Commands

```powershell
# Run Unit Tests
cd "HiResPlayer-Android"
.\gradlew.bat testDebugUnitTest

# Assemble Release APK
.\gradlew.bat assembleRelease
```

Or run the 1-click batch script from the repository root:
```powershell
.\Build-AndroidApp.bat
```

---

## 📲 Installation

Copy `LuxNote.apk` to your Android device or install via ADB:

```powershell
adb install -r "LuxNote.apk"
```
