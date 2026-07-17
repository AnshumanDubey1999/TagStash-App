# Specification: 015-avif-support

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: How should we handle hardware bitmaps (allowHardware) in Coil to prevent native GPU/decoder crashes on large/unsupported AVIF images?
  - **A**: Disable hardware bitmaps (`allowHardware(false)`) specifically for AVIF images to prevent GPU/decoder crashes while maintaining standard performance for other image formats.
- **Q**: Should we hardcode a MIME type fallback for the '.avif' extension in `getMimeType()`?
  - **A**: Yes, hardcode a fallback for 'avif' to return 'image/avif' in `FileHelper.kt`'s `getMimeType()` function.
- **Assumptions**:
  - AVIF files are recognized as images in file lists, filtering, and media player navigation.
  - Sibling navigation inside the media player should seamlessly support AVIF images alongside existing image/video files.
  - Robolectric unit tests might not decode AVIF dimensions natively on the host machine. The code will handle `getImageDimensions` returning `0x0` or negative values gracefully without crashes (which it already does).
  - A valid small AVIF sample file will be placed in the test resources folder `app/src/test/resources/testData/images/` for unit testing.

---

## 🛠️ Feature Requirements
- Support `.avif` file extension as an image format.
- Display the emerald/green image icon for `.avif` files in file explorer listings.
- Open `.avif` files in the custom image viewer within `MediaPlayerScreen.kt` when clicked.
- Disable hardware bitmap acceleration (`allowHardware(false)`) specifically when loading `.avif` files to prevent native decoder crashes.
- Explicitly map `.avif` files to the `"image/avif"` MIME type in `getMimeType`.
- Ensure AVIF files are recognized by `getSiblingMedia` to allow swipe/tap navigation through folders containing both AVIF and other media.

---

## 🎨 UI/UX Changes
- Display the standard image icon and color for `.avif` files in the explorer view.
- Handle displaying of `.avif` images in `MediaPlayerScreen.kt` using Coil's `SubcomposeAsyncImage` with `allowHardware(false)`.

---

## ⚙️ Android-specific requirements
- Since Min SDK is 33, standard platform decoding via `ImageDecoder` handles AVIF natively. No extra NDK dependencies needed.

---

## 🧪 Edge Cases & Tests
- **Zero-byte or broken AVIF files**: Ensure they fall back to the error state UI gracefully in Coil.
- **Extreme resolutions**: Ensure downsampling is kept active.
- **Unit Tests**:
  - Update `FileHelperTest.kt` to check `isImage("photo.avif")` and `isImage("PICTURE.AVIF")` returns true.
  - Update `FileHelperTest.kt` to check `getMimeType` returns `"image/avif"` for `.avif` files.
  - Add a sample AVIF file to test resources and verify `isImage` detects it correctly.

---

## 🚀 Expected Code Changes
- Modify [FileHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/utils/FileHelper.kt)
  - Add `"avif"` extension to `isImage`.
  - Add hardcoded fallback for `"avif"` mapping to `"image/avif"` in `getMimeType`.
- Modify [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)
  - Dynamically disable hardware bitmaps (`allowHardware(false)`) inside Coil's `ImageRequest` when loading a file with an `.avif` extension.
- Modify [FileHelperTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/FileHelperTest.kt)
  - Add tests for `isImage` with AVIF.
  - Add tests for `getMimeType` with AVIF.
- Add test resource: `app/src/test/resources/testData/images/sample.avif` (small valid AVIF file).
