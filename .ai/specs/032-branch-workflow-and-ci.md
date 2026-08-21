# Specification: 032-branch-workflow-and-ci

**Status**: DONE

---

## 📌 Background & Context
To improve development safety, code review hygiene, and test validation, TagStash is transitioning to a strict **branch-and-PR-based workflow**.
- Direct commits to the `main` branch will be prohibited for the Agent.
- Every new specification must be developed on an isolated feature branch branched from up-to-date `main`.
- The Agent is empowered and encouraged to make frequent, small, focused commits on the feature branch.
- Pushing to GitHub, opening Pull Requests, code reviewing, and merging to `main` remains the user's responsibility.
- The CI pipeline will automatically execute on any Pull Request opened or updated against `main`.

---

## 🎯 Requirements & Scope

### 1. Agent Workflow & Rules in [`.agents/AGENTS.md`](file:///Users/personal/AndroidStudioProjects/TagStash/.agents/AGENTS.md)
- **Branch Creation Before Work**:
  - Whenever a new spec begins (transitions to `IN_PROGRESS`), checkout `main`, ensure it is up to date (`git pull --rebase`), and create a new feature branch named `<spec-num>-<short-description>` (e.g. `033-media-player-playlists`).
- **Never Commit to `main`**:
  - Agent must never commit or push directly to `main`. Commits to `main` happen solely via GitHub Pull Request merges performed by the user.
- **Encourage Small & Frequent Commits**:
  - Agent is permitted and encouraged to make multiple incremental, logical commits directly on the feature branch during implementation without needing explicit confirmation per micro-commit.
  - Every commit message must begin with `[<spec-num>]` (e.g., `[032] Add CI concurrency control`).
- **Post-Implementation Hand-Off**:
  - Once implementation and local checks (`./gradlew test`) pass, verify all changes on the feature branch are committed.
  - Request user permission to mark the spec state as `DONE`.
  - Provide a ready-to-use **Pull Request description** formatted according to the `.github/pull_request_template.md` so the user can easily paste it into GitHub when creating the PR.
  - User pushes the branch, opens the GitHub Pull Request, inspects CI run, and merges to `main`.

### 2. CI Pipeline Enhancements in [`.github/workflows/ci.yml`](file:///Users/personal/AndroidStudioProjects/TagStash/.github/workflows/ci.yml)
- **Pull Request Triggering**:
  - Run on `push` to `main` (post-merge verification) and on `pull_request` targeting `main` (for any feature branch PR).
- **Concurrency Control**:
  - Add `concurrency` block with `cancel-in-progress: true` so that pushing new commits to an open PR automatically cancels outdated in-progress CI runs.
- **Report & Screenshot Artifacts**:
  - Preserve the always-upload behavior for `unit-test-reports`, `unit-test-results`, and `roborazzi-screenshots`.

### 3. Standard Pull Request Template in [`.github/pull_request_template.md`](file:///Users/personal/AndroidStudioProjects/TagStash/.github/pull_request_template.md)
- Provide a clear GitHub PR template containing:
  - Related Spec Reference (e.g. `Spec: 032`).
  - Summary of changes implemented.
  - Testing checklist (Unit tests, UI tests, Roborazzi screenshots).

---

## 💡 Recommended GitHub Repository Settings (For User)
To enforce this workflow on GitHub:
1. **Branch Protection on `main`** (`Settings -> Branches -> Branch protection rules`):
   - Check **"Require a pull request before merging"** (1 approval).
   - Check **"Require status checks to pass before merging"** $\rightarrow$ select `Build & Test` status check.
   - Check **"Require branches to be up to date before merging"**.
   - Check **"Do not allow bypassing the above settings"**.
2. **Pull Request Merging Settings** (`Settings -> General -> Pull Requests`):
   - Enable **"Allow squash merging"** or **"Allow rebase merging"** according to repository preference.
   - Enable **"Automatically delete head branches"** to keep the repo tidy after PR merge.

---

## 📋 Q&A & Assumptions Preserved
- **Q**: How are branches named?
  - **A**: Format is `<spec-num>-<short-description>` (e.g., `032-branch-workflow-and-ci`).
- **Q**: What actions are Agent vs User responsibilities?
  - **A**: Agent creates local branch, writes code/tests, makes small commits on branch, and updates spec. User pushes branch to remote, creates PR, reviews, and merges into `main`.

---

## 📂 Target Files & Planned Modifications
1. [`.agents/AGENTS.md`](file:///Users/personal/AndroidStudioProjects/TagStash/.agents/AGENTS.md):
   - Update Git Commits and Spec-Driven Development Workflow sections to mandate branch creation, never committing to `main`, and encouraging small commits on feature branches.
2. [`.github/workflows/ci.yml`](file:///Users/personal/AndroidStudioProjects/TagStash/.github/workflows/ci.yml):
   - Add concurrency cancellation for PRs and ensure triggers cover PRs targeting `main`.
3. [`.github/pull_request_template.md`](file:///Users/personal/AndroidStudioProjects/TagStash/.github/pull_request_template.md):
   - Create standard PR description template linking spec, change summary, and test checklists.
