import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ReportRun, ReportAuditEvent, ReportService } from '../../services/report.service';

import { AuthService } from '../../services/auth.service';

interface Report {
  id: number;
  name: string;
  sql: string;
  description: string;
}

interface ReportData {
  data: any[];
  count?: number;
  custom?: boolean;
}

@Component({
  selector: 'app-report-viewer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './report-viewer.component.html',
  styleUrls: ['./report-viewer.component.css']
})
export class ReportViewerComponent implements OnInit {
  reports: Report[] = [];
  selectedReport: Report | null = null;
  reportData: ReportData | null = null;
  loading = false;
  error: string | null = null;
  exportError: string | null = null;

  // 登录相关状态
  username = 'admin';
  password = '123456';
  loggingIn = false;
  loginError: string | null = null;

  // 当前报表运行实例（Maker 视角）
  currentRun: ReportRun | null = null;
  submitMessage: string | null = null;
  submitError: string | null = null;

  currentRunAudit: ReportAuditEvent[] = [];
  currentRunAuditError: string | null = null;

  // Maker 历史运行列表
  makerRuns: ReportRun[] = [];
  makerRunsError: string | null = null;

  // Checker 待办列表与审批状态
  checkerRuns: ReportRun[] = [];
  selectedCheckerRun: ReportRun | null = null;

  checkerDecision: 'APPROVED' | 'REJECTED' = 'APPROVED';
  checkerComment = '';
  checkerMessage: string | null = null;
  checkerError: string | null = null;

  checkerAudit: ReportAuditEvent[] = [];
  checkerAuditError: string | null = null;

  // Checker 历史审批记录
  checkerHistoryRuns: ReportRun[] = [];
  checkerHistoryError: string | null = null;

  // 手工编辑状态
  manualSnapshotText = '';
  manualNote = '';
  manualEditedAt: string | null = null;
  manualGridRows: Record<string, any>[] = [];
  manualGridColumns: string[] = [];
  manualShowRawJson = false;
  manualNewColumnName = '';
  manualColumnError: string | null = null;
  manualEditorError: string | null = null;
  manualSaveMessage: string | null = null;
  manualSaveError: string | null = null;
  manualDirty = false;
  manualSnapshotValid = false;
  manualSaveLoading = false;
  private manualSnapshotBaseline = '';
  private manualNoteBaseline = '';

  // 中文报表名称映射
  private reportNameMap: { [key: string]: string } = {
    'Customer Transaction Analysis': '客户交易分析',
    'VIP Customer Revenue Report': 'VIP客户收入报告',
    'Merchant Performance Analysis': '商家绩效分析',
    'Department Budget Analysis': '部门预算分析',
    'Product Profitability Report': '产品盈利能力报告',
    'Customer Segmentation Analysis': '客户细分分析',
    'Monthly Revenue Trend Analysis': '月度收入趋势分析',
    'Order Fulfillment Analysis': '订单履行分析',
    'Employee Performance Metrics': '员工绩效指标',
    'Customer-Merchant Revenue Matrix': '客户商家收入矩阵',
    'Inventory Velocity Analysis': '库存周转分析',
    'Financial Health Scorecard': '财务健康仪表板'
  };

  // 中文报表描述映射
  private reportDescriptionMap: { [key: string]: string } = {
    'Customer Transaction Analysis': '综合客户交易分析，包含信用评分关联和平均交易计算',
    'VIP Customer Revenue Report': '详细VIP客户收入分析，包含账户余额和利润计算',
    'Merchant Performance Analysis': '分析商家绩效指标，包含交易量、计数和佣金估算',
    'Department Budget Analysis': '综合部门预算分析，比较分配预算与实际薪资成本及差异计算',
    'Product Profitability Report': '详细产品盈利能力分析，包含销售量、收入、成本和利润率',
    'Customer Segmentation Analysis': '基于交易行为、收入水平和价值分类的高级客户细分',
    'Monthly Revenue Trend Analysis': '显示收入、支出和交易计数的月度收入趋势分析',
    'Order Fulfillment Analysis': '跟踪订单量、价值和状态分布的订单履行分析',
    'Employee Performance Metrics': '包含薪资分布和部门预算影响的员工绩效分析',
    'Customer-Merchant Revenue Matrix': '显示客户和商家之间收入关系的交叉表分析及排名指标',
    'Inventory Velocity Analysis': '显示销售量和盈利能力指标的库存管理分析',
    'Financial Health Scorecard': '显示收入、支出、利润和客户指标等关键绩效指标的高管财务健康仪表板'
  };

