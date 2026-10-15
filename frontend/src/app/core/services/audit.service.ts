import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PagedAuditLog } from '../models/audit.model';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly baseUrl = `${environment.apiUrl}/audit`;

  constructor(private http: HttpClient) {}

  getAuditLogs(page: number = 0, size: number = 20): Observable<PagedAuditLog> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedAuditLog>(`${this.baseUrl}/logs`, { params });
  }
}
