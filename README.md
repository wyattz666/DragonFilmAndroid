# DragonFilm Android

<p align="center">
  <img src="app/src/main/res/drawable/logo.png" alt="DragonFilm Logo" width="130" />
</p>

<p align="center">
  <strong>A cutting-edge, high-performance cinema streaming application for Android (Phones & Tablets) crafted with Kotlin & Jetpack Compose.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84.svg?style=flat&logo=android" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin%201.9+-7F52FF.svg?style=flat&logo=kotlin" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Media-Media3%20ExoPlayer-F5C518.svg?style=flat" alt="ExoPlayer" />
  <img src="https://img.shields.io/badge/Architecture-MVI%20%2F%20Coroutines%20%2F%20Flow-673AB7.svg?style=flat" alt="Architecture" />
  <img src="https://img.shields.io/badge/Release-v1.0.1-F5C518.svg?style=flat" alt="Release" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat" alt="License" />
</p>

---

> [!TIP]
> **Zero-Lag Performance**: Engineered with hardware-accelerated rendering, 25% RAM + 150MB Disk image caching via Coil, and aggressive in-memory query caching (TTL) for instantaneous **0ms** tab transitions.

> [!NOTE]
> **Multi-Platform Ecosystem**: DragonFilm is also available as a static Web application and a native iOS client (SwiftUI). All watch histories and saved libraries seamlessly synchronize across all devices.

---

## Key Highlights

| Feature | Description |
| :--- | :--- |
| **High-End Media3 Player** | Custom video controller powered by **AndroidX Media3 ExoPlayer**. Supports Native HLS (`.m3u8`), double-tap to seek +/-10s, hold-to-speed-up (2x), speed dropdown (0.5x to 2.0x), aspect ratio switcher (Fit / Zoom Fill), and automatic resume playback. |
| **Smart Embed Fallback** | Integrated WebView fallback player for embed-only stream sources (Server 3 & Server 4). |
| **Real-Time Cloud Sync** | Full 2-way cloud merge (`/api/user-data`) compatible with DragonFilm Web & iOS Schema v4. Seamlessly synchronizes watch history, resume bookmarks, watch-later list, liked movies, and favorite actors. |
| **Flexible Authentication** | Secure user authentication supporting both native DragonFilm accounts and **Sign in with Google (Google OAuth)**. |
| **Obsidian & Gold Aesthetics** | Cinema-grade dark theme featuring pure OLED deep black (`#07080A`), metallic gold gradients (`#F5C518`), and frosted glassmorphism overlays. |
| **Live Trends & Rankings** | Real-time trending rankings curated from **Netflix Vietnam Top 10**, **TMDB Drama Weekly (KR & CN)**, and **AniList GraphQL Trending Anime (Weekly & Seasonal)**. |
| **14-Day Release Calendar** | Interactive release schedule tracker for ongoing TV series and seasonal anime. |
| **Instant Search Engine** | Fast, debounced search querying across multiple high-speed server clusters (Server 1, Server 2, Server 3, Server 4). |
| **Live Social Comments** | Real-time community discussion section for every movie and the homepage with 15s live polling. |

---

## Sideloading & Installation

