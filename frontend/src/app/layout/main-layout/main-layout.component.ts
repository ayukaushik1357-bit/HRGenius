import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, SharedModule],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss']
})
export class MainLayoutComponent {
  authService = inject(AuthService);

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

  get isAdminOrHr(): boolean {
    return this.authService.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  }

  logout(): void {
    this.authService.logout();
  }
}
