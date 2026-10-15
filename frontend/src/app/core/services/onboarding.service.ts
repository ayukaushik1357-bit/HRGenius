import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OnboardingTemplate, OnboardingProgress, OnboardingTask } from '../models/onboarding.model';

@Injectable({
  providedIn: 'root'
})
export class OnboardingService {
  private readonly baseUrl = `${environment.apiUrl}/onboarding`;

  constructor(private http: HttpClient) {}

  getTemplates(): Observable<OnboardingTemplate[]> {
    return this.http.get<OnboardingTemplate[]>(`${this.baseUrl}/templates`);
  }

  getTemplateById(id: number): Observable<OnboardingTemplate> {
    return this.http.get<OnboardingTemplate>(`${this.baseUrl}/templates/${id}`);
  }

  createTemplate(template: Partial<OnboardingTemplate>): Observable<OnboardingTemplate> {
    return this.http.post<OnboardingTemplate>(`${this.baseUrl}/templates`, template);
  }

  getEmployeeProgress(employeeId: number): Observable<OnboardingProgress> {
    return this.http.get<OnboardingProgress>(`${this.baseUrl}/employees/${employeeId}/progress`);
  }

  assignTemplate(employeeId: number, templateId: number, ownerId?: number): Observable<any> {
    const url = ownerId ?
      `${this.baseUrl}/employees/${employeeId}/assign-template/${templateId}?ownerId=${ownerId}` :
      `${this.baseUrl}/employees/${employeeId}/assign-template/${templateId}`;
    return this.http.post(url, {});
  }

  updateTaskStatus(taskId: number, status: string): Observable<OnboardingTask> {
    return this.http.patch<OnboardingTask>(`${this.baseUrl}/tasks/${taskId}/status`, { status });
  }

  addTask(employeeId: number, task: Partial<OnboardingTask>): Observable<OnboardingTask> {
    return this.http.post<OnboardingTask>(`${this.baseUrl}/employees/${employeeId}/tasks`, task);
  }
}
