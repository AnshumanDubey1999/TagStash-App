# Spec 003: In-App Image Viewer

- **Status**: DONE
- **Date**: 2026-06-30

---

## 📋 Feature Requirements
* Open image files (PNG, JPG, JPEG, WEBP, GIF, BMP) inside a dedicated, full-screen in-app media player instead of launching the OS default app.
* **Image Sizing Rules**:
  * Decode image bounds first to determine original dimensions.
  * If both width and height are smaller than the screen width and height, render the image in its original size centered on the screen.
  * Otherwise, scale the image to fit the screen while preserving its aspect ratio.
* **Hidden Gesture Overlay**:
  * Divide the screen width into 3 invisible tap zones:
    * **Left 30%** width zone.
    * **Right 30%** width zone.
    * **Center 40%** width zone (toggles header overlay visibility).
  * **Navigation Boundary Rules**:
    * If there are **no other images** in the directory: Both Left and Right zones act as the Center zone (toggling header overlay).
    * If viewing the **first image**: The Left zone acts as the Center zone (toggling header overlay). The Right zone navigates to the next image.
    * If viewing the **last image**: The Left zone navigates to the previous image. The Right zone acts as the Center zone (toggling header overlay).
  * Navigation ordering must match the alphabetical listing order of images seen in the directory explorer.
* **Header Overlay**:
  * Semi-transparent dark background positioned at the top of the screen.
  * Displays:
    * A back/close button (arrow icon) to return to the file explorer.
    * Image filename.
    * Image file size (formatted).
    * Last modified date (formatted).
* **Back Button Handling**:
  * Intercept the system back button/gesture to close the media player and return to the file list.

## 🎨 UI/UX Changes
* **Black Screen Background**: To ensure maximum contrast when viewing images.
* **Semi-transparent Header**: Dark overlay with clear white typography.
* **Interactive Zones**: Invisible tap targets.
* **Display Loader**: Visual progress indicator shown while Coil loads the bitmap.
* **Broken File State**: Clear message/illustration displayed if the image is corrupted or fails to decode.

## 🛠️ Tech Stack & Android Specifics
* **Coil**: Integrate `io.coil-kt:coil-compose:2.7.0` for asynchronous image loading and memory caching.
* **BitmapFactory**: Use `BitmapFactory.Options` with `inJustDecodeBounds = true` to query image resolution instantly on `Dispatchers.IO` without memory overhead.
* **State Navigation**: Manage the active file view state (`var activeImageViewerFile by remember { mutableStateOf<File?>(null) }`) inside the MainScreen scope.

## ⚠️ Edge Cases & Failure Modes
* **No other images / Boundaries**: Covered by specific navigation boundary rules.
* **Broken files**: Handled gracefully by showing an error message on screen.

## 🧪 Testing Strategy
* **Unit Tests**:
  * Test sibling image list calculations and indexing (first, middle, last indices).
* **UI/Roborazzi Tests**:
  * Render `ImageViewerScreen` with both small and large test images, verifying screenshot boundaries and overlay toggle visibility.

## 📝 Expected Code Changes
* [libs.versions.toml](file:///Users/personal/AndroidStudioProjects/TagStash/gradle/libs.versions.toml) & [app/build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts): Add Coil dependencies.
* [FileHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/utils/FileHelper.kt): Add logic to retrieve sorted sibling images and decode resolution bounds.
* [ImageViewerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/ImageViewerScreen.kt): Implement the full-screen viewer layout.
* [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt): Hook up the image click event to open `ImageViewerScreen`.

---

## ❓ Open Questions & Assumptions (Resolved)
1. **Coil**: Approved. We will use Coil for loading and memory caching.
2. **Back Button**: Approved. We will place a visual back button inside the header overlay in addition to system gesture handlers.
3. **Boundary Tap Logic**: Resolved. If no navigation is possible, Left/Right clicks default to toggling the details header.
