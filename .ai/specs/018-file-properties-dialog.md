# Specification: 018-file-properties-dialog

**Status**: IN_PROGRESS

---

## 📋 Q&A & Assumptions
- **Q**: How will a user open the file properties dialog in File Explorer?
  - **A**: Long-pressing any file or folder item displays a context menu containing an "Info" / "Properties" item. Tapping "Info" opens the properties popup dialog.
- **Q**: What information should the properties popup display?
  - **A**: 
    - **General**: File Name, Location/Path, Size (formatted & raw bytes), Date Modified, MIME Type, Extension.
    - **Media Specific**:
      - Images: Resolution (WxH), Aspect Ratio.
      - Videos: Duration, Resolution, Video Codec, Video Bitrate, Audio Codec, Audio Configuration.
      - Audio: Duration, Audio Codec, Bitrate, Sample Rate & Channels, ID3 Track Info.
    - **Additional / Extended Metadata**:
      - Image EXIF: Camera Make/Model, ISO, Aperture, Shutter Speed, Focal Length, Date Taken, GPS Location.
      - Audio/Video Extended: Genre, Year, Composer, Copyright, Software/Encoder.
      - TagStash Tags: Custom tags assigned in Room DB.
- **Q**: How will the dialog be closed?
  - **A**: Top-right `✕` cross button and a bottom "Close" button.
- **Assumptions**:
  - Metadata extraction occurs asynchronously on `Dispatchers.IO` using `MediaMetadataRetriever`, `ExifInterface`, and `BitmapFactory`.
  - Empty or missing metadata fields are hidden gracefully without showing null/blank rows.

---

## 🛠️ Feature Requirements
- Support long-press context menu on file/folder items in `FileExplorerScreen.kt`.
- Build `FileMetadataHelper.kt` to extract detailed properties and EXIF/extended metadata asynchronously.
- Build `FilePropertiesDialog.kt` composable featuring:
  - Material 3 Dark Slate theme (`#121212` / `#1E1E1E`).
  - Header with File Type Icon, Title, and top-right `✕` cross button.
  - Organized scrollable sections: General, Media Details, Codec Info, Additional EXIF/Extended Metadata, TagStash Tags.
  - Formatted Key-Value rows with clean dividers.

---

## 🎨 UI/UX Changes
- Long-pressing a file item brings up a context menu with "Info" (with info icon).
- Tapping "Info" opens the modal dialog with clean typography and smooth entrance animation.

---

## ⚙️ Android-specific Requirements
- Uses standard `androidx.exifinterface:exifinterface` / `android.media.ExifInterface` and `android.media.MediaMetadataRetriever`.

---

## 🧪 Edge Cases & Tests
- **Files without metadata / broken files**: Show general properties gracefully without crashing.
- **Large directories / slow I/O**: Extract metadata asynchronously on `Dispatchers.IO` with loading spinner indicator in dialog.
- **Unit & Build Tests**:
  - Unit test `FileMetadataHelperTest.kt` verifying property extraction for images, audio, and video sample files.
  - Run `./gradlew compileDebugKotlin`, `./gradlew test`, and `./gradlew check`.

---

## 🚀 Expected Code Changes
- Create [FileMetadataHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/utils/FileMetadataHelper.kt)
  - Helper functions to extract standard, media, codec, EXIF, and extended metadata.
- Create [FilePropertiesDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FilePropertiesDialog.kt)
  - Composables for the properties dialog, sections, key-value rows, and close buttons.
- Modify [FileExplorerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/FileExplorerScreen.kt)
  - Add long-press gesture handling and state for properties dialog.
- Create [FileMetadataHelperTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/FileMetadataHelperTest.kt)
  - Unit tests for metadata extraction logic.
