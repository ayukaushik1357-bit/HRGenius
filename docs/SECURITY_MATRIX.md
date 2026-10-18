# HRGenius API Security Matrix

This document defines the complete access control specification for all REST endpoints across HRGenius.
Every endpoint enforces explicit role-based access control (RBAC) and ownership/hierarchy rules via Spring Security (`@PreAuthorize`) and the reusable `@authz` authorization evaluator component. No protected endpoint relies on default authenticated permissions.

---

## Roles Overview
- **ADMIN**: System super-administrator. Full system-wide access to all configurations, user management, audit logs, and employee records.
- **HR_MANAGER**: Human Resources manager. Full access to employee lifecycles, payroll runs, onboarding, offboarding, recruitment, and company-wide analytics.
- **MANAGER**: Team people manager. Access scoped strictly to direct reports (hierarchy checks via `reporting_manager_id`) and self.
- **EMPLOYEE**: Standard staff member. Access scoped strictly to own records (`isSelf` / `currentEmployeeId`).

---

## 1. Authentication & Session Management (`/api/auth`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| POST | `/api/auth/login` | Anonymous / Public | Rate-limited (5 attempts/min per IP). Validates active user and credentials. |
| POST | `/api/auth/refresh` | Anonymous / Public (Cookie) | Requires valid `type=refresh` JWT in HttpOnly cookie; DB hash validation with single-use rotation. |
| POST | `/api/auth/logout` | Authenticated | Revokes refresh token in database and clears authentication cookie. |
| POST | `/api/auth/logout-all` | Authenticated | Revokes all active refresh tokens for the current user across all devices. |
| POST | `/api/auth/change-password` | Authenticated (Self) | Enforces password complexity rules and current password verification; clears `must_change_password`. |
| POST | `/api/auth/reset-password` | ADMIN | Resets user password to temporary password with `must_change_password = true`. |
| POST | `/api/auth/forgot-password` | Anonymous / Public | Generates password reset link if account exists. |

---

## 2. User Account Administration (`/api/users`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/users` | ADMIN | `@PreAuthorize("hasRole('ADMIN')")` — Paginated user directory. |
| GET | `/api/users/{id}` | ADMIN | `@PreAuthorize("hasRole('ADMIN')")` — Single user profile details. |
| PATCH | `/api/users/{id}/status` | ADMIN | `@PreAuthorize("hasRole('ADMIN')")` — Enable or disable account. |
| PUT | `/api/users/{id}/roles` | ADMIN | `@PreAuthorize("hasRole('ADMIN')")` — Assign or revoke roles. |
| POST | `/api/users/{id}/unlock` | ADMIN | `@PreAuthorize("hasRole('ADMIN')")` — Unlock locked account. |

---

## 3. Employee Directory & Profiles (`/api/employees`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/employees` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` — Scoped: MANAGER sees only direct reports + self. |
| GET | `/api/employees/{id}` | ADMIN, HR_MANAGER, MANAGER (Team), Self | `@PreAuthorize("@authz.canViewEmployee(#id)")` — Sensitive fields (salary, PAN, Aadhaar, bank) masked for others. |
| POST | `/api/employees` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Creates linked User account with temporary credentials. |
| PUT | `/api/employees/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Update employee demographic and job profile. |
| DELETE | `/api/employees/{id}` | ADMIN | `@PreAuthorize("hasRole('ADMIN')")` — Soft delete / deactivate employee profile. |
| GET | `/api/employees/search` | ADMIN, HR_MANAGER, MANAGER | Search employees with department and status filters. |

---

## 4. Departments & Designations (`/api/departments`, `/api/designations`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/departments` | Authenticated | Read list of company departments. |
| GET | `/api/departments/{id}` | Authenticated | Read single department details. |
| POST | `/api/departments` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| PUT | `/api/departments/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| DELETE | `/api/departments/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/designations` | Authenticated | Read list of job designations. |
| GET | `/api/designations/{id}` | Authenticated | Read single designation details. |
| POST | `/api/designations` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| PUT | `/api/designations/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| DELETE | `/api/designations/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |

---

## 5. Attendance & Holidays (`/api/attendance`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| POST | `/api/attendance/check-in` | Authenticated (Self) | Punch check-in timestamp for current employee. |
| POST | `/api/attendance/check-out` | Authenticated (Self) | Punch check-out timestamp for current employee. |
| GET | `/api/attendance/status` | Authenticated (Self) | Current employee today's punch status. |
| GET | `/api/attendance/monthly` | ADMIN, HR_MANAGER, MANAGER (Team), Self | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or @authz.canViewEmployee(#employeeId)")` |
| GET | `/api/attendance/today` | ADMIN, HR_MANAGER, MANAGER | Daily attendance overview for HR and team managers. |
| GET | `/api/attendance/holidays` | Authenticated | View company holiday calendar for given year. |
| POST | `/api/attendance/holidays` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Add company holiday. |
| DELETE | `/api/attendance/holidays/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Delete company holiday. |

---

## 6. Leave Management (`/api/leaves`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| POST | `/api/leaves/apply` | Authenticated (Self) | Current employee submits leave application. |
| GET | `/api/leaves/my-leaves` | Authenticated (Self) | List leave requests submitted by logged-in employee. |
| GET | `/api/leaves/employee/{id}` | ADMIN, HR_MANAGER, MANAGER (Team), Self | `@PreAuthorize("@authz.canViewEmployee(#employeeId)")` — Employee leave history. |
| GET | `/api/leaves` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` — Scoped: Manager sees only team leaves. |
| GET | `/api/leaves/balance/{id}` | ADMIN, HR_MANAGER, MANAGER (Team), Self | `@PreAuthorize("@authz.canViewEmployee(#employeeId)")` — Leave balances per policy. |
| POST | `/api/leaves/{id}/action` | ADMIN, HR_MANAGER, MANAGER (Direct Report) | `@PreAuthorize("@authz.canApproveLeave(#id)")` — Strictly prohibits self-approval. |
| POST | `/api/leaves/{id}/cancel` | Authenticated (Owner) | Cancel own pending leave application. |

