# Spec 014: Pull To Refresh support

- **Status**: DONE
- **Date**: 2026-07-12

---

## 📋 Q&A and Assumptions
* **Q**: Which API will we use for pull-to-refresh?
  * **A**: We will use the modern Material 3 `PullToRefreshBox` from the `androidx.compose.material3.pulltorefresh` package, which is available in our Compose BOM version (2026.06.00).
* **Q**: Where will `PullToRefreshBox` be integrated?
  * **A**: It will wrap the content area of the file explorer directly below the breadcrumbs bar in [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt).
* **Q**: How do we make non-scrollable states (like empty folder and error views) refreshable?
  * **A**: We will wrap `EmptyDirectoryView` and `ErrorView` in a `Box` modifier with `verticalScroll(rememberScrollState())` to make them scrollable, enabling `PullToRefreshBox` to catch the drag gesture.
* **Q**: How do we prevent overlapping spinners?
  * **A**: We will only show the full-screen `CircularProgressIndicator` if `isLoading && !isRefreshing`. If a user manually pulls to refresh, only the top pull-to-refresh loading indicator will be shown.

## 📋 Feature Requirements
1. **Pull-to-Refresh Container**:
   * Wrap the content area below the breadcrumbs with `PullToRefreshBox`.
   * Bind `isRefreshing` state and increment a `refreshTrigger` state on refresh to force reload directory list in `LaunchedEffect`.

2. **Scrollable Empty and Error States**:
   * Wrap the `EmptyDirectoryView` inside a `Box` with `Modifier.fillMaxSize().verticalScroll(rememberScrollState())`.
   * Wrap the `ErrorView` inside a `Box` with `Modifier.fillMaxSize().verticalScroll(rememberScrollState())`.

3. **Loading State Suppression**:
   * Suppress the main screen center `CircularProgressIndicator` if `isRefreshing` is true.

## 📝 Expected Code Changes
* [MainScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt):
  * Import `PullToRefreshBox` and related scroll modifiers.
  * Implement `isRefreshing` and `refreshTrigger` state variables.
  * Integrate `PullToRefreshBox` around the file list / empty / error content area.
  * Wrap empty and error composables in scrollable containers.

## 🧪 Testing Strategy
* Compile the app and verify it builds successfully.
* Open a directory, pull down from below the breadcrumbs, verify that the pull-to-refresh indicator appears, spins, and disappears after reload.
* Add or delete a file in the directory using a separate file explorer, pull-to-refresh in TagStash, and verify the changes reflect instantly.
* Test pull-to-refresh on an empty directory, verify it triggers refresh successfully.
* Test pull-to-refresh on an error directory (e.g. denying permissions or reading restricted directories), verify it triggers refresh successfully.
