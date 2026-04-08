# Maker Manual Report Edit Spec

**Date:** 2026-04-08  
**Owner:** Workflow: doc-to-pr-pipeline

## Problem Statement
Makers currently generate report runs and must immediately submit the raw SQL output. Any ad-hoc adjustments (rounding, commentary, corrections) are impossible prior to the maker-checker handoff, forcing them to export to Excel, edit locally, and lose auditability.

## Goals
1. Allow makers to edit a generated run's snapshot before submission without leaving the system.
2. Persist manual adjustments (data + rationale) with audit trails so checkers see what changed.
3. Preserve maker-only access: only the run owner can edit, and only while the run is still `Generated`.

## Non-goals
- Spreadsheet-like cell diffing or collaborative editing.
- Editing of approved/rejected historical runs.
- Changing maker-checker role logic or submission workflow timing.

## Functional Requirements
1. **Data model**:
   - Augment `report_run` with `manual_note` (TEXT) and `manual_edited_at` (timestamp). The existing `result_snapshot` column will hold the latest (possibly manual) payload.
   - Optional boolean `has_manual_edits` (derived or stored) for quick filtering.
2. **API / Backend**:
   - New endpoint `PUT /api/report-runs/{id}/manual-snapshot` accepting `{ "snapshot": <JSON string or object>, "note": "..." }`.
   - Validations: run exists, status=`Generated`, maker owns run, payload parses to JSON array/object, note optional but capped (e.g., 1,000 chars).
   - Persist snapshot (normalized string), update manual metadata, and record an audit event `ManualEdited` containing the note.
   - Expose manual metadata and `resultSnapshot` in report-run responses used by makers/checkers.
3. **Frontend (Maker)**:
   - In the Maker tab, when a `Generated` run is present, display the current snapshot in an editable JSON textarea with validation + preview table.
   - Provide "保存手工调整" (save) and "提交审批" buttons; save must succeed before submission is allowed.
   - Capture optional note explaining the edit and show last edited timestamp + note.
4. **Frontend (Checker)**:
   - Checker list/detail should highlight when `hasManualEdits=true`, showing the note and edited timestamp in the review panel.
5. **Audit & Security**:
   - Manual edit events appear in `/audit` timeline.
   - Snapshot saves should be idempotent and include optimistic locking (using `@Version`) to avoid overwriting newer edits.

## Acceptance Criteria
- Maker can edit JSON, save, and see confirmation + updated preview.
- Attempting to edit someone else's run, or a non-`Generated` run, returns 403/400.
- After save, checker view exposes the manual note and treats the edited data as the authoritative snapshot.
- Audit trail includes a `ManualEdited` record with maker username, timestamp, and note.
- Unit tests cover service validation + controller request contract; frontend includes UI test or e2e stub for save+submit flow.

## Risks / Mitigations
- **Risk**: malformed JSON stored -> UI break. **Mitigation**: backend parses & re-serializes before persisting to guarantee valid JSON structure.
- **Risk**: race conditions if multiple saves. **Mitigation**: rely on JPA `@Version`; surface 409 to frontend to show "请刷新".
- **Risk**: large snapshots slowing textarea. **Mitigation**: keep current dataset size small; future improvement could use tabular editor.

## Implementation Plan (High Level)
1. Update schema + entity (`schema.sql`, `ReportRun`) with manual fields and versioning support for optimistic locking during edits.
2. Extend `ReportRunRepository`/`Service` with `updateManualSnapshot` logic + audit event creation.
3. Add controller endpoint for manual edits and wire security checks.
4. Frontend: update `ReportService` interfaces; add maker UI for editing + preview, disable submit until save passes; show manual metadata to checkers.
5. Tests: backend service/controller tests for validation; frontend unit test for editor component (or logic method) plus manual QA doc.

## Test Plan
- Backend unit tests for `ReportRunService.saveManualSnapshot` (happy path, invalid status, other maker, invalid JSON, optimistic lock failure).
- Integration smoke via `ReportRunController` manual edit endpoint (MockMvc controller test covering happy path + validation failure).
- Frontend: manual QA script verifying JSON binding & error states (automated component test deferred for now, but submission flow must still block when JSON 未保存/无效).

## Open Questions
- Should we keep the original auto snapshot? (Current plan: not stored separately; rely on audit + Git history. Could add `original_snapshot` later.)
- Maximum payload size for manual edits? (Default to existing column capacity; rely on DB limits.)
