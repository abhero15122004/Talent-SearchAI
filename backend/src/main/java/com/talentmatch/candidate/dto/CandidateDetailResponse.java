package com.talentmatch.candidate.dto;

import com.talentmatch.candidate.entity.Candidate;

import java.time.Instant;

public class CandidateDetailResponse {

    private Long id;
    private Long organizationId;
    private String fullName;
    private String email;
    private String phone;
    private String summary;
    private String primarySkills;
    private String educationHistory;
    private String experienceHistory;
    private String projectHistory;
    private String rawText;
    private String sourceFilename;
    private Instant createdAt;
    private Instant updatedAt;

    public static CandidateDetailResponse fromEntity(Candidate candidate) {
        CandidateDetailResponse resp = new CandidateDetailResponse();
        resp.setId(candidate.getId());
        resp.setOrganizationId(candidate.getOrganization() != null ? candidate.getOrganization().getId() : null);
        resp.setFullName(candidate.getFullName());
        resp.setEmail(candidate.getEmail());
        resp.setPhone(candidate.getPhone());
        resp.setSummary(candidate.getSummary());
        resp.setPrimarySkills(candidate.getPrimarySkills());
        resp.setEducationHistory(candidate.getEducationHistory());
        resp.setExperienceHistory(candidate.getExperienceHistory());
        resp.setProjectHistory(candidate.getProjectHistory());
        resp.setRawText(candidate.getRawText());
        resp.setSourceFilename(candidate.getSourceFilename());
        resp.setCreatedAt(candidate.getCreatedAt());
        resp.setUpdatedAt(candidate.getUpdatedAt());
        return resp;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPrimarySkills() {
        return primarySkills;
    }

    public void setPrimarySkills(String primarySkills) {
        this.primarySkills = primarySkills;
    }

    public String getEducationHistory() {
        return educationHistory;
    }

    public void setEducationHistory(String educationHistory) {
        this.educationHistory = educationHistory;
    }

    public String getExperienceHistory() {
        return experienceHistory;
    }

    public void setExperienceHistory(String experienceHistory) {
        this.experienceHistory = experienceHistory;
    }

    public String getProjectHistory() {
        return projectHistory;
    }

    public void setProjectHistory(String projectHistory) {
        this.projectHistory = projectHistory;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
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
