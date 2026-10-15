# HRGenius — Enterprise HR Management System (Phase 1)

HRGenius is a modern, full-stack Human Resources Management System designed for scalable workforce management with high visual aesthetics and zero-trust security.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 17+, Spring Boot 3.2.5, Spring Data JPA / Hibernate, Spring Security 6 (Stateless JWT), Flyway |
| **Frontend** | Angular 21 (latest stable), Angular Material, SCSS Design System, RxJS |
| **Databases** | Oracle Database (Oracle XE for dev/prod with Flyway migrations) & H2 (In-memory dev profile) |
| **Build & Tooling** | Maven, npm, SpringDoc OpenAPI 3.0 / Swagger UI |

---

## 🏗️ Project Architecture

```
HRGenius/
├── backend/                                   # Spring Boot 3 API
│   ├── mvnw.cmd / pom.xml
│   ├── src/main/java/com/hrgenius/
│   │   ├── HrGeniusApplication.java
│   │   ├── config/                            # CORS, OpenAPI, JPA Auditing, File Storage, H2Seeder
│   │   ├── controller/                        # REST Controllers (Auth, Employee, Dept, Desig, Document)
│   │   ├── dto/                               # Bean Validated DTOs (Request / Response / Paged)
│   │   ├── entity/                            # JPA Audited Entities (Employee, Department, Document, etc.)
│   │   ├── exception/                         # GlobalExceptionHandler & ApiError response shape
│   │   ├── mapper/                            # Entity ↔ DTO Manual Mappers
│   │   ├── repository/                        # Spring Data JPA Repositories
│   │   ├── security/                          # JWT Token Provider, Filter, UserPrincipal, SecurityConfig
│   │   └── service/                           # Business logic layer
│   ├── src/main/resources/
│   │   ├── application.yml                    # Multi-profile config (h2, dev, prod)
│   │   └── db/migration/                      # Flyway Oracle SQL migrations (V1 - V4)
│   └── src/test/java/com/hrgenius/            # JUnit 5 + Mockito + WebMvcTest test suites
│
└── frontend/                                  # Angular 21 Single Page App
    ├── src/
    │   ├── app/
    │   │   ├── core/                          # AuthService, Guards (auth & role), JWT Interceptor, Models
    │   │   ├── layout/                        # MainLayoutComponent (Responsive Sidebar & Header)
    │   │   ├── features/
    │   │   │   ├── auth/                      # Modern split-panel login page
    │   │   │   ├── employees/                 # Employee list, onboard/edit form, rich profile view
    │   │   │   ├── departments/               # Department management CRUD
    │   │   │   └── designations/              # Designation management CRUD
    │   │   └── shared/                        # SharedModule exporting all Angular Material components
    │   ├── styles.scss                        # Curated CSS custom variables & Material theme
    │   └── environments/                      # API endpoints config
```

---

## 🔐 Security & Authentication Features

- **Access Token:** Kept strictly in-memory on the Angular client (not stored in `localStorage` or `sessionStorage` to mitigate XSS attacks).
- **Refresh Token:** Stored in an **HttpOnly, SameSite=Lax** secure cookie that rotates automatically on token refresh (`/api/auth/refresh`).
- **Automatic Interceptor:** Angular `jwtInterceptor` attaches bearer tokens and transparently refreshes expired access tokens on 401 responses.
- **Role-Based Access Control:** Role hierarchy (`ADMIN`, `HR_MANAGER`, `MANAGER`, `EMPLOYEE`) enforced via `@PreAuthorize` on backend endpoints and `roleGuard` on frontend routes.

---

## 🚀 Getting Started

### 1. Prerequisites
- **Java 17 or higher** (`java -version`)
- **Maven** (or use the included `mvnw.cmd` in `backend/`)
- **Node.js 18+ and npm** (`node -v` & `npm -v`)

---

### 2. Backend Setup & Run

#### Option A: Quickstart with H2 In-Memory Database (No Oracle install required)
For instant local testing without setting up an Oracle instance:
```bash
cd backend
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```
*The `h2` profile automatically seeds sample departments, designations, and the default `admin` user!*

#### Option B: Running with Oracle XE Database
1. Ensure Oracle XE is running on `localhost:1521/XEPDB1` (or configure via environment variables).
2. Create the user and grant privileges:
   ```sql
   CREATE USER hrgenius IDENTIFIED BY hrgenius;
   GRANT CONNECT, RESOURCE, DBA TO hrgenius;
   ALTER USER hrgenius QUOTA UNLIMITED ON USERS;
   ```
3. Run with the `dev` profile (Flyway runs all migrations automatically):
   ```bash
   cd backend
   ./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
   ```

#### Run Backend Tests:
```bash
cd backend
./mvnw.cmd test
```

---

### 3. Frontend Setup & Run

1. Open a new terminal in the `frontend/` directory:
   ```bash
   cd frontend
   npm install --legacy-peer-deps
   ```
2. Start the Angular development server:
   ```bash
   npm start
   ```
3. Open your browser and navigate to:
   ```
   http://localhost:4200
   ```

---

## 🔑 Default Credentials

| Username | Password | Role | Permissions |
| :--- | :--- | :--- | :--- |
| **`admin`** | **`Admin@123`** | `ROLE_ADMIN` | Full permissions (Employees, Departments, Designations, Deletion) |

*The login page also provides a one-click button to automatically fill the demo admin credentials.*

---

## 📖 API Documentation (Swagger / OpenAPI)

Once the backend is running, explore and test the interactive API documentation at:
- **Swagger UI:** [http://localhost:8080/api/swagger-ui/index.html](http://localhost:8080/api/swagger-ui/index.html)
- **OpenAPI JSON:** [http://localhost:8080/api/v3/api-docs](http://localhost:8080/api/v3/api-docs)

---

## 🧪 Verification Walkthrough

1. **Sign In:**
   - Go to `http://localhost:4200/auth/login`.
   - Click **"Use Admin Credentials"** (or enter `admin` / `Admin@123`) and click **Sign In**.
2. **Explore Employees:**
   - The Employee list shows all active personnel records.
   - Use the search bar to filter by name, or filter by Department or Status (`ACTIVE`, `ON_NOTICE`, `EXITED`).
3. **Create Employee:**
   - Click **"Add Employee"** to open the onboarding form.
   - Fill in personal info, department, designation, residential address, and emergency contact.
   - Submitting generates a unique `EMP000X` code and redirects to the full profile view.
4. **Upload Documents:**
   - On the Employee Profile page, navigate to the **"Documents"** tab.
   - Select a document type (`ID_PROOF`, `OFFER_LETTER`, `RESUME`) and upload a local file.
   - Files are securely stored on disk in `backend/uploads/` with metadata recorded in the database, and can be downloaded anytime.
5. **Manage Departments & Designations:**
   - Use the left sidebar to navigate to **Departments** and **Designations** to create, edit, or delete organizational structures.
