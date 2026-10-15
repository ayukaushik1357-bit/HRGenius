import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Department, DepartmentRequest } from '../models/department.model';

@Injectable({
  providedIn: 'root'
})
export class DepartmentService {
  private readonly apiUrl = `${environment.apiUrl}/departments`;

  constructor(private http: HttpClient) {}

  getAllDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(this.apiUrl);
  }

  getDepartmentById(id: number): Observable<Department> {
    return this.http.get<Department>(`${this.apiUrl}/${id}`);
  }

  createDepartment(request: DepartmentRequest): Observable<Department> {
    return this.http.post<Department>(this.apiUrl, request);
  }

  updateDepartment(id: number, request: DepartmentRequest): Observable<Department> {
    return this.http.put<Department>(`${this.apiUrl}/${id}`, request);
  }

  deleteDepartment(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/${id}`);
  }
}
