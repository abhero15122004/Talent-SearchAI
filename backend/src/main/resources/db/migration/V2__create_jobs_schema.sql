-- ===================================================================
-- TalentMatch AI Database Migration V2: Job Requisitions & Requirements
-- Compatible with MySQL 8.x and H2 (MySQL Mode)
-- ===================================================================

-- 1. Job Requisitions Table
CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    department VARCHAR(100) NOT NULL,
    location VARCHAR(150) NOT NULL,
    employment_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    min_years_experience INT NOT NULL DEFAULT 0,
    max_years_experience INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    shortlist_count INT NOT NULL DEFAULT 10,
    min_score_threshold DECIMAL(5,2) NOT NULL DEFAULT 60.00,
    weight_skill_coverage DECIMAL(5,2) NOT NULL DEFAULT 40.00,
    weight_experience_relevance DECIMAL(5,2) NOT NULL DEFAULT 25.00,
    weight_project_relevance DECIMAL(5,2) NOT NULL DEFAULT 15.00,
    weight_semantic_similarity DECIMAL(5,2) NOT NULL DEFAULT 20.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_jobs_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_jobs_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- 2. Normalized Job Skills Table (Mandatory & Preferred)
CREATE TABLE IF NOT EXISTS job_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    min_years_required INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- 3. Normalized Job Qualifications & Eligibility Requirements Table
CREATE TABLE IF NOT EXISTS job_qualifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    qualification_type VARCHAR(50) NOT NULL,
    requirement_text VARCHAR(255) NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_qual_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Performance and Tenant Scoping Indexes
CREATE INDEX idx_jobs_org ON jobs(organization_id);
CREATE INDEX idx_jobs_org_status ON jobs(organization_id, status);
CREATE INDEX idx_jobs_created_at ON jobs(created_at);
CREATE INDEX idx_job_skills_job ON job_skills(job_id);
CREATE INDEX idx_job_skills_name ON job_skills(skill_name);
CREATE INDEX idx_job_qual_job ON job_qualifications(job_id);
