# Spec 007: Java Upgrade and CI Pipeline Workflow

- **Status**: DONE
- **Date**: 2026-07-10

---

## 📋 Q&A and Assumptions
* **Q**: What Java version should the app compilation target?
  * **A**: Java 17 (`JavaVersion.VERSION_17` and `jvmTarget = "17"`). This is natively supported by the project's `minSdk 33` without requiring extra desugaring.
* **Q**: What Java version should the build tooling and CI runner use?
  * **A**: Java 21. This provides the fastest compilation performance and matches the modern Gradle 8.13 capabilities.
* **Q**: What triggers the CI pipeline?
  * **A**: Triggers on pushes to the `main` branch and pull requests targeting the `main` branch.
* **Q**: What Gradle tasks should run in the pipeline?
  * **A**: `./gradlew assembleDebug` (build check) and `./gradlew test` (unit/UI testing).
* **Assumptions**:
  * The GitHub Actions environment has access to the standard `ubuntu-latest` runner.
  * Caching is utilized for Gradle dependencies to optimize pipeline runtimes.

## 📋 Feature Requirements
1. **Java 17 Target Upgrade**:
   * Update `compileOptions` in `app/build.gradle.kts` to use `JavaVersion.VERSION_17` for both `sourceCompatibility` and `targetCompatibility`.
   * Update `kotlinOptions` in `app/build.gradle.kts` to set `jvmTarget = "17"`.

2. **GitHub Actions CI Configuration**:
   * Create a workflow file `.github/workflows/ci.yml`.
   * Trigger on:
     * `push` to `main`.
     * `pull_request` targeting `main`.
   * Steps in workflow:
     * Check out repository using `actions/checkout@v4`.
     * Set up Java JDK 21 using `actions/setup-java@v4` (using `temurin` distribution).
     * Set up and cache Gradle using `gradle/actions/setup-gradle@v4` (handles dependency and build caching automatically).
     * Make `./gradlew` executable.
     * Run `./gradlew assembleDebug` to compile and build the debug APK.
     * Run `./gradlew test` to execute all unit and screenshot tests.

## 📝 Expected Code Changes
* [app/build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts): Update source/target compatibility and Kotlin JVM target to `17`.
* `.github/workflows/ci.yml`: Create the new GitHub Actions workflow configuration.

## 🧪 Testing Strategy
* Run `./gradlew compileDebugKotlin` locally with local JDK (or verified with JDK 17/21 compatibility) to verify build success.
* Run `./gradlew test` locally to verify tests run and pass on JVM 17.
* Validate the YAML syntax of `.github/workflows/ci.yml`.
