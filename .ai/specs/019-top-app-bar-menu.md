# Specification: 019-top-app-bar-menu

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: Where will the 3-dot overflow menu button be added?
  - **A**: 
    1. On the **File Explorer page** (`MainScreen.kt` / `BreadcrumbsBar.kt`) at the top right of the navigation header bar.
    2. On the **Media Player page** (`MediaPlayerScreen.kt`) in the top right overlay control bar.
- **Q**: What actions will the 3-dot dropdown menu contain?
  - **A**: Currently contains an **"Info"** option (with info icon).
- **Q**: What happens when "Info" is tapped in File Explorer vs Media Player?
  - **A**:
    1. In File Explorer: Opens `FilePropertiesDialog` for the currently viewed folder (`currentDirectory`).
    2. In Media Player: Opens `FilePropertiesDialog` for the active media file (`file`).
- **Assumptions**:
  - The dropdown menu uses standard Material 3 `DropdownMenu` and `DropdownMenuItem`.
  - Additional menu options will be added in future specifications without breaking existing layout.

---

## 🛠️ Feature Requirements
- Add 3-dot overflow menu button (`Icons.Default.MoreVert`) to `BreadcrumbsBar.kt` / top header bar in `MainScreen.kt`.
- Add 3-dot overflow menu button (`Icons.Default.MoreVert`) to `MediaPlayerScreen.kt` top control bar.
- Implement "Info" dropdown option in both screens.
- Open `FilePropertiesDialog` upon clicking "Info".

---

## 🎨 UI/UX Changes
- Clean top-right 3-dot icon button (`MoreVert`) in both File Explorer and Media Player.
- Smooth dropdown animation opening a dark slate Material 3 context menu.

---

## ⚙️ Android-specific Requirements
- Uses standard Jetpack Compose Material 3 `DropdownMenu`, `DropdownMenuItem`, and `Icons.Default.MoreVert`.

---

## 🧪 Edge Cases & Tests
- **Root Directory**: Opening Info for `/storage/emulated/0` or home directory displays folder properties cleanly.
- **Media Player Overlay**: Opening Info dialog in Media Player pauses or overlays cleanly over video/audio without control glitching.
- **Unit & Build Tests**:
  - Verify Gradle build (`./gradlew compileDebugKotlin`) succeeds.
  - Verify unit tests (`./gradlew test`) pass.
  - Verify quality check (`./gradlew check`) passes.

---

## 🚀 Expected Code Changes
- Modify [BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt)
  - Add 3-dot overflow menu button and "Info" action callback for current directory.
- Modify [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
  - Wire folder "Info" action to `FilePropertiesDialog`.
- Modify [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)
  - Add 3-dot overflow menu button to top bar overlay with "Info" action opening `FilePropertiesDialog`.
