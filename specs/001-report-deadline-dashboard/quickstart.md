# Quickstart: 报表报送监控看板

## Prerequisites

- Java 17+, Node.js 18+, npm
- 项目已 clone 且在 `001-report-deadline-dashboard` 分支

## 启动步骤

### 1. 后端
```bash
cd backend
.\gradlew bootRun
```
- H2 内存库自动初始化，包含 `report_schedule` 测试数据
- 看板 API: `http://localhost:8080/api/report-schedules/dashboard`

### 2. 前端
```bash
cd frontend
npm install
npm start
```
- 看板页面: `http://localhost:4200/dashboard`

## 验证

1. 使用 `maker1 / 123456` 登录
2. 点击导航栏"报送监控"链接
3. 确认看板显示预置的报送计划，带有颜色高亮标记
4. 确认即将到期报表显示橙色/黄色，已逾期显示红色
5. 点击"去执行"按钮确认跳转至对应报表

## 测试

```bash
# 后端单元测试
cd backend
.\gradlew test

# 前端单元测试
cd frontend
npm test
```
