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
   > If the `spec/` directory does not exist yet, run `specify init . --here --force --no-git` once to scaffold it.

   1. **Generate requirements** — Run `/speckit.specify` with the user-provided feature description.
      - This creates a feature branch, generates `spec/<slug>-<yyyymmdd>.md` with structured requirements (functional requirements, user stories, success criteria, edge cases, etc.).
      - Review and resolve any `[NEEDS CLARIFICATION]` markers interactively.
      - Output: `spec/<slug>-<yyyymmdd>.md` with the **Requirement** sections populated.

   2. **Generate quality checklist** — Based on step 1 output, run `/speckit.checklist` against the generated spec file.
      - Uses the feature branch and `FEATURE_DIR` created by `/speckit.specify` in step 1.
      - All checklist items (requirements completeness, clarity, consistency, coverage, etc.) are consolidated into **a single file** under `quality-check/`, with the same filename as the spec (e.g., `spec/manual-report-edit-20260408.md` → `quality-check/manual-report-edit-20260408.md`).
      - Ensure all checklist items pass before proceeding; update the spec if any items fail.

   3. **Generate technical plan** — Based on step 1 output, run `/speckit.plan` against the generated spec file.
      - Uses the same feature branch and `FEATURE_SPEC` created by `/speckit.specify` in step 1.
      - This generates `plan.md` (architecture, tech stack, phases), `research.md` (decisions), `data-model.md` (entities), and `contracts/` (interface contracts) inside `FEATURE_DIR`.
      - The technical plan content is written into the spec file's **Implementation Plan** section as a summary, with detailed artifacts in the feature directory.
      - Validates against the project constitution (`.specify/memory/constitution.md`).

   4. **Create/update pipeline checklist** — Immediately create a local workflow checklist:
      - Store it under `checklist/<spec-file-name>.md` so it matches the spec filename (e.g., `spec/manual-report-edit-20260408.md` ↔ `checklist/manual-report-edit-20260408.md`). The `checklist/` directory is gitignored on purpose so you can freely mark progress.
      - Use Markdown checkboxes for the canonical pipeline tasks Cascade must finish:
        ```markdown
        - [ ] Requirements generated via /speckit.specify
        - [ ] Quality checklist generated via /speckit.checklist
        - [ ] Technical plan generated via /speckit.plan
        - [ ] Code implemented per spec
        - [ ] Tests updated/passing locally
        - [ ] Feature branch pushed & PR updated
        - [ ] /review workflow run with inline comments
        - [ ] Review comments resolved & tests rerun
        - [ ] Final hand-off summary posted
        ```
   5. Keep the checklist updated throughout the workflow (mark items `[x]` once complete). Do not end the workflow until every required checkbox is marked. Checklist files must stay local (never `git add`/commit them)—if you run `git add .`, follow up with `git reset checklist/` before committing.
      - Checklist completion is a hard gate: if any item remains unchecked, continue working (code/tests/docs/push) until all entries are `[x]` before moving to the next phase or closing the task.
   6. Mirror the finalized requirements into `docs/maker-checker-spec.md` so the canonical system spec stays current (summaries, API updates, risks, etc.). Mention in chat which sections changed.
   7. Share the spec summary in chat and commit the spec before implementation begins.

3. **Phase 2 — Implementation & Local Validation (Executor/Tester roles)**
   1. **Generate task breakdown** — Run `/speckit.tasks` based on Phase 1 output (`FEATURE_SPEC` + `plan.md`).
      - Uses the feature branch and `FEATURE_DIR` from Phase 1.
      - Produces `tasks.md` with dependency-ordered, checklist-formatted tasks organized by user story.
      - Optionally run `/speckit.analyze` to validate cross-artifact consistency before implementation.

   2. **Execute implementation** — Run `/speckit.implement` to implement all tasks from `tasks.md`.
      - Reads `tasks.md`, `plan.md`, `data-model.md`, `contracts/` from `FEATURE_DIR`.
      - Checks all quality checklists from Phase 1; prompts if any are incomplete.
      - Executes tasks phase-by-phase, marks each `[X]` on completion.
      - After each coherent change, stage and commit:
        ```powershell
        git add -A
        git commit -m "<concise change description>"
        ```
      - Keep coverage > 70% target in mind; address test failures before moving on.

   3. **Post-implementation quality gate** — Verify all work is complete before proceeding:
      - Check `tasks.md`: all tasks must be marked `[X]`. If any remain unchecked, loop back to step 2.
      - Check `quality-check/<spec-file-name>.md` (generated in Phase 1 step 2): scan the quality checklist and confirm all items pass. If any fail, update the implementation or spec accordingly and re-verify.
      - Check pipeline checklist (`checklist/<spec-file-name>.md`): mark "Code implemented per spec" and "Tests updated/passing locally" as `[x]`.
      - Run `/speckit.analyze` for a final cross-artifact consistency check across `spec.md`, `plan.md`, and `tasks.md`. Address any CRITICAL or HIGH issues before moving on.
      - **Hard gate**: Do not proceed to Phase 3 until all tasks, quality checklists, and pipeline checklist items for this phase are complete.

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
