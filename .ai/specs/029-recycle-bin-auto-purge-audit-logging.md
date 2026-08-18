# Specification: 029-recycle-bin-auto-purge-audit-logging

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: What trigger causes automatic permanent purge of items in Recycle Bin?
  - **A**: Trashed items whose `expiry_timestamp <= currentTime` (retention duration of 1 hour has elapsed).
- **Q**: Where is auto-purge invoked in the app lifecycle?
  - **A**: In `MainScreen` on initial launch/composition, in `SettingsScreen` on display, in `RecycleBinScreen` on display, and on directory/screen transitions.
- **Q**: How should the audit log entry for auto-purged items be formatted?
  - **A**:
    - `actionType`: `AuditActionType.AUTO_DELETE`
    - `summary`: `"Auto-purged expired file <fileName> (1 hour retention)"` or `"Auto-purged <count> expired files (1 hour retention)"`
    - `destinationDirectory`: `"Auto Cleanup"`
    - `totalItems`: `expiredItems.size`
    - `items`: `AuditLogItemDetail` list with `sourcePath = item.trashedPath`, `destinationPath = null`, `command = "AUTO_DELETE"`, `outcome = AuditItemOutcome.AUTO_DELETED_EXPIRED`, `fileName = item.fileName`, `fileSize = item.fileSize`, `isDirectory = item.isDirectory`.
- **Q**: Where should this audit entry be created?
  - **A**: Directly inside `RecycleBinDatabaseHelper.cleanupExpiredItems(context: Context)` whenever `expiredItems.isNotEmpty()`, guaranteeing consistent logging across all trigger points.

---

## 🛠️ Feature Requirements

### 1. Database & Auto-Purge Audit Logging
- In [RecycleBinDatabaseHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt):
  - In `cleanupExpiredItems(context: Context)`:
    - If `expiredItems.isNotEmpty()`:
      - Construct `AuditLogItemDetail` records with outcome `AuditItemOutcome.AUTO_DELETED_EXPIRED` and command `"AUTO_DELETE"`.
      - Construct `AuditLogEntry` with `AuditActionType.AUTO_DELETE`, summary, and destination `"Auto Cleanup"`.
      - Call `AuditLogDatabaseHelper.getInstance(context).insertLog(entry)`.

### 2. UI Audit Representation
- Verify and refine [ActionDetailDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/ActionDetailDialog.kt) and [PastActionsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/PastActionsScreen.kt):
  - Ensure `AuditActionType.AUTO_DELETE` shows appropriate icon (`Icons.Default.DeleteSweep` or `Icons.Default.DeleteForever`) with error/caution tint and title `"Auto-Purge Action"`.
  - Ensure `AuditItemOutcome.AUTO_DELETED_EXPIRED` displays `"EXPIRED"` / `"PURGED"` badge with distinct styling.

---

## 🧪 Edge Cases & Tests
- **No Expired Items**: When `cleanupExpiredItems` runs but no items are expired, no audit log entry should be created.
- **Single Expired Item**: Trashing an item with simulated past timestamp and running cleanup creates a single-item `AUTO_DELETE` log.
- **Batch Expired Items**: Trashing multiple items with past timestamps and running cleanup creates a multi-item `AUTO_DELETE` log with correct count and details.
- **Past Actions Display**: Navigating to Past Actions after auto-cleanup properly renders the `AUTO_DELETE` entry with item list dialog.

---

## 🚀 Expected Code Changes
- Modify [app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/database/RecycleBinDatabaseHelper.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/components/ActionDetailDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/ActionDetailDialog.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/PastActionsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/PastActionsScreen.kt)
- Unit & UI Tests: [RecycleBinDatabaseHelperTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/RecycleBinDatabaseHelperTest.kt) & [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
