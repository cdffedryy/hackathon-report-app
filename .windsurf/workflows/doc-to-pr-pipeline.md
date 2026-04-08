---
description: end-to-end workflow for turning a requirements doc into a merged PR using the doc-to-pr-pipeline skill
---

1. **Phase 0 — Repository & Tooling Preconditions**
   1. Make sure you are inside the target git repo.
   2. Verify GitHub CLI auth works:
      ```powershell
      gh auth status
      ```
   3. Inspect remotes and current branch so you know the default base and upstream:
      ```powershell
      git remote -v
      git branch --show-current
      ```

2. **Phase 1 — Planning (Planner role)**
   1. Run Spec Kit (`spec-kit init` / `spec-kit update`) to generate a new spec file under `spec/` (e.g., `spec/<slug>-<date>.md`). Capture requirements, assumptions, risks, and explicit tasks.
   2. Share the spec summary in chat and commit the spec before implementation begins.

3. **Phase 2 — Implementation & Local Validation (Executor/Tester roles)**
   1. Checkout a feature branch derived from the spec summary:
      ```powershell
      git checkout -b feature/<slug>-<yyyymmdd>
      ```
   2. Implement tasks iteratively, keeping tests alongside code. After each coherent change:
      - run the appropriate test/format commands (keep coverage >70% target in mind)
      - stage and commit, squashing as needed before handoff:
        ```powershell
        git add -A
        git commit -m "<concise change description>"
        ```
   3. Keep rerunning the project's test suite and capture coverage numbers locally. Address failures before moving on.

4. **Phase 3 — Push & Create/Update PR (Executor role)**
   1. Ensure commits are squashed/fixup as required, then push the feature branch (same name) to origin:
      ```powershell
      git push -u origin HEAD
      ```
   2. Open or update the PR targeting `main` and include spec summary + test results in the PR body:
      ```powershell
      gh pr create --title "<title>" --body "<summary + tests>" --base main --head <branch>
      ```
      - If a PR already exists, skip creation and continue pushing updates.

5. **Phase 4 — Review Loop (Viewer/Reviewer roles)**
   1. Viewer inspects the PR via `gh pr view`/`gh pr diff` (or the `gh-pr-review` skill) and leaves explicit inline comments for all issues.
   2. Viewer signals the executor when the review pass is complete.
   3. Executor implements fixes, reruns tests, squashes commits, and pushes to origin.
   4. Viewer repeats review until there are zero unresolved comments and coverage remains ≥70%.

6. **Phase 5 — Quality Gates & Completion Output**
   1. Confirm all automated/manual tests pass.
   2. Ensure global coverage > 70% and capture the numeric output in chat.
   3. If you need to review artifacts under gitignored directories (e.g., `build/reports/tests`), read them via PowerShell `Get-Content` instead of IDE viewers.
   4. Verify there are zero unresolved review comments via `gh pr view`.
   5. Ensure the `spec/` document reflects the delivered work.
   6. When ready to finalize, provide to the user:
      - PR URL
      - Summary of key changes
      - How to run tests locally
      - Latest coverage number
      - Any deferred/non-blocking follow-ups
