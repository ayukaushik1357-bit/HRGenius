import { TestBed } from '@angular/core/testing';
import { ChangePasswordComponent } from './change-password.component';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

describe('ChangePasswordComponent', () => {
  let component: ChangePasswordComponent;
  let authServiceSpy: any;
  let notificationServiceSpy: any;
  let routerSpy: any;

  beforeEach(async () => {
    authServiceSpy = {
      changePassword: vi.fn()
    };
    notificationServiceSpy = {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn()
    };
    routerSpy = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [ChangePasswordComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(ChangePasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with invalid form', () => {
    expect(component.changeForm.valid).toBe(false);
  });

  it('should reject password mismatch', () => {
    component.changeForm.setValue({
      currentPassword: 'OldPassword@123',
      newPassword: 'NewPassword@123',
      confirmPassword: 'DifferentPassword@123'
    });

    expect(component.changeForm.hasError('passwordMismatch')).toBe(true);
  });

  it('should reject weak password', () => {
    component.changeForm.setValue({
      currentPassword: 'OldPassword@123',
      newPassword: 'weak',
      confirmPassword: 'weak'
    });

    expect(component.changeForm.get('newPassword')?.valid).toBe(false);
  });

  it('should submit valid form and navigate to portal on success', () => {
    authServiceSpy.changePassword.mockReturnValue(of({ message: 'Password changed successfully' }));

    component.changeForm.setValue({
      currentPassword: 'OldPassword@123',
      newPassword: 'ValidNewPassword@123',
      confirmPassword: 'ValidNewPassword@123'
    });

    component.onSubmit();

    expect(authServiceSpy.changePassword).toHaveBeenCalledWith('OldPassword@123', 'ValidNewPassword@123');
    expect(notificationServiceSpy.success).toHaveBeenCalledWith('Password changed successfully!');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/portal']);
  });
});
