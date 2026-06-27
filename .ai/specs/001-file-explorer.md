# Spec 001: File Explorer

- **Status**: IN_PROGRESS
- **Date**: 2026-06-27

---

## 📋 Feature Requirements
* Request `MANAGE_EXTERNAL_STORAGE` permission at app startup if not already granted.
* Redirect user to System settings to enable "All Files Access" if permission is missing.
* Open the primary shared storage directory (`/storage/emulated/0`) by default once permission is granted.
* Read and list all files and subdirectories in the current path.
* Allow clicking on a folder to navigate into it.
* Allow clicking on a file to launch it via the Android OS using an appropriate Intent and a secure `FileProvider`.
* Provide a clickable breadcrumb navigation mechanism at the top to navigate to parent directories.

## 🎨 UI/UX Changes
* Modern dark-themed layout matching the design guidelines.
* **Top App Bar**: Interactive breadcrumb trail:
  * Clickable breadcrumbs for quick navigation to parent folders.
  * Truncation rule: Each folder name in the breadcrumb trail has a maximum length of 10 characters. If it exceeds 10 characters, it will be truncated to 10 characters total (the first 7 characters followed by `...`).
  * Wrapping rule: If the breadcrumb trail exceeds the width of the screen, wrap it to a new line so that all parts are visible (using a flow layout, e.g., `FlowRow`).
* **Main Area**: A scrollable list of files/folders.
* **File/Folder Items**: Standard Material Icons for file types (Folder icon for folders, Generic file icon for others) along with file/folder names, size, and modification date.
* **Permission Request Screen**: Informative empty/onboarding state explaining why the permission is needed with a "Grant Permission" button.

## 🛠️ Tech Stack & Android Specifics
* **Language**: Kotlin
* **UI**: Jetpack Compose (using Material 3 components)
* **Manifest Additions**:
  * `<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />`
  * Configuration of a `<provider>` for `androidx.core.content.FileProvider` to safely share file URIs.
* **FileProvider Config**: `res/xml/file_paths.xml` defining `<external-path>` for accessing files under `/storage/emulated/0`.

## ⚠️ Edge Cases & Failure Modes
* **Permission Denied/Revoked**: App must show the permission request screen instead of crashing.
* **Empty Directories**: Show a clear "This folder is empty" illustration or text.
* **Unreadable/Restricted Directories**: Some system folders under Android 11+ might be restricted even with All Files Access (e.g., `Android/data` or `Android/obb`). Handle `SecurityException` or list failures gracefully by showing an error message (e.g., "Access Denied").
* **MIME Type Resolution**: Handle files with unknown extensions gracefully by prompting the OS with a generic `*/*` MIME type.
* **FileProvider Sharing Limits**: Ensure the file URI sharing uses `FileProvider.getUriForFile` and sets `Intent.FLAG_GRANT_READ_URI_PERMISSION`.

## 🧪 Testing Strategy
* **Unit Tests**:
  * Unit test for path navigation logic (e.g., moving up a level, formatting file sizes).
* **UI Tests**:
  * Mock directory listing flow to test directory navigation UI.

## 📝 Expected Code Changes
* [build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts) & [libs.versions.toml](file:///Users/personal/AndroidStudioProjects/TagStash/gradle/libs.versions.toml): Add Compose dependencies, Activity Compose, and navigation if needed.
* [AndroidManifest.xml](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/AndroidManifest.xml): Declare `MANAGE_EXTERNAL_STORAGE` and `FileProvider`.
* [file_paths.xml](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/res/xml/file_paths.xml): Configure shared external paths.
* [MainActivity.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/MainActivity.kt): Set up the main application UI, permissions, and directory navigation state.

---

## ❓ Open Questions & Assumptions (Resolved)
1. **Compose Setup**: Approved. We will add Jetpack Compose dependencies to the build system.
2. **Top Navigation**: Approved clickable breadcrumbs with name truncation (max 10 chars, showing first 7 + `...`) and text-wrapping on multiple lines when width limits are exceeded.
3. **MIME Types**: OS default MIME type matching is sufficient via `FileProvider`.
