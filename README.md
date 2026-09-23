<div align="center">

# LASTWAVEX

**Modern Android YouTube Music Client & Universal Last.fm Scrobbler with Real-Time Lyrics, Algorithmic Mixes, and Local Offline Music Storage.**

<p align="center">
  <a href="#">
    <img src="https://img.shields.io/badge/Client-YouTube%20Music-FF0000?style=for-the-badge&logo=youtubemusic&logoColor=white&labelColor=2d2d2d" alt="YouTube Music Client" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Scrobbler-Last.fm-D51007?style=for-the-badge&logo=lastdotfm&logoColor=white&labelColor=2d2d2d" alt="Last.fm Scrobbler" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Platform-Android%2010%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=2d2d2d" alt="Android 10+" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge&labelColor=2d2d2d" alt="License GPLv3" />
  </a>
</p>

</div>

<br/>

## Overview

**LASTWAVEX** (`com.specttre404.lastwavex`) is an open-source native Android music application powered by the YouTube Music catalog and Last.fm scrobbler services. Built with 100% Kotlin and Jetpack Compose (Material 3 Expressive), LASTWAVEX combines ad-free background streaming, real-time synchronized karaoke lyrics, personalized recommendation feeds, and a background media scrobbler.

---

## Key Features

| Feature | Description |
|:---|:---|
| **YouTube Music Streaming** | Stream tracks, albums, artists, and playlists from YouTube Music with background playback and lockscreen controls. |
| **Universal Last.fm Scrobbler** | Background media scrobbler tracking listening activity across installed music players with zero battery overhead. |
| **Real-Time Synced Lyrics** | Millisecond-accurate synchronized lyrics powered by LRCLIB with customizable karaoke animations. |
| **Smart Discovery & Mixes** | Algorithmic taste mixes, artist radios, and recommendation feeds built from your listening history and taste profile. |
| **Offline Music Downloader** | One-tap downloads stored locally (`Music/LastWave`), complete with high-resolution artwork and synced `.lrc` lyrics. |
| **Material 3 Expressive UI** | Dynamic wallpaper theming, artwork color extraction, fluid transition animations, and tactile haptic feedback. |

---

## Tech Stack & Open-Source Libraries

- **Language & Framework:** 100% Kotlin, Jetpack Compose, Material 3 Expressive
- **Audio Engine & Service:** AndroidX Media3 ExoPlayer, MediaSessionCompat, Android Auto integration
- **Network & Data:** Retrofit, OkHttp, Room Database, Jetpack DataStore, Dagger Hilt
- **Lyrics Provider:** [LRCLIB](https://lrclib.net) millisecond-synchronized lyrics
- **Decoder & Native Runtimes:** Jellyfin Media3 FFmpeg software decoder, Oboe native audio, QuickJS runtime

---

## System Requirements & Installation

1. **Android Version:** Android 10.0+ (API Level 29 or higher).
2. Download `app-release.apk` (or `app-debug.apk`) from the **Releases** tab.
3. Install the APK on your Android device.
4. Optional: Connect your Last.fm account in **Settings → Integrations** to enable scrobbling and global stats.

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/specttre404/LastWaveX.git
cd LastWaveX

# Build debug APK
./gradlew app:assembleDebug

# Build minified release APK
./gradlew app:assembleRelease
```

Generated APK paths:
- **Debug APK:** `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK:** `app/build/outputs/apk/release/app-release.apk`

---

## Credits & Upstream Acknowledgments

LASTWAVEX is a re-engineered FOSS fork building upon open-source Android music architecture and community projects:

- **[LastWave](https://github.com/Clash-Projects/LastWave-native)** — Original open-source project architecture and UI design inspiration.
- **[InnerTubeX](https://github.com/metrolist/innertubex)** — InnerTube YouTube Music API client adapter.
- **[NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor)** — YouTube extraction and metadata library.
- **[LRCLIB](https://lrclib.net)** — Synced lyrics database service.
- **[Media3 FFmpeg Decoder](https://github.com/jellyfin/jellyfin-ffmpeg)** — High-fidelity software audio decoding.

---

## Disclaimer

> [!NOTE]
> **Educational & Non-Commercial Notice**
>
> LASTWAVEX is an open-source, non-commercial application developed strictly for research, educational, and personal use to demonstrate modern Android Media3 architecture and Jetpack Compose design patterns.
>
> **Notice of Non-Affiliation**
> - LASTWAVEX is an independent community project and is **not affiliated with, endorsed, or sponsored by Google LLC, YouTube, YouTube Music, Last.fm, or any music streaming service**.
> - LASTWAVEX **does not host, store on private servers, or distribute copyrighted media files**. All content and metadata are retrieved from public web endpoints under fair research use. All trademarks belong to their respective owners.