  constructor(
    private reportService: ReportService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    // 只有在已登录的情况下才自动加载报表
    if (this.authService.isLoggedIn()) {
      this.loadReports();
      this.loadMakerRunsIfNeeded();
      this.loadCheckerRunsIfNeeded();
    }
  }

  login() {
    this.loggingIn = true;
    this.loginError = null;
    this.error = null;
    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.loggingIn = false;
        this.loadReports();
        this.loadMakerRunsIfNeeded();
        this.loadCheckerRunsIfNeeded();
      },
      error: (err) => {
        this.loggingIn = false;
        this.loginError = '登录失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  private loadMakerRunsIfNeeded() {
    const user = this.authService.getCurrentUser();
    if (!user || !user.role || !user.role.includes('MAKER')) {
      return;
    }
    this.loadMakerRuns();
  }

  loadMakerRuns() {
    this.makerRunsError = null;
    this.reportService.getMyRuns().subscribe({
      next: (runs) => {
        this.makerRuns = runs;
      },
      error: (err) => {
        this.makerRunsError = '加载我的提交失败: ' + (err.error?.message || err.message || '');
        this.makerRuns = [];
      }
    });
  }

  private loadCurrentRunForSelectedReport() {
    if (!this.selectedReport) {
      return;
    }
    this.reportService.getMyLatestRun(this.selectedReport.id).subscribe({
      next: (run) => {
        this.currentRun = run;
        this.initializeManualEditor(run);
        this.loadCurrentRunAudit();
      },
      error: () => {
        // 简单版：忽略错误，不阻塞主流程
        this.currentRun = null;
        this.currentRunAudit = [];
        this.currentRunAuditError = null;
        this.initializeManualEditor(null);
      }
    });
  }

