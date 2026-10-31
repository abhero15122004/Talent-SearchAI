package com.talentmatch.candidate.entity;

import com.talentmatch.organization.entity.Organization;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "raw_text", columnDefinition = "MEDIUMTEXT")
    private String rawText;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "primary_skills", columnDefinition = "TEXT")
    private String primarySkills;

    @Column(name = "education_history", columnDefinition = "TEXT")
    private String educationHistory;

    @Column(name = "experience_history", columnDefinition = "TEXT")
    private String experienceHistory;

    @Column(name = "project_history", columnDefinition = "TEXT")
    private String projectHistory;

    @Column(name = "source_filename", length = 255)
    private String sourceFilename;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Candidate() {
    }

    public Candidate(Organization organization, String fullName, String email, String phone,
                     String rawText, String summary, String primarySkills,
                     String educationHistory, String experienceHistory, String projectHistory,
                     String sourceFilename) {
        this.organization = organization;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.rawText = rawText;
        this.summary = summary;
        this.primarySkills = primarySkills;
        this.educationHistory = educationHistory;
        this.experienceHistory = experienceHistory;
        this.projectHistory = projectHistory;
        this.sourceFilename = sourceFilename;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

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

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
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
