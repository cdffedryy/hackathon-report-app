# Research: 报表报送监控看板

## Decision 1: ReportSchedule 存储方式

**Decision**: 新建独立的 `report_schedule` JPA 实体和对应的 H2 表，通过 `report_id` 外键关联 `report_config`。

**Rationale**: 
- 现有 `Report` 模型是 POJO（非 JPA 实体），由 `ReportDao` 通过 `JdbcTemplate` 访问 `report_config` 表。直接修改它会影响现有的读取链路。
- 新建独立实体可保持向后兼容，且 `ReportSchedule` 只有看板功能需要，职责清晰。
- 使用 JPA（与 `ReportRun` 一致）而非 JdbcTemplate，保持新代码的一致性。

**Alternatives considered**:
- 在 `report_config` 表上加字段：侵入性大，`Report` POJO + `ReportDao` 需要同步修改。
- 使用配置文件存储截止日期：不灵活，无法动态管理。

## Decision 2: 看板数据聚合方式

**Decision**: 后端提供单个 API 端点返回完整的看板数据列表（包含报送状态、剩余天数、紧急程度），前端直接渲染。

**Rationale**:
- 看板数据量预期不大（~50条），一次查询返回全量数据可行。
- 状态计算（剩余天数、是否逾期、当前 Run 状态）在后端统一处理，避免前后端逻辑不一致。
- DTO 模式将多表 JOIN 结果扁平化为前端友好的结构。

**Alternatives considered**:
- 前端分别调用 schedules + runs 再本地聚合：增加请求数和前端复杂度。
- GraphQL：过度设计，项目规模不需要。

## Decision 3: 周期性截止日期自动滚动

**Decision**: 在审批通过（Approved）时，由 `ReportRunService.decideRun` 调用 `ReportScheduleService.rollForward` 方法，根据频率计算并更新下一个截止日期。

**Rationale**:
- 审批通过是明确的触发点，无需定时任务。
- 在已有的 `decideRun` 事务中执行，保证原子性。

**Alternatives considered**:
- 定时任务扫描：增加复杂度，且审批时间不确定，难以精确触发。
- 前端触发：不可靠，且违反业务逻辑应在服务端的原则。

## Decision 4: 前端看板组件方案

**Decision**: 新建独立的 `DeadlineDashboardComponent`（standalone Angular 组件），通过 `/dashboard` 路由访问，使用现有的 CSS 风格和 Angular HttpClient。

**Rationale**:
- 独立组件不影响现有的 `ReportViewerComponent`，降低耦合。
- Angular 17 支持 standalone component，无需修改 NgModule。
- 复用现有的 `auth.interceptor` 和 `auth.guard` 进行 JWT 认证和路由保护。

**Alternatives considered**:
- 在 `ReportViewerComponent` 中嵌入看板 tab：该组件已经很复杂（400+ 行），不宜继续膨胀。
- 使用第三方看板库（如 ngx-kanban）：过度设计，简单的卡片列表足够。
