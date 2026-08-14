# Specification: 024-release-keystore-signing

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: How will the release signing key be integrated into the project?
  - **A**: Using Option B (CI/CD automated signing with GitHub Actions repository secrets):
    1. The keystore (`.jks`) is encoded as a Base64 string in GitHub Secret `KEYSTORE_BASE64`.
    2. Credentials are stored in GitHub Secrets: `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
    3. The GitHub Actions release workflow decodes `KEYSTORE_BASE64` into a temporary `app/release.jks` file and passes credentials via environment variables during `./gradlew assembleRelease`.
    4. The temporary keystore file is automatically deleted after the build step for security.
- **Q**: What should happen if the release keystore or credentials are missing during a release build?
  - **A**: Fail the release build immediately to prevent accidental deployment of un-signed or debug-signed APKs.
- **Q**: How will local development builds work?
  - **A**: Debug builds (`assembleDebug`, `test`, `check`) continue to use the default Android debug keystore without requiring release secrets. Developers who wish to build signed release APKs locally can provide the environment variables or declare them in their private `gradle.properties` / `local.properties`.

---

## 🛠️ Feature Requirements
- Update [app/build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts):
  - Add `signingConfigs.create("release")` reading `RELEASE_KEYSTORE_FILE`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` from environment variables or project properties.
  - Assign `signingConfig = signingConfigs.getByName("release")` to `buildTypes.release`.
  - Validate that `storeFile` and passwords exist when packaging release builds, failing with a clear error message if missing.
- Update [.github/workflows/release.yml](file:///Users/personal/AndroidStudioProjects/TagStash/.github/workflows/release.yml):
  - Add a step to decode `secrets.KEYSTORE_BASE64` to `app/release.jks`.
  - Pass the signing environment variables into the `Build Release APK` step.
  - Add a cleanup step to delete `app/release.jks` upon job completion (even if build fails).

---

## 🔐 GitHub Secrets Configuration (For User Setup)
In the GitHub repository settings (`Settings > Secrets and variables > Actions`), add the following Repository Secrets:
1. `KEYSTORE_BASE64`: Output of `base64 -i <path-to-keystore>.jks`
2. `RELEASE_KEYSTORE_PASSWORD`: Keystore password
3. `RELEASE_KEY_ALIAS`: Key alias (e.g. `tagstash_key`)
4. `RELEASE_KEY_PASSWORD`: Key password

---

## 🧪 Edge Cases & Tests
- **Local Debug Compilation**: `./gradlew compileDebugKotlin`, `./gradlew test`, and `./gradlew check` must pass without requiring release secrets.
- **Release Build Validation**: Running `./gradlew assembleRelease` without secrets should fail with a descriptive message indicating the missing keystore/credentials.
- **CI Cleanup**: Ensure temporary keystore decoded in CI runner is deleted after build execution (`always()` condition).

---

## 🚀 Expected Code Changes
- Modify [app/build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts)
- Modify [.github/workflows/release.yml](file:///Users/personal/AndroidStudioProjects/TagStash/.github/workflows/release.yml)
