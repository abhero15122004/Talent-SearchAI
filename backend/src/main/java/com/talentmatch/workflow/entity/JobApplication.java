package com.talentmatch.workflow.entity;

import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.job.entity.Job;
import com.talentmatch.matching.entity.JobCandidateMatch;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 40)
    private ApplicationStage stage = ApplicationStage.NEW;

    @Column(name = "is_shortlisted", nullable = false)
    private boolean shortlisted = false;

    @Column(name = "decision_reason", length = 100)
    private String decisionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedToUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_match_id")
    private JobCandidateMatch latestMatch;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<JobApplicationNote> notes = new ArrayList<>();

    @Column(name = "stage_updated_at", nullable = false)
    private Instant stageUpdatedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public JobApplication() {
    }

    public JobApplication(Job job, Candidate candidate, Organization organization, ApplicationStage stage) {
        this.job = job;
        this.candidate = candidate;
        this.organization = organization;
        this.stage = stage != null ? stage : ApplicationStage.NEW;
        this.shortlisted = (this.stage == ApplicationStage.SHORTLISTED);
        this.stageUpdatedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (stageUpdatedAt == null) {
            stageUpdatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addNote(JobApplicationNote note) {
        notes.add(note);
        note.setApplication(this);
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

    public ApplicationStage getStage() {
        return stage;
    }

    public void setStage(ApplicationStage stage) {
        this.stage = stage;
        this.stageUpdatedAt = Instant.now();
        if (stage == ApplicationStage.SHORTLISTED) {
            this.shortlisted = true;
        }
    }

    public boolean isShortlisted() {
        return shortlisted;
    }

    public void setShortlisted(boolean shortlisted) {
        this.shortlisted = shortlisted;
        if (shortlisted && this.stage == ApplicationStage.NEW) {
            this.stage = ApplicationStage.SHORTLISTED;
            this.stageUpdatedAt = Instant.now();
        }
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public User getAssignedToUser() {
        return assignedToUser;
    }

    public void setAssignedToUser(User assignedToUser) {
        this.assignedToUser = assignedToUser;
    }

    public JobCandidateMatch getLatestMatch() {
        return latestMatch;
    }

    public void setLatestMatch(JobCandidateMatch latestMatch) {
        this.latestMatch = latestMatch;
    }

    public List<JobApplicationNote> getNotes() {
        return notes;
    }

    public void setNotes(List<JobApplicationNote> notes) {
        this.notes = notes;
    }

    public Instant getStageUpdatedAt() {
        return stageUpdatedAt;
    }

    public void setStageUpdatedAt(Instant stageUpdatedAt) {
        this.stageUpdatedAt = stageUpdatedAt;
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
