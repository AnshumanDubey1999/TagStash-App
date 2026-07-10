# Spec 009: Video Playback Fixes

- **Status**: DONE
- **Date**: 2026-07-10

---

## 📋 Q&A and Assumptions
* **Q**: What is causing the autoplay bug?
  * **A**: Two main issues:
    1. A state-sync race condition: `isPlaying` is initialized to `true` on file load but immediately overwritten to `false` by `exoPlayer.isPlaying` in `DisposableEffect` before the player is prepared.
    2. Consecutive video navigation: Calling `exoPlayer.stop()` during a file-change transition triggers `onIsPlayingChanged(false)` on the listener. This updates the Compose `isPlaying` state to `false` and prevents the next video from autoplaying.
* **Q**: How will we fix it?
  * **A**: 
    1. Remove the line `isPlaying = exoPlayer.isPlaying` from the initial setup of `DisposableEffect(exoPlayer, file)`.
    2. Force `isPlaying = true` and `exoPlayer.playWhenReady = true` in the `LaunchedEffect(file, exoPlayer)` block when loading a new video file to override any transient `stop()` callback states.
* **Assumptions**:
  * Videos should automatically play upon opening or navigation.
  * Rotating the screen preserves play/pause state correctly since `rememberSaveable` handles state restoration.

## 📋 Feature Requirements
1. **Fix Autoplay Bug**:
   * Modify the player-state sync logic in `MediaPlayerScreen` to prevent setting `isPlaying` to the player's current playback state during listener binding.
   * Rework `DisposableEffect(exoPlayer, file)` to only attach/detach listeners and not manually overwrite the initial state during composition.
   * Update the media loading `LaunchedEffect(file, exoPlayer)` to explicitly force `isPlaying = true` and `exoPlayer.playWhenReady = true` when preparing a new video file, preventing stopped state propagation from corrupting the autoplay state.

## 📝 Expected Code Changes
* [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt): 
  * Remove line `isPlaying = exoPlayer.isPlaying` inside the `DisposableEffect(exoPlayer, file)` block.
  * Explicitly assign `isPlaying = true` and `exoPlayer.playWhenReady = true` inside the `LaunchedEffect(file, exoPlayer)` block for videos.

## 🧪 Testing Strategy
* Compile the app locally.
* Run the app, open a video file, and confirm it plays automatically.
* Navigate between videos using left/right swipe/tap areas and confirm they autoplay.
* Pause a video, rotate the screen, and verify that it remains paused.
