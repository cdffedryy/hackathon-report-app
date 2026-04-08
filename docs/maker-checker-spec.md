# 报表管理系统 Maker-Checker 审批规范

## 1. 背景与目标
- **产品定位**：一个用于执行既定 SQL 报表、并通过“双人复核”确保数据发布合规的管理系统。
- **技术栈**：后端基于 Spring Boot 3 + H2 内存库，提供 JWT 保护的 `/api` 接口；前端为 Angular 17 单页应用，通过 `HttpClient` 调用后端并根据角色切换视图。@backend/src/main/resources/application.yml#1-44 @frontend/package.json#1-37
- **目标角色**：Maker 创建/运行报表并提交；Checker 审核、给出意见并最终批准或驳回；具备双角色的管理员可全流程操作。@backend/src/main/java/com/legacy/report/config/UserInitializer.java#20-44

## 2. 核心角色
| 角色 | 职责 | 权限边界 |
| --- | --- | --- |
| Maker | 执行模板 SQL、查看结果、发起提交、重导出 | 只能操作自己生成的 run，提交前需要保持 `Generated` 状态 | 
| Checker | 查看待办、读取执行快照、批注并做决定 | 只能审批 `Submitted` 状态 run，并需在拒绝时提供 comment | 
| Admin (Maker+Checker) | 兼具 Maker/Checker 能力，用于演示或紧急接管 | 同时可访问 Maker/Checker 视图 | @backend/src/main/java/com/legacy/report/service/CurrentUserService.java#18-43 @frontend/src/app/components/auth/login.component.ts#104-123

## 3. 域模型
| 实体 | 描述 | 关键字段 |
| --- | --- | --- |
| `Report` | 报表模板（SQL + 元信息） | `id`, `name`, `sql`, `description` @backend/src/main/java/com/legacy/report/model/Report.java#1-19 |
| `ReportRun` | 每次执行的业务单据，是审批的原子单位 | 状态机：`Generated → Submitted → Approved/Rejected`；关联 Maker/Checker、时间戳、结果快照。@backend/src/main/java/com/legacy/report/model/ReportRun.java#6-154 |
| `ReportAuditEvent` | 审批轨迹，记录节点、操作者、角色、意见 | 事件类型：`Generated/Submitted/Approved/Rejected/Exported*`，带时间与备注。@backend/src/main/java/com/legacy/report/model/ReportAuditEvent.java#1-40 |
| `User` | 登录账号，角色以逗号分隔（如 `MAKER,CHECKER`） | `username`, `password`, `role`。@backend/src/main/java/com/legacy/report/model/User.java#9-54 |

## 4. 系统架构概览
### 后端
1. **认证**：`/api/auth/login` 通过 `AuthService` 校验用户并颁发 JWT，过滤器在除 `/api/auth/**` 之外的请求上验证 Token。@backend/src/main/java/com/legacy/report/controller/AuthController.java#24-54 @backend/src/main/java/com/legacy/report/service/AuthService.java#22-34 @backend/src/main/java/com/legacy/report/security/JwtAuthenticationFilter.java#28-47
2. **授权**：`CurrentUserService.requireRole` 在服务层强制 Maker/Checker 边界；Spring Security 默认保护所有接口。@backend/src/main/java/com/legacy/report/service/CurrentUserService.java#18-43 @backend/src/main/java/com/legacy/report/config/SecurityConfig.java#24-53
3. **报表执行**：`ReportService` 目前直接运行 SQL（存在注入风险），`ReportDao` 通过 `JdbcTemplate` 访问 `report_config` 表。@backend/src/main/java/com/legacy/report/service/ReportService.java#17-65 @backend/src/main/java/com/legacy/report/dao/ReportDao.java#17-56
4. **Run 生命周期**：`ReportRunService` 负责创建 run、提交、审批、查询审计，并写入 Prometheus 指标。@backend/src/main/java/com/legacy/report/service/ReportRunService.java#25-267
5. **Excel 导出**：`ReportExcelExportService` 结合 JXLS 模板导出最新或指定 run 的快照，并记审计事件。@backend/src/main/java/com/legacy/report/service/ReportExcelExportService.java#54-155

