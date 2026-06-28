# Spec 002: UI & Screenshot Testing

- **Status**: TODO
- **Date**: 2026-06-28

---

## 📋 Feature Requirements
* Configure a visual/screenshot testing framework to capture and verify Compose UI layouts.
* Add interaction test cases for the following user flows:
  * **Permission Request Screen**: Verify correct appearance when permission is not granted.
  * **Directory Explorer View**: Verify grid/list layout showing folders, files, and sub-details (item counts, sizes, modification dates).
  * **Breadcrumbs Wrap**: Verify that extremely long folder names or multiple levels of directories wrap cleanly.
  * **Navigation Interaction**: Test that clicking folders correctly updates the current path and that hardware back triggers go back up.
  * **Empty Directory State**: Verify the empty illustration and visual back button state.
  * **Error State**: Verify restricted directory error view rendering.

## 🎨 UI/UX Verification (Visual Regression)
* Generate baseline screenshots for core screens and views.
* Compare screenshot outputs pixel-by-pixel during build checks to catch layout regressions.
* Baselines will be stored under `app/screenshots/` or equivalent.

## 🛠️ Tech Stack & Android Specifics
* **Robolectric**: For hosting Android environment and lifecycle execution on local JVM.
* **Roborazzi**: For rendering Compose trees and capturing visual comparison screenshots on local JVM.
* **JUnit 4 & Compose Test Rule**: For standard UI hierarchy interactions.
* **Gradle Configuration**:
  * Apply `io.github.takahirom.roborazzi` plugin.
  * Add Roborazzi and Robolectric dependencies to `libs.versions.toml` and `app/build.gradle.kts`.

## ⚠️ Edge Cases & Failure Modes
* **Theme consistency**: Ensure screenshots are captured consistently in Dark Mode to avoid false positive comparison failures.
* **Font differences**: System fonts on different developer machines can cause pixel mismatches. We will configure Roborazzi to use uniform font rendering.
* **System Dialogs**: Direct permissions settings requests (System settings activity launch) cannot be visually verified on local JVM. We will mock the permission check function.

## 🧪 Testing Strategy
* **Local JVM Execution**: Running `./gradlew recordRoborazziDebug` to record baseline images.
* **Verification Checks**: Running `./gradlew verifyRoborazziDebug` (or standard `./gradlew check` which runs verification) to assert layout correctness.

## 📝 Expected Code Changes
* [libs.versions.toml](file:///Users/personal/AndroidStudioProjects/TagStash/gradle/libs.versions.toml): Add Roborazzi and Robolectric versions/libraries/plugins.
* [build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/build.gradle.kts) (root) & [app/build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts): Apply Roborazzi plugin and add test dependencies.
* UI Test Classes: Create one or more Compose UI test files verifying screen layouts and visual baselines.

---

## ❓ Open Questions & Assumptions (Resolved)
1. **Roborazzi vs. Instrumented**: Resolved. Roborazzi (local JVM screenshot testing via Robolectric) is preferred because it runs fast without boot latency.
2. **Mocking Files**: Resolved. We will mock directory listings at the data/view level rather than relying on temporary physical folders on disk during UI tests.
