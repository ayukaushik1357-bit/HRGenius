import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ReviewCycle, Goal, GoalRequest, PerformanceReview, PeerFeedback } from '../models/performance.model';

@Injectable({
  providedIn: 'root'
})
export class PerformanceService {
  private readonly baseUrl = `${environment.apiUrl}/performance`;

  constructor(private http: HttpClient) {}

  getCycles(): Observable<ReviewCycle[]> {
    return this.http.get<ReviewCycle[]>(`${this.baseUrl}/cycles`);
  }

  createCycle(cycle: { name: string; startDate: string; endDate: string; status: string }): Observable<ReviewCycle> {
    return this.http.post<ReviewCycle>(`${this.baseUrl}/cycles`, cycle);
  }

  getGoals(employeeId?: number, cycleId?: number): Observable<Goal[]> {
    let params = new HttpParams();
    if (employeeId) params = params.set('employeeId', employeeId.toString());
    if (cycleId) params = params.set('cycleId', cycleId.toString());
    return this.http.get<Goal[]>(`${this.baseUrl}/goals`, { params });
  }

  createGoal(goal: GoalRequest): Observable<Goal> {
    return this.http.post<Goal>(`${this.baseUrl}/goals`, goal);
  }

  updateGoalProgress(goalId: number, progressPercentage: number): Observable<Goal> {
    return this.http.patch<Goal>(`${this.baseUrl}/goals/${goalId}/progress`, null, {
      params: { progressPercentage: progressPercentage.toString() }
    });
  }

  getReviewsForCycle(cycleId: number): Observable<PerformanceReview[]> {
    return this.http.get<PerformanceReview[]>(`${this.baseUrl}/reviews/cycle/${cycleId}`);
  }

  getTeamReviews(): Observable<PerformanceReview[]> {
    return this.http.get<PerformanceReview[]>(`${this.baseUrl}/reviews/team`);
  }

  getMyReview(cycleId: number): Observable<PerformanceReview> {
    return this.http.get<PerformanceReview>(`${this.baseUrl}/reviews/my`, {
      params: { cycleId: cycleId.toString() }
    });
  }

  submitSelfReview(reviewId: number, rating: number, comments: string): Observable<PerformanceReview> {
    return this.http.post<PerformanceReview>(`${this.baseUrl}/reviews/${reviewId}/self-review`, null, {
      params: { rating: rating.toString(), comments }
    });
  }

  submitManagerReview(reviewId: number, rating: number, comments: string): Observable<PerformanceReview> {
    return this.http.post<PerformanceReview>(`${this.baseUrl}/reviews/${reviewId}/manager-review`, null, {
      params: { rating: rating.toString(), comments }
    });
  }

  submitPeerFeedback(feedback: { performanceReviewId: number; feedback: string; rating?: number }): Observable<PeerFeedback> {
    return this.http.post<PeerFeedback>(`${this.baseUrl}/360-feedback`, feedback);
  }
}
