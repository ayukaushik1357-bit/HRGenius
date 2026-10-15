import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LeaveApplication, LeaveBalance, LeaveApplicationRequest } from '../models/leave.model';

@Injectable({
  providedIn: 'root'
})
export class LeaveService {
  private readonly baseUrl = `${environment.apiUrl}/leaves`;

  constructor(private http: HttpClient) {}

  applyLeave(request: LeaveApplicationRequest): Observable<LeaveApplication> {
    return this.http.post<LeaveApplication>(this.baseUrl, request);
  }

  getMyLeaves(): Observable<LeaveApplication[]> {
    return this.http.get<LeaveApplication[]>(`${this.baseUrl}/my`);
  }

  getLeaveBalance(year?: number, employeeId?: number): Observable<LeaveBalance> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    if (employeeId) params = params.set('employeeId', employeeId.toString());
    return this.http.get<LeaveBalance>(`${this.baseUrl}/balance`, { params });
  }

  getPendingApprovals(): Observable<LeaveApplication[]> {
    return this.http.get<LeaveApplication[]>(`${this.baseUrl}/pending-approvals`);
  }

  actionLeave(id: number, approved: boolean, rejectionReason?: string): Observable<LeaveApplication> {
    return this.http.post<LeaveApplication>(`${this.baseUrl}/${id}/action`, { approved, rejectionReason });
  }

  getAllLeaves(status?: string): Observable<LeaveApplication[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<LeaveApplication[]>(this.baseUrl, { params });
  }
}
