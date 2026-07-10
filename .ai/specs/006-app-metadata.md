# Spec 006: App Icon, Versioning, and README Configuration

- **Status**: DONE
- **Date**: 2026-07-10

---

## 📋 Q&A and Assumptions
* **Q**: How should we handle the app icons?
  * **A**: Generate density-specific PNGs (48x48 to 192x192) using macOS `sips`, and remove the default adaptive XML/webp files so Android falls back to these PNGs.
* **Q**: How should the versioning system be configured?
  * **A**: Define `app.version.code` and `app.version.name` in `gradle.properties` and read them dynamically in `app/build.gradle.kts`.
* **Q**: What is the difference between `versionCode` and `versionName`?
  * **A**: 
    * `versionCode` (integer, e.g. `1`): Internal version number used by Android and Google Play to determine if one build is newer than another. Must always increase sequentially with every release (nightly, minor, major).
    * `versionName` (string, e.g. `"0.1.0"`): User-facing version string following Semantic Versioning (`MAJOR.MINOR.PATCH`).
* **Q**: How will they change in future automated workflows?
  * **A**: 
    * **Nightly**: Increment `versionCode` by 1; update `versionName` by incrementing the patch version (e.g. `0.1.0` -> `0.1.1-nightly`).
    * **Minor**: Increment `versionCode` by 1; update `versionName` by incrementing the minor version (e.g. `0.1.0` -> `0.2.0`).
    * **Major**: Increment `versionCode` by 1; update `versionName` by incrementing the major version (e.g. `0.2.0` -> `1.0.0`).
* **Assumptions**:
  * The source files `/Users/personal/Downloads/icon-full.png` and `/Users/personal/Downloads/icon-round.png` are valid PNG files.
  * System tools (`sips`) are available on the macOS host to resize images.

## 📋 Feature Requirements
1. **App Icon Assets**:
   * Copy and resize `/Users/personal/Downloads/icon-full.png` to:
     * `app/src/main/res/mipmap-mdpi/ic_launcher.png` (48x48)
     * `app/src/main/res/mipmap-hdpi/ic_launcher.png` (72x72)
     * `app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96x96)
     * `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` (144x144)
     * `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (192x192)
   * Copy and resize `/Users/personal/Downloads/icon-round.png` to:
     * `app/src/main/res/mipmap-mdpi/ic_launcher_round.png` (48x48)
     * `app/src/main/res/mipmap-hdpi/ic_launcher_round.png` (72x72)
     * `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png` (96x96)
     * `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png` (144x144)
     * `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png` (192x192)
   * Delete existing launcher webp files in mipmap density folders.
   * Delete `mipmap-anydpi/ic_launcher.xml` and `mipmap-anydpi/ic_launcher_round.xml` so Android uses the legacy PNG icons directly.

2. **Versioning System**:
   * Add version properties to `gradle.properties`:
     * `app.version.code=1`
     * `app.version.name=0.1.0`
   * Update `app/build.gradle.kts` to load and apply these values to `versionCode` and `versionName`.

3. **Project Documentation**:
   * Create a comprehensive `README.md` at the project root describing:
     * Application overview (TagStash - local file explorer, media player, tagging).
     * Tech stack (Kotlin, Jetpack Compose, Room, Media3 ExoPlayer, Coil).
     * Features (Navigation, grid/list file layout, image/video support, tag assignment).
     * Build & run instructions (`./gradlew assembleDebug`, test run commands).
     * Explanation of the versioning structure and launcher icon workflow.

## 📝 Expected Code Changes
* [gradle.properties](file:///Users/personal/AndroidStudioProjects/TagStash/gradle.properties): Add version code and name properties.
* [app/build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts): Reference version properties from project properties.
* `app/src/main/res/mipmap-*/ic_launcher*`: Generate PNGs and clean up old WebP/XML assets.
* [README.md](file:///Users/personal/AndroidStudioProjects/TagStash/README.md): Create the new file in the root.

## 🧪 Testing Strategy
* Verify successful compilation after modifying `app/build.gradle.kts`.
* Check resource directories to ensure the PNGs are correctly sized and placed.
* Verify that Android manifest resolves `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round` correctly.
