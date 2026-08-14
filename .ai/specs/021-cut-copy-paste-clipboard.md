# Specification: 021-cut-copy-paste-clipboard

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: What are the capabilities and scope of this specification?
  - **A**:
    - Adds **Cut** and **Copy** actions to the Media Player top 3-dot overflow menu.
    - Adds an in-app internal **Clipboard** manager storing up to 100 items (Cut or Copy operations).
    - Adds **"Clipboard"** viewer option to the 3-dot menu in Media Player (when clipboard is not empty) and in File Explorer.
    - Adds **"Paste"** and **"Clipboard"** options to the File Explorer (`MainScreen` / `BreadcrumbsBar`) top 3-dot menu when clipboard has items.
    - Out of scope for this spec: Cut/Copy/Paste from File Viewer rows or Selection Mode, and Undo functionality (these will be added in subsequent specifications).
- **Q**: How does Cut vs Copy work?
  - **A**:
    - **Cut**: The file stays in place until it is pasted; upon pasting, it is moved to the target directory.
    - **Copy**: The file is duplicated into the target directory upon pasting.
- **Q**: What happens to clipboard items after a Paste operation?
  - **A**: All items in the clipboard (both Cut and Copy) are cleared from the clipboard after a paste operation completes.
- **Q**: How are file naming conflicts handled during Paste?
  - **A**:
    - If a destination file already exists with the same name, a Conflict Resolution Dialog prompts the user with 3 options:
      1. **Replace**: Overwrites the existing file in the target directory.
      2. **Keep Both**: Renames the incoming file by appending ` (copy)` (or ` (copy 1)`, ` (copy 2)`, etc. before the extension).
      3. **Skip**: Skips pasting the conflicting file.
    - An **"Apply to all conflicts"** checkbox applies the chosen action to all subsequent conflicts in the current paste batch.
- **Q**: What is the maximum size and behavior of the clipboard?
  - **A**: Max capacity of 1000 items. If the same file is added again, its action (Cut/Copy) is updated in place. If the clipboard is at max capacity (1000) and the user attempts to Cut or Copy another file, an error dialog pops up explaining that only up to 1000 items can be in the clipboard at a time. No automatic eviction happens.
- **Q**: What does the Clipboard Viewer look like?
  - **A**: A clean Material 3 modal dialog showing the list of queued items with filename, type icon, and a CUT / COPY badge, along with a "Clear All" button and a "Close" button.

---

## 🛠️ Feature Requirements
- Create `ClipboardManager` (in-memory app clipboard state) to store `ClipboardItem(file: File, opType: ClipboardOpType)` where `ClipboardOpType` is `CUT` or `COPY`.
- Add **Cut**, **Copy**, and **Clipboard** (when not empty) menu items in [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt).
- Add **Paste** and **Clipboard** menu items in [BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt) / [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt) when clipboard has items.
- Implement file copying, moving (cut), and conflict resolution helpers in `FileOperationHelper.kt`.
- Create `ConflictResolutionDialog.kt` for interactive conflict handling (Replace, Keep Both, Skip, Apply to all).
- Create `ClipboardDialog.kt` to view queued clipboard items with a Clear All action.
- Automatically refresh the directory listing in `MainScreen` after a paste operation completes.

---

## 🎨 UI/UX Changes
- **Media Player 3-Dot Menu**:
  - `Cut` with `Icons.Default.ContentCut`.
  - `Copy` with `Icons.Default.ContentCopy`.
  - `Clipboard (<count>)` with `Icons.Default.ContentPasteGo` (visible when clipboard has items).
- **File Explorer 3-Dot Menu**:
  - `Paste (<count>)` with `Icons.Default.ContentPaste` (visible when clipboard has items).
  - `Clipboard (<count>)` with `Icons.Default.ContentPasteGo` (visible when clipboard has items).
- **Conflict Resolution Dialog**:
  - Clear file name and preview of conflicting file.
  - Radio/Action buttons for Replace, Keep Both, and Skip.
  - Checkbox for "Apply to all conflicts".
- **Clipboard Dialog**:
  - Scrollable list of items with colored badges: `CUT` (Amber) / `COPY` (Blue/Teal).
  - Action buttons: "Clear All" and "Close".

---

## ⚙️ Android-specific Requirements
- Disk operations offloaded to `Dispatchers.IO` with proper error handling and file streams.
- Preserves file modification timestamps where possible.

---

## 🧪 Edge Cases & Tests
- **Paste into same directory**:
  - For Copy: Creates `(copy)` duplicate automatically or prompts conflict.
  - For Cut: Skips or alerts that source and destination are identical.
- **Multiple conflicts**: "Apply to all conflicts" applies chosen action across all subsequent conflicting items without re-prompting.
- **Keep Both sequence**: Correctly generates `filename (copy).ext`, `filename (copy 1).ext`, `filename (copy 2).ext`.
- **Max capacity**: Clipboard caps at 1000 items. Attempting to add beyond 1000 displays an error dialog and prevents adding new items.
- **Unit & UI Tests**:
  - Unit tests for `ClipboardManager`, copy file, move file, conflict name generator, and batch paste resolution.
  - UI tests for Cut/Copy from media player, Paste into folder, and conflict dialog.

---

## 🚀 Expected Code Changes
- Create [ClipboardManager.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/clipboard/ClipboardManager.kt)
  - Data model and state manager for in-app clipboard items.
- Create [FileOperationHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/utils/FileOperationHelper.kt)
  - File copying, moving, name generator, and conflict resolution logic.
- Create [ConflictResolutionDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/ConflictResolutionDialog.kt)
  - Interactive dialog for handling file overwrite/keep both/skip conflicts.
- Create [ClipboardDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/ClipboardDialog.kt)
  - Modal list viewer for current clipboard contents.
- Modify [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)
  - Add Cut, Copy, and Clipboard menu options and connect to `ClipboardManager`.
- Modify [BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt) & [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
  - Add Paste and Clipboard options, handle paste batch execution with conflict dialogs, and refresh directory listing.
- Create / Modify Tests:
  - [ClipboardManagerTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ClipboardManagerTest.kt)
  - [FileOperationHelperTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/FileOperationHelperTest.kt)
  - [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
