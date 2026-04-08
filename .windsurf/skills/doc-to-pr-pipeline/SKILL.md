---
name: doc-to-pr-pipeline
description: Convert a requirements document into an executable plan, implement code, generate tests, reach >70% coverage, and open a PR with iterative review/comment resolution. Use when you want an end-to-end agentic workflow from doc -> spec -> code -> tests -> commit -> push -> PR -> review loop until all comments resolved and tests pass.
metadata:
---

# Doc-to-PR Pipeline Skill

You run a multi-role workflow (planner, executor, tester, viewer, reviewer) that takes a requirements document and produces a GitHub PR. You iterate until:

1. All tests pass
2. Test coverage > 70%
3. No unresolved review comments remain

## Agents

This skill is split into role-specific agent documents:

- `agents/planner.md`
- `agents/executor.md`
- `agents/tester.md`
- `agents/viewer.md`

## Inputs

- Requirements document provided by the user (inline text or a file path)
- Target repository (local working directory)
- Target branch base (default: repository default branch)

## Global Rules

- Never log or commit secrets (tokens, keys, credentials). If the requirements include secrets, redact and ask the user for a secure handling approach.
- Prefer minimal, incremental commits.
- Keep commands cross-platform; this user is on Windows PowerShell by default.
- If any external command requires confirmation in this environment, ask the user to approve execution.
- When you need to inspect artifacts under gitignored directories (e.g., `build/reports`), use PowerShell `Get-Content` instead of the IDE file viewer, which respects `.gitignore`.
- Every workflow run must produce a checked-in spec document inside `spec/` generated via the Spec Kit tooling during Phase 1.

## Role Overview

The detailed responsibilities and checklists for each role live in the `agents/` files listed above.

## End-to-End Workflow

### Phase 0: Repository & Tooling Preconditions

1. Confirm current directory is a git repo
2. Confirm GitHub auth for `gh` is available:

```powershell
gh auth status
```

3. Determine default branch:

```powershell
git remote -v
git branch --show-current
```

### Phase 1: Planning

- Planner runs Spec Kit to create/update `spec/<ticket-or-date>.md`, capturing assumptions, acceptance criteria, risks, and explicit task breakdown. Commit this spec immediately.
- Summaries shared in chat should mirror the written spec to keep executor/viewer aligned.
- Spec Kit usage:
  - Initialize Spec Kit in the repository root
  - Run `spec-kit init` to create a new spec document
  - Use `spec-kit update` to update an existing spec document
  - Use `spec-kit validate` to validate the spec document

### Phase 2: Implementation & Local Validation

Executor:

1. Create branch

```powershell
git checkout -b feature/<slug>-<yyyymmdd>
```

2. Implement tasks incrementally
3. Run tests + coverage frequently
4. Commit after each coherent change

```powershell
git add -A
git commit -m "<message>"
```

### Phase 3: Create/Update PR

- Once tests pass locally, executor squashes work as directed and pushes the feature branch to origin with the same name.
- Open or update the PR (base `main` unless instructed otherwise) using `gh pr create`. Include the spec summary and testing notes in the PR body.

### Phase 4: Review Loop (Viewer/Reviewer)

- Viewer uses `gh pr view` + `gh pr diff` (or the gh-pr-review skill) to inspect the PR, and every review comment must be an inline GitHub comment referencing the specific file + line(s). Use `gh pr review [<number>|<url>|<branch>] --comment --body "..." --path <relative-path> --line <line> --side right` (or equivalent tooling) so executors can jump directly to the issue.
- After finishing a review pass, the viewer notifies the executor to resume work based on the comments.
- When the reviewer agrees the PR is ready, they must leave a final “no-questions” comment (e.g., `gh pr review --comment --body "LGTM"`) to document approval before exiting the workflow.
- Executor addresses feedback, re-runs tests, then squashes the updated commits before pushing to the same remote branch.
- Viewer repeats the review process until no unresolved comments remain and test coverage is ≥ 70%.

### Phase 5: Quality Gates

Before finishing, ensure:

- Tests pass (command depends on stack)
- Coverage > 70% (capture the numeric output in chat)
- `gh pr view` shows zero unresolved review comments
- Spec in `spec/` reflects the delivered scope

If any gate fails, loop back to the relevant phase.

## Completion Output

When finished, provide:

- PR URL
- Summary of changes
- How to run tests locally
- Coverage result
- Any follow-ups deferred (non-blocking)
