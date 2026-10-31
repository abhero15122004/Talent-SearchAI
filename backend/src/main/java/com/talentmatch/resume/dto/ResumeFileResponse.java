package com.talentmatch.resume.dto;

import com.talentmatch.resume.entity.ResumeFile;
import com.talentmatch.resume.entity.ResumeFileStatus;

import java.time.Instant;

public class ResumeFileResponse {

    private Long id;
    private Long batchId;
    private Long candidateId;
    private String originalFilename;
    private long fileSizeBytes;
    private String contentType;
    private ResumeFileStatus status;
    private String errorMessage;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static ResumeFileResponse fromEntity(ResumeFile file) {
        ResumeFileResponse resp = new ResumeFileResponse();
        resp.setId(file.getId());
        resp.setBatchId(file.getBatch() != null ? file.getBatch().getId() : null);
        resp.setCandidateId(file.getCandidate() != null ? file.getCandidate().getId() : null);
        resp.setOriginalFilename(file.getOriginalFilename());
        resp.setFileSizeBytes(file.getFileSizeBytes());
        resp.setContentType(file.getContentType());
        resp.setStatus(file.getStatus());
        resp.setErrorMessage(file.getErrorMessage());
        resp.setRetryCount(file.getRetryCount());
        resp.setCreatedAt(file.getCreatedAt());
        resp.setUpdatedAt(file.getUpdatedAt());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public ResumeFileStatus getStatus() {
        return status;
    }

    public void setStatus(ResumeFileStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
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
