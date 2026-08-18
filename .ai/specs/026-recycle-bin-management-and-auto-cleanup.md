# Specification: 026-recycle-bin-management-and-auto-cleanup

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: Where is the Recycle Bin screen located?
  - **A**: Under Settings (`SettingsScreen.kt`) as a dedicated "Recycle Bin" list option, displaying the current count of trashed items.
- **Q**: What actions can be performed from the Recycle Bin screen?
  - **A**:
    1. **Restore Item**: Moves the file back to its original directory. If the original directory no longer exists, its directory hierarchy is automatically recreated. If a file with the same name exists, it is renamed with a `(restored)` suffix.
    2. **Delete Permanently**: Immediately purges the file from internal storage and removes the database record (with confirmation dialog).
    3. **Restore All**: Restores every item currently in the Recycle Bin.
    4. **Empty Recycle Bin**: Permanently deletes all items in the Recycle Bin (with confirmation dialog).
- **Q**: How does the 1-hour auto-deletion mechanism work?
  - **A**: 
    1. Each item placed in the Recycle Bin has an `expiryTimestamp` set to `deletedTimestamp + 3600_000L` (1 hour).
    2. A cleanup routine (`cleanupExpiredItems`) checks for items where `System.currentTimeMillis() >= expiryTimestamp`, deletes the physical files from `context.filesDir/recycle_bin/`, and removes their database records.
    3. The cleanup check runs automatically on app start and whenever the Settings, Recycle Bin, or Past Actions screens are opened.
    4. Auto-deleted items are recorded in the Past Actions audit log with `actionType = AUTO_DELETE` and outcome `EXPIRED`.

---

## 🛠️ Feature Requirements

### 1. Settings & Navigation
- Update [SettingsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/SettingsScreen.kt):
  - Add **"Recycle Bin"** row displaying trash icon and active items count.
  - Route navigation between `SETTINGS` $\leftrightarrow$ `RECYCLE_BIN`.
- Update [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt):
  - Add `RECYCLE_BIN` screen state and BackHandler support.
  - Run `cleanupExpiredItems` on initial composition.

### 2. Recycle Bin Screen
- Create [RecycleBinScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/RecycleBinScreen.kt):
  - Top app bar with back navigation, screen title, and 3-dots overflow menu ("Restore All", "Empty Recycle Bin").
  - List of trashed items displaying:
    - File icon, full file name without truncation.
    - Original file path and file size.
    - Expiration countdown chip (e.g. "Expires in 45m" or "Expires in <1m").
    - Action buttons: "Restore" (`Icons.Default.Restore`) and "Delete Permanently" (`Icons.Default.DeleteForever`).
  - Empty state displaying clean message when Recycle Bin has 0 items.

### 3. Restore & Permanent Deletion Logic
- Update [RecycleBinDatabaseHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt):
  - `restoreItem(id: Long, context: Context): Boolean`
  - `restoreAllItems(context: Context): Int`
  - `permanentlyDeleteItem(id: Long, context: Context): Boolean`
  - `emptyRecycleBin(context: Context): Int`
  - `cleanupExpiredItems(context: Context): List<RecycleBinItem>`

### 4. Audit Logging for Restore & Auto-Deletion
- Update [AuditLogModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt):
  - Add `AuditActionType.RESTORE`, `AuditActionType.DELETE_PERMANENT`, `AuditActionType.AUTO_DELETE`.
  - Add `AuditItemOutcome.RESTORED`, `AuditItemOutcome.PERMANENTLY_DELETED`, `AuditItemOutcome.AUTO_DELETED_EXPIRED`.
- Record single/batch audit logs on restore, manual permanent delete, and auto-expiry purge.

---

## 🧪 Edge Cases & Tests
- **Missing Original Directory**: Automatically recreates folders when restoring files whose original parent directories were deleted.
- **Naming Conflict on Restore**: Handles name collisions by sequentially appending `(restored)` or `(restored 1)`.
- **Expired File Cleanup**: Tests verify that items older than 1 hour are purged and logged in Past Actions as auto-deleted.
- **Empty State & UI Transitions**: Verifies empty state when Recycle Bin has no items or when all items are restored/emptied.

---

## 🚀 Expected Code Changes
- Modify [app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt)
- Create [app/src/main/java/com/anshuman/tagstash/ui/screens/RecycleBinScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/RecycleBinScreen.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/SettingsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/SettingsScreen.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
- Unit & UI Tests: [RecycleBinDatabaseHelperTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/RecycleBinDatabaseHelperTest.kt) and [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
