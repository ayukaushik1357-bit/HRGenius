import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Employee, EmployeeRequest, DocumentMetadata } from '../models/employee.model';
import { PagedResponse } from '../models/paged-response.model';

export interface EmployeeSearchParams {
  name?: string;
  departmentId?: number;
  status?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {
  private readonly apiUrl = `${environment.apiUrl}/employees`;
  private readonly docsUrl = `${environment.apiUrl}/documents`;

  constructor(private http: HttpClient) {}

  searchEmployees(params: EmployeeSearchParams): Observable<PagedResponse<Employee>> {
    let httpParams = new HttpParams();

    if (params.name) httpParams = httpParams.set('name', params.name);
    if (params.departmentId) httpParams = httpParams.set('departmentId', params.departmentId.toString());
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
    if (params.sortDir) httpParams = httpParams.set('sortDir', params.sortDir);

    return this.http.get<PagedResponse<Employee>>(this.apiUrl, { params: httpParams });
  }

  getEmployeeById(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.apiUrl}/${id}`);
  }

  createEmployee(request: EmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(this.apiUrl, request);
  }

  updateEmployee(id: number, request: EmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.apiUrl}/${id}`, request);
  }

  deleteEmployee(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/${id}`);
  }

  uploadDocument(employeeId: number, documentType: string, file: File): Observable<DocumentMetadata> {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);

    return this.http.post<DocumentMetadata>(`${this.apiUrl}/${employeeId}/documents`, formData);
  }

  getEmployeeDocuments(employeeId: number): Observable<DocumentMetadata[]> {
    return this.http.get<DocumentMetadata[]>(`${this.apiUrl}/${employeeId}/documents`);
  }

  downloadDocument(documentId: number): Observable<Blob> {
    return this.http.get(`${this.docsUrl}/${documentId}/download`, {
      responseType: 'blob'
    });
  }

  deleteDocument(documentId: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.docsUrl}/${documentId}`);
  }
}
