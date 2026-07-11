# Spec 012: Keep Screen Awake and Pause on Background

- **Status**: IN_PROGRESS
- **Date**: 2026-07-11

---

## 📋 Q&A and Assumptions
* **Q**: Under what conditions should the screen stay awake?
  * **A**: The screen should stay awake only when a video is actively playing (`isPlaying == true`). If the video is paused, ended, or if the user is viewing a photo, the screen should respect the system sleep timeout.
* **Q**: How will we keep the screen awake?
  * **A**: We will use Android's standard view-level property `keepScreenOn` on the `PlayerView` instance inside the `AndroidView` update block:
    `view.keepScreenOn = isPlaying`
* **Q**: What happens when the user leaves the app (moves to another app, goes home, or locks the screen)?
  * **A**: We will listen to Compose's lifecycle events using `LocalLifecycleOwner` and `LifecycleEventObserver`. When the lifecycle transitions to `Lifecycle.Event.ON_PAUSE`, we will pause the video player and update `isPlaying = false`.
* **Q**: How do we clean it up when navigating away?
  * **A**: Since `keepScreenOn` is a view-level flag, the Android framework automatically clears the window sleep prevention state as soon as the `PlayerView` is detached/disposed from the window when the user navigates away or closes the player.

## 📋 Feature Requirements
1. **Video Playback Sleep Prevention**:
   * Set `keepScreenOn = isPlaying` on the `PlayerView` in the `update` block of the `AndroidView` inside `MediaPlayerScreen`.
   * This ensures the screen remains awake while the video is playing and yields back sleep control when paused or when navigating away.

2. **Auto-Pause on Background/Sleep**:
   * Observe lifecycle events on `LocalLifecycleOwner` in a `DisposableEffect(lifecycleOwner, exoPlayer)`.
   * Intercept `Lifecycle.Event.ON_PAUSE`.
   * Pause playback (`exoPlayer.pause()`) and set `isPlaying = false` when the event fires.

## 📝 Expected Code Changes
* [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt): 
  * Add lifecycle library imports.
  * Register a `LifecycleEventObserver` to pause ExoPlayer and update state on `ON_PAUSE`.
  * Set `view.keepScreenOn = isPlaying` inside `AndroidView.update` block.

## 🧪 Testing Strategy
* Compile the app and verify it builds successfully.
* Play a video and verify that the screen stays awake.
* Pause the video, and verify that the screen can sleep normally.
* While a video is playing, press Home, lock the screen, or open another app. Re-open the app and verify the video is paused.
