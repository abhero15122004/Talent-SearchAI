# TalentMatch AI &bull; Enterprise Recruitment &amp; Talent Intelligence Platform

[![Build & Test](https://img.shields.io/badge/Build-Passing-brightgreen)]()
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-blue)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)]()
[![React](https://img.shields.io/badge/React-18.3%20%7C%20TypeScript-61DAFB)]()
[![Vite](https://img.shields.io/badge/Vite-6.x-646CFF)]()
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1)]()
[![Security](https://img.shields.io/badge/Spring%20Security-JWT%20%2B%20BCrypt-red)]()

TalentMatch AI is an enterprise-grade recruitment and talent intelligence platform. It replaces manual candidate screening with structured competency extraction, verifiable evidence tracking, and transparent multi-factor talent-to-job matching.

---

## Key Capabilities

### Phase 1: Core Foundation & Multi-Tenancy
- **Multi-Tenant Boundary Isolation**: Recruiter access is cryptographically and logically scoped to their organization (`tenant_id`). Cross-tenant access is strictly denied at both the security context and database layer.
- **Defense-in-Depth Authentication**: Stateless HMAC-SHA256 JWT tokens containing subject, roles, and organization claims, paired with 12-round BCrypt password hashing.
- **Immutable Schema Migrations**: Flyway version-controlled migrations with Hibernate schema validation (`spring.jpa.hibernate.ddl-auto=validate`).
- **Comprehensive Audit Trail**: Security, authentication, and organizational events are captured in an append-only audit registry (`audit_logs`).

### Phase 2: Job Requisition Management
- **Structured Job Specifications**: Title, department, location, employment type, description, and experience bounds.
- **Normalized Requirements**: Mandatory vs. preferred skills and qualifications (Education, Certification, Experience, Skill).
- **Normalized Matching Weights**: Recruiter-configurable weights for skill coverage, experience relevance, project relevance, and semantic similarity, strictly validated to sum to 100.00%.
- **Recruiter Controls**: Configurable candidate shortlist counts and minimum recommendation score thresholds.

### Phase 3: Bulk Resume Ingestion Pipeline (No RabbitMQ)
- **High-Throughput Ingestion**: Supports browser folder upload (`webkitdirectory`) with multi-file fallback and ZIP archive upload.
- **Security & Storage Protection**: Strict Zip Slip path traversal rejection (`..` prevention), Zip Bomb protection with decompressed byte limits, bounded file counts, and UUID-based storage keys outside MySQL in local non-public storage.
- **Content-Based MIME Detection**: Apache Tika 2.9.2 inspection of stream magic bytes (rejects fake extensions, encrypted, or corrupted files).
- **Organization-Scoped Deduplication**: SHA-256 content hashes identify duplicate resumes within the organization, marking duplicates `SKIPPED_DUPLICATE` without duplicating talent profiles.
- **Bounded Concurrency**: Bounded `ThreadPoolTaskExecutor` (core: 4, max: 8, queue: 200) with immediate HTTP `202 ACCEPTED` batch ID response.
- **Unhallucinated Candidate Extraction**: Strict heuristic parsing of contact details, skills, summary, education, experience, and projects with zero AI hallucinations.
- **Startup Recovery**: Automatic detection and recovery of interrupted ingestion jobs on server restart.
- **Recruiter Dashboard UI**: Live status polling, per-file extraction breakdown, single-click retry for failed documents, candidate talent pool directory, and verbatim text inspection.

---

## System Architecture & Stack

```
TalentMatch-AI/
├── backend/                  # Java 21 & Spring Boot 3.5.6 REST API
│   ├── src/main/java/com/talentmatch/
│   │   ├── audit/            # Audit logging entity, repository, service
│   │   ├── auth/             # Login, logout, /me endpoints & DTOs
│   │   ├── candidate/        # Candidate profiles, talent search & retrieval
│   │   ├── common/           # Global exceptions, ApiResponse, SecurityUtils
│   │   ├── config/           # SecurityConfig, AsyncConfig, StorageProperties, CorsConfig
│   │   ├── job/              # Job requisitions, skills, qualifications, weights
│   │   ├── organization/     # Tenant organization domain models & API
│   │   ├── resume/           # Ingestion pipeline, Tika parsing, bounded executor, storage
│   │   ├── security/         # JWT provider, filter, UserPrincipal, UserDetailsService
│   │   └── user/             # User domain models, BCrypt hashing, roles (ADMIN/RECRUITER)
│   └── src/main/resources/
│       ├── db/migration/     # Flyway SQL migrations (V1, V2, V3)
│       └── application.properties
├── frontend/                 # React 18 + TypeScript + Vite Recruiter Portal
│   ├── src/
│   │   ├── api/              # Axios client (jobs, resumes, candidates, auth)
│   │   ├── components/       # Layout, Navbar, Sidebar, Badges, WeightMeter, LoadingSpinner
│   │   ├── context/          # AuthContext for session management
│   │   ├── pages/            # Dashboard, Jobs, Resumes Ingestion, Candidates
│   │   ├── styles/           # Accessible, responsive design system
│   │   └── types/            # TypeScript interfaces for API contracts & models
│   └── package.json
├── database/migrations/      # Canonical copy of Flyway schema migrations
├── docs/                     # Architecture and operational documentation
│   └── ARCHITECTURE.md
└── .env.example              # Environment variables template
```

---

## Getting Started

### Prerequisites
- **Java**: JDK 21+ (JDK 24 compatible with `--release 21`)
- **Maven**: 3.9+
- **Node.js**: v18+ (Node 22 LTS recommended) and npm
- **MySQL**: 8.x (or run in `dev` profile using in-memory H2)

### 1. Database Configuration
By default, the backend expects a local MySQL 8.x database:
```sql
CREATE DATABASE IF NOT EXISTS talentmatch_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'talentmatch_user'@'localhost' IDENTIFIED BY 'your_secure_db_password';
GRANT ALL PRIVILEGES ON talentmatch_db.* TO 'talentmatch_user'@'localhost';
FLUSH PRIVILEGES;
```
Configure your credentials in `CRM/backend/src/main/resources/application.properties` or set environment variables according to `.env.example`.

### 2. Running the Backend
From the repository root:
```bash
# Production/MySQL mode:
mvn -f backend/pom.xml spring-boot:run

# Local Development / In-Memory H2 mode (no MySQL required):
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```
The API starts on `http://localhost:8080`.
- **Swagger / OpenAPI Documentation**: `http://localhost:8080/swagger-ui.html`
- **Actuator Health Endpoint**: `http://localhost:8080/actuator/health`

### 3. Development Seed Accounts
When running with `app.security.init-dev-data=true` (enabled by default in `dev` profile), the system automatically seeds sample organizations and initial users:
- **Admin**: `admin@talentmatch.ai` (Role: `ADMIN`)
- **Recruiter**: `recruiter@acme.com` (Role: `RECRUITER`, Organization: Acme Corp)

If `APP_SECURITY_DEV_ADMIN_PASSWORD` and `APP_SECURITY_DEV_RECRUITER_PASSWORD` environment variables are omitted, strong 16-character random passwords are generated at startup and logged to console.

### 4. Running the Frontend
```bash
cd frontend
npm install
npm run dev
```
The recruiter UI launches on `http://localhost:5173`.

To create a production frontend bundle:
```bash
cd frontend
npm run build
```

---

## Running Automated Tests

### Backend Tests
Execute the complete Spring Boot unit and integration test suite:
```bash
mvn -f backend/pom.xml test
```
The test suite covers:
- **`AuthControllerTest`**: Login authentication, invalid credentials rejection (401), `/me` identity verification, and unauthenticated access blocking.
- **`JobControllerIntegrationTest` & `JobServiceTest`**: CRUD operations, matching weight normalization (must sum to 100%), recruiter organization scoping, and status transitions.
- **`OrganizationIsolationTest`**: Multi-tenant isolation verification — confirms recruiters can access their own organization data but are strictly blocked (403 Forbidden) from accessing other tenants' data.
- **`ResumeIngestionIntegrationTest`**: End-to-end multi-part and ZIP upload, bounded async execution, duplicate file detection (`SKIPPED_DUPLICATE`), failed file retry, and candidate profile extraction.
- **`StorageServiceTest`**: Zip Slip path traversal rejection, Zip Bomb protection, max file bounds, and SHA-256 calculation.
- **`TikaParserServiceTest`**: MIME byte inspection, unhallucinated candidate heuristic extraction, and error handling for corrupt documents.
- **`JwtTokenProviderTest`**: Cryptographic HMAC-SHA256 signature generation, subject/claims extraction, and token tampering rejection.
- **`UserServiceTest`**: 12-round BCrypt password hashing verification and duplicate email prevention.
- **`TalentMatchApplicationTests`**: Flyway migration execution, Hibernate DDL schema validation, and Spring ApplicationContext initialization.

### Frontend Typecheck & Build
```bash
cd frontend
npm run build
```

---

## Security Model

1. **Password Storage**: Passwords are never stored in plaintext. They are salted and hashed using BCrypt.
2. **Stateless Sessions**: Authentication relies entirely on signed JSON Web Tokens (JWT). The server maintains zero session state.
3. **Strict RBAC**:
   - `ADMIN`: Global administration, system health, tenant creation.
   - `RECRUITER`: Scoped strictly to their assigned `organization_id`. Cannot inspect or manipulate candidates, jobs, or resumes belonging to competing organizations.
4. **Tenant Access Violations**: Any cross-tenant resource probe results in an immediate `TenantAccessDeniedException` and an entry in `audit_logs`.
5. **Storage Defense**: User filenames are never trusted as storage keys; UUID storage keys isolate files on disk. ZIP entries with `..` or path escape attempts are rejected immediately.

---

## Roadmap

- [x] **Phase 1**: Clean modular architecture, Java 21 + Spring Boot 3.5.6, React + TypeScript recruiter dashboard, Flyway migrations, JWT security, multi-tenant isolation, automated test suite.
- [x] **Phase 2**: Job requisition management, skill & qualification requirements, normalized matching weights, tenant isolation, full recruiter UI.
- [x] **Phase 3**: Bulk resume ingestion pipeline (no RabbitMQ), bounded Java thread pool, Apache Tika 2.9.2 extraction, duplicate detection, candidate talent inventory, retry mechanisms, live polling UI.
- [ ] **Phase 4**: Python FastAPI service with `all-MiniLM-L6-v2` Sentence Transformers for multi-factor semantic matching and explainable scoring.
