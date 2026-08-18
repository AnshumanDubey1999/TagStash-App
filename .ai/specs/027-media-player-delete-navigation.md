# Specification: 027-media-player-delete-navigation

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: What should happen when a media file is deleted from inside the Media Player viewer?
  - **A**: 
    - If a **next** media file exists in the directory, the Media Player automatically advances to that next file.
    - If there is **no next file** (the deleted file was the last media item in the list), it moves to the **previous** media file.
    - If there are **no other media files** left in the directory (it was the only media file), the Media Player automatically exits back to the File Explorer view.
- **Q**: How is state updated so the deleted file is not seen again?
  - **A**: 
    - The file is moved to the sandboxed Recycle Bin directory (`context.filesDir/recycle_bin/`).
    - The directory's media sibling list is re-queried or updated.
    - File Explorer in `MainScreen` is notified to refresh its list from disk so returning to the file view never shows the deleted item.

---

## 🚀 Feature Requirements

### 1. Sibling Media Resolution & Post-Delete Target
- Before or during deletion execution in `MediaPlayerScreen`:
  - Calculate `currentSiblings = getSiblingMedia(file)`.
  - Find `currentIndex = currentSiblings.indexOfFirst { it.absolutePath == file.absolutePath }`.
  - Compute `remaining = currentSiblings.filter { it.absolutePath != file.absolutePath }`.
- Navigation decision:
  - If `remaining.isEmpty()`: Call `onClose()` (closes media player and returns to file explorer).
  - Else if `currentIndex in 0 until remaining.size`: Call `onNavigateToMedia(remaining[currentIndex])` (moves to next file).
  - Else (`currentIndex >= remaining.size`): Call `onNavigateToMedia(remaining.last())` (moves to previous file).

### 2. View & Player Reset
- When navigating to the new media file:
  - Reset zoom scale (`scale = 1.0f`) and pan offset (`offset = Offset.Zero`).
  - Reset video player (`currentPosition = 0L`, ExoPlayer loads new file URI).
  - Sibling navigation state (`siblingMedia`, `currentIndex`) recalculates based on remaining items in the folder.

### 3. File Explorer List Refresh
- Ensure `MainScreen` refreshes its disk file list whenever a file is deleted from `MediaPlayerScreen`, so exiting or swiping never exposes the deleted file.

---

## 📁 Expected Code Changes
1. `app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt`:
   - Update `onConfirmDelete` callback logic in `DeleteConfirmationDialog` to compute target file (next, previous, or close) and navigate accordingly.
2. `app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt`:
   - Trigger file list refresh (`refreshTrigger++`) on media deletion / navigation.
3. `app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt`:
   - Add unit/UI tests verifying delete navigation behavior (delete first, delete middle, delete last, delete only file).

---

## 🧪 Testing Strategy
- **Unit & Robolectric Tests**:
  - Test deleting the first file in a folder containing multiple media files -> advances to the second file.
  - Test deleting the last file in a folder containing multiple media files -> navigates back to previous file.
  - Test deleting the single/only media file in a folder -> closes media viewer and displays file explorer.
  - Test that deleted file is not present in subsequent sibling lists or file explorer list.
