import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthService } from '../../core/services/auth.service';
import { AppNotificationService } from '../../core/services/app-notification.service';
import { AppNotification } from '../../core/models/app-notification.model';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, SharedModule],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss']
})
export class MainLayoutComponent implements OnInit {
  authService = inject(AuthService);
  notificationService = inject(AppNotificationService);

  get username(): string {
    return this.authService.currentUser()?.username || 'User';
  }

  get userRoleDisplay(): string {
    const roles = this.authService.currentUser()?.roles || [];
    if (roles.includes('ROLE_ADMIN')) return 'Admin';
    if (roles.includes('ROLE_HR_MANAGER')) return 'HR Manager';
    if (roles.includes('ROLE_MANAGER')) return 'Manager';
    if (roles.includes('ROLE_EMPLOYEE')) return 'Employee';
    return 'User';
  }

  get isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  get isAdminOrHr(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  }

  get canAccessEmployees(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER', 'MANAGER']);
  }

  get canAccessAnalytics(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER', 'MANAGER']);
  }

  get canAccessRecruitment(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  }

  ngOnInit(): void {
    this.notificationService.fetchUnreadCount();
    // Poll notifications every 30 seconds
    setInterval(() => {
      this.notificationService.fetchUnreadCount();
    }, 30000);
  }

  loadNotifications(): void {
    this.notificationService.fetchNotifications().subscribe();
  }

  markRead(n: AppNotification): void {
    if (!n.read) {
      this.notificationService.markAsRead(n.id).subscribe();
    }
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe();
  }

  logout(): void {
    this.authService.logout();
  }
}
