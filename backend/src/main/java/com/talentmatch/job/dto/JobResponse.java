package com.talentmatch.job.dto;

import com.talentmatch.job.entity.EmploymentType;
import com.talentmatch.job.entity.JobStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JobResponse {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private Long createdByUserId;
    private String createdByUserName;
    private String title;
    private String department;
    private String location;
    private EmploymentType employmentType;
    private String description;
    private int minYearsExperience;
    private Integer maxYearsExperience;
    private JobStatus status;
    private int shortlistCount;
    private BigDecimal minScoreThreshold;
    private BigDecimal weightSkillCoverage;
    private BigDecimal weightExperienceRelevance;
    private BigDecimal weightProjectRelevance;
    private BigDecimal weightSemanticSimilarity;
    private List<JobSkillDto> skills = new ArrayList<>();
    private List<JobQualificationDto> qualifications = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public JobResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByUserName() {
        return createdByUserName;
    }

    public void setCreatedByUserName(String createdByUserName) {
        this.createdByUserName = createdByUserName;
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

    public List<JobSkillDto> getSkills() {
        return skills;
    }

    public void setSkills(List<JobSkillDto> skills) {
        this.skills = skills;
    }

    public List<JobQualificationDto> getQualifications() {
        return qualifications;
    }

    public void setQualifications(List<JobQualificationDto> qualifications) {
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