  private loadCurrentRunAudit() {
    if (!this.currentRun) {
      this.currentRunAudit = [];
      this.currentRunAuditError = null;
      return;
    }
    this.currentRunAuditError = null;
    this.reportService.getAuditTrail(this.currentRun.id).subscribe({
      next: (events) => {
        this.currentRunAudit = events;
      },
      error: (err) => {
        this.currentRunAudit = [];
        this.currentRunAuditError = '加载审计轨迹失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  submitCurrentRun() {
    if (!this.currentRun || this.currentRun.status !== 'Generated') {
      return;
    }
    if (this.manualDirty) {
      this.submitError = '存在未保存的手工调整，请先保存后再提交';
      return;
    }
    if (!this.manualSnapshotValid) {
      this.submitError = '手工快照 JSON 无效，请修正后再提交';
      return;
    }
    this.submitMessage = null;
    this.submitError = null;
    this.reportService.submitRun(this.currentRun.id).subscribe({
      next: () => {
        this.submitMessage = '已提交审批';
        this.loadCurrentRunForSelectedReport();
      },
      error: (err) => {
        this.submitError = '提交审批失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  private loadCheckerRunsIfNeeded() {
    const user = this.authService.getCurrentUser();
    if (!user || !user.role || !user.role.includes('CHECKER')) {
      return;
    }
    this.loadCheckerRuns();
  }

  loadCheckerRuns() {
    this.checkerMessage = null;
    this.checkerError = null;
    this.reportService.getSubmittedRuns().subscribe({
      next: (runs) => {
        this.checkerRuns = runs;
        if (!this.selectedCheckerRun && this.checkerRuns.length > 0) {
          this.selectedCheckerRun = this.checkerRuns[0];
        }
      },
      error: (err) => {
        this.checkerError = '加载待审批报表失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  private loadCheckerAudit() {
    if (!this.selectedCheckerRun) {
      this.checkerAudit = [];
      this.checkerAuditError = null;
      return;
    }
    this.checkerAuditError = null;
    this.reportService.getAuditTrail(this.selectedCheckerRun.id).subscribe({
      next: (events) => {
        this.checkerAudit = events;
      },
      error: (err) => {
        this.checkerAudit = [];
        this.checkerAuditError = '加载审计轨迹失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  loadCheckerHistory() {
    this.checkerHistoryError = null;
    this.reportService.getCheckerHistoryRuns().subscribe({
      next: (runs) => {
        this.checkerHistoryRuns = runs;
      },
      error: (err) => {
        this.checkerHistoryError = '加载历史审批记录失败: ' + (err.error?.message || err.message || '');
        this.checkerHistoryRuns = [];
      }
    });
  }

  selectCheckerRun(runId: string) {
    const idNum = +runId;
    this.selectedCheckerRun = this.checkerRuns.find(r => r.id === idNum) || null;
    this.checkerComment = '';
    this.checkerMessage = null;
    this.checkerError = null;
    this.loadCheckerAudit();
  }

  decideSelectedRun() {
    if (!this.selectedCheckerRun) {
      return;
    }
    this.checkerMessage = null;
    this.checkerError = null;
    this.reportService.decideRun(this.selectedCheckerRun.id, this.checkerDecision, this.checkerComment).subscribe({
      next: () => {
        this.checkerMessage = this.checkerDecision === 'APPROVED' ? '已批准' : '已拒绝';
        this.checkerComment = '';
        this.loadCheckerRuns();
        this.checkerAudit = [];
        this.checkerAuditError = null;
      },
      error: (err) => {
        this.checkerError = '审批操作失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  logout() {
    this.authService.logout();
    this.reports = [];
    this.selectedReport = null;
    this.reportData = null;
    this.error = null;
    this.currentRun = null;
    this.submitMessage = null;
    this.submitError = null;
    this.currentRunAudit = [];
    this.currentRunAuditError = null;
    this.checkerRuns = [];
    this.selectedCheckerRun = null;
    this.checkerMessage = null;
    this.checkerError = null;
    this.checkerAudit = [];
    this.checkerAuditError = null;
    this.initializeManualEditor(null);
    this.router.navigate(['/login']);
  }

  // 获取中文报表名称
  getChineseReportName(englishName: string): string {
    return this.reportNameMap[englishName] || englishName;
  }

  // 获取中文报表描述
  getChineseReportDescription(englishName: string): string {
    return this.reportDescriptionMap[englishName] || englishName;
  }

  loadReports() {
    this.reportService.getReports().subscribe({
      next: (data: Report[]) => {
        this.reports = data;
      },
      error: (err) => {
        this.error = 'Failed to load reports: ' + err.message;
      }
    });
  }

  selectReport(reportId: string) {
    const report = this.reports.find(r => r.id === +reportId);
    if (report) {
      this.selectedReport = report;
      this.reportData = null;
      this.error = null;
      this.submitMessage = null;
      this.submitError = null;
      this.currentRun = null;
      this.initializeManualEditor(null);
    }
  }

  runReport() {
    if (!this.selectedReport) return;

    this.loading = true;
    this.error = null;

    this.reportService.executeReport(this.selectedReport.id).subscribe({
      next: (data: any[]) => {
        this.reportData = { data: data, count: data.length };
        this.loading = false;
        this.loadCurrentRunForSelectedReport();
      },
      error: (err) => {
        this.error = 'Failed to execute report: ' + err.message;
        this.loading = false;
      }
    });
  }

  exportReport() {
    if (!this.selectedReport) {
      return;
    }
    this.exportError = null;
    this.reportService.downloadReport(this.selectedReport.id).subscribe({
      next: (blob) => {
        const baseName = this.getChineseReportName(this.selectedReport!.name) || 'report-' + this.selectedReport!.id;
        const filename = baseName + '.xlsx';
        this.triggerDownload(blob, filename);
      },
      error: (err) => {
        this.exportError = '导出失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  exportCurrentRun() {
    if (!this.currentRun) {
      return;
    }
    this.exportError = null;
    this.reportService.downloadRun(this.currentRun.id).subscribe({
      next: (blob) => {
        const baseName = this.currentRun!.reportName || 'report-run-' + this.currentRun!.id;
        const filename = baseName + '.xlsx';
        this.triggerDownload(blob, filename);
      },
      error: (err) => {
        this.exportError = '导出失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  exportRun(run: ReportRun) {
    this.exportError = null;
    this.reportService.downloadRun(run.id).subscribe({
      next: (blob) => {
        const baseName = run.reportName || 'report-run-' + run.id;
        const filename = baseName + '.xlsx';
        this.triggerDownload(blob, filename);
      },
      error: (err) => {
        this.exportError = '导出失败: ' + (err.error?.message || err.message || '');
      }
    });
  }

  private triggerDownload(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  viewRunFlow(runId: number) {
    this.router.navigate(['/runs', runId, 'flow']);
  }

  onManualSnapshotInput(value: string) {
    this.manualSnapshotText = value;
    if (!value || !value.trim()) {
      this.manualSnapshotValid = false;
      this.manualEditorError = '手工快照不能为空';
      this.manualGridRows = [];
      this.manualGridColumns = [];
    } else {
      const parsed = this.syncGridFromJson(value);
      this.manualSnapshotValid = parsed;
      if (parsed && !this.manualShowRawJson) {
        this.rebuildManualSnapshotFromGrid();
      }
    }
    this.updateManualDirtyFlag();
  }

  onManualNoteInput(value: string) {
    this.manualNote = value;
    this.updateManualDirtyFlag();
  }

  saveManualEdits() {
    if (!this.currentRun || this.manualSaveLoading) {
      return;
    }
    if (!this.manualSnapshotValid) {
      this.manualSaveError = '手工快照 JSON 无效，无法保存';
      return;
    }
    let parsed: any;
    try {
      parsed = JSON.parse(this.manualSnapshotText);
    } catch (err) {
      this.manualSnapshotValid = false;
      this.manualEditorError = 'JSON 格式错误: ' + (err as Error).message;
      return;
    }

    this.manualSaveMessage = null;
    this.manualSaveError = null;
    this.manualSaveLoading = true;
    this.reportService.saveManualSnapshot(this.currentRun.id, parsed, this.manualNote).subscribe({
      next: (run) => {
        this.currentRun = run;
        this.manualEditedAt = run.manualEditedAt || null;
        this.manualNote = run.manualNote || '';
        this.manualNoteBaseline = this.manualNote;
        const formatted = this.prettyPrintJson(run.resultSnapshot || parsed);
        const synced = this.syncGridFromJson(formatted);
        if (synced) {
          this.rebuildManualSnapshotFromGrid();
          this.manualSnapshotBaseline = this.manualSnapshotText;
          this.manualSnapshotValid = true;
        } else {
          this.manualSnapshotText = formatted;
          this.manualSnapshotBaseline = formatted;
          this.manualSnapshotValid = false;
        }
        this.manualDirty = false;
        this.manualEditorError = null;
        this.manualSaveMessage = '手工调整已保存';

        this.loadMakerRuns();
        this.loadCurrentRunAudit();
      },
      error: (err) => {
        this.manualSaveError = '保存失败: ' + (err.error?.message || err.message || '');
      },
      complete: () => {
        this.manualSaveLoading = false;
      }
    });
  }

  private initializeManualEditor(run: ReportRun | null) {
    this.manualSaveMessage = null;
    this.manualSaveError = null;
    this.manualShowRawJson = false;
    this.manualNewColumnName = '';
    this.manualColumnError = null;
    if (!run) {
      this.manualSnapshotText = '';
      this.manualSnapshotBaseline = '';
      this.manualNote = '';
      this.manualNoteBaseline = '';
      this.manualEditedAt = null;
      this.manualGridRows = [];
      this.manualGridColumns = [];
      this.manualDirty = false;
      this.manualSnapshotValid = false;
      this.manualEditorError = null;
      return;
    }

    const snapshotSource = run.resultSnapshot
      ? this.prettyPrintJson(run.resultSnapshot)
      : this.reportData?.data
        ? this.prettyPrintJson(this.reportData.data)
        : '';

    this.manualNote = run.manualNote || '';
    this.manualNoteBaseline = this.manualNote;
    this.manualEditedAt = run.manualEditedAt || null;
    this.manualDirty = false;
    this.manualEditorError = snapshotSource ? null : '暂无可编辑的快照，请重新执行报表';
    const valid = snapshotSource ? this.syncGridFromJson(snapshotSource) : false;
    if (valid) {
      this.rebuildManualSnapshotFromGrid();
      this.manualSnapshotBaseline = this.manualSnapshotText;
      this.manualSnapshotValid = true;
    } else {
      this.manualSnapshotText = snapshotSource;
      this.manualSnapshotBaseline = snapshotSource;
      this.manualSnapshotValid = false;
    }
  }

  private updateManualDirtyFlag() {
    this.manualDirty = this.manualSnapshotText !== this.manualSnapshotBaseline
      || this.manualNote !== this.manualNoteBaseline;
  }

  private rebuildManualSnapshotFromGrid() {
    const source = this.manualGridRows.length > 0 ? this.manualGridRows : [];
    this.manualSnapshotText = this.prettyPrintJson(source);
    this.manualSnapshotValid = true;
    this.manualEditorError = null;
  }

  private syncGridFromJson(text: string): boolean {
    try {
      if (!text || !text.trim()) {
        this.manualGridRows = [];
        this.manualGridColumns = [];
        this.manualEditorError = null;
        return true;
      }
      const parsed = JSON.parse(text);
      let rows: Record<string, any>[] = [];
      if (Array.isArray(parsed)) {
        rows = parsed.map(item => this.normalizeGridRow(item));
      } else if (parsed && typeof parsed === 'object') {
        rows = [this.normalizeGridRow(parsed)];
      }
      const columnSet = new Set<string>();
      rows.forEach(row => Object.keys(row || {}).forEach(col => columnSet.add(col)));
      this.manualGridRows = rows;
      this.manualGridColumns = Array.from(columnSet);
      this.manualEditorError = null;
      this.manualColumnError = null;
      return true;
    } catch (err) {
      this.manualGridRows = [];
      this.manualGridColumns = [];
      this.manualEditorError = 'JSON 格式错误: ' + (err as Error).message;
      return false;
    }
  }

  updateManualCell(rowIndex: number, columnKey: string, value: string) {
    if (!this.manualGridRows[rowIndex]) {
      return;
    }
    this.manualGridRows = this.manualGridRows.map((row, idx) => idx === rowIndex ? { ...row, [columnKey]: value } : row);
    this.persistManualGridChange();
  }

  addManualRow() {
    if (this.manualGridColumns.length === 0) {
      this.manualColumnError = '请先添加至少一列';
      return;
    }
    const newRow: Record<string, any> = {};
    this.manualGridColumns.forEach(col => newRow[col] = '');
    this.manualGridRows = [...this.manualGridRows, newRow];
    this.manualColumnError = null;
    this.persistManualGridChange();
  }

  removeManualRow(index: number) {
    this.manualGridRows = this.manualGridRows.filter((_, i) => i !== index);
    this.persistManualGridChange();
  }

  addManualColumn() {
    const name = (this.manualNewColumnName || '').trim();
    if (!name) {
      this.manualColumnError = '列名不能为空';
      return;
    }
    if (this.manualGridColumns.includes(name)) {
      this.manualColumnError = '列已存在';
      return;
    }
    this.manualGridColumns = [...this.manualGridColumns, name];
    this.manualGridRows = this.manualGridRows.length > 0
      ? this.manualGridRows.map(row => ({ ...row, [name]: row[name] ?? '' }))
      : [{ [name]: '' }];
    this.manualNewColumnName = '';
    this.manualColumnError = null;
    this.persistManualGridChange();
  }

  removeManualColumn(column: string) {
    this.manualGridColumns = this.manualGridColumns.filter(col => col !== column);
    this.manualGridRows = this.manualGridRows.map(row => {
      const clone = { ...row };
      delete clone[column];
      return clone;
    });
    this.persistManualGridChange();
  }

  toggleManualJsonView() {
    if (this.manualShowRawJson) {
      this.manualSnapshotValid = this.syncGridFromJson(this.manualSnapshotText);
      if (this.manualSnapshotValid) {
        this.rebuildManualSnapshotFromGrid();
      }
    } else {
      this.rebuildManualSnapshotFromGrid();
    }
    this.manualShowRawJson = !this.manualShowRawJson;
  }

  private persistManualGridChange() {
    this.rebuildManualSnapshotFromGrid();
    this.updateManualDirtyFlag();
  }

  private normalizeGridRow(item: any): Record<string, any> {
    if (item && typeof item === 'object') {
      return { ...item } as Record<string, any>;
    }
    return { value: item };
  }

  private prettyPrintJson(raw: any): string {
    try {
      if (typeof raw === 'string') {
        return JSON.stringify(JSON.parse(raw), null, 2);
      }
      return JSON.stringify(raw, null, 2);
    } catch {
      return typeof raw === 'string' ? raw : JSON.stringify(raw);
    }
  }
}