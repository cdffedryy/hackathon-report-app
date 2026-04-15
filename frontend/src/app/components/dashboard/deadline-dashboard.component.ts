import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ScheduleService } from '../../services/schedule.service';
import { AuthService } from '../../services/auth.service';
import { DashboardItem } from '../../models/dashboard-item.model';

@Component({
  selector: 'app-deadline-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './deadline-dashboard.component.html',
  styleUrls: ['./deadline-dashboard.component.css']
})
export class DeadlineDashboardComponent implements OnInit {
  items: DashboardItem[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private scheduleService: ScheduleService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;
    this.scheduleService.getDashboardItems().subscribe({
      next: (data) => {
        this.items = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = '加载看板数据失败: ' + (err.error?.message || err.message || '');
        this.loading = false;
      }
    });
  }

  getUrgencyClass(level: string): string {
    switch (level) {
      case 'OVERDUE': return 'urgency-overdue';
      case 'APPROACHING': return 'urgency-approaching';
      case 'COMPLETED': return 'urgency-completed';
      default: return 'urgency-normal';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'NOT_SUBMITTED': return '未提交';
      case 'GENERATED': return '已生成';
      case 'SUBMITTED': return '已提交待审';
      case 'APPROVED': return '已审批通过';
      case 'REJECTED': return '已驳回';
      default: return status;
    }
  }

  getUrgencyLabel(level: string): string {
    switch (level) {
      case 'OVERDUE': return '已逾期';
      case 'APPROACHING': return '即将到期';
      case 'COMPLETED': return '已完成';
      default: return '正常';
    }
  }

  getFrequencyLabel(freq: string): string {
    switch (freq) {
      case 'ONCE': return '一次性';
      case 'DAILY': return '日报';
      case 'WEEKLY': return '周报';
      case 'MONTHLY': return '月报';
      case 'QUARTERLY': return '季报';
      case 'YEARLY': return '年报';
      default: return freq;
    }
  }

  getDaysRemainingText(days: number): string {
    if (days < 0) return '逾期 ' + Math.abs(days) + ' 天';
    if (days === 0) return '今天到期';
    return '剩余 ' + days + ' 天';
  }

  goToReport(item: DashboardItem): void {
    this.router.navigate(['/maker'], { queryParams: { reportId: item.reportId } });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goToReports(): void {
    const user = this.authService.getCurrentUser();
    const role = user?.role || '';
    if (role.includes('CHECKER')) {
      this.router.navigate(['/checker']);
    } else {
      this.router.navigate(['/maker']);
    }
  }
}
