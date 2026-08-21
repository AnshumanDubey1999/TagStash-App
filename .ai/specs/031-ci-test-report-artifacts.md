# Specification: 031-ci-test-report-artifacts

**Status**: DONE

---

## 📌 Background & Context
When unit and UI tests fail on GitHub Actions CI runners, the console log only shows summary failure lines and truncates stack traces or HTML visual reports. To diagnose CI-specific test failures and visual regressions effectively, test execution outputs (HTML reports, XML test results, and Roborazzi screenshot captures/reports) must be extracted and uploaded as downloadable workflow artifacts even when Gradle tasks fail.

---

## 🎯 Requirements & Scope
- **Always Upload on Completion**:
  - Test artifact upload steps must run with `if: always()` condition so reports are uploaded whether tests pass, fail, or encounter build exceptions.
- **HTML Test Reports**:
  - Upload Gradle HTML test reports directory: `app/build/reports/tests/` as artifact `unit-test-reports`.
- **XML Test Results**:
  - Upload Gradle XML test result files: `app/build/test-results/` as artifact `unit-test-results`.
- **Roborazzi Screenshot Outputs**:
  - Upload Roborazzi screenshot outputs and report directories (`app/screenshots/`, `app/build/outputs/roborazzi/`, `app/build/reports/roborazzi/`) as artifact `roborazzi-screenshots`.
- **Workflow Versioning**:
  - Use `actions/upload-artifact@v4` adhering to current GitHub Actions standard.
- **Retention**:
  - Set artifact retention period (e.g., 14 days) to ensure diagnostic logs are accessible without consuming unnecessary storage quota.

---

## 📋 Q&A & Assumptions Preserved
- **Q**: Which test reporting artifacts and summaries should be included in the CI pipeline?
  - **A**: Upload HTML test reports (`app/build/reports/tests/`), XML results (`app/build/test-results/`), and Roborazzi screenshot outputs on `always()`.

---

## 🔍 Edge Cases & Verification
- **Test Task Failure**: When `./gradlew test` exits with code 1, `if: always()` ensures subsequent upload steps still execute.
- **Missing Directories**: Set `if-no-files-found: warn` so that if a directory was not generated (e.g. build failure before test step), the workflow does not fail on the upload step itself.

---

## 📂 Target Files & Planned Modifications
- [`.github/workflows/ci.yml`](file:///Users/personal/AndroidStudioProjects/TagStash/.github/workflows/ci.yml):
  - Add `Upload Unit Test HTML Reports` step using `actions/upload-artifact@v4` targeting `app/build/reports/tests/` with `if: always()`.
  - Add `Upload Unit Test XML Results` step using `actions/upload-artifact@v4` targeting `app/build/test-results/` with `if: always()`.
  - Add `Upload Roborazzi Screenshots & Outputs` step using `actions/upload-artifact@v4` targeting `app/screenshots/`, `app/build/outputs/roborazzi/`, and `app/build/reports/roborazzi/` with `if: always()`.
