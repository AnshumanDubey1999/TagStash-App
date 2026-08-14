# TagStash 🏷️

TagStash is a modern, privacy-focused Android application designed for local file exploration, rich media viewing, and organization. Built entirely with Jetpack Compose and Material Design 3, TagStash offers an intuitive dark-themed user interface, seamless media playback with pinch-to-zoom, in-app batch clipboard operations, and permanent audit logging of file modifications.

---

## 📥 Download & Installation

Get the latest APK directly from the GitHub Releases:

* **[Download Latest Release (APK)](https://github.com/AnshumanDubey1999/TagStash-App/releases/latest)**
* **[All Releases](https://github.com/AnshumanDubey1999/TagStash-App/releases)**

---

## ✨ Features

### 📁 Local File Explorer
* **Intuitive Navigation**: Dynamic breadcrumb bar with direct jump-to-directory support and back stack handling.
* **Pull-to-Refresh**: Easily rescan disk directories to sync file additions and removals.
* **Multi-Line Text Flow**: Full, non-truncated file names and directory paths with clean multi-line wrapping.
* **Permission Management**: Granular runtime permission handling for Android 13+ (Images, Video, Audio) and Scoped Storage.

### 🖼️ Immersive Media Viewer & Player
* **Comprehensive Image Support**: High-performance image loading powered by **Coil**, supporting JPEG, PNG, WebP, animated GIF, and standard/high-resolution 10-bit HDR **AVIF** formats.
* **Full-Screen Video Playback**: Built with **Jetpack Media3 (ExoPlayer)** with gesture controls (double tap to seek, single tap overlay toggles, progress scrubbing, volume toggle, and global loop toggle).
* **Zoom & Pan Engine**: Smooth pinch-to-zoom (up to 5x), boundary clamping, and double-tap reset for both images and videos.
* **Continuous Playback**: Screen stays awake during video playback, with seamless sibling media preloading and edge-swipe transitions.

### 📋 Selection Mode & In-App Clipboard
* **Multi-Item Selection**: Activate multi-selection via file long-press or 3-dot overflow menu. Floating bottom bar displays selection count and instant Cut/Copy actions.
* **In-App Clipboard (Up to 1,000 items)**: Queue both Cut and Copy operations across multiple directories and media screens with atomic capacity protection.
* **Batch Paste with Conflict Resolution**: Perform batch paste operations into any folder. When conflicts arise, choose between **Replace**, **Keep Both** (with sequential automatic renaming `(copy 1)`), or **Skip**, with an option to apply the decision to all conflicts in the batch.
* **Floating Paste Button (FAB)**: Quick-access circular FAB with notification badge indicating current queued clipboard items.

### 🔍 Rich File Properties & Metadata
* **Single & Multi-File Properties**: Inspect detailed file sizes, absolute paths, child counts, and modification dates. Multi-item inspection provides aggregated sizes and file type distribution breakdowns.
* **EXIF Metadata**: Deep image EXIF metadata extraction (resolution, camera make/model, aperture, focal length, ISO, exposure, and capture timestamps).

### ⚙️ Settings & Permanent Audit Log
* **Permanent SQLite Storage**: All file-modifying operations (such as batch paste) are persistently recorded in an internal SQLite database (`tagstash_audit_logs.db`).
* **Past Actions History**: Chronological log of past operations with item counts, destination paths, and outcome breakdowns (`MOVED`, `COPIED`, `REPLACED`, `RENAMED`, `SKIPPED`).
* **Detailed Action Breakdown**: Inspect full source $\rightarrow$ destination addresses, commands (`CUT`/`COPY`), and statuses for every individual file in a past operation.
* **Log Management**: Delete individual past action logs or clear the entire history with safe confirmation dialogs.

---

## 🛠️ Technology Stack

* **Platform & Language**: Android (Min SDK 33, Target SDK 36), Kotlin
* **UI Framework**: Jetpack Compose with Material Design 3 tokens
* **Image Loading**: Coil (with GifDecoder and AvifCoder)
* **Media Engine**: Jetpack Media3 (ExoPlayer & PlayerView)
* **Database & Persistence**: SQLite via Android SQLiteOpenHelper
* **Concurrency**: Kotlin Coroutines & Flow (`Dispatchers.IO`)
* **Testing & Verification**: JUnit 4, Robolectric, Roborazzi Screenshot Testing

---

## 🗺️ Future Priorities & Roadmap

* **Comprehensive File Operations**: Adding native file creation, renaming, and file deletion (with a dedicated Trash / Recycle Bin).
* **Archive Support**: Viewing, exploring, and extracting compressed archives (ZIP, TAR, 7Z, RAR).
* **Tag-based Organization**: Custom tag creation, color coding, and reactive multi-tag filtering across directories powered by Room Database.
* **Seamless Audio Playback in Media Player**: Extending the built-in media player to support audio files just as seamlessly as image and video files, complete with playback controls and metadata displays.
* **Personalized Settings**: Custom themes, customizable default sort orders, and adaptive grid/list view switchers.
* **Multi-Platform Distribution**: Packaging and publishing TagStash across different app distribution platforms and stores.
