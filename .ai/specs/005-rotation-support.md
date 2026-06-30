# Spec 005: Configuration Change & State Persistence Support

- **Status**: IN_PROGRESS
- **Date**: 2026-06-30

---

## 📋 Feature Requirements
* **File Explorer State Persistence**:
  * Persist the active directory path (`currentDirectory`) across screen rotations and configuration changes using `rememberSaveable` with a custom `Saver`.
  * Ensure the app doesn't reset to the home directory (`/storage/emulated/0`) when the screen is rotated.
* **Media Player State Persistence**:
  * Persist the currently opened media file (`activeMediaPlayerFile`) across rotation using `rememberSaveable`.
  * Persist overlay visibility (`showOverlays`) in `MediaPlayerScreen` across rotation.
  * Restore video playback position (`currentPosition`), playback state (`isPlaying`), and mute state (`isMuted`) in `MediaPlayerScreen` after rotation so the video resumes smoothly from the same frame.

## 🎨 UI/UX Changes
* None (behavioral persistence only).

## 🛠️ Tech Stack & Android Specifics
* Use Jetpack Compose `rememberSaveable` and custom `Saver<File, String>` implementations to bundle `File` structures.

## 🧪 Testing Strategy
* **UI/Roborazzi Tests**:
  * Add a test case that verifies state restoration or custom saver conversion works without throwing exceptions.

## 📝 Expected Code Changes
* [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt): Update `currentDirectory`, `activeMediaPlayerFile`, and `globalLoopEnabled` to use `rememberSaveable`.
* [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt): Update player state variables and overlay visibility state to use `rememberSaveable`.
