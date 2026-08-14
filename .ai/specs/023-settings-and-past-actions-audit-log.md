# Specification: 023-settings-and-past-actions-audit-log

**Status**: IN_PROGRESS

---

## 📋 Q&A & Assumptions
- **Q**: Where does the Settings option appear?
  - **A**: Added to the 3-dot overflow menu in both the File Manager (`BreadcrumbsBar` / `MainScreen`) and the `MediaPlayerScreen`.
- **Q**: How is the Settings space presented?
  - **A**: As a full-screen view (`SettingsScreen`) with a top App Bar containing a back arrow to return to the previous screen.
- **Q**: What items are inside the Settings screen?
  - **A**: A clean Material 3 list layout containing a single setting item for now: **"Past Actions"** (with `Icons.Default.History` icon and subtitle "View history of file operations like paste, rename, and delete").
- **Q**: What does the Past Actions screen show?
  - **A**: A chronological list of all file-altering operations performed by the app, ordered descending with the latest action at the top. For now, it logs Paste actions (and future operations like Rename/Delete will hook into the same system).
- **Q**: How is a batch paste action logged?
  - **A**: A batch paste operation creates exactly **one** entry in the Past Actions log. The entry contains a high-level summary (e.g. timestamp, destination folder, item count) and a full detailed list of all items changed:
    - Operation command: `CUT` or `COPY`
    - Source file path
    - Destination file path / created file path
    - Outcome status: `COPIED`, `MOVED`, `REPLACED`, `RENAMED_COPY`, or `SKIPPED`
    - File size and type
- **Q**: What happens when an action entry in Past Actions is clicked?
  - **A**: Opens a detailed **Action Detail Screen / Dialog** displaying the complete breakdown of every file involved, what moved where, what was renamed or replaced, and timestamps.
- **Q**: Can past actions be deleted?
  - **A**: Yes:
    1. A single action can be deleted from the audit log via a "Delete" button inside the Action Detail dialog (with confirmation).
    2. The entire history can be cleared via "Clear History" in the top menu of the Past Actions screen (with confirmation).
- **Q**: How is internal permanent memory implemented?
  - **A**: Using an internal SQLite database (`AppDatabase` / `AuditLogDatabaseHelper`) to persistently store audit records across app launches.

---

## 🛠️ Feature Requirements
- **Database & Persistence**:
  - Create [AuditLogDatabaseHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/database/AuditLogDatabaseHelper.kt) managing `audit_logs` table.
  - Create `AuditLogEntry` and `AuditLogItemDetail` data classes.
  - Implement async DAO/repository methods: `insertLog(entry)`, `getAllLogs()`, `getLogById(id)`, `clearAllLogs()`.
- **Logging Integration**:
  - When a paste operation completes in [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt), record an `AuditLogEntry` with details of all processed items (source, destination, cut/copy, replaced, renamed, skipped).
- **Settings Screen**:
  - Create [SettingsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/SettingsScreen.kt) with top app bar, back button, and "Past Actions" item.
- **Past Actions Screen & Detail Dialog**:
  - Create [PastActionsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/PastActionsScreen.kt) displaying list of past actions in descending order with "Clear History" menu option.
  - Create [ActionDetailDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/ActionDetailDialog.kt) showing the full list of files, commands, and address changes for a selected action.
- **Navigation & Menus**:
  - Add "Settings" option (`Icons.Default.Settings`) to 3-dot menus in [BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt) and [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt).

---

## 🎨 UI/UX Changes
- **Settings Screen**:
  - Material 3 Dark theme list item with icon container, title "Past Actions", and description text.
- **Past Actions Screen**:
  - Top bar with title "Past Actions", back button, and 3-dot menu with "Clear History".
  - Empty state with `Icons.Default.History` and message "No past actions recorded".
  - Action card with timestamp, action type badge (`PASTE`), item count, destination path, and summary badges (e.g. `2 Copied`, `1 Replaced`).
- **Action Detail Dialog**:
  - Header with action timestamp, destination folder, and a "Delete" button (`Icons.Default.DeleteOutline`) with confirmation dialog.
  - Scrollable list of items showing original source path $\rightarrow$ destination path, command badge (`CUT` / `COPY`), and status badge (`MOVED`, `COPIED`, `REPLACED`, `RENAMED`, `SKIPPED`).

---

## 🧪 Edge Cases & Tests
- **Empty state**: Opening Past Actions when no actions exist shows empty state.
- **Single vs batch paste**: 1 item paste and 50 items batch paste both create a single audit log entry with full item lists.
- **Conflict resolutions recorded**: Replaced files marked as `REPLACED`, renamed files marked as `RENAMED_COPY`, skipped files marked as `SKIPPED`.
- **Clear history**: "Clear History" empties the database and immediately updates the UI.
- **Database persistence across app reloads**: Stored entries persist and reload reliably.
- **Unit & UI Tests**:
  - Unit tests for `AuditLogDatabaseHelper` (insert, retrieve, clear).
  - UI tests for Settings screen, Past Actions list, Action Detail dialog, and clearing history.

---

## 🚀 Expected Code Changes
- Create [AuditLogDatabaseHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/database/AuditLogDatabaseHelper.kt)
- Create [AuditLogModels.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/model/AuditLogModels.kt)
- Create [SettingsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/SettingsScreen.kt)
- Create [PastActionsScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/PastActionsScreen.kt)
- Create [ActionDetailDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/ActionDetailDialog.kt)
- Modify [BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt)
- Modify [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)
- Modify [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
- Create / Update Tests:
  - [AuditLogDatabaseHelperTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/AuditLogDatabaseHelperTest.kt)
  - [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
