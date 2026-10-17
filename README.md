# LuxNote
### Studio-Grade Audiophile Local Music Player for Android with Native C++20 DSP & Obsidian Aesthetics

[![Platform](https://img.shields.io/badge/Platform-Android_7.0+_(API_24+)--36-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Architecture](https://img.shields.io/badge/Architecture-Kotlin_+_C++20_NEON-blue?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Build & Verify](https://github.com/vivekvijayan7227-cmd/LuxNote/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/vivekvijayan7227-cmd/LuxNote/actions)
[![Offline](https://img.shields.io/badge/Security-100%25_Offline_(Zero_Internet)-success)](https://github.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

**LuxNote** is an ultra-premium, privacy-first offline music player for Android phones, tablets, and dedicated Digital Audio Players (DAPs: FiiO, Sony Walkman, Astell&Kern, HiBy). Engineered with a zero-copy local storage engine, a real-time `MediaStore` filesystem observer, a native 64-bit C++20 DSP core with ARM NEON SIMD intrinsics, and the signature **Obsidian Studio** design system.

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

## ⚡ Core Engineering & Architecture

### 1. Direct Local Storage Playback (Zero Disk Duplication)
* **Zero Disk Duplication:** Plays directly from device storage paths and Android `content://` URIs without copying files to sandbox storage.
* **Direct MediaStore Indexing:** Pre-indexed SQLite cursor queries read metadata directly from Android's MediaStore.
* **Universal Format Support:** Plays FLAC, WAV, ALAC, MP3, AAC, M4A, OGG, OPUS, and DSD-in-PCM.

### 2. Real-Time Storage Discovery
* **MediaStore ContentObserver:** Automatically observes `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
* **Instant Sync:** Detects file additions, deletions, and tag updates in real time without requiring manual rescanning.
* **Direct Storage Traversal:** Traverses standard directories (`/storage/emulated/0/Music`, `Download`, SD cards) for non-indexed audio files.

### 3. Continuous Background Playback
* **Audio Focus Resilience:** Intelligently handles transient focus changes and route transitions.
* **Foreground MediaStyle Service:** System notification with standard Android lock-screen transport controls and `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
* **Bounded Partial WakeLocks:** Service and playback engine manage CPU wake locks strictly during active playback.

### 4. Compiled Native C++20 DSP Core (`libhires_audio.so`)
* Compiled directly into APK via Android NDK (`26.1.10909125`) and CMake (`3.22.1`) with `-O3 -ffast-math -fvectorize` and ARM NEON intrinsics on `arm64-v8a`.
* **Canonical Lock-Free Triple Buffer:** Single-producer single-consumer parameter state packing with atomic acquire/release synchronization. Zero allocations and zero mutexes on the audio render thread. Formally proven in [docs/TRIPLE_BUFFER_OWNERSHIP_PROOF.md](docs/TRIPLE_BUFFER_OWNERSHIP_PROOF.md).
* **Direct Form II Transposed Biquads:** 10-band ISO equalizer filter and 12-band parametric EQ. Complete mathematical parity between Kotlin fallback and native C++ (verified in `CenterChannelParityContractTest` and `DspAudioEngineTest`).
* **Phase-Coherent Mid/Side Processor:** Center-channel vocal and instrumental focus with matched 2.6 kHz peaking (+6 dB) and 6 kHz high-shelf (+3 dB) acoustic presence.
* **1024-Point Radix-2 FFT Analyzer:** Real-time spectrum analysis.
* **Studio Soft Limiter:** Smooth `tanh` saturation knee at 0.95 preventing digital clipping.

### 5. Truthful Hardware USB DAC & Audio Routing Inspector
* **Conservative Classification:** Implemented via `UsbCapabilityClassifier` without inferring UAC2 or Asynchronous clocking from subclass alone.
* **Descriptor-Grounded Reporting:** Explicitly parses endpoint synchronization attributes (Asynchronous, Adaptive, Synchronous) and interface protocol.
* **Honest Audio Sinks:** Accurately reports whether Android AudioFlinger is mixing (32-bit Float AudioTrack) or if direct hardware routing is verified.

### 6. Fail-Closed Release Signing & Cryptographic Provenance
* **Fail-Closed Release Builds:** Gradle release task fails closed if production credentials or keystore are missing.
* **Cryptographic Verification:** Automated post-build verification with `apksigner` verifying signature validity and the trusted production certificate SHA-256 fingerprint:
  `992566A7DE642165F51526D7B5E0E0A6EB6CCB8954919B1DC82D2B348857B31A`
* Quality criteria and checklists documented in [docs/RELEASE_QUALITY_GATES.md](docs/RELEASE_QUALITY_GATES.md) and [docs/UI_UX_ACCEPTANCE_CHECKLIST.md](docs/UI_UX_ACCEPTANCE_CHECKLIST.md).

### 7. 100% Offline & Privacy First
* Strict absence of `android.permission.INTERNET` in `AndroidManifest.xml` (audited in CI).
* Zero analytics, zero cloud dependencies, zero telemetry.

---

## 📁 Repository Structure

```
├── HiResPlayer-Android/               # Complete Android Studio project
│   ├── app/
│   │   ├── src/main/cpp/             # Native C++20 DSP engine (NEON SIMD biquad, FFT, M/S)
│   │   ├── src/main/java/com/audioplayer/hires/
│   │   │   ├── audio/                # AudioTrackPlayer, DSP pipeline, USB DAC inspector
│   │   │   ├── data/                 # AudioLibraryRepository, MediaStoreObserver, Diagnostics
│   │   │   ├── playback/             # PlaybackManager, HiResAudioService
│   │   │   └── ui/                   # Jetpack Compose UI (Obsidian Studio theme & screens)
│   │   └── src/test/                 # Comprehensive unit & parity test suite
│   ├── build.gradle.kts              # Fail-closed signing configuration
│   └── gradlew.bat
├── docs/
│   ├── RELEASE_QUALITY_GATES.md      # Acceptance criteria and release gates
│   ├── TRIPLE_BUFFER_OWNERSHIP_PROOF.md # Formal mathematical concurrency proof
│   └── UI_UX_ACCEPTANCE_CHECKLIST.md # UI/UX & accessibility quality checklist
├── scripts/
│   └── verify-release-apk.ps1        # Strict apksigner certificate fingerprint verifier
├── Build-AndroidApp.bat               # Windows 1-click test & verified release build tool
├── LuxNote.apk                        # Production release APK (Scheme v2 signed & verified)
├── LuxNote.apk.sha256                 # Cryptographic verification checksum
└── README.md                          # Documentation
```

---

## 🛠️ Building from Source

### Prerequisites
* JDK 17 or JDK 21 (`JAVA_HOME`)
* Android SDK (API 24 to 36, Platform Tools, Build Tools 36.0.0)
* Android NDK (`26.1.10909125`)
* CMake (`3.22.1`)

### Build Commands

```powershell
# Run Unit & Parity Tests
cd "HiResPlayer-Android"
.\gradlew.bat testDebugUnitTest

# Assemble Release APK
.\gradlew.bat assembleRelease
```

Or run the 1-click build and verification script from the repository root:
```powershell
.\Build-AndroidApp.bat
```

---

## 📲 Installation

Copy `LuxNote.apk` to your Android device or install via ADB:

```powershell
adb install -r "LuxNote.apk"
```