### 前端
1. **路由**：`/reports` 主视图、`/maker`/`/checker` 受角色保护、`/runs/:id/flow` 展示单次运行的时间线。@frontend/src/app/app.routes.ts#7-15 @frontend/src/app/services/auth.guard.ts#5-41
2. **登录**：`LoginComponent` 使用 `AuthService` 登录并根据角色跳转。@frontend/src/app/components/auth/login.component.ts#7-123
3. **主界面**：`ReportViewerComponent` 将 Maker/Checker 的操作集中呈现，调用 `ReportService` API，维护状态和错误提示。@frontend/src/app/components/report/report-viewer.component.ts#1-431
4. **审批流程视图**：`ReportRunFlowComponent` 仅读取指定 run 的审计轨迹并以时间线展示。@frontend/src/app/components/report/report-run-flow.component.ts#1-128

## 5. 业务流程
### 5.1 Maker 正向流程
1. **登录**：输入账号密码（默认 `maker1/123456`），获取 JWT 并缓存到 `localStorage`。@frontend/src/app/components/auth/login.component.ts#92-123 @frontend/src/app/services/auth.service.ts#31-65
2. **选择报表并执行**：下拉选择模板 → 调用 `POST /api/reports/{id}/execute` 执行 SQL，后端会保存 `ReportRun`（状态 `Generated`）并写审计事件。@frontend/src/app/components/report/report-viewer.component.html#27-87 @backend/src/main/java/com/legacy/report/controller/ReportController.java#84-89 @backend/src/main/java/com/legacy/report/service/ReportRunService.java#75-128
3. **查看运行状态与结果**：前端轮询「我的最新运行」和审计轨迹，展示快照与下载入口。@frontend/src/app/components/report/report-viewer.component.ts#158-208
4. **提交审批**：只有 run 状态为 `Generated` 且由本人生成才能提交；提交后状态转 `Submitted` 并记录时间。@backend/src/main/java/com/legacy/report/service/ReportRunService.java#130-168
5. **历史追踪**：Maker 可在“我的提交历史”中查看所有 run、导出 Excel、或跳转审批流程视图。@frontend/src/app/components/report/report-viewer.component.html#89-126 @frontend/src/app/components/report/report-viewer.component.ts#137-208

### 5.2 Checker 审批流程
1. **登录 / 导航**：Checker 登录后默认落在 `/checker` 视图，加载待审批列表。@frontend/src/app/components/auth/login.component.ts#111-123 @frontend/src/app/components/report/report-viewer.component.ts#211-233
2. **查看待办**：列表展示 run 编号、报表名、Maker、提交时间；选择后可加载审计轨迹和上下文。@frontend/src/app/components/report/report-viewer.component.html#163-235
3. **决策**：调用 `POST /api/report-runs/{id}/decision`，当决策为拒绝时必须附带 comment；服务层会校验状态、角色，并记录决策时间/审计事件/Prometheus 计数。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#33-52 @backend/src/main/java/com/legacy/report/service/ReportRunService.java#170-225
4. **历史记录**：Checker 可查询自己处理过的 run，支持导出与流程查看。@frontend/src/app/components/report/report-viewer.component.html#239-280 @backend/src/main/java/com/legacy/report/service/ReportRunService.java#255-260

### 5.3 审计与可观测性
- 每个节点（执行、提交、审批、导出）都会触发 `AuditService.recordEvent`，写入 `report_audit_event` 表供前端时间线使用。@backend/src/main/java/com/legacy/report/service/AuditService.java#17-44
- `ReportRunService` 在关键操作上累积 Counter 与 Timer，用于统计生成/提交/批准/拒绝总量以及审批时长。@backend/src/main/java/com/legacy/report/service/ReportRunService.java#48-223

