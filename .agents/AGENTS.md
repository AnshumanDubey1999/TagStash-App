# Workspace Rules & Instructions: TagStash

This file defines the project-scoped rules, constraints, and development guidelines for **TagStash**, an Android application designed for local file exploration, media viewing/playback, and tag-based organization.

---

## 🛠️ Technology Stack
1. **Platform & Language**: Android (Min SDK 33, Target/Compile SDK 36), Kotlin
2. **UI Framework**: Jetpack Compose (using Material Design 3 components, Compose navigation, and state management)
3. **Database & Storage**: Room Database (for managing tags and tag-to-file mappings)
4. **Media Handling**: 
   * **Images**: Coil (Kotlin Image Loading library)
   * **Audio & Video**: Jetpack Media3 (ExoPlayer for video and audio playback)
5. **Concurrency & Reactivity**: Kotlin Coroutines & Flow (for reactive database queries and async disk exploration)

---

## ⚙️ Environment & Commands
* **Build System**: Gradle (Kotlin DSL)
* **Common Commands**:
  * **Compile & Check**:
    ```bash
    ./gradlew compileDebugKotlin
    ```
  * **Run Tests**:
    ```bash
    ./gradlew test
    ```
  * **Build Debug APK**:
    ```bash
    ./gradlew assembleDebug
    ```
  * **Code Quality / Linting**:
    ```bash
    ./gradlew check
    ```

---

## 🎨 Design System & Aesthetics
* **Theme**: Modern, elegant Dark Mode by default using Material Design 3 tokens. Deep Slate/Charcoal background (`#121212` / `#1E1E1E`), Indigo/Violet accent colors, and Glassmorphic/Semi-transparent overlays for media player controls.
* **Typography**: Clean, modern system fonts or Outfit/Inter loaded dynamically.
* **Layouts**: 
  * Adaptive Grid and List views for file browsing.
  * Rich media preview cards with thumbnail generation.
  * Immersive full-screen viewer/player with overlay controls for video/audio.
* **Icons**: Standard Material Icons for file types (Folder, Image, Video, Audio, Generic File, Tag). Clear visual differentiation between folders and files.
* **Interactions**: Smooth touch transitions, state-driven animations, drag-and-drop support for tagging (optional/future), and quick filter chips.

---

## 🚀 Development Principles
1. **Android Permission Management**:
   * Min SDK is 33, which requires granular permissions:
     * `android.permission.READ_MEDIA_IMAGES`
     * `android.permission.READ_MEDIA_VIDEO`
     * `android.permission.READ_MEDIA_AUDIO`
   * Handle Scoped Storage guidelines gracefully, requesting access using standard system pickers or storage access framework where necessary.
   * Provide informative UX explanation before requesting runtime permissions.
2. **Robustness & Edge Cases**:
   * Handle permission denial gracefully by showing empty states or custom permission-granting request screens.
   * Address file exploration edge cases: missing directories, restricted system directories, zero-byte/broken media files, file names with special characters, and very large directories (using paging or background flow streaming).
3. **Reactivity & Flow**:
   * Use room database flows to automatically update search results when files are added or tagged.
   * Offload disk operations and database writes to background threads via `Dispatchers.IO` to ensure smooth UI performance (60+ FPS).
4. **Testing**:
   * Plan unit tests for tag-filtering logic, Room DAO operations, and file size formatting utilities.
   * Implement UI/Screenshot testing strategies for Compose views.
5. **Git Commits & Branching**:
   * **Never commit or push directly to `main`**. All development is conducted on dedicated feature branches.
   * **Branch Naming**: Branch off from updated `main` (`git pull --rebase`) using the format `<spec-num>-<short-description>` (e.g., `033-media-player-playlists`).
   * **Small & Frequent Commits**: The Agent is permitted and encouraged to make frequent, small, logical commits directly on the feature branch during implementation without asking for confirmation per commit.
   * **Commit Message Format**: Every commit message must start with the specification number (e.g., `[033] Add playlist model and DAO`).
   * **Remote Actions**: Pushing branches to GitHub, opening Pull Requests, reviewing, and merging to `main` is handled exclusively by the user.

---

## 📋 Spec-Driven Development Workflow
When implementing new features or making significant changes:
1. **Spec Creation**:
   * Before creating the specification file in `.ai/specs/`, raise questions and explicitly flag assumptions. Discuss and resolve these with the user.
   * **File Naming**: Use three-digit prefix numbering (e.g., `.ai/specs/001-file-explorer.md`, `.ai/specs/002-media-viewer.md`).
   * **Preserve Comments & Questions**: Ensure all specification-related Q&A remains documented in the spec file to preserve context.
2. **Spec State & Transitions**:
   * Track spec status at the top: `TODO`, `IN_PROGRESS`, or `DONE`.
   * **CRITICAL**: The state of a spec file must never be changed without explicit user permission.
   * **CRITICAL**: Code changes should only be done when a spec is in `IN_PROGRESS` state.
3. **Spec Style & Content**:
   * Write using short, bulleted pointers instead of long, dense text blocks.
   * Clearly list: Feature requirements, UI/UX changes, Database/Backend changes, Android-specific requirements (permissions/manifest).
   * **Edge Cases & Tests**: Detail possible failure modes, permission scenarios, and testing strategies.
   * **Expected Code Changes**: List target files to modify and a brief outline of the changes.
4. **Spec Splitting Rule**:
   * If a spec represents a change that is too large for a single implementation action, plan to split it into a separate spec focusing on the simplest possible next step first.
   * **CRITICAL**: You must ask the user for explicit permission before creating a new/split spec file.
5. **Review & Approval**: Ask the user to review the spec and provide feedback. Refine the spec file based on this feedback until the user explicitly approves it.
6. **Branch Setup & Implementation**:
   * Once approved and transitioned to `IN_PROGRESS`:
     * Ensure `main` is updated and create/switch to the feature branch `<spec-num>-<short-description>`.
     * Implement changes incrementally, making small, focused commits on the feature branch.
7. **Post-Implementation Verification & Hand-Off**:
   * Once implementation work is finished:
     * Verify the code compiles and `./gradlew test` passes locally.
     * Ensure all code changes are committed to the feature branch.
     * Request user permission to mark the spec state as `DONE`.
     * Provide a ready-to-use **Pull Request summary** formatted according to `.github/pull_request_template.md` for the user to copy/paste into GitHub.
