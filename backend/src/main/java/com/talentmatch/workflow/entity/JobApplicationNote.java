package com.talentmatch.workflow.entity;

import com.talentmatch.organization.entity.Organization;
import com.talentmatch.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_application_notes")
public class JobApplicationNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User authorUser;

    @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "stage_at_creation", nullable = false, length = 40)
    private String stageAtCreation;

    @Column(name = "decision_reason", length = 100)
    private String decisionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public JobApplicationNote() {
    }

    public JobApplicationNote(JobApplication application,
                              Organization organization,
                              User authorUser,
                              String noteText,
                              String stageAtCreation,
                              String decisionReason) {
        this.application = application;
        this.organization = organization;
        this.authorUser = authorUser;
        this.noteText = noteText;
        this.stageAtCreation = stageAtCreation;
        this.decisionReason = decisionReason;
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

    public JobApplication getApplication() {
        return application;
    }

    public void setApplication(JobApplication application) {
        this.application = application;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getAuthorUser() {
        return authorUser;
    }

    public void setAuthorUser(User authorUser) {
        this.authorUser = authorUser;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getStageAtCreation() {
        return stageAtCreation;
    }

    public void setStageAtCreation(String stageAtCreation) {
        this.stageAtCreation = stageAtCreation;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