### Option 1: Direct APK Install (Recommended)
1. Download the latest pre-compiled **`DragonFilm.apk`** from [GitHub Releases](https://github.com/wyattz666/DragonFilmAndroid/releases/latest).
2. Transfer or open the `.apk` on your Android device and tap **Install** *(Allow installation from unknown sources if prompted)*.
3. Or install directly via ADB:
   ```bash
   adb install -r DragonFilm.apk
   ```

### Option 2: Android Studio Emulator
- Start any virtual device in Android Studio (**Device Manager** -> **Play**).
- Drag and drop `DragonFilm.apk` directly into the emulator window.

---

## Prerequisites & Building from Source

### Requirements
- **Java Development Kit (JDK)**: OpenJDK 17+
- **Android SDK**: Build-Tools 34.0.0, Platform API Level 34
- **Gradle**: 8.7+

### Quick CLI Build
Clone the repository and run the automated build script:
```bash
git clone https://github.com/wyattz666/DragonFilmAndroid.git
cd DragonFilmAndroid

chmod +x export_apk.sh
./export_apk.sh
```
The output APK file will be automatically generated at:
```text
./DragonFilm.apk
```

### Running with Android Studio
1. Open **Android Studio**.
2. Select **Open** and choose the `DragonFilmAndroid` project directory.
3. Allow Gradle to sync dependencies.
4. Select your target device or emulator and press **Run** (`Control + R` / `Shift + F10`).

---

## Project Architecture

```text
DragonFilmAndroid/
├── app/
│   ├── build.gradle.kts          # Dependencies (Compose BOM, Media3, Coil, Retrofit, OkHttp)
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, Activity, Picture-in-Picture configuration
│       ├── java/com/dragonfilm/app/
│       │   ├── DragonFilmApp.kt  # Application entry point, Coil ImageLoader cache & DI Singletons
│       │   ├── MainActivity.kt   # Edge-to-edge Root Compose Activity
│       │   ├── data/
│       │   │   ├── api/          # Retrofit endpoints, OkHttp client, GraphQL queries
│       │   │   ├── model/        # Movie, Episode, Server, Rankings, User & Comment models
│       │   │   ├── repository/   # MovieRepository with in-memory caching (TTL) & source aggregations
│       │   │   └── storage/      # LocalStore (JSON persistence), AuthManager & CloudSync
│       │   ├── ui/
│       │   │   ├── theme/        # Theme tokens (DFColor, DFTypography, GlassCard, Shimmer)
│       │   │   ├── components/   # PosterCard, SectionHeader, Badge, EmptyState, RankingViews
│       │   │   ├── navigation/   # BottomNavBar (Safe Insets) & AppNavigation Router
│       │   │   ├── home/         # Hero Carousel, Ranking Rows, Filter Dialog, Latest Grid
│       │   │   ├── detail/       # Movie Details, Multi-Server Switcher, Version Tabs, Cast
│       │   │   ├── player/       # Media3 ExoPlayer Controller, Custom Gestures & Fallback Embed
│       │   │   ├── search/       # Instant Debounced Search & Recent History
│       │   │   ├── schedule/     # 14-Day Calendar Release Tracker
│       │   │   ├── library/      # Watch History, Liked, Watch Later & Favorite Actors
│       │   │   ├── comments/     # Live Polling Comment Section & Composer
│       │   │   └── profile/      # User Profile, Auth Dialog & Cloud Synchronization
│       │   └── util/             # StreamResolver (HLS stream decoder) & SourceNormalizer
│       └── res/
│           ├── drawable/         # Logos & vector graphics
│           ├── values/           # Strings, Colors, Themes
│           └── mipmap/           # Launcher Icons
├── DragonFilm.apk                # Prebuilt APK ready for installation
├── export_apk.sh                 # One-click command line build script
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Gradle repositories & plugin definitions
└── README.md
```

---

## Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Video Engine**: [AndroidX Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3) with HLS Extensions
- **Image Pipeline**: [Coil Compose](https://coil-kt.github.io/coil/compose/) (Hardware Bitmaps, Memory & Disk Caching)
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) + [OkHttp 4](https://square.github.io/okhttp/) + [Gson](https://github.com/google/gson)
- **Asynchrony**: Kotlin Coroutines + StateFlow / Flow

---

## License

This project is licensed under the [MIT License](LICENSE).

**Disclaimer:** DragonFilm is an open-source educational project. All movie information, images, and video streams are retrieved from publicly available third-party APIs. DragonFilm does not host or store any media files on its servers.

---

<p align="center">
  Developed by the <strong>DragonFilm Team</strong>.
</p>
