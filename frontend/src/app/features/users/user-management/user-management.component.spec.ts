import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserManagementComponent } from './user-management.component';
import { UserService } from '../../../core/services/user.service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { UserResponse } from '../../../core/models/user.model';
import { PagedResponse } from '../../../core/models/paged-response.model';

describe('UserManagementComponent', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;
  let userServiceSpy: any;

  const mockUser: UserResponse = {
    id: 1,
    username: 'admin',
    email: 'admin@hrgenius.com',
    enabled: true,
    mustChangePassword: false,
    failedAttempts: 0,
    locked: false,
    roles: ['ROLE_ADMIN'],
    employeeId: 10,
    employeeCode: 'EMP001',
    employeeName: 'Admin User'
  };

  const mockPagedResponse: PagedResponse<UserResponse> = {
    content: [mockUser],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    last: true
  };

  beforeEach(async () => {
    userServiceSpy = {
      getUsers: vi.fn().mockReturnValue(of(mockPagedResponse)),
      updateStatus: vi.fn(),
      unlockUser: vi.fn(),
      updateRoles: vi.fn(),
      resetPassword: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [UserManagementComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: UserService, useValue: userServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load users on init', () => {
    expect(component).toBeTruthy();
    expect(userServiceSpy.getUsers).toHaveBeenCalled();
    expect(component.users.length).toBe(1);
    expect(component.users[0].username).toBe('admin');
  });

  it('should toggle user status', () => {
    const updatedUser = { ...mockUser, enabled: false };
    userServiceSpy.updateStatus.mockReturnValue(of(updatedUser));

    component.toggleStatus(component.users[0]);
    expect(userServiceSpy.updateStatus).toHaveBeenCalledWith(1, false);
    expect(component.users[0].enabled).toBe(false);
  });

  it('should unlock user account', () => {
    const lockedUser = { ...mockUser, locked: true, failedAttempts: 5 };
    component.users = [lockedUser];
    const unlockedUser = { ...mockUser, locked: false, failedAttempts: 0 };
    userServiceSpy.unlockUser.mockReturnValue(of(unlockedUser));

    component.unlockAccount(component.users[0]);
    expect(userServiceSpy.unlockUser).toHaveBeenCalledWith(1);
    expect(component.users[0].locked).toBe(false);
    expect(component.users[0].failedAttempts).toBe(0);
  });

  it('should update user roles', () => {
    const updatedUser = { ...mockUser, roles: ['ROLE_ADMIN', 'ROLE_HR_MANAGER'] };
    userServiceSpy.updateRoles.mockReturnValue(of(updatedUser));

    component.toggleRole(component.users[0], 'HR_MANAGER');
    expect(userServiceSpy.updateRoles).toHaveBeenCalled();
  });
});
