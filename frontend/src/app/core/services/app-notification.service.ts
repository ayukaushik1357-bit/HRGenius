import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AppNotification } from '../models/app-notification.model';

@Injectable({
  providedIn: 'root'
})
export class AppNotificationService {
  private readonly baseUrl = `${environment.apiUrl}/notifications`;

  unreadCount = signal<number>(0);
  notifications = signal<AppNotification[]>([]);

  constructor(private http: HttpClient) {}

  fetchUnreadCount(): void {
    this.http.get<{ unreadCount: number }>(`${this.baseUrl}/unread-count`).subscribe({
      next: res => this.unreadCount.set(res.unreadCount),
      error: () => this.unreadCount.set(0)
    });
  }

  fetchNotifications(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(this.baseUrl).pipe(
      tap(list => {
        this.notifications.set(list);
        this.unreadCount.set(list.filter(n => !n.read).length);
      })
    );
  }

  markAsRead(id: number): Observable<any> {
    return this.http.patch(`${this.baseUrl}/${id}/read`, {}).pipe(
      tap(() => {
        this.notifications.update(list => list.map(n => n.id === id ? { ...n, read: true } : n));
        this.unreadCount.update(c => Math.max(0, c - 1));
      })
    );
  }

  markAllAsRead(): Observable<any> {
    return this.http.patch(`${this.baseUrl}/read-all`, {}).pipe(
      tap(() => {
        this.notifications.update(list => list.map(n => ({ ...n, read: true })));
        this.unreadCount.set(0);
      })
    );
  }
}
