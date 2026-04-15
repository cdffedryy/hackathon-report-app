# Implementation Plan: 报表报送监控看板（Report Submission Deadline Dashboard）

**Branch**: `001-report-deadline-dashboard` | **Date**: 2026-04-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/001-report-deadline-dashboard/spec.md`

## Summary

新增"报表报送监控看板"功能，通过新建 `ReportSchedule` 实体为报表模板配置报送截止日期和频率，后端提供看板数据 API，前端新增独立看板页面以卡片形式展示各报表的报送状态，对一周内即将到期和已逾期的报表进行颜色高亮提醒，支持从看板快速跳转执行/提交/审批。

## Technical Context

**Language/Version**: Java 17 (Backend), TypeScript 5.x (Frontend)  
**Primary Dependencies**: Spring Boot 3 + Spring Data JPA + H2 (Backend), Angular 17 + HttpClient (Frontend)  
**Storage**: H2 内存数据库（已有），新增 `report_schedule` 表  
**Testing**: JUnit 5 + Spring Boot Test (Backend), Karma + Jasmine (Frontend)  
**Target Platform**: Web (localhost:8080 后端 / localhost:4200 前端)  
**Project Type**: Web application (full-stack)  
**Performance Goals**: 看板页面 50 条数据量下加载 ≤2s  
**Constraints**: 复用现有 JWT 认证体系，不引入新的外部依赖  
**Scale/Scope**: ~50 报表计划，3 角色（Maker/Checker/Admin）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

项目宪法尚未具体配置（模板占位状态），无特定约束需要检查。按照现有代码风格和技术栈约定执行。

## Project Structure

### Documentation (this feature)

```text
specs/001-report-deadline-dashboard/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (REST API contracts)
├── checklists/          # Quality checklists
│   ├── requirements.md
│   └── dashboard-ux.md
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/legacy/report/
│   ├── model/
│   │   └── ReportSchedule.java          # 新增：报送计划实体
│   ├── repository/
│   │   └── ReportScheduleRepository.java # 新增：报送计划 Repository
│   ├── service/
│   │   └── ReportScheduleService.java    # 新增：看板业务逻辑
│   ├── controller/
│   │   └── ReportScheduleController.java # 新增：看板 API
│   └── dto/
│       └── DashboardItemDto.java         # 新增：看板条目 DTO
├── src/main/resources/
│   ├── schema.sql                        # 修改：新增 report_schedule 表
│   └── data.sql                          # 修改：预置报送计划测试数据
└── src/test/java/com/legacy/report/
    └── service/
        └── ReportScheduleServiceTest.java # 新增：看板服务单元测试

frontend/
├── src/app/
│   ├── components/
│   │   └── dashboard/
│   │       ├── deadline-dashboard.component.ts    # 新增：看板组件
│   │       ├── deadline-dashboard.component.html  # 新增：看板模板
│   │       └── deadline-dashboard.component.css   # 新增：看板样式
│   ├── services/
│   │   └── schedule.service.ts                    # 新增：报送计划服务
│   ├── models/
│   │   └── dashboard-item.model.ts                # 新增：看板数据模型
│   └── app.routes.ts                              # 修改：新增 /dashboard 路由
└── src/app/components/
    └── report/
        └── report-viewer.component.html           # 修改：导航栏增加看板入口
```

**Structure Decision**: 采用现有的 Web 应用双项目结构（backend/ + frontend/），在各自目录下按照已有的分层模式（model/repository/service/controller + component/service/model）添加新文件。

## Complexity Tracking

无宪法违规需要记录。
