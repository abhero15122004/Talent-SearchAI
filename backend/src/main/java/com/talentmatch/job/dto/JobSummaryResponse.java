package com.talentmatch.job.dto;

import com.talentmatch.job.entity.EmploymentType;
import com.talentmatch.job.entity.JobStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class JobSummaryResponse {

    private Long id;
    private Long organizationId;
    private String title;
    private String department;
    private String location;
    private EmploymentType employmentType;
    private JobStatus status;
    private int minYearsExperience;
    private Integer maxYearsExperience;
    private int shortlistCount;
    private BigDecimal minScoreThreshold;
    private int mandatorySkillsCount;
    private int preferredSkillsCount;
    private int qualificationsCount;
    private Instant createdAt;
    private Instant updatedAt;

    public JobSummaryResponse() {
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

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
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

    public int getMandatorySkillsCount() {
        return mandatorySkillsCount;
    }

    public void setMandatorySkillsCount(int mandatorySkillsCount) {
        this.mandatorySkillsCount = mandatorySkillsCount;
    }

    public int getPreferredSkillsCount() {
        return preferredSkillsCount;
    }

    public void setPreferredSkillsCount(int preferredSkillsCount) {
        this.preferredSkillsCount = preferredSkillsCount;
    }

    public int getQualificationsCount() {
        return qualificationsCount;
    }

    public void setQualificationsCount(int qualificationsCount) {
        this.qualificationsCount = qualificationsCount;
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
