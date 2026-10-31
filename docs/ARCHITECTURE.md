# TalentMatch AI - System Architecture Documentation

## 1. System Overview

TalentMatch AI is an enterprise-grade recruitment and talent intelligence platform designed to ingest candidate resumes, extract structured skills and experiences via natural language processing, and provide explainable, multi-factor job-to-candidate matching.

### Core Architectural Pillars
- **Strict Multi-Tenant Boundary Isolation**: Recruiter access is cryptographically and logically scoped to their organization at both the API filter and database query layers.
- **Defense in Depth**: Stateless JWT authentication, BCrypt salted hashing, and fine-grained Role-Based Access Control (RBAC).
- **Immutable Database Schema Migrations**: Managed exclusively via Flyway versioned scripts with Hibernate schema validation (`validate` mode).
- **Modular Monolith Foundation**: Clean separation between Spring Boot backend, React + TypeScript frontend, and an upcoming Python FastAPI AI service.

---

## 2. Technology Stack

| Layer | Component | Version / Specification | Purpose |
|---|---|---|---|
| **Backend** | Spring Boot | 3.5.6 | Enterprise REST API and business logic |
| **Language** | Java | 21 LTS | Core backend platform |
| **Security** | Spring Security + JJWT | 0.12.6 | Stateless JWT authentication & RBAC |
| **Password Hashing** | BCrypt | 12 rounds | Cryptographic credential protection |
| **Database** | MySQL | 8.x | Relational persistence |
| **Migrations** | Flyway | 11.x (managed) | Version-controlled DDL migrations |
| **ORM** | Hibernate 6 / Spring Data JPA | 3.5.x | Object-relational mapping |
| **API Docs** | Springdoc OpenAPI | 2.8.5 | Swagger UI & OpenAPI 3.0 specs |
| **Frontend** | React + TypeScript | React 18.3, TS 5.6 | Recruiter portal & interactive UI |
| **Bundler** | Vite | 6.x | Fast ESM module bundling |
| **Icons** | Lucide React | 0.460+ | Consistent enterprise iconography |
| **AI Service (Phase 4)** | Python FastAPI | Python 3.11+ | NLP skill extraction & vector embeddings |

---

## 3. Multi-Tenancy & Security Architecture

### Tenant Boundary Enforcement
Every recruiter belongs to exactly one `Organization` (`tenant_id`). The system enforces isolation through multiple layers:

1. **JWT Claims**: Tokens issued upon authentication include:
   - `sub`: User ID
   - `email`: User email
   - `role`: Role (`ADMIN` or `RECRUITER`)
   - `organizationId`: Tenant ID
2. **Spring Security Context**: The `JwtAuthenticationFilter` populates a `UserPrincipal` in `SecurityContextHolder`.
3. **Programmatic Boundary Checks**: `SecurityUtils.validateOrganizationAccess(targetOrgId)` ensures:
   - If user is `ADMIN`, cross-organization administration is permitted.
   - If user is `RECRUITER`, accessing any resource outside `user.organizationId` immediately aborts with `HTTP 403 Forbidden` (`TenantAccessDeniedException`).
4. **Data Isolation**: All tenant-scoped entities (`users`, `jobs`, `candidates`, `resumes`, `audit_logs`) reference `organization_id` with foreign keys and composite database indexes.

### Authentication & Authorization Flow
```
User Login (POST /api/v1/auth/login)
  │
  ▼
CustomUserDetailsService loads User by email
  │
  ▼
BCryptPasswordEncoder checks submitted password against salted hash
  │
  ▼
JwtTokenProvider signs HMAC-SHA256 token with organizationId claim
  │
  ▼
Frontend stores token in localStorage and attaches `Authorization: Bearer <token>`
```

---

## 4. Database Migrations (Flyway)

All schema changes are immutable and sequentially tracked in `src/main/resources/db/migration/`:

- `V1__init_schema.sql`:
  - `organizations`: Multi-tenant boundary registry.
  - `users`: User credentials, BCrypt hashes, roles, and organization references.
  - `audit_logs`: Append-only audit record for security events, authentication, and compliance.

Hibernate is explicitly configured with `spring.jpa.hibernate.ddl-auto=validate`. Hibernate will never generate or alter DDL directly, preventing unexpected production schema drift.

---

## 5. Development Seed & Security Bootstrap

To avoid insecure default production passwords, `DataInitializer` operates under strict safety rules:
- Enabled only when `app.security.init-dev-data=true` (active in `application-dev.properties`).
- Checks environment variables `APP_SECURITY_DEV_ADMIN_PASSWORD` and `APP_SECURITY_DEV_RECRUITER_PASSWORD`.
- If passwords are not supplied, high-entropy 16-character cryptographically random passwords are generated at startup and output once to server logs.
- Plaintext passwords are never hardcoded or persisted to disk.

---

## 6. Project Roadmap

- **Phase 1 (Complete)**: Foundational Architecture, Spring Boot 3.x, React 18 + TS dashboard, Flyway migrations, JWT security, tenant isolation, automated test suite.
- **Phase 2**: Candidate Domain Model, Job Requisitions schema, and initial REST endpoints.
- **Phase 3**: Resume Ingestion Pipeline (Apache Tika & PDFBox document parsing) and spaCy NLP skill taxonomy.
- **Phase 4**: Python FastAPI microservice with `all-MiniLM-L6-v2` Sentence Transformers for semantic matching and explainable candidate scoring.
