# HRGenius — Enterprise HR Management System (Phases 1 & 2)

HRGenius is a modern, full-stack Human Resources Management System designed for scalable workforce management with high visual aesthetics, zero-trust security, and complete lifecycle coverage from recruitment to payroll.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 17+, Spring Boot 3.2.5, Spring Data JPA / Hibernate, Spring Security 6 (Stateless JWT), Flyway |
| **Frontend** | Angular 21 (latest stable), Angular Material, SCSS Design System, RxJS Signals |
| **Databases** | Oracle Database (Oracle XE for dev/prod with Flyway migrations V1–V7) & H2 (In-memory dev profile) |
| **Build & Tooling** | Maven, npm, SpringDoc OpenAPI 3.0 / Swagger UI, OpenPDF (Payslip PDF Generation), Spring Mail |

---

## 🏗️ Modules & Core Capabilities

### Phase 1 Modules
1. **Zero-Trust Security & Authentication**
   - Access token kept strictly in-memory in Angular client; refresh token stored in `HttpOnly`, `SameSite=Lax` secure cookies.
   - Role-Based Access Control (`ADMIN`, `HR_MANAGER`, `MANAGER`, `EMPLOYEE`).
2. **Workforce Management (Employees, Departments, Designations)**
   - Multi-field search, pagination, sorting with portable JPA specification criteria.
   - Employee profile with addresses, emergency contacts, and document repository upload/download.

### Phase 2 Modules
1. **Recruitment & Applicant Tracking System (ATS)**
   - `JobPosting`: Linked to department and designation with status (`DRAFT`, `OPEN`, `CLOSED`).
   - `Candidate` & `Application`: Pipeline Kanban board (`APPLIED` → `SCREENING` → `INTERVIEW` → `OFFERED` → `HIRED` → `REJECTED`).
   - `Interview`: Multi-round scheduling with assigned interviewers, ratings (1–5), and evaluation feedback.
   - `Offer`: CTC calculation and official offer generation.
   - **Hire to Employee Conversion**: 1-click conversion from hired candidate to an active employee profile, user account provisioning, salary structure setup, and automatic onboarding checklist assignment.
2. **Employee Onboarding & Induction**
   - Onboarding checklist templates (Documents, IT Setup, Induction, Training).
   - Auto-assigned to new joiners with due dates and owners.
   - Real-time progress bar (%) and interactive task completion toggles.
3. **Time, Attendance & Leave Management**
   - Daily Check-in / Check-out with automatic working hours and shift status calculation (`PRESENT`, `HALF_DAY`, `ABSENT`).
   - Monthly calendar log view and company holiday calendar.
   - Annual leave quota balances (`Casual`, `Sick`, `Earned`, `Unpaid / Loss of Pay`).
   - Leave application workflow with overlap prevention, balance quota enforcement, and manager approval inbox.
4. **Payroll & Statutory Compliance (Indian Context)**
   - Monthly salary structures (Basic, HRA, Special Allowance, Other Allowances).
   - Statutory Indian deductions:
     - **Provident Fund (PF)**: 12% of basic wages.
     - **Employee State Insurance (ESI)**: 0.75% for gross salaries ≤ INR 21,000.
     - **Professional Tax (PT)**: INR 200/month.
     - **TDS**: Progressive annual tax brackets with standard deduction.
     - **Loss of Pay (LOP)**: Prorated salary deduction based on approved unpaid leaves and mid-month joiner dates.
   - Monthly payroll runs (`DRAFT` → `PROCESSED` → `PAID` with record locking).
   - Downloadable payslip PDF generation powered by OpenPDF.
5. **In-App & Email Notifications**
   - Configurable Spring Mail SMTP dispatch for interviews, leaves, and salary credit.
   - In-app notification bell with live unread counter badge and dropdown drawer.

---

## 🚀 Running the Project

### 1. Backend (Spring Boot 3)
```bash
cd backend
.\mvnw.cmd spring-boot:run
```
- Runs on **`http://localhost:8080/api`**
- H2 profile is active by default with automatic schema seeding.
- Swagger UI / OpenAPI docs: **`http://localhost:8080/api/swagger-ui.html`**
- H2 Console: **`http://localhost:8080/api/h2-console`** (`jdbc:h2:mem:hrgenius`, User: `sa`, Password: empty)

To run with Oracle XE:
```bash
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

### 2. Frontend (Angular 21)
```bash
cd frontend
npm start
```
- Available at **`http://localhost:4200`**

### 3. Default Demo Accounts
| Username | Password | Role | Description |
| :--- | :--- | :--- | :--- |
| `admin` | `Admin@123` | `ADMIN`, `HR_MANAGER` | Full enterprise control across all modules |

---

## 🧪 Automated Testing
Backend tests cover unit and integration testing including Indian payroll calculations, mid-month joiners, and leave balance quota enforcement:
```bash
cd backend
.\mvnw.cmd test
```
*All 21 unit and integration tests passing.*