## 6. API 规格
| Endpoint | 方法 | 角色 | 说明 |
| --- | --- | --- | --- |
| `/api/auth/login` | POST | Public | 用户登录，返回 JWT + 用户角色。@backend/src/main/java/com/legacy/report/controller/AuthController.java#27-36 |
| `/api/reports` | GET | Maker/Checker | 列出所有报表模板。@backend/src/main/java/com/legacy/report/controller/ReportController.java#54-58 |
| `/api/reports/{id}` | GET | Maker/Checker | 获取单个报表定义。@backend/src/main/java/com/legacy/report/controller/ReportController.java#60-63 |
| `/api/reports/{id}/execute` | POST | Maker | 执行报表 + 生成 run（返回结果集）。@backend/src/main/java/com/legacy/report/controller/ReportController.java#84-89 |
| `/api/reports/{id}/export` | GET | Maker | 导出最新 run 的 Excel。@backend/src/main/java/com/legacy/report/controller/ReportController.java#91-103 |
| `/api/report-runs/{id}/submit` | POST | Maker | 将 `Generated` run 提交至审批。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#28-31 |
| `/api/report-runs/{id}/manual-snapshot` | PUT | Maker | 保存 Maker 手工编辑后的 JSON 快照与备注，仅允许 `Generated` 且本人运行。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#33-116 |
| `/api/report-runs/{id}/decision` | POST | Checker | 执行批准/拒绝，拒绝需 comment。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#33-52 |
| `/api/report-runs/my-latest?reportId=` | GET | Maker | 取某报表最近 run（用于刷新状态）。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#54-57 |
| `/api/report-runs/my-runs` | GET | Maker | Maker 历史列表。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#59-62 |
| `/api/report-runs/submitted` | GET | Checker | 全量待审批 run（按提交时间排序）。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#64-67 |
| `/api/report-runs/checker/history` | GET | Checker | Checker 历史审批记录。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#69-72 |
| `/api/report-runs/{id}/audit` | GET | Maker/Checker | 审计轨迹。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#74-77 |
| `/api/report-runs/{id}/export` | GET | Maker/Checker | 基于 run 快照导出（权限在服务层校验）。@backend/src/main/java/com/legacy/report/controller/ReportRunController.java#79-88 |

## 7. 前端交互要点
1. **状态分区**：同一页面根据角色展示不同 section，Maker 区域包括“选择报表”“当前运行”“历史”“结果表格”；Checker 区域包括“待审批列表”“审批表单”“历史记录”。@frontend/src/app/components/report/report-viewer.component.html#1-281
2. **Maker 手工调整**：`Generated` 状态下提供「网格单元格 + JSON 切换」编辑器，可增删行列、直接修改 cell 值，也可切回原始 JSON；必须保存成功后才能提交，历史/Checker 视图继续标记“含手工调整”与备注。@frontend/src/app/components/report/report-viewer.component.html#40-205 @frontend/src/app/components/report/report-viewer.component.ts#72-700
3. **错误与 Loading 处理**：每个 API 调用在 `ReportViewerComponent` 里维护独立的 `error/info` 消息，确保 Maker/Checker 互不干扰。@frontend/src/app/components/report/report-viewer.component.ts#31-392
4. **审计可视化**：表格与 `/runs/:id/flow` 时间线两种呈现，使 Maker/Checker 均可追溯每个事件。@frontend/src/app/components/report/report-viewer.component.html#97-125 @frontend/src/app/components/report/report-run-flow.component.ts#10-127
5. **安全拦截**：`auth.interceptor` 只对 `http://localhost:8080/api` 前缀附加 Token，避免误加第三方请求。@frontend/src/app/services/auth.interceptor.ts#1-20

## 8. 已知风险与改进方向
1. **SQL 注入与越权**：`ReportService.runReport` 与 `generateReport` 将用户参数直接拼接 SQL，应改为参数化查询并增加白名单。@backend/src/main/java/com/legacy/report/service/ReportService.java#26-65
2. **DAO 责任过重**：`ReportDao` 混杂业务逻辑且缺乏更新/删除接口，需要 Repository 化 & 补齐审计。@backend/src/main/java/com/legacy/report/dao/ReportDao.java#17-56
3. **前端安全性**：本地存储 Token 未加密，需配合刷新机制与过期处理。@frontend/src/app/services/auth.service.ts#24-65
4. **审批人体验**：当前 Checker 只能看到结果快照缺少明细下载入口，可在待办区增加“导出 run”按钮复用现有 API。@frontend/src/app/components/report/report-viewer.component.html#174-235
5. **多租户/隔离**：所有用户共享报表列表与数据源，未来需在 `Report` 实体和 SQL 层引入租户或标签。

## 9. 后续工作建议
1. **安全重构**：引入参数模板、限制可执行 SQL、在服务层做输入验证与字段白名单。
2. **审批队列优化**：增加分页/搜索、实时通知（WebSocket）和 SLA 计时展示，利用已有 Prometheus 指标扩展运营看板。@backend/src/main/java/com/legacy/report/service/ReportRunService.java#48-223
3. **模板管理**：补齐报表 CRUD，加入草稿、发布、版本号等字段，Maker 只能使用已发布模板。
4. **流程自动化**：允许配置「必须 2 个不同 Checker」或“金额阈值自动提交”等规则，可在 `ReportRun` 增加字段描述策略。
