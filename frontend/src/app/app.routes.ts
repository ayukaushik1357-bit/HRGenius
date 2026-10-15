import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'portal'
      },
      {
        path: 'portal',
        loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent)
      },
      {
        path: 'analytics',
        loadComponent: () => import('./features/analytics/analytics.component').then(m => m.AnalyticsComponent)
      },
      {
        path: 'employees',
        loadComponent: () => import('./features/employees/employee-list/employee-list.component').then(m => m.EmployeeListComponent)
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
        loadComponent: () => import('./features/recruitment/recruitment.component').then(m => m.RecruitmentComponent)
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
        data: { roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'portal'
  }
];
