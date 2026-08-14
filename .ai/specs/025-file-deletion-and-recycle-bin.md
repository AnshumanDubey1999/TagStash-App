# Specification: 025-file-deletion-and-recycle-bin

**Status**: IN_PROGRESS

---

## 📋 Q&A & Assumptions
- **Q**: Where are delete actions accessible in the app?
  - **A**: 
    1. Single file long-press context dropdown menu in File Explorer (`FileRowItem.kt`).
    2. Selection mode bottom hovering bar (Delete icon button) and top 3-dots dropdown menu (`BreadcrumbsBar.kt`).
    3. Media viewer top 3-dots dropdown menu (`MediaPlayerScreen.kt`).
- **Q**: What does the Delete Confirmation Dialog look like?
  - **A**:
    1. Displays a confirmation checkbox at the top: *"Click here if you are sure you want to delete this item"* (or *"these N items"* for multi-selection).
    2. Displays a summary header when multiple items are selected showing the total count and aggregated size (e.g. *"3 items • Total: 14.8 MB"*).
    3. Displays the file or scrollable list of files with icons, names, and sizes (no truncation, styled key-value format).
    4. Bottom action buttons: "Cancel" (text button) and "Delete" (red button).
    5. The "Delete" button is **disabled** by default and only becomes enabled once the confirmation checkbox is checked.
- **Q**: Where are deleted files moved when trashed?
  - **A**: Moved to the app's private internal storage directory (`context.filesDir/recycle_bin/`). Each item is stored with a unique ID and recorded in a persistent SQLite table (`recycle_bin`) tracking original path, trashed path, deletion timestamp, and 1-hour expiry timestamp (`System.currentTimeMillis() + 3600_000L`).
- **Q**: How is this deletion logged in Past Actions?
  - **A**: Each deletion operation (single or batch) generates an entry in `AuditLogDatabaseHelper` with `actionType = AuditActionType.DELETE` / `TRASH`, listing each deleted item's original path, destination in recycle bin, size, and outcome `TRASHED`.
- **Q**: How is the feature split across specifications?
  - **A**: 
    - **Spec 025 (this spec)**: Delete entry points, Delete Confirmation Dialog (with required confirmation checkbox and disabled/enabled button), file move to internal Recycle Bin storage, Recycle Bin database helper & data models, and deletion audit logging.
    - **Spec 026 (next spec)**: Settings Recycle Bin screen UI, individual & batch restore, permanent deletion, 1-hour auto-expiry cleanup worker/check, and restore/purge audit logging.

---

## 🛠️ Feature Requirements

### 1. Data Layer & Models
- Update [AuditLogModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt):
  - Add `AuditActionType.DELETE` (or `TRASH`).
  - Add `AuditItemOutcome.TRASHED`.
- Create [RecycleBinModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/RecycleBinModels.kt):
  - Data class `RecycleBinItem`: `id: Long`, `originalPath: String`, `trashedPath: String`, `fileName: String`, `fileSize: Long`, `isDirectory: Boolean`, `deletedTimestamp: Long`, `expiryTimestamp: Long`.
- Create [RecycleBinDatabaseHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt):
  - SQLite table `recycle_bin` (`id`, `original_path`, `trashed_path`, `file_name`, `file_size`, `is_directory`, `deleted_timestamp`, `expiry_timestamp`).
  - Async methods: `moveToRecycleBin(files: List<File>, context: Context): List<RecycleBinItem>`, `getAllItems(): List<RecycleBinItem>`, `deleteItem(id: Long)`, `clearAll()`.

### 2. Delete Confirmation Dialog
- Create [DeleteConfirmationDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/DeleteConfirmationDialog.kt):
  - Checkbox at the top with text: *"Click here if you are sure you want to delete this item"* (or *"these N items"* for multi-selection).
  - Summary header: For multi-selection, displays item count and total size (e.g., *"3 items • Total: 14.8 MB"*).
  - Scrollable list of items to be deleted (file icon, file name without truncation, formatted size, original location).
  - Info note: *"Deleted items will be moved to the Recycle Bin for 1 hour before being permanently deleted."*
  - Bottom buttons: "Cancel" and red "Delete" button (enabled **only** when checkbox is checked).

### 3. UI Entry Points & Navigation
- **File Row Context Menu** ([FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt)):
  - Add **"Delete"** menu item (`Icons.Default.DeleteOutline`, color error red).
- **Selection Mode** ([BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt)):
  - Add **Delete icon button** on the selection mode bottom floating bar.
  - Add **"Delete"** option in selection mode top 3-dots dropdown menu.
- **Media Player** ([MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)):
  - Add **"Delete"** option in top 3-dots dropdown menu.
- **Main Screen Integration** ([MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)):
  - State management for triggering `DeleteConfirmationDialog`.
  - Performing file movement to Recycle Bin via `Dispatchers.IO`, refreshing directory contents, recording single `AuditLogEntry`, and displaying toast/snackbar confirmation.

---

## 🧪 Edge Cases & Tests
- **Directory Refresh & State**: Ensure file explorer directory list refreshes immediately upon deletion.
- **Selection Mode Exit**: Exiting selection mode automatically after deleting selected items.
- **Non-existent or Locked Files**: Handling failure gracefully if a file was moved or deleted externally before confirmation.
- **Checkbox Toggle State**: Verifying Delete button remains disabled until checkbox is explicitly toggled.
- **Audit Log Verification**: Verifying that trashing an item generates a corresponding `DELETE` past action entry with accurate source and trashed addresses.

---

## 🚀 Expected Code Changes
- Modify [app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt)
- Create [app/src/main/java/com/anshuman/tagstash/data/model/RecycleBinModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/RecycleBinModels.kt)
- Create [app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt)
- Create [app/src/main/java/com/anshuman/tagstash/ui/components/DeleteConfirmationDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/DeleteConfirmationDialog.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
- Unit & UI Tests: [RecycleBinDatabaseHelperTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/RecycleBinDatabaseHelperTest.kt) and [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
