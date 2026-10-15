import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Designation, DesignationRequest } from '../models/designation.model';

@Injectable({
  providedIn: 'root'
})
export class DesignationService {
  private readonly apiUrl = `${environment.apiUrl}/designations`;

  constructor(private http: HttpClient) {}

  getAllDesignations(): Observable<Designation[]> {
    return this.http.get<Designation[]>(this.apiUrl);
  }

  getDesignationById(id: number): Observable<Designation> {
    return this.http.get<Designation>(`${this.apiUrl}/${id}`);
  }

  createDesignation(request: DesignationRequest): Observable<Designation> {
    return this.http.post<Designation>(this.apiUrl, request);
  }

  updateDesignation(id: number, request: DesignationRequest): Observable<Designation> {
    return this.http.put<Designation>(`${this.apiUrl}/${id}`, request);
  }

  deleteDesignation(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/${id}`);
  }
}
