# Specification: 030-file-operation-progress-indicator

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: Which operations should display the operation progress indicator?
  - **A**:
    1. **Paste Operations (Copy / Cut & Move)**: While pasting single or multiple items from clipboard in `MainScreen`.
    2. **Deletion to Recycle Bin**: While batch moving files to the Recycle Bin in `MainScreen`.
    3. **Recycle Bin Batch Operations**: While "Restore All" or "Empty Recycle Bin" is executing in `RecycleBinScreen`.
- **Q**: What is the visual presentation of the progress indicator?
  - **A**: A centered **Modal Progress Dialog** (non-dismissible during active execution) featuring:
    - Operation Icon (Copy, Cut/Move, Trash, Restore) with contextual tint.
    - Operation Title (e.g., *"Copying items..."*, *"Moving items..."*, *"Deleting items..."*, *"Restoring items..."*).
    - Progress fraction and percentage (e.g., *"3 of 10 items (30%)"*).
    - `LinearProgressIndicator` tracking item completion progress.
    - Current filename being processed (with ellipsis for long names).
- **Q**: Can the dialog be dismissed by clicking outside or pressing back during an active operation?
  - **A**: No, `dismissOnClickOutside = false` and `dismissOnBackPress = false` are enforced to prevent state desynchronization and corruption during background I/O operations.

---

## 🛠️ Feature Requirements

### 1. Operation Progress Data Model & Component
- Create [OperationProgressDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/OperationProgressDialog.kt):
  - Data class `OperationProgressState(val title: String, val currentItem: Int, val totalItems: Int, val currentFileName: String, val icon: ImageVector, val iconTint: Color)`.
  - Compose dialog rendering centered card with Material 3 dark styling, animated linear progress bar, item counters, percentage display, and current file name label.

### 2. Integration in MainScreen (Paste & Deletion)
- Update [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt):
  - Add state `var operationProgress by remember { mutableStateOf<OperationProgressState?>(null) }`.
  - In `executePaste()`: update `operationProgress` on each item iteration (e.g., `operationProgress = OperationProgressState("Copying items...", index + 1, total, file.name, ...)`), clearing when complete.
  - In `filesPendingDelete` confirmation: update `operationProgress` as files are transferred into the Recycle Bin, clearing upon completion.

### 3. Integration in RecycleBinScreen (Restore All & Empty Bin)
- Update [RecycleBinScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/RecycleBinScreen.kt):
  - Update `operationProgress` while batch restoring all items or emptying all items from the bin.

---

## 🧪 Edge Cases & Tests
- **Zero Items**: Progress state is not triggered if item list is empty.
- **Single Item vs Multi-Item**: Works consistently for single-file and large multi-file batch operations.
- **Dialog Non-Dismissible**: Verifies dialog remains displayed and does not dismiss on root tap until the operation completes.
- **Completion Teardown**: Progress dialog automatically dismisses upon completion of the I/O task and screen reflects updated file list.

---

## 🚀 Expected Code Changes
- Create [app/src/main/java/com/anshuman/tagstash/ui/components/OperationProgressDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/OperationProgressDialog.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
- Modify [app/src/main/java/com/anshuman/tagstash/ui/screens/RecycleBinScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/RecycleBinScreen.kt)
- Unit & UI Tests: [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
