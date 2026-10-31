package com.talentmatch.job.entity;

import com.talentmatch.organization.entity.Organization;
import com.talentmatch.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "location", nullable = false, length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 50)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_years_experience", nullable = false)
    private int minYearsExperience = 0;

    @Column(name = "max_years_experience")
    private Integer maxYearsExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "shortlist_count", nullable = false)
    private int shortlistCount = 10;

    @Column(name = "min_score_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal minScoreThreshold = new BigDecimal("60.00");

    @Column(name = "weight_skill_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightSkillCoverage = new BigDecimal("40.00");

    @Column(name = "weight_experience_relevance", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightExperienceRelevance = new BigDecimal("25.00");

    @Column(name = "weight_project_relevance", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightProjectRelevance = new BigDecimal("15.00");

    @Column(name = "weight_semantic_similarity", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightSemanticSimilarity = new BigDecimal("20.00");

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JobSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JobQualification> qualifications = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Job() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addSkill(JobSkill skill) {
        skills.add(skill);
        skill.setJob(this);
    }

    public void removeSkill(JobSkill skill) {
        skills.remove(skill);
        skill.setJob(null);
    }

    public void clearSkills() {
        for (JobSkill skill : skills) {
            skill.setJob(null);
        }
        skills.clear();
    }

    public void addQualification(JobQualification qualification) {
        qualifications.add(qualification);
        qualification.setJob(this);
    }

    public void removeQualification(JobQualification qualification) {
        qualifications.remove(qualification);
        qualification.setJob(null);
    }

    public void clearQualifications() {
        for (JobQualification q : qualifications) {
            q.setJob(null);
        }
        qualifications.clear();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMinYearsExperience() {
        return minYearsExperience;
    }

    public void setMinYearsExperience(int minYearsExperience) {
        this.minYearsExperience = minYearsExperience;
    }

    public Integer getMaxYearsExperience() {
        return maxYearsExperience;
    }

    public void setMaxYearsExperience(Integer maxYearsExperience) {
        this.maxYearsExperience = maxYearsExperience;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getShortlistCount() {
        return shortlistCount;
    }

    public void setShortlistCount(int shortlistCount) {
        this.shortlistCount = shortlistCount;
    }

    public BigDecimal getMinScoreThreshold() {
        return minScoreThreshold;
    }

    public void setMinScoreThreshold(BigDecimal minScoreThreshold) {
        this.minScoreThreshold = minScoreThreshold;
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

    public List<JobSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<JobSkill> skills) {
        this.skills = skills;
    }

    public List<JobQualification> getQualifications() {
        return qualifications;
    }

    public void setQualifications(List<JobQualification> qualifications) {
        this.qualifications = qualifications;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
