import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ExitRequest, ClearanceItem, FnFSettlement } from '../models/offboarding.model';

@Injectable({
  providedIn: 'root'
})
export class OffboardingService {
  private readonly baseUrl = `${environment.apiUrl}/offboarding`;

  constructor(private http: HttpClient) {}

  submitResignation(req: { resignationDate: string; noticePeriodDays: number; lastWorkingDay: string; reason: string }): Observable<ExitRequest> {
    return this.http.post<ExitRequest>(`${this.baseUrl}/resign`, req);
  }

  getMyExitRequests(): Observable<ExitRequest[]> {
    return this.http.get<ExitRequest[]>(`${this.baseUrl}/my-requests`);
  }

  getAllExitRequests(): Observable<ExitRequest[]> {
    return this.http.get<ExitRequest[]>(`${this.baseUrl}/requests`);
  }

  getExitRequestById(id: number): Observable<ExitRequest> {
    return this.http.get<ExitRequest>(`${this.baseUrl}/requests/${id}`);
  }

  processExitRequest(id: number, action: { approved: boolean; remarks?: string; agreedLastWorkingDay?: string }): Observable<ExitRequest> {
    return this.http.post<ExitRequest>(`${this.baseUrl}/requests/${id}/action`, action);
  }

  updateClearanceItem(itemId: number, update: { status: string; remarks?: string }): Observable<ClearanceItem> {
    return this.http.patch<ClearanceItem>(`${this.baseUrl}/clearance/${itemId}`, update);
  }

  finalizeSettlement(settlementId: number): Observable<FnFSettlement> {
    return this.http.post<FnFSettlement>(`${this.baseUrl}/settlement/${settlementId}/finalize`, {});
  }
}
