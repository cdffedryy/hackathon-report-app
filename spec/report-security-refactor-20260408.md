# Report System SQL Security Refactor Spec

**Date:** 2026-04-08
**Owner:** Backend Security Refactor Initiative

## Problem Statement
Existing report execution flow concatenates user-provided SQL fragments directly, exposing the maker-checker workflow to SQL injection and unauthorized data exfiltration.

## Goals
1. Restrict executable statements to vetted SELECT queries only.
2. Enforce per-report parameter templates with whitelisted columns/operators/values.
3. Add centralized configuration for allowed tables, forbidden tokens, and SQL length limits.
4. Provide service/controller-level validation and meaningful error handling.
5. Cover new security logic with unit tests.

## Non-Goals
- Changing maker/checker roles or audit storage semantics.
- Frontend UX updates beyond accepting structured parameter payloads.

## Functional Requirements
- `ReportSecurityProperties` must define allowed tables, forbidden tokens, and max SQL length via `application.yml`.
- `ReportParameterTemplateProperties` must map report IDs to filter definitions (name, column, operator, allowed values).
- DAO must support named-parameter execution to prevent literal substitution.
- `ReportService` must validate SQL before execution and build safe WHERE clauses using templates.
- Controllers must validate request payloads and reject ad-hoc SQL fragments.

## Acceptance Criteria
- Submitting a report run with disallowed tokens returns a `ReportSecurityException`.
- Missing or unknown filters are rejected with actionable error messages.
- Parameter templates exist for key reports (IDs 1 and 7) with starter filters.
- Unit test coverage includes validator happy-path + failure cases.

## Risks & Mitigations
- **Risk:** Legitimate reports referencing new tables. **Mitigation:** allowlist is configurable via YAML.
- **Risk:** Parameter template drift vs. data.sql definitions. **Mitigation:** central spec and follow-up governance.

## Task Breakdown
1. Create security/parameter configuration classes and exception type.
2. Implement SQL validator + parameter builder utilities.
3. Refactor DAO/service/controller to consume new components.
4. Update `application.yml` with security + template settings.
5. Add unit tests for validator logic.
6. Run `gradle test` and document results.

## Test Plan
- `gradle test` covering `ReportSqlValidatorTest` cases (non-SELECT, forbidden token, missing allowed table).
- Manual smoke by hitting `/api/reports/run` with invalid SQL to confirm rejection (future work).

## Sign-off Checklist
- [x] Spec committed before PR.
- [x] Branch + PR references spec path.
- [x] Tests documented in PR body.
