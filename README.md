# TagStash 🏷️

TagStash is a modern, elegant Android application designed for local file exploration, media viewing/playback, and tag-based organization.

---

## 🚀 Key Features

* **Local File Explorer**:
  * Navigate system files with an adaptive Grid and List layout.
  * Real-time search and filter capabilities.
* **Immersive Media Player**:
  * High-quality image rendering powered by **Coil**.
  * Custom full-screen video and audio playback using **Jetpack Media3 (ExoPlayer)**.
  * Persistent video playback state, positions, and controls overlay visibility across configuration changes (screen rotations).
* **Tag-based Organization**:
  * Categorize files/folders using custom tags.
  * Robust data layer powered by **Room Database** with Kotlin Coroutines/Flow for reactive data streams.
* **Modern Design**:
  * Beautiful Dark Mode UI using Material Design 3 tokens.
  * Sleek semi-transparent overlays for player controls.

---

## 🛠️ Technology Stack

* **Platform & Language**: Android (Min SDK 33, Target SDK 36), Kotlin
* **UI Framework**: Jetpack Compose (Material 3)
* **Image Loading**: Coil
* **Media Engine**: Jetpack Media3 (ExoPlayer)
* **Database & Persistence**: Room Database
* **Concurrency**: Kotlin Coroutines & Flow

---

## ⚙️ Development & Commands

Run the following Gradle commands in the project root:

* **Compile & Check**:
  ```bash
  ./gradlew compileDebugKotlin
  ```
* **Run Unit & UI/Roborazzi Tests**:
  ```bash
  ./gradlew test
  ```
* **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
* **Run Code Quality Lints**:
  ```bash
  ./gradlew check
  ```

---

## 📦 Versioning System

TagStash manages its versioning parameters dynamically. The values are declared in:
* [`gradle.properties`](file:///Users/personal/AndroidStudioProjects/TagStash/gradle.properties)

Properties:
* `app.version.code`: The internal version code (integer) updated sequentially on release/nightly builds.
* `app.version.name`: The user-facing version name (string) following Semantic Versioning (`MAJOR.MINOR.PATCH`).

These properties are automatically loaded and applied in [`app/build.gradle.kts`](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts).

---

## 🎨 Launcher Icons

Launcher icons are configured as legacy density-specific PNGs to provide reliable cross-device rendering:
* **Normal**: `ic_launcher.png`
* **Round**: `ic_launcher_round.png`

The source icons are placed in the density-specific folders under `app/src/main/res/mipmap-*` (from `mdpi` to `xxxhdpi`). The default adaptive launcher XML configuration (`mipmap-anydpi`) was removed so that the launcher falls back to these custom assets.
