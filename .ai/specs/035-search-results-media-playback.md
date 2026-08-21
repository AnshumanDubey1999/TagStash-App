# Specification: 035-search-results-media-playback

**Status**: IN_PROGRESS

---

## 📌 Background & Context
Following Specification 033 (Folder Search & Filter), search results currently render as a static list of matching files and directories. This specification adds interactive capabilities to the search results screen, enabling media items (images and videos) to be opened in the full-screen `MediaPlayerScreen` with playlist navigation across all search matches, while supporting in-player file operations (Cut, Copy, Rename, Delete, File Properties) with synchronized state updates.

---

## 🎯 Requirements & Scope

### 1. Search Results Click Behaviors
- **Media Files (Images & Videos)**:
  - Clicking any image or video result opens `MediaPlayerScreen`.
  - The media player is initialized with the clicked file and an ordered `playlist` containing all media files present in the active `searchResults` list.
- **Directories / Folders**:
  - Clicking a directory item in search results dismisses the search mode and navigates into that directory in the main file explorer.
- **Other File Types (Audio, Documents, Generic Files)**:
  - Clicking opens the file with the default external system handler using `openFileWithOS(context, file)`.

### 2. Media Player Playlist Navigation in Search Mode
- **Custom Playlist Parameter in `MediaPlayerScreen`**:
  - Add an optional `playlist: List<File>? = null` parameter to `MediaPlayerScreen`.
  - When `playlist` is provided (e.g. from search results), the media player uses this ordered list for navigation instead of scanning directory siblings (`getSiblingMedia(file)`).
  - When `playlist` is `null`, default to `getSiblingMedia(file)` for standard directory browsing.
- **Previous & Next Navigation**:
  - Top bar arrows (`chevron_left` and `chevron_right`) navigate back and forth across the search media matches.
  - Previous / Next buttons disable gracefully when at the first or last item of the playlist.
- **Return / Dismissal**:
  - Closing the media player (via `X` button or Back gesture) returns directly to the search results view, preserving the query, include-subfolders toggle state, and scanned results.

### 3. Media Player Internal Actions in Search Mode
- **Cut & Copy**:
  - Adds the active media file to `AppClipboard`.
  - Shows capacity limit dialog if clipboard maximum is reached.
- **File Properties**:
  - Opens `FilePropertiesDialog` populated with metadata for the active media file.
- **Rename**:
  - Launches `RenameDialog`.
  - Upon successful rename:
    - Renames file on disk and logs an `AuditLogEntry` (`AuditActionType.RENAME`).
    - Updates the file in `searchResults` list and the active `playlist`.
    - Updates active player file to the new renamed `File` without exiting playback.
- **Delete (Move to Recycle Bin)**:
  - Launches `DeleteConfirmationDialog`.
  - Upon confirmed deletion:
    - Moves file to Recycle Bin and logs an `AuditLogEntry` (`AuditActionType.DELETE`).
    - Removes the deleted file from `searchResults` list and the active `playlist`.
    - If other media items remain in the playlist, navigates to the next item (or previous item if the last was deleted).
    - If no media items remain in the search results, closes the media player and returns to the search results screen.

---

## 🎨 UI & UX Design
- **Interactive Feedback**:
  - Make `SearchResultItem` clickable with Material ripple effect (`Modifier.clickable { ... }`).
- **Media Player Overlay**:
  - Displays top bar, bottom controls (for video), and zoom gestures (for images) identical to normal media viewer mode.
  - Smooth transitions between search result items during Next/Previous actions.

---

## 🗄️ Database & Backend Changes
- No new database schema tables required.
- Reuses `RecycleBinDatabaseHelper` and `AuditLogDatabaseHelper` for delete and rename operations.

---

## 📱 Android-Specific Requirements
- No new runtime permissions or manifest modifications required.
- Uses existing file provider and scoped storage permissions.

---

## 🧪 Edge Cases & Tests
1. **Search Results with Single Media Item**:
   - Previous and Next buttons are disabled in `MediaPlayerTopBar`.
2. **Deleting All Media Items in Search**:
   - Deleting the only/last media item closes the media player and returns to the search results view showing remaining non-media files or empty search state.
3. **Deleting Item in the Middle of Playlist**:
   - Player seamlessly advances to the next media item in the search list.
4. **Deleting the Last Item in Playlist**:
   - Player steps backward to the previous media item in the search list.
5. **Renaming Media File**:
   - Media player title, properties, and playback update to reflect the new file name immediately without crashing or reloading full search.
6. **Opening Non-Media File in Search**:
   - External chooser or OS handler launched via Intent without affecting search state.
7. **Automated Testing**:
   - Unit tests for search media playlist extraction and navigation index calculations.
   - Compose UI tests verifying search result clicks, media player playback, navigation, and delete/rename actions in search results.

---

## 📂 Expected Code Changes
- [`app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt):
  - Support `playlist: List<File>? = null` parameter.
  - Support callback notifications for in-player delete and rename to sync with parent search state.
- [`app/src/main/java/com/anshuman/tagstash/ui/components/SearchResultsView.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/SearchResultsView.kt):
  - Pass click callbacks (`onItemClick`, `onOpenMediaFile`, `onNavigateDirectory`).
- [`app/src/main/java/com/anshuman/tagstash/ui/components/SearchResultItem.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/SearchResultItem.kt):
  - Add clickable modifier and click dispatching.
- [`app/src/main/java/com/anshuman/tagstash/ui/components/MainScreenContent.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/MainScreenContent.kt):
  - Connect search results item clicks to media player, folder navigation, and OS file opening.
- [`app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt):
  - Maintain active search media playlist and handle search item rename/delete synchronization.
- [`app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt):
  - Add automated Compose UI tests for search result media playback, navigation, and actions.

---

## 📋 Q&A & Assumptions Preserved
- **Q**: What happens when clicking on a folder in search results?
  - **A**: Navigates to that folder in main file explorer, exiting search mode.
- **Q**: What happens when clicking on a generic non-media file?
  - **A**: Opens with default OS app via `openFileWithOS()`.
- **Q**: How does media player navigate among search matches?
  - **A**: Media player uses an explicit playlist containing all images and videos from the search results in the same order.
- **Q**: What happens when deleting or renaming an item from within the media player while in search mode?
  - **A**: The file is deleted/renamed on disk and in database, the search results list and playlist are updated, and player advances/updates accordingly.
