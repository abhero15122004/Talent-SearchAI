-- ===================================================================
-- TalentMatch AI Database Migration V3: Resume Ingestion & Candidates
-- Compatible with MySQL 8.x and H2 (MySQL Mode)
-- ===================================================================

-- 1. Resume Batches Table
CREATE TABLE IF NOT EXISTS resume_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    total_files INT NOT NULL DEFAULT 0,
    processed_files INT NOT NULL DEFAULT 0,
    successful_files INT NOT NULL DEFAULT 0,
    failed_files INT NOT NULL DEFAULT 0,
    duplicate_files INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_batches_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_batches_user FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- 2. Candidates Table
CREATE TABLE IF NOT EXISTS candidates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    full_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    raw_text MEDIUMTEXT NULL,
    summary TEXT NULL,
    primary_skills TEXT NULL,
    education_history TEXT NULL,
    experience_history TEXT NULL,
    project_history TEXT NULL,
    source_filename VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_candidates_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- 3. Resume Files Table
CREATE TABLE IF NOT EXISTS resume_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    candidate_id BIGINT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_files_batch FOREIGN KEY (batch_id) REFERENCES resume_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_files_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_files_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE SET NULL
);

-- Indexes for querying and tenant scoping
CREATE INDEX idx_batches_org ON resume_batches(organization_id);
CREATE INDEX idx_batches_status ON resume_batches(organization_id, status);
CREATE INDEX idx_candidates_org ON candidates(organization_id);
CREATE INDEX idx_candidates_email ON candidates(organization_id, email);
CREATE INDEX idx_files_batch ON resume_files(batch_id);
CREATE INDEX idx_files_hash_org ON resume_files(organization_id, content_hash);
CREATE INDEX idx_files_status ON resume_files(organization_id, status);
