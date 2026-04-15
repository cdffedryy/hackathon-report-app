import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardItem } from '../models/dashboard-item.model';

@Injectable({
  providedIn: 'root'
})
export class ScheduleService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getDashboardItems(): Observable<DashboardItem[]> {
    return this.http.get<DashboardItem[]>(`${this.apiUrl}/report-schedules/dashboard`);
  }
}
