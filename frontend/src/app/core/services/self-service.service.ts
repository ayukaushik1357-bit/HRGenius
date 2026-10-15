import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PortalDashboard, ProfileUpdateRequest } from '../models/selfservice.model';

@Injectable({
  providedIn: 'root'
})
export class SelfServiceService {
  private readonly baseUrl = `${environment.apiUrl}/portal`;

  constructor(private http: HttpClient) {}

  getPortalDashboard(): Observable<PortalDashboard> {
    return this.http.get<PortalDashboard>(`${this.baseUrl}/dashboard`);
  }

  submitProfileUpdateRequest(changes: Record<string, any>): Observable<ProfileUpdateRequest> {
    return this.http.post<ProfileUpdateRequest>(`${this.baseUrl}/profile-update`, {
      requestedChanges: JSON.stringify(changes)
    });
  }

  getMyRequests(): Observable<ProfileUpdateRequest[]> {
    return this.http.get<ProfileUpdateRequest[]>(`${this.baseUrl}/profile-updates/my`);
  }

  getAllPendingRequests(): Observable<ProfileUpdateRequest[]> {
    return this.http.get<ProfileUpdateRequest[]>(`${this.baseUrl}/profile-updates`);
  }

  reviewProfileUpdate(id: number, approved: boolean, remarks?: string): Observable<ProfileUpdateRequest> {
    return this.http.post<ProfileUpdateRequest>(`${this.baseUrl}/profile-updates/${id}/action`, {
      approved,
      remarks
    });
  }
}
