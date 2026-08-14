# Specification: 022-expanded-cut-copy-paste-fab

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: What are the new locations for Cut and Copy buttons in this specification?
  - **A**:
    1. **File Item Long-Press Context Menu**: Added "Cut" and "Copy" options alongside "Select" and "Info" in order: "Select", "Cut", "Copy", "Info".
    2. **Selection Mode Bottom Hovering Bar**: Added Cut (`Icons.Default.ContentCut`) and Copy (`Icons.Default.ContentCopy`) icon buttons on the right side of the floating bar, next to the Close (`Icons.Default.Close`) button.
    3. **Selection Mode Top 3-Dot Menu**: Added "Cut" and "Copy" menu items (disabled when 0 items are selected), alongside "Select All/Deselect All", "Clipboard (<count>)", and "Info".
- **Q**: What happens to Selection Mode when Cut or Copy is clicked?
  - **A**: All selected files are added to `AppClipboard` with the selected operation (`CUT` or `COPY`), and the screen automatically exits Selection Mode.
- **Q**: Where does Paste appear?
  - **A**: Paste is NOT present in Selection Mode or in the file item long-press menu. Paste is only available in:
    1. The normal file viewer top 3-dot overflow menu (existing from Spec 021).
    2. A new bottom-right circular Floating Action Button (FAB) with a badge bubble showing clipboard count.
- **Q**: What are the visibility rules for the bottom-right circular Floating Paste Button (FAB)?
  - **A**:
    - Visible ONLY in normal file viewer mode when `AppClipboard.items.isNotEmpty()`.
    - Hidden during Selection Mode.
    - Hidden when a file item long-press context menu is open.
- **Q**: What happens when the Floating Paste Button is tapped?
  - **A**: Executes the batch paste operation into the current directory with conflict resolution and refreshes the directory listing (same as the 3-dot Paste menu item).
- **Q**: What happens if adding items from long-press or selection mode exceeds the 1000 item capacity limit?
  - **A**: If adding a batch of items would cause the total number of items in the clipboard to exceed 1000 (e.g. clipboard already has 950 items and user attempts to Cut or Copy 70 items), **none** of the items are added to the clipboard and only the `CapacityLimitDialog` is displayed.

---

## 🛠️ Feature Requirements
- Update [FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt):
  - Add `onCutClick: (() -> Unit)? = null` and `onCopyClick: (() -> Unit)? = null` parameters.
  - In the long-press dropdown menu, display: "Select", "Cut", "Copy", and "Info".
  - Add a callback/state notification when context menu is opened or closed (`onContextMenuVisibilityChanged: ((Boolean) -> Unit)? = null`).
- Update [BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt):
  - In Selection Mode: Add "Cut" and "Copy" options to the 3-dot menu (enabled when `selectedCount > 0`).
  - Add `onCutSelected: (() -> Unit)? = null` and `onCopySelected: (() -> Unit)? = null` parameters.
- Update [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt):
  - Add Cut and Copy icon buttons to the bottom hovering selection bar.
  - Connect Cut and Copy actions from selection mode to batch add selected files to `AppClipboard` and exit selection mode.
  - Render bottom-right circular `FloatingActionButton` with `BadgedBox` / `Badge` showing clipboard item count.
  - Hide the FAB when in selection mode or when any file row's context menu is open.
  - Connect FAB tap to `executePaste()`.

---

## 🎨 UI/UX Changes
- **File Row Long-Press Menu**:
  - `Select` (`Icons.Default.Checklist`)
  - `Cut` (`Icons.Default.ContentCut`, Primary accent color)
  - `Copy` (`Icons.Default.ContentCopy`, Primary accent color)
  - `Info` (`Icons.Default.Info`, Primary accent color)
- **Selection Mode Bottom Bar**:
  - Left: `X items selected`
  - Right:
    - Cut icon button (`Icons.Default.ContentCut`, enabled if `selectedCount > 0`)
    - Copy icon button (`Icons.Default.ContentCopy`, enabled if `selectedCount > 0`)
    - Close icon button (`Icons.Default.Close`)
- **Selection Mode Top 3-Dot Menu**:
  - `Select All` / `Deselect All`
  - `Cut` (enabled if `selectedCount > 0`)
  - `Copy` (enabled if `selectedCount > 0`)
  - `Clipboard (<count>)` (shown if `clipboardCount > 0`)
  - `Info` (enabled if `selectedCount > 0`)
- **Floating Paste Button**:
  - Circular FAB with container color `MaterialTheme.colorScheme.primaryContainer` or primary accent.
  - Paste icon (`Icons.Default.ContentPaste`).
  - Notification badge bubble at top-right with clipboard count (e.g. `1`, `5`, `12`).
  - Smooth visibility transitions when clipboard changes.

---

## 🧪 Edge Cases & Tests
- **Multiple items added to clipboard from selection mode**:
  - Adding 10 selected files updates clipboard size by +10.
  - If adding a batch of items would exceed the 1000 limit (e.g. 950 existing + 70 new = 1020), none of the items are added, and CapacityLimitDialog is shown.
- **FAB visibility**:
  - Clipboard empty -> FAB hidden.
  - Media player adds item -> FAB visible upon returning to normal file explorer.
  - Entering selection mode -> FAB hidden.
  - Long pressing a file row -> FAB hidden while context menu is open.
  - After paste -> Clipboard cleared, FAB automatically hides.
- **Unit & UI Tests**:
  - Test long-press context menu Cut and Copy actions.
  - Test selection mode bottom bar Cut and Copy actions.
  - Test selection mode 3-dot menu Cut and Copy actions.
  - Test Floating Paste Button appearance, badge count, tap paste action, and dismissal on long press.

---

## 🚀 Expected Code Changes
- Modify [FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt)
  - Add Cut and Copy items to context menu; add context menu visibility callback.
- Modify [BreadcrumbsBar.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/BreadcrumbsBar.kt)
  - Add Cut and Copy items to Selection Mode 3-dot menu.
- Modify [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)
  - Add Cut and Copy to bottom selection bar.
  - Add Badged Floating Action Button for paste with visibility conditions.
  - Handle selection mode Cut/Copy batch actions.
- Update [MainScreenTest.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/test/java/com/anshuman/tagstash/ui/screens/MainScreenTest.kt)
  - Add UI tests and Roborazzi screenshot tests for long-press Cut/Copy, selection mode Cut/Copy, and Floating Paste Button.
