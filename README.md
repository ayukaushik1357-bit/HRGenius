<div align="center">

# 🧠 HRGenius

**A full-stack HR platform: hiring, attendance, leave, Indian payroll, appraisals and exits in one secure system.**

[![CI](https://github.com/ayukaushik1357-bit/HRGenius/actions/workflows/ci.yml/badge.svg)](https://github.com/ayukaushik1357-bit/HRGenius/actions)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)
![Oracle](https://img.shields.io/badge/Oracle-XE%2021c-F80000?logo=oracle&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

[Architecture](#-architecture) · [Security](#-security) · [Features](#-features) · [Run it](#-run-it) · [Deploy](#-deploy-free-auto-deploys-on-push)

</div>

---

## ✨ Highlights

| | |
|---|---|
| 🔐 **Real auth** | Email/username or Google sign-in, open sign-up, 15-min JWTs in memory, rotating HttpOnly refresh cookie with stolen-token detection, lockout, password reset |
| 🛡️ **Real authorization** | Every endpoint checks the **role** and the **owner**: employees see only themselves, managers only their team |
| 🇮🇳 **Indian payroll** | PF, ESI, professional tax, slab-based TDS, loss of pay, PDF payslips |
| 📊 **Analytics** | Headcount, attrition, tenure, payroll cost, hiring funnel; Excel and PDF export |
| 🧪 **Tested** | 223 backend tests incl. a 61-case access matrix, 16 frontend tests, CI on every push |
| 🚀 **Runs anywhere** | One-command Docker (Oracle), free cloud demo (Vercel + Render + Neon), or zero-setup local H2 |

---

## 🏛️ Architecture

### The big picture

```mermaid
flowchart TB
    U(["👤 Employee · Manager · HR · Admin"])

    subgraph CLIENT["🅰️ Angular 21 SPA"]
        direction LR
        P["Landing · Sign-in<br/>14 app pages"] --> GU["Route guards<br/>hide what you can't open"] --> IC["HTTP interceptor<br/>Bearer token · silent refresh"]
    end

    subgraph SERVER["🍃 Spring Boot 3 API"]
        direction LR
        F["Security filters<br/>CORS · rate limit · JWT"] --> C["17 REST controllers<br/>@PreAuthorize + @authz"] --> S["Services<br/>@Transactional rules"] --> R["Spring Data JPA<br/>Hibernate 6"]
    end

    subgraph DATA["🗄️ One codebase, three databases"]
        direction LR
        O[("Oracle XE<br/>Flyway V1–V12")] ~~~ PG[("PostgreSQL<br/>cloud demo")] ~~~ H[("H2<br/>local · tests")]
    end

    EXT["📧 SMTP · 🌐 Google ID tokens · 📑 PDF / Excel"]

    U --> P
    IC == "/api · JSON over HTTPS" ==> F
    R ==> DATA
    S -.-> EXT

    style CLIENT fill:#FFF1F2,stroke:#DD0031,stroke-width:2px,color:#881337
    style SERVER fill:#F0FDF4,stroke:#6DB33F,stroke-width:2px,color:#14532D
    style DATA fill:#EFF6FF,stroke:#2563EB,stroke-width:2px,color:#1E3A8A
    classDef box fill:#FFFFFF,stroke:#94A3B8,color:#0F172A
    classDef ext fill:#FEFCE8,stroke:#CA8A04,color:#713F12
    classDef user fill:#EEF2FF,stroke:#4F46E5,stroke-width:2px,color:#1E1B4B
    class P,GU,IC,F,C,S,R box
    class EXT ext
    class U user
```

The same backend runs on three databases, picked by Spring profile:

| Profile | Database | Schema | Used for |
| :--- | :--- | :--- | :--- |
| `h2` *(default)* | H2 file in `backend/data` | Hibernate `update` | Local run, no setup, data kept across restarts |
| `dev` / `prod` | Oracle XE 21c | Flyway + `validate` | Docker Compose and production |
| `cloud` | PostgreSQL (Neon) | Hibernate `update` | Free hosted demo |

### Inside the backend

```mermaid
flowchart TB
    REQ(["HTTP request"]) --> SEC

    subgraph SEC["🛡️ Security layer"]
        direction LR
        CO["CORS"] --> RL["Rate limiter<br/>sign-in endpoints"] --> JW["JWT filter"]
    end

    SEC --> CTRL

    subgraph CTRL["🎯 Web layer"]
        direction LR
        CT["Controllers<br/>DTOs + Bean Validation"] --> AZ["@authz<br/>self / team / HR checks"]
    end

    CTRL --> SVC

    subgraph SVC["⚙️ Service layer"]
        direction LR
        EMP["Employees<br/>Org"] ~~~ ATT["Attendance<br/>Leave"] ~~~ PAY["Payroll<br/>engine"] ~~~ PERF["Performance"] ~~~ EXIT["Offboarding<br/>FnF"] ~~~ ANA["Analytics"]
    end

    SVC --> REPO["🗃️ Repositories · JPQL aggregates"] --> DB[("Database")]
    SVC -. "events" .-> SIDE["📝 Audit log · 🔔 Notifications · 📧 Async email"]
    CTRL -. "errors" .-> GEH["Global exception handler<br/>400 · 401 · 403 · 404 · 409 · 423 JSON"]

    style SEC fill:#FEF2F2,stroke:#DC2626,color:#7F1D1D
    style CTRL fill:#EEF2FF,stroke:#4F46E5,color:#1E1B4B
    style SVC fill:#F0FDF4,stroke:#16A34A,color:#14532D
    classDef box fill:#FFFFFF,stroke:#94A3B8,color:#0F172A
    class CO,RL,JW,CT,AZ,EMP,ATT,PAY,PERF,EXIT,ANA,REPO,SIDE,GEH box
```

### Data model (core)

```mermaid
erDiagram
    USER }o--o{ ROLE : has
    USER ||--o| EMPLOYEE : "signs in as"
    DEPARTMENT ||--o{ EMPLOYEE : employs
    EMPLOYEE ||--o{ EMPLOYEE : manages
    EMPLOYEE ||--o{ ATTENDANCE : punches
    EMPLOYEE ||--o{ LEAVE_APPLICATION : applies
    PAYROLL_RUN ||--o{ PAYSLIP : produces
    EMPLOYEE ||--o{ PAYSLIP : receives
    REVIEW_CYCLE ||--o{ PERFORMANCE_REVIEW : contains
    EMPLOYEE ||--o{ PERFORMANCE_REVIEW : "is reviewed"
    EMPLOYEE ||--o| EXIT_REQUEST : resigns
    EXIT_REQUEST ||--o| FNF_SETTLEMENT : "settles in"

    EMPLOYEE {
        string employee_code UK
        string email UK
        string status "ACTIVE · ON_NOTICE · EXITED"
        decimal salary_ctc
        boolean is_deleted "soft delete"
    }
    USER {
        string username UK
        string password "BCrypt"
        int failed_attempts "lockout"
    }
    PAYSLIP {
        decimal gross_salary
        decimal pf_deduction
        decimal tds_deduction
        decimal net_salary
    }
```

Deleted employees are **soft deleted**: hidden and unable to sign in, while their payslips, leave and attendance stay for audit.

---

## 🔐 Security

### Five gates on every request

```mermaid
flowchart LR
    R(["Request"]) --> A{"CORS"} -->|ok| B{"Rate limit<br/>10/min/IP"} -->|ok| C{"Valid<br/>access JWT?"} -->|yes| D{"Right<br/>role?"} -->|yes| E{"Own record<br/>or own team?"} -->|yes| OK["✅ Handled<br/>PAN · bank masked<br/>for non-HR"]
    A -->|no| X1["❌"]
    B -->|no| X2["❌ 429"]
    C -->|no| X3["❌ 401"]
    D -->|no| X4["❌ 403"]
    E -->|no| X5["❌ 403"]

    classDef bad fill:#FEE2E2,stroke:#DC2626,color:#7F1D1D
    classDef good fill:#DCFCE7,stroke:#16A34A,color:#14532D
    classDef gate fill:#F1F5F9,stroke:#475569,color:#0F172A
    class X1,X2,X3,X4,X5 bad
    class OK good
    class A,B,C,D,E gate
```

### Sign-in and token rotation

```mermaid
sequenceDiagram
    autonumber
    actor B as Browser
    participant API as Spring Boot
    participant DB as Database

    B->>API: POST /auth/login (or Google ID token)
    API->>DB: BCrypt check · lockout after 5 fails
    API-->>B: access JWT (15 min, memory only) + HttpOnly refresh cookie (7 days)
    B->>API: GET /api/... with Bearer token
    API-->>B: 200

    Note over B,API: Token expired or page reloaded
    B->>API: POST /auth/refresh (cookie)
    API->>DB: revoke old token, store SHA-256 of new one
    API-->>B: new access JWT + new cookie

    rect rgb(254, 226, 226)
    Note over B,DB: 🚨 An already-used refresh token comes back (stolen)
    B->>API: POST /auth/refresh (old cookie)
    API->>DB: revoke ALL sessions of that user
    API-->>B: 400 · sign in again
    end
```

### Who can do what

| | Admin | HR manager | Manager | Employee |
| :--- | :---: | :---: | :---: | :---: |
| Employee profiles | ✅ all | ✅ all | 👥 team | 🙋 self |
| Salary, PAN, bank | ✅ | ✅ | 🔒 masked | 🙋 self |
| Approve leave | ✅ | ✅ | 👥 direct reports | ❌ |
| Payroll runs | ✅ | ✅ | ❌ | ❌ |
| Payslips | ✅ | ✅ | ❌ | 🙋 self |
| Analytics | ✅ company | ✅ company | 👥 team | ❌ |
| Users, roles, audit log | ✅ | audit log | ❌ | ❌ |

New sign-ups start as **Employee**; the admin promotes people in **User Management**. Rules live on the server; the endpoint-by-endpoint list is in [`docs/SECURITY_MATRIX.md`](docs/SECURITY_MATRIX.md).

---

## 🧩 Features

```mermaid
flowchart LR
    J["📢 Job posting"] --> I["🗓️ Interviews"] --> H["✅ Hire<br/>employee + login"] --> O["🧭 Onboarding"]
    O --> W["⏱️ Attendance<br/>& leave"]
    W --> P["💰 Payroll"] --> W
    W --> A["⭐ Appraisal"] --> W
    W --> E["🚪 Exit"] --> F["🧾 Full & final"]

    classDef hire fill:#EEF2FF,stroke:#4F46E5,color:#1E1B4B
    classDef run fill:#ECFDF5,stroke:#059669,color:#064E3B
    classDef exit fill:#FFF7ED,stroke:#EA580C,color:#7C2D12
    class J,I,H,O hire
    class W,P,A run
    class E,F exit
```

| Module | What it does |
| :--- | :--- |
| 👥 **People** | Profiles, departments, designations, reporting lines, document vault, masked PAN/Aadhaar/bank |
| 🧲 **Recruitment** | Job postings, candidate pipeline, interviews with ratings, offers; hiring creates the employee and a login |
| ⏱️ **Attendance & leave** | Check-in/out, half days, holiday calendar, casual/sick/earned/unpaid quotas, approval by the employee's own manager |
| 💰 **Payroll** | Monthly runs (draft → processed → paid, locked once paid) and PDF payslips |
| ⭐ **Performance** | Review cycles, weighted goals, self and manager review, 360° feedback; rating = 60 % manager + 40 % goals |
| 🚪 **Offboarding** | Resignation, clearance checklist, full and final with leave encashment and gratuity |
| 📊 **Analytics & portal** | Company or team dashboard, Excel/PDF export, self-service portal, audit log, notifications |

<details>
<summary><b>💰 Payroll rules</b></summary>

| Component | Rule |
| :--- | :--- |
| Provident fund | 12 % of basic (after loss of pay) |
| ESI | 0.75 % of gross when gross ≤ ₹21,000 |
| Professional tax | ₹200 / month when gross ≥ ₹10,000 |
| TDS | Annual slab tax, deducted monthly |
| Loss of pay | Pro-rated for unpaid leave and mid-month joiners |
| Gratuity (FnF) | 15 × basic ÷ 26 × years, after 5 years |

</details>

---

## 🛠️ Tech Stack

| | |
| :--- | :--- |
| **Backend** | Java 17 · Spring Boot 3.2 · Spring Security 6 · Spring Data JPA / Hibernate 6 · Bean Validation |
| **Auth** | JJWT (HS512) · BCrypt · HttpOnly cookies · Google Identity Services + OAuth2 JOSE |
| **Frontend** | Angular 21 (standalone, signals) · Angular Material · RxJS · Chart.js · SCSS |
| **Data** | Oracle XE 21c · PostgreSQL · H2 · Flyway |
| **Reports** | Apache POI (Excel) · OpenPDF (payslips, reports) |
| **Quality** | JUnit 5 · Mockito · MockMvc · Vitest · GitHub Actions |
| **Ops** | Docker multi-stage builds · Docker Compose · Nginx · Vercel · Render · Neon |

---

## 🚀 Run it

**Needs:** Java 17+, Node.js 22, Git.

### Local, no database setup

```bash
# backend → http://localhost:8080/api   (Windows: .\mvnw.cmd spring-boot:run)
cd backend
export ADMIN_PASSWORD='Choose-a-strong-one1!'   # PowerShell: $env:ADMIN_PASSWORD="Choose-a-strong-one1!"
./mvnw spring-boot:run

# frontend → http://localhost:4200
cd frontend && npm install && npm start
```

- Sign in as `admin` with your `ADMIN_PASSWORD`, or **Create account**. Demo logins: `hrmanager / Hr@123`, `manager / Manager@123`, `employee / Employee@123`.
- 50 sample employees are loaded. Data is saved in `backend/data`; delete that folder to start fresh.
- API docs: http://localhost:8080/api/swagger-ui.html

### Docker (Oracle + backend + frontend)

```bash
cp .env.example .env        # set JWT_SECRET, ADMIN_PASSWORD, database passwords
docker compose up --build   # app on http://localhost, API on :8080
```

### Google sign-in with your own client

In [Google Cloud Console](https://console.cloud.google.com/apis/credentials): create a **Web application** client, add every address the app runs on under **Authorized JavaScript origins**, **Publish** the app, and start the backend with `GOOGLE_CLIENT_ID=<id>`. No client secret is used.

---

## ☁️ Deploy (free, auto-deploys on push)

```mermaid
flowchart LR
    DEV(["💻 git push"]) --> GH["🐙 GitHub<br/>main"]
    GH -->|CI: tests + builds| CI["✅ GitHub Actions"]
    GH -->|auto deploy| V["▲ Vercel<br/>Angular app"]
    GH -->|auto deploy| R["🟣 Render<br/>Spring Boot (Docker)"]
    USER(["🌐 Visitor"]) -->|https://your-app.vercel.app| V
    V -->|"/api/* proxied<br/>same-origin cookie"| R
    R -->|JDBC + TLS| N[("🐘 Neon<br/>PostgreSQL")]
    KA["⏰ Keep-alive workflow<br/>every 10 min"] -.-> R

    classDef host fill:#F8FAFC,stroke:#334155,color:#0F172A
    classDef db fill:#EFF6FF,stroke:#2563EB,color:#1E3A8A
    class GH,CI,V,R,KA host
    class N db
```

1. **Neon:** create a project (region Singapore), turn **connection pooling off**, copy host, database, user and password.
2. **Render:** *New → Web Service* → this repo, **Root Directory** `backend`, **Docker**, **Free**, health check `/api/auth/config`, with these variables:

   | Variable | Value |
   | :--- | :--- |
   | `SPRING_PROFILES_ACTIVE` | `cloud` |
   | `DB_URL` | `jdbc:postgresql://<host>/<database>?sslmode=require` |
   | `DB_USERNAME` · `DB_PASSWORD` | from Neon |
   | `JWT_SECRET` | 64+ random characters |
   | `ADMIN_PASSWORD` | password for `admin` |
   | `APP_FRONTEND_URL` · `CORS_ALLOWED_ORIGINS` | your Vercel URL |
   | `APP_SEED_DEMO_DATA` | `false` to start without the 50 sample employees |

3. **Vercel:** make sure the Render URL in [`frontend/vercel.json`](frontend/vercel.json) is right, then *Add New → Project* → this repo, **Root Directory** `frontend`.
4. **Google:** add the Vercel URL to **Authorized JavaScript origins**.
5. **Keep awake** *(optional)*: add the repository variable `BACKEND_URL` = Render URL. Free Render services otherwise sleep after 15 idle minutes.

---

## 🧪 Tests

```bash
cd backend && ./mvnw verify   # 223 tests
cd frontend && npm test       # 16 tests
```

| Suite | Proves |
| :--- | :--- |
| `SecurityRulesParameterizedTest` | 61 role, self, team and IDOR cases across all controllers with real JWTs |
| `RealFlowIntegrationTest` | Stolen-token detection, reload race, analytics on real data |
| `AccountFlowIntegrationTest` | Sign-up, email and Google sign-in, lockout, password reset |
| `DeleteRulesIntegrationTest` | Soft delete keeps history; records in use return a clear 409 |
| Service tests | PF, ESI, PT, TDS, LOP, leave balances, appraisal score, FnF |

---

## 📁 Project Structure

```text
HRGenius/
├── backend/                 Spring Boot API
│   └── src/main/java/com/hrgenius/
│       ├── security/        JWT, filters, rate limiter, @authz rules
│       ├── controller/      17 REST controllers
│       ├── service/         business rules and transactions
│       ├── repository/      Spring Data + JPQL aggregates
│       ├── entity/ dto/     JPA entities, request/response models
│       └── config/          CORS, OpenAPI, demo data, admin password
├── frontend/                Angular 21 SPA (core/, features/, layout/)
├── docs/SECURITY_MATRIX.md  endpoint access rules
├── docker-compose.yml       Oracle + backend + Nginx frontend
└── .github/workflows/       CI and keep-alive
```
