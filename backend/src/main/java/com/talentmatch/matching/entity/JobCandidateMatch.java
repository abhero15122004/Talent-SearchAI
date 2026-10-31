package com.talentmatch.matching.entity;

import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.organization.entity.Organization;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "job_candidate_matches")
public class JobCandidateMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_run_id", nullable = false)
    private JobMatchRun matchRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @Column(name = "final_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "semantic_similarity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal semanticSimilarityScore;

    @Column(name = "skill_coverage_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal skillCoverageScore;

    @Column(name = "experience_relevance_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal experienceRelevanceScore;

    @Column(name = "project_relevance_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal projectRelevanceScore;

    @Column(name = "is_eligible", nullable = false)
    private boolean eligible = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "mandatory_status", nullable = false, length = 30)
    private MandatoryStatus mandatoryStatus = MandatoryStatus.PASSED;

    @Column(name = "is_shortlisted", nullable = false)
    private boolean shortlisted = false;

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(name = "missing_mandatory_skills", columnDefinition = "TEXT")
    private String missingMandatorySkills;

    @Column(name = "matched_preferred_skills", columnDefinition = "TEXT")
    private String matchedPreferredSkills;

    @Column(name = "evidence_snippets", columnDefinition = "TEXT")
    private String evidenceSnippets;

    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public JobCandidateMatch() {
    }

    public JobCandidateMatch(JobMatchRun matchRun,
                             Candidate candidate,
                             Organization organization,
                             int rankPosition,
                             BigDecimal finalScore,
                             BigDecimal semanticSimilarityScore,
                             BigDecimal skillCoverageScore,
                             BigDecimal experienceRelevanceScore,
                             BigDecimal projectRelevanceScore,
                             boolean eligible,
                             MandatoryStatus mandatoryStatus,
                             boolean shortlisted,
                             String matchedSkills,
                             String missingMandatorySkills,
                             String matchedPreferredSkills,
                             String evidenceSnippets,
                             String warnings) {
        this.matchRun = matchRun;
        this.candidate = candidate;
        this.organization = organization;
        this.rankPosition = rankPosition;
        this.finalScore = finalScore;
        this.semanticSimilarityScore = semanticSimilarityScore;
        this.skillCoverageScore = skillCoverageScore;
        this.experienceRelevanceScore = experienceRelevanceScore;
        this.projectRelevanceScore = projectRelevanceScore;
        this.eligible = eligible;
        this.mandatoryStatus = mandatoryStatus;
        this.shortlisted = shortlisted;
        this.matchedSkills = matchedSkills;
        this.missingMandatorySkills = missingMandatorySkills;
        this.matchedPreferredSkills = matchedPreferredSkills;
        this.evidenceSnippets = evidenceSnippets;
        this.warnings = warnings;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JobMatchRun getMatchRun() {
        return matchRun;
    }

    public void setMatchRun(JobMatchRun matchRun) {
        this.matchRun = matchRun;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public int getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(int rankPosition) {
        this.rankPosition = rankPosition;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public BigDecimal getSemanticSimilarityScore() {
        return semanticSimilarityScore;
    }

    public void setSemanticSimilarityScore(BigDecimal semanticSimilarityScore) {
        this.semanticSimilarityScore = semanticSimilarityScore;
    }

    public BigDecimal getSkillCoverageScore() {
        return skillCoverageScore;
    }

    public void setSkillCoverageScore(BigDecimal skillCoverageScore) {
        this.skillCoverageScore = skillCoverageScore;
    }

    public BigDecimal getExperienceRelevanceScore() {
        return experienceRelevanceScore;
    }

    public void setExperienceRelevanceScore(BigDecimal experienceRelevanceScore) {
        this.experienceRelevanceScore = experienceRelevanceScore;
    }

    public BigDecimal getProjectRelevanceScore() {
        return projectRelevanceScore;
    }

    public void setProjectRelevanceScore(BigDecimal projectRelevanceScore) {
        this.projectRelevanceScore = projectRelevanceScore;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public MandatoryStatus getMandatoryStatus() {
        return mandatoryStatus;
    }

    public void setMandatoryStatus(MandatoryStatus mandatoryStatus) {
        this.mandatoryStatus = mandatoryStatus;
    }

    public boolean isShortlisted() {
        return shortlisted;
    }

    public void setShortlisted(boolean shortlisted) {
        this.shortlisted = shortlisted;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(String matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public String getMissingMandatorySkills() {
        return missingMandatorySkills;
    }

    public void setMissingMandatorySkills(String missingMandatorySkills) {
        this.missingMandatorySkills = missingMandatorySkills;
    }

    public String getMatchedPreferredSkills() {
        return matchedPreferredSkills;
    }

    public void setMatchedPreferredSkills(String matchedPreferredSkills) {
        this.matchedPreferredSkills = matchedPreferredSkills;
    }

    public String getEvidenceSnippets() {
        return evidenceSnippets;
    }

    public void setEvidenceSnippets(String evidenceSnippets) {
        this.evidenceSnippets = evidenceSnippets;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
