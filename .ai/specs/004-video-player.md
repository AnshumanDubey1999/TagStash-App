# Spec 004: In-App Video & Combined Media Player

- **Status**: IN_PROGRESS
- **Date**: 2026-06-30

---

## 📋 Feature Requirements
* Extend the in-app media player to support video formats (MP4, MKV, etc.) in addition to image formats.
* **Unified Sibling Navigation**:
  * Clicking Left (30% width) or Right (30% width) navigates between all supported media types (images and videos) within the directory.
  * Sorting remains alphabetical matching the directory list order.
* **Video Playback Behavior (ExoPlayer)**:
  * Automatically play videos on load.
  * Start unmuted.
  * **Looping State**:
    * Maintain a global loop state (persisted across files during the session, on by default).
    * If loop is ON: Video loops infinitely.
    * If loop is OFF: When the video completes playback, automatically navigate to the next media item. If it is the last item in the list, pause/stop at the end.
* **Header & Footer Overlays**:
  * Tapping the center 40% zone toggles visibility of the overlays.
  * **Header**: Displays file details and visual back/close button (same for both images and videos).
  * **Footer (Videos Only)**:
    * Hidden entirely when viewing static images.
    * For videos, contains:
      * Play/Pause toggle button.
      * Mute/Unmute toggle button.
      * Fast Forward button (skips forward by 10 seconds).
      * Loop toggle button (toggles global loop state).
      * A seekable timeline slider showing current position and total duration (formatted as MM:SS).
* **Back Navigation & Cleanup**:
  * Pressing back closes the player, returns to the directory list, and fully releases ExoPlayer resources (stopping all audio and video).

## 🎨 UI/UX Changes
* **Media3 Player View**: Renders the video surfaces within the layout boundaries.
* **Footer Control Bar**: Styled with a semi-transparent dark background matching the header.
* **Progress Slider**: Smooth seeking and state updates (timeline matches current playback position).

## 🛠️ Tech Stack & Android Specifics
* **Jetpack Media3 (ExoPlayer)**: Include `androidx.media3:media3-exoplayer:1.5.0` (or compatible version) to manage video rendering and playback states.
* **Android Lifecycle Integration**: Release ExoPlayer in `DisposableEffect` to ensure resources are cleaned up when leaving the screen or backgrounding.

## ⚠️ Edge Cases & Failure Modes
* **Audio Focus**: Ensure playback pauses if the app is backgrounded.
* **Corrupt Video Files**: Show the standard error view with a failure message if ExoPlayer encounters a playback exception.
* **Timeline updates**: Run a coroutine-based polling loop (e.g. every 200ms) while the video is playing to update the timeline position smoothly.

## 🧪 Testing Strategy
* **Unit Tests**:
  * Update media sibling resolver tests to verify combined list containing both images and videos.
* **UI/Roborazzi Tests**:
  * Render `MediaPlayerScreen` with a video file, verifying layout positioning of the footer controls, play state, and timeline.

## 📝 Expected Code Changes
* [libs.versions.toml](file:///Users/personal/AndroidStudioProjects/TagStash/gradle/libs.versions.toml) & [app/build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts): Add Media3 ExoPlayer libraries.
* [FileHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/utils/FileHelper.kt): Refactor `getSiblingImages` to `getSiblingMedia` (returning files matching both `isImage` and `isVideo`).
* [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt): Replace/refactor `ImageViewerScreen.kt` to handle both image scaling and ExoPlayer media surfaces.
* [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt): Update file click handler to load both image and video files inside `MediaPlayerScreen`.

---

## ❓ Open Questions & Assumptions (Resolved)
1. **Static Image Footer**: Hide the footer control bar entirely for images.
2. **Visual Feedback Overlay**: Rely strictly on the footer controls without displaying transient center-screen overlays.
