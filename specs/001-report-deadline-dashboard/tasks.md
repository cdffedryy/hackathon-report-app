# Tasks: 报表报送监控看板

**Branch**: `001-report-deadline-dashboard`
**Generated**: 2026-04-16
**Total Tasks**: 14

## Phase 1: Setup

- [ ] T001 Add `report_schedule` table DDL to `backend/src/main/resources/schema.sql`
- [ ] T002 Add seed data for report schedules to `backend/src/main/resources/data.sql`

## Phase 2: Foundational — Backend Model & Repository

- [ ] T003 [P] Create `ReportSchedule` JPA entity in `backend/src/main/java/com/legacy/report/model/ReportSchedule.java`
- [ ] T004 [P] Create `DashboardItemDto` DTO in `backend/src/main/java/com/legacy/report/dto/DashboardItemDto.java`
- [ ] T005 Create `ReportScheduleRepository` in `backend/src/main/java/com/legacy/report/repository/ReportScheduleRepository.java`

## Phase 3: US1+US2 — Dashboard API & Urgency Logic (P1)

- [ ] T006 [US1] [US2] Create `ReportScheduleService` with dashboard data assembly and urgency calculation in `backend/src/main/java/com/legacy/report/service/ReportScheduleService.java`
- [ ] T007 [US1] [US2] Create `ReportScheduleController` with GET /api/report-schedules/dashboard endpoint in `backend/src/main/java/com/legacy/report/controller/ReportScheduleController.java`
- [ ] T008 [US1] [US2] Add `report_schedule` to allowed-tables in `backend/src/main/resources/application.yml`

## Phase 4: US1+US2 — Frontend Dashboard (P1)

- [ ] T009 [P] [US1] Create `DashboardItem` model in `frontend/src/app/models/dashboard-item.model.ts`
- [ ] T010 [P] [US1] Create `ScheduleService` in `frontend/src/app/services/schedule.service.ts`
- [ ] T011 [US1] [US2] Create `DeadlineDashboardComponent` (ts+html+css) in `frontend/src/app/components/dashboard/`
- [ ] T012 [US1] Add `/dashboard` route to `frontend/src/app/app.routes.ts` and add nav link in login component

## Phase 5: US4 — Schedule Management API (P2)

- [ ] T013 [US4] Add POST /api/report-schedules and PUT /api/report-schedules/{id} endpoints to `ReportScheduleController`

## Phase 6: Tests

- [ ] T014 Create `ReportScheduleServiceTest` in `backend/src/test/java/com/legacy/report/service/ReportScheduleServiceTest.java`

## Dependencies

- T003, T004 → T005 → T006 → T007
- T009, T010 → T011 → T012
- T007 must complete before T011 can be integration-tested
- T006 → T013 (schedule management reuses service)
