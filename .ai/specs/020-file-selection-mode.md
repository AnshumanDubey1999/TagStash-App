# Specification: 020-file-selection-mode

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: What are the entry points to Selection Mode in the File Manager?
  - **A**:
    1. **Top 3-dot overflow menu** (`BreadcrumbsBar.kt`): Contains a **"Select"** option above the "Info" option. Clicking it enters Selection Mode with 0 items selected.
    2. **Item Long-press context menu** (`FileRowItem.kt`): Contains a **"Select"** option above the "Info" option. Clicking it enters Selection Mode and pre-selects the long-pressed file or folder item.
- **Q**: How do file and folder items behave and look in Selection Mode?
  - **A**:
    - Clicking on any file or folder row toggles its selected state (normal file opening and directory navigation are suppressed).
    - Rows display a selection checkbox indicator on the start of each row.
    - Selected rows are styled with a subtle accent/primary background highlight.
- **Q**: What does the bottom hovering/floating bar contain?
  - **A**:
    - Displays the count of selected items (e.g. `"3 items selected"`).
    - Contains a Close (`✕`) button to exit Selection Mode and clear all selections.
- **Q**: What options appear in the top 3-dot overflow menu during Selection Mode?
  - **A**:
    - **"Select All" / "Deselect All"** toggle: Selects all items in the current folder, or deselects all if all items are already selected.
    - **"Info"** option:
      - **Disabled** (`enabled = false`) if 0 items are selected.
      - **Enabled** if $\ge 1$ items are selected, opening the properties dialog for the aggregated selection.
- **Q**: What information is displayed in the properties dialog for selected items?
  - **A**:
    - Total items count (e.g., `"5 items (3 files, 2 folders)"`).
    - Total size across selected items (formatted size + exact byte count).
    - Type breakdown (e.g., Images: 2, Videos: 1, Documents: 1, Folders: 1).
    - Location / directory path.
    - (Tag aggregation is deferred to future tag-management specifications).
- **Q**: How does the hardware/gesture Back button behave?
  - **A**: If Selection Mode is active, pressing Back exits Selection Mode first; pressing Back again performs standard directory navigation.
- **Assumptions**:
  - Selection mode is currently scoped to the File Explorer (`MainScreen.kt`).
  - Selection is cleared when navigating to another directory.

---

## 🛠️ Feature Requirements
- Add "Select" option to top-bar 3-dot menu and file item long-press context menu.
- Introduce selection state management (`isSelectionMode: Boolean`, `selectedFiles: Set<File>`).
- Add visual selection indicators (checkbox + highlighted row container) to `FileRowItem.kt`.
- Intercept item clicks in selection mode to toggle item selection.
- Implement bottom floating selection bar with selected count and close button.
- Support "Select All" / "Deselect All" in top menu.
- Support aggregated multi-item properties calculation and dialog display.
- Disable "Info" button in selection mode when 0 items are selected.
- Handle Back button to exit Selection Mode gracefully.

---

## 🎨 UI/UX Changes
- **File List**:
  - Checkbox indicator with smooth enter animation when selection mode is activated.
  - Accent-tinted background highlight on selected rows.
- **Bottom Floating Bar**:
  - Floating card with rounded corners, dark slate/charcoal background (`#1E1E1E`), subtle border/elevation, positioned above the system navigation bar.
  - Selected count text in bold typography and an `IconButton` with `Icons.Default.Close`.
- **Properties Dialog**:
  - Displays multi-item summary statistics with clear section cards.

---

## ⚙️ Android-specific Requirements
- Uses Compose `BackHandler` with priority for selection mode dismissal.
- Floating bar uses `Modifier.navigationBarsPadding()` to avoid overlapping system navigation controls.

---

## 🧪 Edge Cases & Tests
- **Empty Selection**: Tapping 3-dot menu with 0 items selected disables the Info button.
- **Select All & Deselect All**: Correctly selects and clears all visible items in the current directory.
- **Mixed Content**: Selection containing both files and folders aggregates counts and sizes accurately.
- **Back Navigation**: Back button closes selection mode without changing directory.
- **Unit & UI Tests**:
  - Verify selection mode activation via top menu and long-press menu.
  - Verify toggling selection on items.
  - Verify Select All / Deselect All.
  - Verify multi-item properties dialog content.
  - Verify `./gradlew test` and `./gradlew check` pass.

---

## 🚀 Expected Code Changes
- Modify [BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt)
  - Add "Select", "Select All", and "Deselect All" menu actions, disable Info when no items are selected in selection mode.
- Modify [FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt)
  - Add "Select" option in long-press menu, support `isSelectionMode` and `isSelected`, show checkbox and row selection background.
- Modify [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
  - Manage selection mode state, render bottom floating bar, wire item click toggle, back handler, and selection properties.
- Modify [FileMetadataHelper.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/utils/FileMetadataHelper.kt) / [FilePropertiesDialog.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FilePropertiesDialog.kt)
  - Add support for aggregating metadata for multiple files/directories.
- Modify [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
  - Add unit and screenshot UI tests for selection mode features.
