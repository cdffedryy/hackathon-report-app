# Data Model: 报表报送监控看板

## New Entity: ReportSchedule

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long (PK, auto-increment) | No | 主键 |
| report_id | Long (FK → report_config.id) | No | 关联的报表模板 ID |
| frequency | String(16) | No | 报送频率：ONCE / DAILY / WEEKLY / MONTHLY / QUARTERLY / YEARLY |
| current_deadline | LocalDate | No | 当前周期的截止日期 |
| period_start | LocalDate | No | 当前周期开始日期（用于查询周期内的 Run） |
| enabled | Boolean | No | 是否启用（默认 true） |
| created_at | LocalDateTime | No | 创建时间 |
| updated_at | LocalDateTime | Yes | 最后更新时间 |

### Constraints
- `report_id` UNIQUE（一个报表最多一个有效计划）
- `report_id` FK → `report_config(id)`
- `frequency` IN ('ONCE', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')

### State Transitions
- **创建**: 设定 `current_deadline` 和 `period_start`，`enabled = true`
- **周期滚动**: 当关联报表在当前周期内被 Approved 后：
  - `period_start` = 旧 `current_deadline` + 1 天
  - `current_deadline` = 根据 `frequency` 计算的下一个截止日期
  - ONCE 频率不滚动
- **禁用**: `enabled = false`，从看板消失

### Frequency → Next Deadline Calculation
| Frequency | Next Deadline |
|-----------|---------------|
| ONCE | 不滚动 |
| DAILY | current_deadline + 1 day |
| WEEKLY | current_deadline + 7 days |
| MONTHLY | current_deadline + 1 month |
| QUARTERLY | current_deadline + 3 months |
| YEARLY | current_deadline + 1 year |

## DTO: DashboardItemDto (API response)

| Field | Type | Description |
|-------|------|-------------|
| scheduleId | Long | 报送计划 ID |
| reportId | Long | 报表模板 ID |
| reportName | String | 报表名称 |
| reportDescription | String | 报表描述 |
| frequency | String | 报送频率 |
| currentDeadline | LocalDate | 当前截止日期 |
| periodStart | LocalDate | 周期开始日期 |
| daysRemaining | int | 距截止日剩余天数（负数表示逾期） |
| submissionStatus | String | 报送状态：NOT_SUBMITTED / GENERATED / SUBMITTED / APPROVED / REJECTED |
| urgencyLevel | String | 紧急程度：OVERDUE / APPROACHING / NORMAL / COMPLETED |
| latestRunId | Long (nullable) | 当前周期内最新 Run 的 ID |

## Existing Entities (no changes)

- **Report** (`report_config` table): 通过 `ReportSchedule.report_id` 关联
- **ReportRun** (`report_run` table): 通过 `report_id` + `generated_at` 在周期范围内查询
