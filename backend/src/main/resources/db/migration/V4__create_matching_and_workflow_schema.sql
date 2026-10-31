-- ===================================================================
-- TalentMatch AI Database Migration V4: Matching Runs & Recruitment Workflow
-- Compatible with MySQL 8.x and H2 (MySQL Mode)
-- ===================================================================

-- 1. Job Match Runs Table
CREATE TABLE IF NOT EXISTS job_match_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    triggered_by_user_id BIGINT NOT NULL,
    run_number INT NOT NULL DEFAULT 1,
    algorithm_version VARCHAR(50) NOT NULL DEFAULT '1.0.0-hybrid',
    weight_skill_coverage DECIMAL(5,2) NOT NULL,
    weight_experience_relevance DECIMAL(5,2) NOT NULL,
    weight_project_relevance DECIMAL(5,2) NOT NULL,
    weight_semantic_similarity DECIMAL(5,2) NOT NULL,
    min_score_threshold DECIMAL(5,2) NULL,
    shortlist_target INT NOT NULL DEFAULT 10,
    total_candidates_evaluated INT NOT NULL DEFAULT 0,
    eligible_candidates_count INT NOT NULL DEFAULT 0,
    shortlisted_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_runs_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_runs_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_runs_user FOREIGN KEY (triggered_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- 2. Job Candidate Matches Table
CREATE TABLE IF NOT EXISTS job_candidate_matches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_run_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    rank_position INT NOT NULL,
    final_score DECIMAL(5,2) NOT NULL,
    semantic_similarity_score DECIMAL(5,2) NOT NULL,
    skill_coverage_score DECIMAL(5,2) NOT NULL,
    experience_relevance_score DECIMAL(5,2) NOT NULL,
    project_relevance_score DECIMAL(5,2) NOT NULL,
    is_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    mandatory_status VARCHAR(30) NOT NULL DEFAULT 'PASSED',
    is_shortlisted BOOLEAN NOT NULL DEFAULT FALSE,
    matched_skills TEXT NULL,
    missing_mandatory_skills TEXT NULL,
    matched_preferred_skills TEXT NULL,
    evidence_snippets TEXT NULL,
    warnings TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_matches_run FOREIGN KEY (match_run_id) REFERENCES job_match_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- 3. Job Applications / Candidate Workflow Stages Table
CREATE TABLE IF NOT EXISTS job_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    stage VARCHAR(40) NOT NULL DEFAULT 'NEW',
    is_shortlisted BOOLEAN NOT NULL DEFAULT FALSE,
    decision_reason VARCHAR(100) NULL,
    assigned_to_user_id BIGINT NULL,
    latest_match_id BIGINT NULL,
    stage_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_assigned_user FOREIGN KEY (assigned_to_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_app_latest_match FOREIGN KEY (latest_match_id) REFERENCES job_candidate_matches(id) ON DELETE SET NULL,
    CONSTRAINT uk_app_job_candidate UNIQUE (job_id, candidate_id)
);

-- 4. Recruiter Notes Table
CREATE TABLE IF NOT EXISTS job_application_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    author_user_id BIGINT NOT NULL,
    note_text TEXT NOT NULL,
    stage_at_creation VARCHAR(40) NOT NULL,
    decision_reason VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_application FOREIGN KEY (application_id) REFERENCES job_applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_user FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Indexes for performance and strict organization scoping
CREATE INDEX idx_match_runs_job_org ON job_match_runs(job_id, organization_id);
CREATE INDEX idx_cand_matches_run ON job_candidate_matches(match_run_id);
CREATE INDEX idx_cand_matches_cand ON job_candidate_matches(candidate_id);
CREATE INDEX idx_cand_matches_org ON job_candidate_matches(organization_id);
CREATE INDEX idx_app_job_org ON job_applications(job_id, organization_id);
CREATE INDEX idx_app_stage ON job_applications(organization_id, stage);
CREATE INDEX idx_notes_application ON job_application_notes(application_id);
CREATE INDEX idx_notes_org ON job_application_notes(organization_id);