---

## 7. Document Repository (`/api/documents`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| POST | `/api/documents/upload` | ADMIN, HR_MANAGER, Self | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or @authz.isSelf(#employeeId)")` — Strict MIME/extension & size limits. |
| GET | `/api/documents/employee/{id}` | ADMIN, HR_MANAGER, Self | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or @authz.isSelf(#employeeId)")` — List documents. |
| GET | `/api/documents/{id}/download` | ADMIN, HR_MANAGER, Owner | `@PreAuthorize("@authz.canAccessDocument(#id)")` — Download file resource with path traversal defense. |
| DELETE | `/api/documents/{id}` | ADMIN, HR_MANAGER, Owner | `@PreAuthorize("@authz.canAccessDocument(#id)")` — Delete document record and storage file. |

---

## 8. Payroll & Payslips (`/api/payroll`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/payroll/salary-structure/{empId}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/payroll/salary-structure` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/payroll/runs` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Monthly payroll runs. |
| GET | `/api/payroll/runs/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/payroll/runs` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Execute monthly calculation. |
| POST | `/api/payroll/runs/{id}/pay` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Lock and mark as PAID. |
| GET | `/api/payroll/runs/{id}/payslips` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/payroll/my-payslips` | Authenticated (Self) | Retrieve payslips of logged-in employee. |
| GET | `/api/payroll/employee/{id}/payslips` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/payroll/payslips/{id}` | ADMIN, HR_MANAGER, Self | `@PreAuthorize("@authz.canAccessPayslip(#id)")` |
| GET | `/api/payroll/payslips/{id}/pdf` | ADMIN, HR_MANAGER, Self | `@PreAuthorize("@authz.canAccessPayslip(#id)")` — PDF download. |

---

## 9. Performance Management (`/api/performance`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/performance/cycles` | Authenticated | View active and archived appraisal review cycles. |
| POST | `/api/performance/cycles` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/performance/goals` | ADMIN, HR_MANAGER, MANAGER (Team), Self | IDOR protected via `currentUserService.getEmployeeIdForUser`. |
| POST | `/api/performance/goals` | ADMIN, HR_MANAGER, MANAGER (Team), Self | IDOR protected goal creation. |
| PATCH | `/api/performance/goals/{id}/progress` | Authenticated (Goal Owner / Mgr) | Update goal progress completion percentage. |
| GET | `/api/performance/reviews/cycle/{cycleId}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/performance/reviews/team` | ADMIN, HR_MANAGER, MANAGER | Direct report reviews for people manager. |
| GET | `/api/performance/reviews/my` | Authenticated (Self) | Review appraisal of logged-in employee. |
| POST | `/api/performance/reviews/{id}/self-review` | Self (Employee of Review) | `@PreAuthorize("@authz.canSubmitSelfReview(#reviewId)")` |
| POST | `/api/performance/reviews/{id}/manager-review` | Direct Manager of Employee | `@PreAuthorize("@authz.canSubmitManagerReview(#reviewId)")` — Prohibits manager self-appraisal. |
| POST | `/api/performance/360-feedback` | Peers (Excluding Self) | `@PreAuthorize("@authz.canSubmitPeerFeedback(#request.performanceReviewId)")` |

---

## 10. Recruitment (`/api/recruitment`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/recruitment/jobs` | Authenticated | Browse job openings. |
| GET | `/api/recruitment/jobs/{id}` | Authenticated | View job details. |
| POST | `/api/recruitment/jobs` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| PUT | `/api/recruitment/jobs/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| DELETE | `/api/recruitment/jobs/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/recruitment/candidates` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/recruitment/candidates/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/recruitment/candidates` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/recruitment/candidates/{id}/resume` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/recruitment/candidates/{id}/resume` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/recruitment/applications` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/recruitment/applications` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| PATCH | `/api/recruitment/applications/{id}/stage` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` |
| GET | `/api/recruitment/interviews/application/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/recruitment/interviews` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` |
| PUT | `/api/recruitment/interviews/{id}/feedback` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` |
| GET | `/api/recruitment/offers/application/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/recruitment/offers` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/recruitment/hire` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Auto-creates linked user account. |

---

## 11. Onboarding (`/api/onboarding`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/onboarding/templates` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/onboarding/templates/{id}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/onboarding/templates` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| GET | `/api/onboarding/employees/{id}/progress` | ADMIN, HR_MANAGER, MANAGER (Team), Self | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or @authz.canViewEmployee(#employeeId)")` |
| POST | `/api/onboarding/employees/{id}/assign-template/{tid}` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/onboarding/employees/{id}/tasks` | ADMIN, HR_MANAGER, MANAGER (Team) | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER') or @authz.isManagerOf(#employeeId)")` |
| PATCH | `/api/onboarding/tasks/{id}/status` | Authenticated (Task Assignee / HR) | Update checklist task status. |

---

## 12. Offboarding & FnF (`/api/offboarding`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| POST | `/api/offboarding/resign` | Authenticated (Self) | Submit employee resignation. |
| GET | `/api/offboarding/my-requests` | Authenticated (Self) | List own exit requests. |
| GET | `/api/offboarding/requests` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` |
| GET | `/api/offboarding/requests/{id}` | ADMIN, HR_MANAGER, MANAGER (Team), Self | `@PreAuthorize("@authz.canAccessExitRequest(#id)")` |
| POST | `/api/offboarding/requests/{id}/action` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` |
| PATCH | `/api/offboarding/clearance/{id}` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` |
| POST | `/api/offboarding/settlement/{id}/finalize` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |

---

## 13. HR Analytics (`/api/analytics`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/analytics/dashboard` | ADMIN, HR_MANAGER, MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'MANAGER')")` — Company-wide for Admin/HR; scoped to team for Manager. |
| GET | `/api/analytics/export/excel` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Executive company report. |
| GET | `/api/analytics/export/pdf` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Executive company report. |

---

## 14. Compliance & Audit Logs (`/api/audit`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/audit/logs` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` — Immutable audit trail. |

---

## 15. In-App Notifications (`/api/notifications`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/notifications` | Authenticated (Self) | Get current user notifications. |
| GET | `/api/notifications/unread-count` | Authenticated (Self) | Badge count for logged-in user. |
| PATCH | `/api/notifications/{id}/read` | Owner (Self) | `@PreAuthorize("@authz.isNotificationOwner(#id)")` |
| PATCH | `/api/notifications/read-all` | Authenticated (Self) | Mark all own notifications as read. |

---

## 16. Employee Self-Service Portal (`/api/portal`)

| Method | Endpoint | Allowed Callers | Authorization Rule / Notes |
|---|---|---|---|
| GET | `/api/portal/dashboard` | Authenticated (Self) | Home dashboard for personal leave, attendance, payslips. |
| POST | `/api/portal/profile-update` | Authenticated (Self) | Request profile changes for HR review. |
| GET | `/api/portal/profile-updates/my` | Authenticated (Self) | Status of own profile change requests. |
| GET | `/api/portal/profile-updates` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
| POST | `/api/portal/profile-updates/{id}/action` | ADMIN, HR_MANAGER | `@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")` |
