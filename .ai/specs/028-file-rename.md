# Specification: 028-file-rename

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: Where are rename entry points accessible in the app?
  - **A**:
    1. Single file/folder long-press dropdown context menu in File Explorer (`FileRowItem.kt`).
    2. Media player top 3-dots overflow menu (`MediaPlayerScreen.kt`).
    3. *(Note: Not available in multi-selection mode).*
- **Q**: How is text pre-selected when opening the Rename dialog?
  - **A**: The text field is populated with the full name, and the selection range is initialized to `TextRange(0, nameWithoutExtension.length)`. For directories or files without an extension, the entire name is pre-selected.
- **Q**: How does the dialog handle multiline and long filenames?
  - **A**: The text input uses a multiline field (`minLines = 1`, `maxLines = 4`) that expands smoothly for longer filenames without clipping or horizontal overflow.
- **Q**: How is an extension change handled?
  - **A**: If the file extension is changed or removed on a regular file:
    - An inline warning banner is displayed within the dialog (*"Changing the file extension from .old to .new may prevent this file from opening properly"*).
    - An **"Undo Extension"** button is provided to quickly revert the extension back to the original.
    - The action button displays *"Rename Anyway"* or *"Rename (Keep Extension)"* to ensure explicit intent.
- **Q**: How are file collisions and invalid names prevented?
  - **A**: 
    - Empty name or unchanged name: Rename button is disabled.
    - Name containing invalid characters (`/` or `\0`): Inline error *"Filename contains invalid characters"*, button disabled.
    - File with same name already exists in the same folder: Inline error *"A file with this name already exists"*, button disabled.
- **Q**: How is Rename logged in Past Actions?
  - **A**: Logged via `AuditLogDatabaseHelper` with `actionType = AuditActionType.RENAME`, recording `sourcePath` (old path), `destinationPath` (new path), and outcome `AuditItemOutcome.RENAMED`.
- **Q**: What happens when renaming from inside the Media Player?
  - **A**: The active file state is updated to the newly renamed file, sibling media list updates, and `MainScreen` is notified to refresh its directory view.

---

## 🛠️ Feature Requirements

### 1. Data Layer & Audit Models
- Update [AuditLogModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt):
  - Add `AuditActionType.RENAME`.
  - Add `AuditItemOutcome.RENAMED`.
- Update [ActionDetailDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/ActionDetailDialog.kt) & [PastActionsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/PastActionsScreen.kt):
  - Add icon (`Icons.Default.DriveFileRenameOutline` / `Icons.Default.Edit`), tint, and badges for `RENAME` / `RENAMED`.

### 2. Rename Dialog Component
- Create [RenameDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/RenameDialog.kt):
  - Header: "Rename File" / "Rename Folder" with icon and close button.
  - Multiline `OutlinedTextField` with `TextFieldValue` pre-selecting the name without extension.
  - Inline error text for invalid characters (`/`), empty names, and duplicate collisions.
  - Inline warning card when extension changes with "Revert Extension" button.
  - Bottom action buttons: "Cancel" and "Rename" (or "Rename Anyway").

### 3. UI Entry Points & Screen Integration
- **File Row Context Menu** ([FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt)):
  - Add **"Rename"** menu item (`Icons.Default.DriveFileRenameOutline`).
- **Media Player** ([MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)):
  - Add **"Rename"** option in top 3-dots overflow menu.
  - On confirm rename: renames file on disk via `file.renameTo(target)`, updates active media file, recalculates siblings, logs audit entry, and refreshes directory.
- **Main Screen** ([MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)):
  - State management for `RenameDialog` on single file long-press context menu.
  - Performs rename on `Dispatchers.IO`, records audit log, and triggers directory list refresh (`refreshTrigger++`).

---

## 🧪 Edge Cases & Tests
- **Pre-Selection Range**: Verify that regular files pre-select only the base name (e.g. `image` in `image.png`), while folders or extension-less files pre-select the entire name.
- **Extension Modification**: Changing or removing extension displays the warning banner; clicking "Revert Extension" restores the original extension.
- **Collision Protection**: Typing an existing filename in the same directory shows a duplicate error and disables the confirm button.
- **Invalid Characters**: Typing slashes (`/`) or blanks shows validation error.
- **Media Player In-Place Rename**: Renaming in media player keeps viewer active with updated name and valid sibling navigation.
- **Audit Log Verification**: Rename creates an entry with accurate source and destination paths in Past Actions.

---

## 🚀 Expected Code Changes
- Modify [app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt)
- Create [app/src/main/java/com/anshuman/tagstash/ui/components/RenameDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/RenameDialog.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/components/ActionDetailDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/ActionDetailDialog.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/PastActionsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/PastActionsScreen.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
- Unit & UI Tests: [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
