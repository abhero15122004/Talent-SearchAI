package com.talentmatch.job.dto;

import com.talentmatch.job.entity.EmploymentType;
import com.talentmatch.job.entity.JobStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CreateJobRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 255, message = "Job title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @NotBlank(message = "Location is required")
    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @NotBlank(message = "Job description is required")
    private String description;

    @Min(value = 0, message = "Minimum experience years cannot be negative")
    private int minYearsExperience = 0;

    private Integer maxYearsExperience;

    private JobStatus status = JobStatus.DRAFT;

    @NotNull(message = "Shortlist count is required")
    @Min(value = 1, message = "Shortlist count must be at least 1")
    @Max(value = 500, message = "Shortlist count cannot exceed 500 candidates")
    private Integer shortlistCount = 10;

    @NotNull(message = "Minimum score threshold is required")
    @DecimalMin(value = "0.0", message = "Minimum score threshold cannot be negative")
    @DecimalMax(value = "100.0", message = "Minimum score threshold cannot exceed 100.0")
    private BigDecimal minScoreThreshold = new BigDecimal("60.00");

    @NotNull(message = "Skill coverage weight is required")
    @DecimalMin(value = "0.0", message = "Skill coverage weight cannot be negative")
    @DecimalMax(value = "100.0", message = "Skill coverage weight cannot exceed 100.0")
    private BigDecimal weightSkillCoverage = new BigDecimal("40.00");

    @NotNull(message = "Experience relevance weight is required")
    @DecimalMin(value = "0.0", message = "Experience relevance weight cannot be negative")
    @DecimalMax(value = "100.0", message = "Experience relevance weight cannot exceed 100.0")
    private BigDecimal weightExperienceRelevance = new BigDecimal("25.00");

    @NotNull(message = "Project relevance weight is required")
    @DecimalMin(value = "0.0", message = "Project relevance weight cannot be negative")
    @DecimalMax(value = "100.0", message = "Project relevance weight cannot exceed 100.0")
    private BigDecimal weightProjectRelevance = new BigDecimal("15.00");

    @NotNull(message = "Semantic similarity weight is required")
    @DecimalMin(value = "0.0", message = "Semantic similarity weight cannot be negative")
    @DecimalMax(value = "100.0", message = "Semantic similarity weight cannot exceed 100.0")
    private BigDecimal weightSemanticSimilarity = new BigDecimal("20.00");

    @Valid
    private List<JobSkillDto> skills = new ArrayList<>();

    @Valid
    private List<JobQualificationDto> qualifications = new ArrayList<>();

    public CreateJobRequest() {
    }

    @AssertTrue(message = "Configured matching weights must sum exactly to 100.00%")
    public boolean isWeightsSumValid() {
        if (weightSkillCoverage == null || weightExperienceRelevance == null ||
            weightProjectRelevance == null || weightSemanticSimilarity == null) {
            return false;
        }
        BigDecimal sum = weightSkillCoverage.add(weightExperienceRelevance)
                .add(weightProjectRelevance)
                .add(weightSemanticSimilarity);
        return sum.compareTo(new BigDecimal("100.00")) == 0 || sum.compareTo(new BigDecimal("100")) == 0;
    }

    @AssertTrue(message = "Maximum experience years cannot be less than minimum experience years")
    public boolean isExperienceRangeValid() {
        if (maxYearsExperience != null && maxYearsExperience < minYearsExperience) {
            return false;
        }
        return true;
    }

    // Getters and Setters
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

    public Integer getShortlistCount() {
        return shortlistCount;
    }

    public void setShortlistCount(Integer shortlistCount) {
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
}
