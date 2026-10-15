import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AttendanceRecord, Holiday } from '../models/attendance.model';

@Injectable({
  providedIn: 'root'
})
export class AttendanceService {
  private readonly baseUrl = `${environment.apiUrl}/attendance`;

  constructor(private http: HttpClient) {}

  checkIn(notes?: string): Observable<AttendanceRecord> {
    return this.http.post<AttendanceRecord>(`${this.baseUrl}/check-in`, { notes });
  }

  checkOut(): Observable<AttendanceRecord> {
    return this.http.post<AttendanceRecord>(`${this.baseUrl}/check-out`, {});
  }

  getTodayAttendance(): Observable<AttendanceRecord> {
    return this.http.get<AttendanceRecord>(`${this.baseUrl}/today`);
  }

  getMonthlyAttendance(year?: number, month?: number, employeeId?: number): Observable<AttendanceRecord[]> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    if (month) params = params.set('month', month.toString());
    if (employeeId) params = params.set('employeeId', employeeId.toString());
    return this.http.get<AttendanceRecord[]>(`${this.baseUrl}/monthly`, { params });
  }

  getDailyAttendance(date?: string): Observable<AttendanceRecord[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<AttendanceRecord[]>(`${this.baseUrl}/daily`, { params });
  }

  getHolidays(year?: number): Observable<Holiday[]> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    return this.http.get<Holiday[]>(`${this.baseUrl}/holidays`, { params });
  }

  createHoliday(holiday: Partial<Holiday>): Observable<Holiday> {
    return this.http.post<Holiday>(`${this.baseUrl}/holidays`, holiday);
  }

  deleteHoliday(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/holidays/${id}`);
  }
}
