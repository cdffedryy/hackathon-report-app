export interface DashboardItem {
  scheduleId: number;
  reportId: number;
  reportName: string;
  reportDescription: string;
  frequency: string;
  currentDeadline: string;
  periodStart: string;
  daysRemaining: number;
  submissionStatus: string;
  urgencyLevel: string;
  latestRunId: number | null;
}
