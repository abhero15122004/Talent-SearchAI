package com.talentmatch.matching.entity;

import com.talentmatch.job.entity.Job;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_match_runs")
public class JobMatchRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id", nullable = false)
    private User triggeredByUser;

    @Column(name = "run_number", nullable = false)
    private int runNumber = 1;

    @Column(name = "algorithm_version", nullable = false, length = 50)
    private String algorithmVersion = "1.0.0-hybrid";

    @Column(name = "weight_skill_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightSkillCoverage;

    @Column(name = "weight_experience_relevance", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightExperienceRelevance;

    @Column(name = "weight_project_relevance", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightProjectRelevance;

    @Column(name = "weight_semantic_similarity", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightSemanticSimilarity;

    @Column(name = "min_score_threshold", precision = 5, scale = 2)
    private BigDecimal minScoreThreshold;

    @Column(name = "shortlist_target", nullable = false)
    private int shortlistTarget = 10;

    @Column(name = "total_candidates_evaluated", nullable = false)
    private int totalCandidatesEvaluated = 0;

    @Column(name = "eligible_candidates_count", nullable = false)
    private int eligibleCandidatesCount = 0;

    @Column(name = "shortlisted_count", nullable = false)
    private int shortlistedCount = 0;

    @OneToMany(mappedBy = "matchRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("rankPosition ASC")
    private List<JobCandidateMatch> matches = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public JobMatchRun() {
    }

    public JobMatchRun(Job job,
                       Organization organization,
                       User triggeredByUser,
                       int runNumber,
                       String algorithmVersion,
                       BigDecimal weightSkillCoverage,
                       BigDecimal weightExperienceRelevance,
                       BigDecimal weightProjectRelevance,
                       BigDecimal weightSemanticSimilarity,
                       BigDecimal minScoreThreshold,
                       int shortlistTarget) {
        this.job = job;
        this.organization = organization;
        this.triggeredByUser = triggeredByUser;
        this.runNumber = runNumber;
        this.algorithmVersion = algorithmVersion;
        this.weightSkillCoverage = weightSkillCoverage;
        this.weightExperienceRelevance = weightExperienceRelevance;
        this.weightProjectRelevance = weightProjectRelevance;
        this.weightSemanticSimilarity = weightSemanticSimilarity;
        this.minScoreThreshold = minScoreThreshold;
        this.shortlistTarget = shortlistTarget;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addMatch(JobCandidateMatch match) {
        matches.add(match);
        match.setMatchRun(this);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getTriggeredByUser() {
        return triggeredByUser;
    }

    public void setTriggeredByUser(User triggeredByUser) {
        this.triggeredByUser = triggeredByUser;
    }

    public int getRunNumber() {
        return runNumber;
    }

    public void setRunNumber(int runNumber) {
        this.runNumber = runNumber;
    }

    public String getAlgorithmVersion() {
        return algorithmVersion;
    }

    public void setAlgorithmVersion(String algorithmVersion) {
        this.algorithmVersion = algorithmVersion;
    }

    public BigDecimal getWeightSkillCoverage() {
        return weightSkillCoverage;
    }

    public void setWeightSkillCoverage(BigDecimal weightSkillCoverage) {
        this.weightSkillCoverage = weightSkillCoverage;
    }

    public BigDecimal getWeightExperienceRelevance() {
        return weightExperienceRelevance;
    }

    public void setWeightExperienceRelevance(BigDecimal weightExperienceRelevance) {
        this.weightExperienceRelevance = weightExperienceRelevance;
    }

    public BigDecimal getWeightProjectRelevance() {
        return weightProjectRelevance;
    }

    public void setWeightProjectRelevance(BigDecimal weightProjectRelevance) {
        this.weightProjectRelevance = weightProjectRelevance;
    }

    public BigDecimal getWeightSemanticSimilarity() {
        return weightSemanticSimilarity;
    }

    public void setWeightSemanticSimilarity(BigDecimal weightSemanticSimilarity) {
        this.weightSemanticSimilarity = weightSemanticSimilarity;
    }

    public BigDecimal getMinScoreThreshold() {
        return minScoreThreshold;
    }

    public void setMinScoreThreshold(BigDecimal minScoreThreshold) {
        this.minScoreThreshold = minScoreThreshold;
    }

    public int getShortlistTarget() {
        return shortlistTarget;
    }

    public void setShortlistTarget(int shortlistTarget) {
        this.shortlistTarget = shortlistTarget;
    }

    public int getTotalCandidatesEvaluated() {
        return totalCandidatesEvaluated;
    }

    public void setTotalCandidatesEvaluated(int totalCandidatesEvaluated) {
        this.totalCandidatesEvaluated = totalCandidatesEvaluated;
    }

    public int getEligibleCandidatesCount() {
        return eligibleCandidatesCount;
    }

    public void setEligibleCandidatesCount(int eligibleCandidatesCount) {
        this.eligibleCandidatesCount = eligibleCandidatesCount;
    }

    public int getShortlistedCount() {
        return shortlistedCount;
    }

    public void setShortlistedCount(int shortlistedCount) {
        this.shortlistedCount = shortlistedCount;
    }

    public List<JobCandidateMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<JobCandidateMatch> matches) {
        this.matches = matches;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
