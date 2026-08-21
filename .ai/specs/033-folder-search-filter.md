# Specification: 033-folder-search-filter

**Status**: IN_PROGRESS

---

## 📌 Background & Context
Users need a convenient way to search and filter files and folders by name within the current directory. This specification introduces a **Search Dialog** accessible from the 3-dot overflow menu in any folder, with options for recursive subfolder scanning, a real-time scanning progress loader, and a dedicated results view displaying matching items alongside their relative directory locations.

---

## 🎯 Requirements & Scope

### 1. Menu Entry & Accessibility
- Add a **"Search"** item (with `Icons.Default.Search`) to the top-right 3-dot overflow menu in [`MainScreen.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt).
- Available during standard folder exploration; hidden while in **Selection Mode**.

### 2. Search Popup Dialog ([`SearchDialog.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/SearchDialog.kt))
- **Input Field**: Outlined text field with clear button (`✕`) for entering the query text.
- **Checkbox**: *"Include subfolders"* (unchecked by default).
- **Actions**:
  - **"Cancel"**: Dismisses the dialog without performing search.
  - **"Search"**: Enabled when query is not blank; triggers the scanning engine.

### 3. Scanning Progress & Indexing Strategy
- Recursive disk exploration executed asynchronously on `Dispatchers.IO` via Kotlin Coroutines.
- When searching (especially across deep subfolders), display a modal progress indicator or inline scanning loader displaying the live scanned item count (e.g. *"Searching... (Scanned 142 items)"*).
- Case-insensitive substring matching on file and directory names.

### 4. Search Results View
- **Top Bar / Address Bar**:
  - Displays `"Results for: \"<query>\""` with a clear count subtitle (e.g., `5 items found`).
  - Left navigation icon is a Back arrow (`←`).
- **Results List**:
  - Renders matching files and directories in a style consistent with `FileRowItem`.
  - Displays file icon, name, formatted file size, and modified date.
  - Includes an **additional location subtitle** showing the item's relative path or parent folder (e.g., `Location: /images/pngs`).
- **Read-Only Scope (Spec 033)**:
  - For this specification, search results are strictly informational/read-only (no context menus or clicks into sub-actions).
- **Empty State**:
  - Displays a clean empty state if no matches are found: *"No files found matching \"<query>\""*.

### 5. Navigation & State Management
- Pressing the Top Bar Back button or hardware Back button immediately exits the search view and returns the user to the exact folder where the search was initiated.
- Preserves search state across device rotations without resetting to root.

---

## 📋 Q&A & Assumptions Preserved
- **Q**: Should the search match both files and folders or files only?
  - **A**: Match both files and subdirectories containing the query string.
- **Q**: What label and icon are used in the menu?
  - **A**: "Search" label with `Icons.Default.Search`.
- **Q**: Can items be opened or manipulated from search results in this spec?
  - **A**: No, this spec focuses on the search dialog, scanning engine, and results display list. Future specs can introduce actions on search results.

---

## 🔍 Edge Cases & Verification
- **Empty or Whitespace Query**: Search button disabled or trimmed.
- **Large Directory Trees**: Non-blocking asynchronous scanning on `Dispatchers.IO` with live count feedback.
- **Special Characters in Query**: Safe substring matching without regex syntax crashing.
- **Restricted / Unreadable Directories**: Gracefully skipped without halting recursive search.
- **Screen Rotation**: Search results and active search query retained across orientation changes.

---

## 📂 Target Files & Planned Modifications
1. [`app/src/main/java/com/anshuman/tagstash/ui/components/SearchDialog.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/SearchDialog.kt):
   - New composable dialog with query text field, "Include subfolders" checkbox, and Cancel/Search buttons.
2. [`app/src/main/java/com/anshuman/tagstash/ui/components/SearchResultItem.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/SearchResultItem.kt):
   - Composable row displaying matching item with file icon, name, details, and parent location path.
3. [`app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt):
   - Add Search item to 3-dot overflow menu.
   - Add search state (`activeSearchQuery`, `searchIncludeSubfolders`, `searchResults`, `isSearching`, `scannedCount`).
   - Integrate `SearchDialog` and Search Results list rendering with top bar `"Results for: <query>"`.
4. [`app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt):
   - Unit and UI tests for Search Dialog opening, current folder search, recursive subfolder search, empty search results, location display, and back navigation.
