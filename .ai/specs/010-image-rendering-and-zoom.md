# Spec 010: Image Rendering and Zoom Support

- **Status**: IN_PROGRESS
- **Date**: 2026-07-11

---

## 📋 Q&A and Assumptions
* **Q**: What are the scale limits for zoom?
  * **A**: Min scale of `1.0f` (fit to screen) and max scale of `5.0f` (5x magnification).
* **Q**: How does the double-tap gesture behave when scale is `1.0f` (not zoomed in)?
  * **A**: Double-tapping **only in the center 40% zone** will zoom the image to `2.0f`. Double-tapping the Left 30% or Right 30% zones does not trigger zoom.
* **Q**: How does the double-tap gesture behave when scale is greater than `1.0f` (zoomed in)?
  * **A**: Double-tapping **anywhere** on the screen resets the scale to `1.0f` and offset to `Offset.Zero`.
* **Q**: Does navigation work while zoomed in?
  * **A**: No. If `scale > 1.0f`, the Left 30% and Right 30% navigation zones are completely disabled. Taps/drags in those zones will not trigger navigation.
* **Q**: How does rotation affect the zoom state?
  * **A**: On device rotation, the zoom scale must automatically reset to `1.0f` and offset to `Offset.Zero` (we will use simple `remember { ... }` instead of `rememberSaveable` so the state resets on configuration change).
* **Q**: How does image fitting change?
  * **A**: All images are scaled using `ContentScale.Fit` to automatically fit the screen bounds while maintaining their aspect ratio.

## 📋 Feature Requirements
1. **Fit-to-Screen Image Sizing**:
   * Use `ContentScale.Fit` for all images, removing raw centering of small images.

2. **State Management**:
   * Declare `scale` and `offset` state variables using standard `remember` (not `rememberSaveable`), so they reset to `1.0f` and `Offset.Zero` during configuration changes (screen rotations) and file changes.

3. **Pinch-to-Zoom & Panning Gestures**:
   * Capture pinch gestures using `detectTransformGestures`.
   * Constrain scale values strictly between `1.0f` and `5.0f`.
   * Allow panning/dragging when `scale > 1.0f`, clamping boundaries so the image doesn't drag off-screen.

4. **Double-Tap Shortcut**:
   * When `scale == 1.0f`: Double-tap in the center 40% zone -> sets scale to `2.0f`.
   * When `scale > 1.0f`: Double-tap **anywhere** on the screen -> resets scale to `1.0f` and offset to `Offset.Zero`.

5. **Interception of Left/Right Navigation**:
   * Navigation areas (Left 30% and Right 30% single-taps) only trigger navigation when `scale == 1.0f`.
   * If `scale > 1.0f`, single-taps in navigation areas are ignored (disabled).

## 📝 Expected Code Changes
* [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt): 
  * Implement `remember` scale and offset states.
  * Integrate gesture handlers inside the image view layout, splitting the gestures based on `scale` and touch coordinates.
  * Update Left/Right tap zones to verify `scale == 1.0f` before triggering navigation.

## 🧪 Testing Strategy
* Compile the app and verify it builds successfully.
* Open an image, verify it scales to fit the screen.
* Rotate the screen while zoomed in, verify scale resets to `1.0f`.
* Double-tap the center 40% zone, verify it zooms in to `2.0f`.
* Double-tap the Left 30% or Right 30% zone when not zoomed, verify it does NOT zoom.
* Zoom in using pinch, verify max scale is `5.0f`.
* While zoomed, double-tap anywhere to reset scale to `1.0f`.
* Verify navigation is disabled when `scale > 1.0f`.
