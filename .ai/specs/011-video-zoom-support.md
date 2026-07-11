# Spec 011: Video Zoom Support

- **Status**: DONE
- **Date**: 2026-07-11

---

## 📋 Q&A and Assumptions
* **Q**: What are the scale limits for video zoom?
  * **A**: Min scale of `1.0f` (fit to screen) and max scale of `5.0f` (5x magnification), matching image zoom.
* **Q**: How do we resolve the performance/transform limitation of `SurfaceView`?
  * **A**: We will create a layout XML file (`res/layout/player_view_texture.xml`) for `PlayerView` with `app:surface_type="texture_view"`. This forces `PlayerView` to use `TextureView`, allowing smooth graphics transformations via Compose's `Modifier.graphicsLayer`.
* **Q**: How do we fetch video dimensions for boundary clamping?
  * **A**: We will declare a `videoDimensions` state variable and update it from the `ExoPlayer` listener `onVideoSizeChanged` callback. We will also initialize it when the screen prepares with the current video size.
* **Q**: How does the double-tap gesture behave on videos?
  * **A**:
    * When scale is `1.0f`: Double-tapping the center 40% zone zooms the video to `2.0f` (centered).
    * When scale is greater than `1.0f`: Double-tapping anywhere on the screen resets the scale to `1.0f` and offset to `Offset.Zero`.
* **Q**: Does navigation work while zoomed in?
  * **A**: No, single-tap navigation zones (Left 30%, Right 30%) are disabled when `scale > 1.0f`.
* **Q**: Does zoom state reset on device rotation or navigation?
  * **A**: Yes, the zoom scale and offset reset to `1.0f` and `Offset.Zero` on orientation changes and navigation (file change).

## 📋 Feature Requirements
1. **TextureView Player Layout XML**:
   * Create `app/src/main/res/layout/player_view_texture.xml` containing `PlayerView` with surface type set to `texture_view` and controller disabled.

2. **Unified State Management**:
   * Move `scale` and `offset` state variables to the top level of `MediaPlayerScreen` so they are shared between images and videos.
   * Add a top-level `videoDimensions` state variable.

3. **ExoPlayer Video Size Synchronization**:
   * Update `Player.Listener` in `DisposableEffect(exoPlayer, file)` to listen for `onVideoSizeChanged` and update `videoDimensions` dynamically.
   * Set `videoDimensions` immediately upon initializing the listener block using `exoPlayer.videoSize`.

4. **Pinch-to-Zoom & Panning for Videos**:
   * Wrap the video `AndroidView` inside a gesture Box.
   * Add `Modifier.pointerInput` to intercept tap and transform (zoom/pan) gestures for video, using the same clamping math as images.
   * Apply scale and offset values to the video container using `Modifier.graphicsLayer`.

5. **Navigation Sync**:
   * Verify `scale == 1.0f` before allowing Left/Right swipe/tap navigation for videos.

## 📝 Expected Code Changes
* **Layout Resources**:
  * Create [player_view_texture.xml](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/res/layout/player_view_texture.xml).
* [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt):
  * Elevate `scale` and `offset` states to the top level.
  * Register video size listener callback.
  * Inflate layout XML inside the `AndroidView` factory.
  * Wrap video rendering in a gesture-enabled container Box.

## 🧪 Testing Strategy
* Compile the app and verify it builds successfully.
* Play a video, verify it scales using the default layout correctly.
* Double-tap the center 40% of the screen while video is playing, verify it zooms to `2.0f` and continues playing smoothly.
* Pinch-zoom in up to `5.0f` scale and drag/pan the video, verifying it clamps correctly at video borders.
* While zoomed, double-tap anywhere on the screen to reset zoom to `1.0f`.
* Rotate the device while zoomed, verify it resets to `1.0f`.
* Verify that tapping left/right edges does not navigate while zoomed in.
