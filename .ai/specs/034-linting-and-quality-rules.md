# Specification: 034-linting-and-quality-rules

**Status**: DONE

---

## 📌 Background & Context
To maintain high code quality, prevent architectural decay, and avoid gigantic monolithic source files, this specification introduces an automated linting and code quality system for TagStash. We establish a **500-line file length limit** (excluding tests), essential Kotlin/Compose clean code rules, and automated CI pipeline enforcement.

---

## 🎯 Requirements & Scope

### 1. Static Analysis Tool: Detekt
- Add **Detekt** Gradle plugin (`io.gitlab.arturbosch.detekt`) to `gradle/libs.versions.toml`, root `build.gradle.kts`, and `app/build.gradle.kts`.
- Configure `config/detekt/detekt.yml` with project-specific rules and thresholds.
- Ensure `./gradlew detekt` runs fast locally and outputs HTML/XML reports into `app/build/reports/detekt/`.

### 2. File Size & Complexity Thresholds
- **File Length (`complexity > FileLength`)**:
  - `max`: `500` lines (excluding comments and blank lines).
  - `excludes`: `["**/test/**", "**/androidTest/**"]` (test files exempt).
- **Function / Method Length (`complexity > LongMethod`)**:
  - `threshold`: `100` lines (ignoring Compose `@Composable` preview annotations if applicable).
- **Cyclomatic Complexity (`complexity > ComplexMethod`)**:
  - `threshold`: `15`.
- **Nesting Depth (`complexity > NestedBlockDepth`)**:
  - `threshold`: `5`.

### 3. Clean Code & Hygiene Rules
- **Import Rules (`style > WildcardImport`)**:
  - Disallow wildcard imports (`import foo.bar.*`) to keep imports explicit.
- **Unused Code (`style > UnusedImports`, `style > UnusedPrivateMember`)**:
  - Disallow unused imports and dead private properties/methods.
- **Error Handling (`exceptions > EmptyCatchBlock`)**:
  - Disallow empty catch blocks without comments or logging.

### 4. Codebase Modularization & Compliance
- Refactor any source files in `app/src/main/java/` currently exceeding 500 lines (primarily [`MainScreen.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt)) to ensure 100% compliance with all lint rules.
- Extract file list content or dialog rendering into dedicated sub-components.

### 5. CI Pipeline Enforcement
- Update [`.github/workflows/ci.yml`](file:///Users/personal/AndroidStudioProjects/TagStash/.github/workflows/ci.yml) to add:
  ```yaml
  - name: Run Detekt Lint Checks
    run: ./gradlew detekt
  ```
- Upload Detekt HTML/XML report artifacts on CI runs (`app/build/reports/detekt/`).
- If any rule is violated or a file exceeds 500 lines, CI fails immediately before merging.

---

## 📋 Q&A & Assumptions Preserved
- **Q**: What is the file length threshold?
  - **A**: 500 lines of code (excluding comments and blank lines).
- **Q**: Are test files subject to the 500-line limit?
  - **A**: No, test files (e.g. `MainScreenTest.kt`) are exempted from `FileLength` to allow thorough UI and Roborazzi screenshot test suites.
- **Q**: Which linting tool should be used?
  - **A**: Detekt (`io.gitlab.arturbosch.detekt`) combined with standard Android Lint (`./gradlew lintDebug`).
- **Q**: Should linting fail the build in CI?
  - **A**: Yes, `./gradlew detekt` will be a mandatory gate in the GitHub Actions CI pipeline.

---

## 🔍 Edge Cases & Verification
- **Generated Code / Build Folders**: Exclude `**/build/**`, `**/.gradle/**`, and generated Room / Compose compiler outputs from Detekt.
- **Roborazzi / Preview Annotations**: Ensure test runner and Compose preview setups are not falsely flagged.
- **Zero Violations**: Verify `./gradlew detekt` completes with 0 errors across the entire repository.

---

## 📂 Target Files & Planned Modifications
1. [`gradle/libs.versions.toml`](file:///Users/personal/AndroidStudioProjects/TagStash/gradle/libs.versions.toml):
   - Add `detekt` version and plugin alias.
2. [`build.gradle.kts`](file:///Users/personal/AndroidStudioProjects/TagStash/build.gradle.kts) & [`app/build.gradle.kts`](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts):
   - Apply Detekt plugin and configure detekt task options.
3. `config/detekt/detekt.yml`:
   - Define custom rule configuration (FileLength, WildcardImport, LongMethod, etc.).
4. [`app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt`](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MainScreen.kt):
   - Modularize remaining inline sections to bring file length well below 500 lines.
5. [`.github/workflows/ci.yml`](file:///Users/personal/AndroidStudioProjects/TagStash/.github/workflows/ci.yml):
   - Add `Run Detekt Lint Checks` step and artifact upload.
