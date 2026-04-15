# Quality Checklist: 报表报送监控看板 — Dashboard UX Requirements Quality

**Purpose**: Validate completeness, clarity, and consistency of dashboard UX requirements
**Created**: 2026-04-16
**Feature**: [spec.md](../spec.md)
**Depth**: Standard
**Audience**: Reviewer (PR)
**Focus Areas**: UX Completeness, Data Display Clarity, Interaction Coverage

## Requirement Completeness

- [x] CHK001 - Are all card/list display fields explicitly specified (name, description, deadline, remaining days, status)? [Completeness, Spec §FR-003]
- [x] CHK002 - Are empty state requirements defined for when no scheduled reports exist? [Completeness, Spec §US1-AS2]
- [x] CHK003 - Are sorting/ordering requirements explicitly defined for the dashboard list? [Completeness, Spec §FR-007]
- [x] CHK004 - Are role-specific action button requirements documented (Maker vs Checker)? [Completeness, Spec §Assumptions]
- [x] CHK005 - Are the report schedule entity attributes fully specified (deadline, frequency, enabled flag)? [Completeness, Spec §Key Entities]

## Requirement Clarity

- [x] CHK006 - Is the 7-day threshold for "approaching deadline" explicitly quantified (inclusive)? [Clarity, Spec §FR-004]
- [x] CHK007 - Are the color-coding rules unambiguous (orange/yellow for approaching, red for overdue, green for complete)? [Clarity, Spec §FR-004/005/006]
- [x] CHK008 - Is "current cycle" for periodic reports clearly defined with start/end boundaries? [Clarity, Spec §Assumptions]
- [x] CHK009 - Are the frequency types explicitly enumerated (ONCE/DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY)? [Clarity, Spec §Assumptions]
- [x] CHK010 - Is the status derivation logic from ReportRun states to dashboard display states clearly mapped? [Clarity, Spec §FR-011]

## Requirement Consistency

- [x] CHK011 - Are deadline-day semantics consistent — spec says "today = approaching, not overdue"? [Consistency, Spec §Edge Cases]
- [x] CHK012 - Is the Submitted-but-not-yet-decided status consistent between edge cases and FR-011? [Consistency, Spec §Edge Cases + FR-011]
- [x] CHK013 - Are access requirements consistent — all roles can view but actions differ? [Consistency, Spec §FR-001 + Assumptions]

## Scenario Coverage

- [x] CHK014 - Are requirements defined for the "Rejected → re-execute" scenario using latest Run? [Coverage, Spec §Edge Cases]
- [x] CHK015 - Are requirements defined for deleted reports being filtered out? [Coverage, Spec §FR-010]
- [x] CHK016 - Are requirements defined for periodic deadline auto-rollover after approval? [Coverage, Spec §FR-009]
- [x] CHK017 - Are requirements defined for multiple reports sharing the same deadline date? [Coverage, Spec §Edge Cases]

## Edge Case Coverage

- [x] CHK018 - Is behavior specified for a report with deadline today but no Run yet? [Edge Case, Spec §Edge Cases]
- [x] CHK019 - Is behavior specified for a report submitted on deadline day pending approval? [Edge Case, Spec §Edge Cases]
- [x] CHK020 - Are requirements specified for what happens when a periodic schedule is disabled mid-cycle? [Edge Case, Spec §Edge Cases]
- [x] CHK021 - Are requirements specified for dashboard behavior when server time/timezone differs from user expectation? [Edge Case, Spec §Edge Cases]

## Non-Functional Requirements

- [x] CHK022 - Are performance requirements specified for dashboard load time (≤2s for 50 schedules)? [Clarity, Spec §SC-005]
- [x] CHK023 - Are pagination or virtual-scroll requirements defined for large numbers of report schedules? [Completeness, Spec §Edge Cases]
- [x] CHK024 - Are navigation depth requirements specified (≤3 clicks to reach dashboard)? [Clarity, Spec §SC-001]

## Notes

- CHK020: Spec does not address disabling a schedule mid-cycle. Recommend adding: "When a schedule is disabled, it disappears from the dashboard immediately."
- CHK021: Spec does not define timezone handling. Recommend assuming server-local date for deadline comparison.
- CHK023: Current spec targets 50 schedules (SC-005). If scale grows, pagination may be needed. Acceptable for v1.
- All items now pass after spec updates addressing disabled schedules, timezone handling, and pagination.
