# Spec 013: Flexible Filename Display

- **Status**: DONE
- **Date**: 2026-07-11

---

## 📋 Q&A and Assumptions
* **Q**: Which files display the file name and need modification?
  * **A**: 
    1. [FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt): The list item that displays directories/files in the file explorer.
    2. [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt): The top details overlay header showing the name of the active media file.
* **Q**: What are the new styling settings?
  * **A**: 
    * `maxLines` will be bumped from `1` to `2`.
    * Font size will be reduced from `15.sp` to `13.sp` (semi-bold/bold).
    * `overflow` will continue to use `TextOverflow.Ellipsis`.
* **Q**: Will the height of the row/header stretch to fit 2 lines?
  * **A**: Yes, the parent Compose containers are dynamic and will naturally expand to fit 2 lines of text when needed, keeping the file metadata (size, last modified) positioned below the wrapped filename.
* **Q**: Are breadcrumbs affected?
  * **A**: No, breadcrumbs are out of scope and will continue to display folder names truncated to 10 characters.

## 📋 Feature Requirements
1. **FileRowItem File Name Layout**:
   * Change text settings of the filename `Text` component:
     * Set `maxLines = 2`.
     * Set `fontSize = 13.sp`.
   * Verify that the column container handles the height change gracefully.

2. **MediaPlayer Details Header File Name Layout**:
   * Change text settings of the filename `Text` component:
     * Set `maxLines = 2`.
     * Set `fontSize = 13.sp`.
   * Verify that the header card does not clip the expanded text block.

## 📝 Expected Code Changes
* [FileRowItem.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/components/FileRowItem.kt): Set `maxLines = 2` and `fontSize = 13.sp` for the file name text.
* [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt): Set `maxLines = 2` and `fontSize = 13.sp` for the header filename text.

## 🧪 Testing Strategy
* Compile the app and verify it builds successfully.
* View a directory containing files with very long names (100+ characters), confirm they wrap up to 2 lines and use the smaller font size.
* Open a file with a very long name in the media viewer, confirm the top header details overlay wraps the filename to 2 lines correctly.
* Rotate the screen to landscape and verify that the layout adapts and fits even more characters on a single line or wrapped cleanly across 2 lines.
