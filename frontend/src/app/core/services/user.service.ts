import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse, UserStatusRequest, UserRolesRequest } from '../models/user.model';
import { PagedResponse } from '../models/paged-response.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}/users`;
  private readonly authUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  getUsers(
    search?: string,
    enabled?: boolean,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'id',
    sortDir: string = 'asc'
  ): Observable<PagedResponse<UserResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    if (enabled !== undefined && enabled !== null) {
      params = params.set('enabled', enabled.toString());
    }

    return this.http.get<PagedResponse<UserResponse>>(this.apiUrl, { params });
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  updateStatus(id: number, enabled: boolean): Observable<UserResponse> {
    const body: UserStatusRequest = { enabled };
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}/status`, body);
  }

  unlockUser(id: number): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/${id}/unlock`, {});
  }

  updateRoles(id: number, roles: string[]): Observable<UserResponse> {
    const body: UserRolesRequest = { roles };
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}/roles`, body);
  }

  resetPassword(id: number): Observable<{ temporaryPassword: string; message: string }> {
    return this.http.post<{ temporaryPassword: string; message: string }>(`${this.authUrl}/reset-password/${id}`, {});
  }
}
