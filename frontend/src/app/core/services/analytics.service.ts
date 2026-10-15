import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalyticsDashboard } from '../models/analytics.model';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private readonly baseUrl = `${environment.apiUrl}/analytics`;

  constructor(private http: HttpClient) {}

  getDashboard(role?: string, departmentId?: number): Observable<AnalyticsDashboard> {
    let params = new HttpParams();
    if (role) params = params.set('role', role);
    if (departmentId) params = params.set('departmentId', departmentId.toString());
    return this.http.get<AnalyticsDashboard>(`${this.baseUrl}/dashboard`, { params });
  }

  exportExcel(year?: number, month?: number): Observable<Blob> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    if (month) params = params.set('month', month.toString());
    return this.http.get(`${this.baseUrl}/export/excel`, { params, responseType: 'blob' });
  }

  exportPdf(year?: number, month?: number): Observable<Blob> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    if (month) params = params.set('month', month.toString());
    return this.http.get(`${this.baseUrl}/export/pdf`, { params, responseType: 'blob' });
  }
}
