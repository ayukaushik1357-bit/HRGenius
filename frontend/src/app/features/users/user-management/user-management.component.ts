import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { UserService } from '../../../core/services/user.service';
import { UserResponse } from '../../../core/models/user.model';
import { PagedResponse } from '../../../core/models/paged-response.model';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatMenuModule
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  users: UserResponse[] = [];
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  searchQuery = '';
  selectedStatus: 'all' | 'true' | 'false' = 'all';
  loading = false;

  availableRoles = ['ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE'];
  temporaryPasswordNotice: { username: string; tempPass: string } | null = null;

  displayedColumns = [
    'user',
    'employee',
    'roles',
    'status',
    'security',
    'actions'
  ];

  constructor(
    private userService: UserService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    const enabledFilter =
      this.selectedStatus === 'all'
        ? undefined
        : this.selectedStatus === 'true';

    this.userService
      .getUsers(this.searchQuery, enabledFilter, this.currentPage, this.pageSize)
      .subscribe({
        next: (page: PagedResponse<UserResponse>) => {
          this.users = page.content;
          this.totalElements = page.totalElements;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          this.snackBar.open(err.error?.message || 'Failed to load users', 'Close', { duration: 4000 });
        }
      });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadUsers();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadUsers();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  toggleStatus(user: UserResponse): void {
    const newStatus = !user.enabled;
    this.userService.updateStatus(user.id, newStatus).subscribe({
      next: (updated) => {
        const wasPending = user.pendingApproval;
        user.enabled = updated.enabled;
        user.pendingApproval = updated.pendingApproval;
        this.snackBar.open(
          wasPending
            ? `Approved '${user.username}'. They can sign in now.`
            : `User '${user.username}' is now ${updated.enabled ? 'Enabled' : 'Disabled'}`,
          'OK',
          { duration: 3000 }
        );
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to update user status', 'Close', { duration: 4000 });
      }
    });
  }

  unlockAccount(user: UserResponse): void {
    this.userService.unlockUser(user.id).subscribe({
      next: (updated) => {
        user.locked = updated.locked;
        user.failedAttempts = updated.failedAttempts;
        user.lockoutTime = updated.lockoutTime;
        this.snackBar.open(`User '${user.username}' account has been unlocked`, 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to unlock user', 'Close', { duration: 4000 });
      }
    });
  }

  toggleRole(user: UserResponse, role: string): void {
    const currentRoles = new Set(user.roles.map(r => r.replace('ROLE_', '')));
    if (currentRoles.has(role)) {
      if (currentRoles.size === 1) {
        this.snackBar.open('User must have at least one role', 'Close', { duration: 3000 });
        return;
      }
      currentRoles.delete(role);
    } else {
      currentRoles.add(role);
    }

    const updatedRoleList = Array.from(currentRoles);
    this.userService.updateRoles(user.id, updatedRoleList).subscribe({
      next: (updated) => {
        user.roles = updated.roles;
        this.snackBar.open(`Roles updated for '${user.username}'`, 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to update roles', 'Close', { duration: 4000 });
      }
    });
  }

  hasRole(user: UserResponse, role: string): boolean {
    return user.roles.some(r => r.replace('ROLE_', '') === role);
  }

  resetPassword(user: UserResponse): void {
    this.userService.resetPassword(user.id).subscribe({
      next: (res) => {
        user.mustChangePassword = true;
        this.temporaryPasswordNotice = {
          username: user.username,
          tempPass: res.temporaryPassword
        };
        this.snackBar.open(`Password reset for '${user.username}'. Temporary password generated.`, 'OK', {
          duration: 6000
        });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to reset password', 'Close', { duration: 4000 });
      }
    });
  }

  closeNotice(): void {
    this.temporaryPasswordNotice = null;
  }
}
