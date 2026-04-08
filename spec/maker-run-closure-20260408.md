# Maker Run Closure & Reopen Spec

**Date:** 2026-04-08  
**Owner:** Workflow: doc-to-pr-pipeline

## Problem Statement
Makers can only move a run from `Generated` to `Submitted`. Once submitted, they must wait for checker action—even if they notice issues (wrong parameters, manual edits needed). There is no way to withdraw a run that is still in progress, nor a path to reopen editing without creating an entirely new run. This blocks maker iteration, keeps incorrect runs in the checker queue, and produces noisy audit trails.

## Goals
1. Allow makers to **close (withdraw)** a run prior to approval, marking it `Closed` so checkers stop seeing it.
2. Permit makers to **re-open a closed run for further editing** (returns to `Generated`), so they can fix manual snapshots or rerun data without recreating the run.
3. Record closure/reopen actions in audit history and reflect status across maker/checker UIs & APIs.

## Non-goals
- Allowing closure after `Approved` or `Rejected` (completed lifecycle stays immutable).
- Auto-reopening runs after some time delay; action remains manual by the maker.
- Changing the existing maker-checker authorization model.

## Functional Requirements
1. **Data Model & Status Flow**
   - Introduce a new run status `Closed`. Valid transitions:
     - `Generated → Submitted` (unchanged).
     - `Submitted → Closed` (maker withdraws before checker decision).
     - `Closed → Generated` (maker reopens for editing/submission).
     - `Generated → Closed` (maker can close before initial submission).
   - Add fields to `report_run`:
     - `closed_at` (timestamp, nullable).
     - `closed_reason` (VARCHAR/TEXT, optional maker note, max 500 chars).
     - `reopened_at` (timestamp, nullable) for latest reopen.
   - `has_manual_edits` continues to describe snapshot edits (unchanged).
2. **Backend APIs**
   - New endpoint `POST /api/report-runs/{id}/close` body `{ "reason": "..." }` (reason optional, trim + length check).
     - Validations: run exists, maker owns it, status ∈ {`Generated`, `Submitted`}, status ≠ `Approved`/`Rejected`/`Closed`.
     - Effects: set status=`Closed`, set `closed_at=now`, `closed_reason`, clear `submitted_at` (so reopened run must re-submit), remove run from checker queue.
     - Audit: record event `Closed` with reason (if any).
   - New endpoint `POST /api/report-runs/{id}/reopen`
     - Validations: maker owns it, current status=`Closed`.
     - Effects: status→`Generated`, `reopened_at=now`, `submitted_at` set null, `decided_at` null, keep existing manual snapshot so maker can continue editing.
     - Audit event `Reopened` capturing timestamp + maker note (optional request field?). To keep symmetry, allow body `{ "note": "..." }` for reopen comment.
   - When a run is closed, `getSubmittedRuns` must exclude it (e.g., query only `status='Submitted'`). Ensure repository queries align.
3. **Frontend Maker UI**
   - In maker panel:
     - When `currentRun.status` is `Generated`, show a "关闭运行" button (with optional reason modal/textarea). Closing should reset UI to indicate no active run or mark as Closed.
     - When `status` is `Submitted`, show "撤回并关闭" button; block action if manual edits unsaved? (closing shouldn't require valid JSON but should warn unsaved changes will be lost?). At minimum, confirm action.
     - When `status` is `Closed`, show a callout plus "重新编辑" button to reopen (returns to `Generated`). After reopen, manual grid remains editable and manualDirty resets.
     - Provide reason input (modal or inline text area) reused for both close + reopen.
   - Maker history table should display `Closed` runs with timestamps and reasons.
4. **Frontend Checker UI**
   - `待审批列表` (`checkerRuns`) should automatically drop closed runs (since backend query excludes).
   - If a run appears in checker history, show `Closed` status and reason in detail table and audit timeline.
5. **Audit Trail**
   - `/api/report-runs/{id}/audit` responses include events `Closed` and `Reopened`, showing actor (maker), timestamp, and reason/note.
6. **Security & Validation**
   - Maker-only actions: both endpoints require `MAKER` role and run ownership.
   - Guard against reopening once checker has approved/rejected (since status would no longer be `Closed`).
   - When closing from `Submitted`, ensure any `checkerUsername` fields or assignments are preserved but the run leaves the queue (no deletion).

## Acceptance Criteria
- Maker can close a `Generated` run; status switches to `Closed`, and the run disappears from checker pending list.
- Maker can close a `Submitted` run provided it is not `Approved` yet; checker pending list reflects removal.
- Maker can re-open a previously closed run, returning it to `Generated`, with manual snapshot preserved and ready for edits/re-submission.
- Audit log shows `Closed`/`Reopened` entries with the maker’s username and reason/note if provided.
- Attempts to close an `Approved` or `Rejected` run respond with 400/409.
- Reopen API rejects when run is not `Closed` or maker mismatch.

## Risks / Mitigations
- **Risk**: Reopening might resurrect stale data. *Mitigation*: keep audit trail + require maker to resubmit explicitly.
- **Risk**: Checker might already be reviewing when maker closes. *Mitigation*: closing removes it from `/submitted` list immediately; audit log clarifies maker action.
- **Risk**: Multiple close/reopen cycles causing confusion. *Mitigation*: audit entries capture each cycle with timestamps; optional reason encourages documentation.

## Implementation Plan (High Level)
1. **Backend**
   - Update `ReportRun` entity + migration (`schema.sql`) for new fields/status values.
   - Add service methods `closeRun` and `reopenRun` with validation + audit logging + Prometheus counters (if needed).
   - Expose controller endpoints and update repository queries (`findSubmittedRuns`) to filter by status.
2. **Frontend Maker**
   - Extend `ReportService` with `closeRun` / `reopenRun` calls.
   - Update `ReportViewerComponent` UI state machine: new buttons, reason dialog, status handling, state resets when reopened.
3. **Frontend Checker**
   - Ensure lists filter by status; update history view to show `Closed` reason/time.
4. **Docs & Workflow**
   - Update `docs/maker-checker-spec.md` statuses and API list.
   - Extend QA checklist/spec to cover close/reopen flow.
5. **Testing**
   - Backend unit tests for close/reopen flows (ownership, status guards, audit events).
   - Controller integration tests for new endpoints.
   - Frontend manual QA/automated test for button visibility and optimistic flow.

## Test Plan
- Backend service tests covering:
  - Close from Generated & Submitted success paths.
  - Close rejection when run is Approved/Rejected/Closed or belongs to another maker.
  - Reopen success & failure cases (wrong owner, not Closed).
- Controller MockMvc tests for new endpoints (happy path + validation errors).
- Frontend: manual QA script verifying button availability per status, reason capture, audit display, and checker list refresh.

## Open Questions
- Should closing from `Submitted` also reset `checkerUsername` assignment? (Current plan: keep for traceability, but open to change.)
- Do we need separate metrics counters for closed/reopened runs?
- Should reopen require the maker to re-run the report, or is editing existing snapshot sufficient? Currently planning to reuse existing snapshot.
