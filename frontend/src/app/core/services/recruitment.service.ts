import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JobPosting, Candidate, Application, Interview, Offer, HireCandidateRequest } from '../models/recruitment.model';
import { Employee } from '../models/employee.model';

@Injectable({
  providedIn: 'root'
})
export class RecruitmentService {
  private readonly baseUrl = `${environment.apiUrl}/recruitment`;

  constructor(private http: HttpClient) {}

  // Job Postings
  getJobPostings(status?: string): Observable<JobPosting[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<JobPosting[]>(`${this.baseUrl}/jobs`, { params });
  }

  getJobPostingById(id: number): Observable<JobPosting> {
    return this.http.get<JobPosting>(`${this.baseUrl}/jobs/${id}`);
  }

  createJobPosting(request: Partial<JobPosting>): Observable<JobPosting> {
    return this.http.post<JobPosting>(`${this.baseUrl}/jobs`, request);
  }

  updateJobPosting(id: number, request: Partial<JobPosting>): Observable<JobPosting> {
    return this.http.put<JobPosting>(`${this.baseUrl}/jobs/${id}`, request);
  }

  deleteJobPosting(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/jobs/${id}`);
  }

  // Candidates
  getCandidates(): Observable<Candidate[]> {
    return this.http.get<Candidate[]>(`${this.baseUrl}/candidates`);
  }

  getCandidateById(id: number): Observable<Candidate> {
    return this.http.get<Candidate>(`${this.baseUrl}/candidates/${id}`);
  }

  createCandidate(request: Partial<Candidate>): Observable<Candidate> {
    return this.http.post<Candidate>(`${this.baseUrl}/candidates`, request);
  }

  uploadResume(candidateId: number, file: File): Observable<Candidate> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Candidate>(`${this.baseUrl}/candidates/${candidateId}/resume`, formData);
  }

  downloadResume(candidateId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/candidates/${candidateId}/resume`, { responseType: 'blob' });
  }

  // Applications
  getApplications(jobPostingId?: number, stage?: string): Observable<Application[]> {
    let params = new HttpParams();
    if (jobPostingId) params = params.set('jobPostingId', jobPostingId.toString());
    if (stage) params = params.set('stage', stage);
    return this.http.get<Application[]>(`${this.baseUrl}/applications`, { params });
  }

  createApplication(jobPostingId: number, candidateId: number, notes?: string): Observable<Application> {
    return this.http.post<Application>(`${this.baseUrl}/applications`, { jobPostingId, candidateId, notes });
  }

  updateStage(applicationId: number, stage: string): Observable<Application> {
    return this.http.patch<Application>(`${this.baseUrl}/applications/${applicationId}/stage`, { stage });
  }

  // Interviews
  getInterviews(applicationId: number): Observable<Interview[]> {
    return this.http.get<Interview[]>(`${this.baseUrl}/interviews/application/${applicationId}`);
  }

  scheduleInterview(request: Partial<Interview>): Observable<Interview> {
    return this.http.post<Interview>(`${this.baseUrl}/interviews`, request);
  }

  submitInterviewFeedback(id: number, rating: number, feedback: string, status?: string): Observable<Interview> {
    return this.http.put<Interview>(`${this.baseUrl}/interviews/${id}/feedback`, { rating, feedback, status });
  }

  // Offers
  getOffer(applicationId: number): Observable<Offer> {
    return this.http.get<Offer>(`${this.baseUrl}/offers/application/${applicationId}`);
  }

  saveOffer(request: Partial<Offer>): Observable<Offer> {
    return this.http.post<Offer>(`${this.baseUrl}/offers`, request);
  }

  // Convert to Employee
  hireCandidate(request: HireCandidateRequest): Observable<Employee> {
    return this.http.post<Employee>(`${this.baseUrl}/hire`, request);
  }
}
