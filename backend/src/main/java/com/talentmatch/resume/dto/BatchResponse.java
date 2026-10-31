package com.talentmatch.resume.dto;

import com.talentmatch.resume.entity.BatchStatus;
import com.talentmatch.resume.entity.ResumeBatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BatchResponse {

    private Long id;
    private Long organizationId;
    private String createdByUserName;
    private String name;
    private BatchStatus status;
    private int totalFiles;
    private int processedFiles;
    private int successfulFiles;
    private int failedFiles;
    private int duplicateFiles;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ResumeFileResponse> files = new ArrayList<>();

    public static BatchResponse fromEntity(ResumeBatch batch) {
        BatchResponse resp = new BatchResponse();
        resp.setId(batch.getId());
        resp.setOrganizationId(batch.getOrganization() != null ? batch.getOrganization().getId() : null);
        resp.setCreatedByUserName(batch.getCreatedByUser() != null ? batch.getCreatedByUser().getFullName() : null);
        resp.setName(batch.getName());
        resp.setStatus(batch.getStatus());
        resp.setTotalFiles(batch.getTotalFiles());
        resp.setProcessedFiles(batch.getProcessedFiles());
        resp.setSuccessfulFiles(batch.getSuccessfulFiles());
        resp.setFailedFiles(batch.getFailedFiles());
        resp.setDuplicateFiles(batch.getDuplicateFiles());
        resp.setErrorMessage(batch.getErrorMessage());
        resp.setCreatedAt(batch.getCreatedAt());
        resp.setUpdatedAt(batch.getUpdatedAt());

        if (batch.getFiles() != null) {
            resp.setFiles(batch.getFiles().stream()
                    .map(ResumeFileResponse::fromEntity)
                    .collect(Collectors.toList()));
        }
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

    public String getCreatedByUserName() {
        return createdByUserName;
    }

    public void setCreatedByUserName(String createdByUserName) {
        this.createdByUserName = createdByUserName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getProcessedFiles() {
        return processedFiles;
    }

    public void setProcessedFiles(int processedFiles) {
        this.processedFiles = processedFiles;
    }

    public int getSuccessfulFiles() {
        return successfulFiles;
    }

    public void setSuccessfulFiles(int successfulFiles) {
        this.successfulFiles = successfulFiles;
    }

    public int getFailedFiles() {
        return failedFiles;
    }

    public void setFailedFiles(int failedFiles) {
        this.failedFiles = failedFiles;
    }

    public int getDuplicateFiles() {
        return duplicateFiles;
    }

    public void setDuplicateFiles(int duplicateFiles) {
        this.duplicateFiles = duplicateFiles;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public List<ResumeFileResponse> getFiles() {
        return files;
    }

    public void setFiles(List<ResumeFileResponse> files) {
        this.files = files;
    }
}
