# API Contracts: 报表报送监控看板

## GET /api/report-schedules/dashboard

**Description**: 获取报送监控看板数据列表

**Auth**: 需要有效 JWT（Maker / Checker / Admin 均可访问）

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| page | int | No | 0 | 页码（0-based） |
| size | int | No | 20 | 每页条数 |

**Response 200**:
```json
{
  "content": [
    {
      "scheduleId": 1,
      "reportId": 1,
      "reportName": "Customer Transaction Analysis",
      "reportDescription": "客户交易分析",
      "frequency": "MONTHLY",
      "currentDeadline": "2026-04-30",
      "periodStart": "2026-04-01",
      "daysRemaining": -2,
      "submissionStatus": "NOT_SUBMITTED",
      "urgencyLevel": "OVERDUE",
      "latestRunId": null
    },
    {
      "scheduleId": 2,
      "reportId": 3,
      "reportName": "Merchant Performance Analysis",
      "reportDescription": "商家绩效分析",
      "frequency": "WEEKLY",
      "currentDeadline": "2026-04-20",
      "periodStart": "2026-04-14",
      "daysRemaining": 4,
      "submissionStatus": "SUBMITTED",
      "urgencyLevel": "APPROACHING",
      "latestRunId": 15
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

**Sorting**: 默认按紧急程度排序：OVERDUE → APPROACHING → NORMAL → COMPLETED，同级别内按 `daysRemaining` 升序。

---

## POST /api/report-schedules

**Description**: 创建报送计划（Admin only）

**Auth**: 需要有效 JWT + CHECKER 或 MAKER+CHECKER（Admin）角色

**Request Body**:
```json
{
  "reportId": 1,
  "frequency": "MONTHLY",
  "currentDeadline": "2026-04-30"
}
```

**Response 201**:
```json
{
  "id": 1,
  "reportId": 1,
  "frequency": "MONTHLY",
  "currentDeadline": "2026-04-30",
  "periodStart": "2026-04-01",
  "enabled": true,
  "createdAt": "2026-04-16T01:30:00"
}
```

**Error 400**: `reportId` 已有有效计划 / 报表不存在 / 截止日期无效

---

## PUT /api/report-schedules/{id}

**Description**: 更新报送计划（Admin only）

**Auth**: 需要有效 JWT + Admin 角色

**Request Body**:
```json
{
  "frequency": "QUARTERLY",
  "currentDeadline": "2026-06-30",
  "enabled": true
}
```

**Response 200**: 返回更新后的 ReportSchedule

**Error 404**: 计划不存在

---

## urgencyLevel 计算规则

| Condition | urgencyLevel |
|-----------|-------------|
| submissionStatus == APPROVED | COMPLETED |
| daysRemaining < 0 且未 APPROVED | OVERDUE |
| 0 ≤ daysRemaining ≤ 7 且未 APPROVED | APPROACHING |
| daysRemaining > 7 | NORMAL |
