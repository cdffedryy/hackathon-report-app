import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

interface Report {
  id: number;
  name: string;
  sql: string;
  description: string;
}

interface CreateReportRequest {
  name: string;
  sql: string;
  description?: string;
}

export interface ReportRun {
  id: number;
  reportId: number;
  reportName: string;
  status: string;
  makerUsername: string;
  checkerUsername?: string;
  generatedAt: string;
  submittedAt?: string;
  decidedAt?: string;
  resultSnapshot?: string;
  manualNote?: string;
  manualEditedAt?: string;
  hasManualEdits?: boolean;
}

export interface ReportAuditEvent {
  id: number;
  reportRunId: number;
  reportId: number;
  actorUsername: string;
  actorRole?: string;
  eventType: string;
  eventTime: string;
  comment?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private apiUrl = 'http://localhost:8080/api';
  
  private reportsSubject = new BehaviorSubject<Report[]>([]);
  reports$ = this.reportsSubject.asObservable();
  
  constructor(private http: HttpClient) {}
  
  getReports(): Observable<Report[]> {
    return this.http.get<Report[]>(this.apiUrl + '/reports');
  }
  
  getReport(id: number): Observable<Report> {
    return this.http.get<Report>(this.apiUrl + '/reports/' + id);
  }
  
  executeReport(id: number): Observable<any[]> {
    return this.http.post<any[]>(this.apiUrl + '/reports/' + id + '/execute', {});
  }
  
  generateReport(reportId: number, params: string): Observable<any> {
    return this.http.post(this.apiUrl + '/reports/generate', {
      reportId: reportId,
      params: params
    });
  }
  
  createReport(report: CreateReportRequest): Observable<void> {
    return this.http.post<void>(this.apiUrl + '/reports', report);
  }

  // 报表运行相关接口

  getMyLatestRun(reportId: number): Observable<ReportRun> {
    return this.http.get<ReportRun>(`${this.apiUrl}/report-runs/my-latest`, {
      params: { reportId: reportId.toString() }
    });
  }

  getSubmittedRuns(): Observable<ReportRun[]> {
    return this.http.get<ReportRun[]>(`${this.apiUrl}/report-runs/submitted`);
  }

  getMyRuns(): Observable<ReportRun[]> {
    return this.http.get<ReportRun[]>(`${this.apiUrl}/report-runs/my-runs`);
  }

  getCheckerHistoryRuns(): Observable<ReportRun[]> {
    return this.http.get<ReportRun[]>(`${this.apiUrl}/report-runs/checker/history`);
  }

  submitRun(runId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/report-runs/${runId}/submit`, {});
  }

  decideRun(runId: number, decision: 'APPROVED' | 'REJECTED', comment: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/report-runs/${runId}/decision`, {
      decision,
      comment
    });
  }

  saveManualSnapshot(runId: number, snapshot: any, note?: string): Observable<ReportRun> {
    return this.http.put<ReportRun>(`${this.apiUrl}/report-runs/${runId}/manual-snapshot`, {
      snapshot,
      note
    });
  }

  getAuditTrail(runId: number): Observable<ReportAuditEvent[]> {
    return this.http.get<ReportAuditEvent[]>(`${this.apiUrl}/report-runs/${runId}/audit`);
  }

  downloadReport(reportId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/${reportId}/export`, {
      responseType: 'blob'
    });
  }

  downloadRun(runId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/report-runs/${runId}/export`, {
      responseType: 'blob'
    });
  }
}