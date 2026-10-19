import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    // Public home page; signed-in users get a button to their workspace
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/public/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'auth/register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'auth/forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'auth/reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'auth/change-password',
    loadComponent: () => import('./features/auth/change-password/change-password.component').then(m => m.ChangePasswordComponent)
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'portal',
        loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent)
      },
      {
        path: 'analytics',
        loadComponent: () => import('./features/analytics/analytics.component').then(m => m.AnalyticsComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'MANAGER'] }
      },
      {
        path: 'employees',
        loadComponent: () => import('./features/employees/employee-list/employee-list.component').then(m => m.EmployeeListComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'MANAGER'] }
      },
      {
        path: 'employees/new',
        loadComponent: () => import('./features/employees/employee-form/employee-form.component').then(m => m.EmployeeFormComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] }
      },
      {
        path: 'employees/:id',
        loadComponent: () => import('./features/employees/employee-profile/employee-profile.component').then(m => m.EmployeeProfileComponent)
      },
      {
        path: 'employees/:id/edit',
        loadComponent: () => import('./features/employees/employee-form/employee-form.component').then(m => m.EmployeeFormComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] }
      },
      {
        path: 'departments',
        loadComponent: () => import('./features/departments/department-list/department-list.component').then(m => m.DepartmentListComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] }
      },
      {
        path: 'designations',
        loadComponent: () => import('./features/designations/designation-list/designation-list.component').then(m => m.DesignationListComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] }
      },
      {
        path: 'recruitment',
        loadComponent: () => import('./features/recruitment/recruitment.component').then(m => m.RecruitmentComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] }
      },
      {
        path: 'onboarding',
        loadComponent: () => import('./features/onboarding/onboarding.component').then(m => m.OnboardingComponent)
      },
      {
        path: 'attendance',
        loadComponent: () => import('./features/attendance/attendance.component').then(m => m.AttendanceComponent)
      },
      {
        path: 'leaves',
        loadComponent: () => import('./features/leaves/leaves.component').then(m => m.LeavesComponent)
      },
      {
        path: 'performance',
        loadComponent: () => import('./features/performance/performance.component').then(m => m.PerformanceComponent)
      },
      {
        path: 'payroll',
        loadComponent: () => import('./features/payroll/payroll.component').then(m => m.PayrollComponent)
      },
      {
        path: 'offboarding',
        loadComponent: () => import('./features/offboarding/offboarding.component').then(m => m.OffboardingComponent)
      },
      {
        path: 'audit',
        loadComponent: () => import('./features/audit/audit.component').then(m => m.AuditComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] }
      },
      {
        path: 'users',
        loadComponent: () => import('./features/users/user-management/user-management.component').then(m => m.UserManagementComponent),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'portal'
  }
];
