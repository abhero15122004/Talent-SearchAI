package com.talentmatch.resume.dto;

import com.talentmatch.resume.entity.BatchStatus;

public class BatchUploadResponse {

    private Long batchId;
    private String name;
    private BatchStatus status;
    private int totalFiles;
    private String message;

    public BatchUploadResponse() {
    }

    public BatchUploadResponse(Long batchId, String name, BatchStatus status, int totalFiles, String message) {
        this.batchId = batchId;
        this.name = name;
        this.status = status;
        this.totalFiles = totalFiles;
        this.message = message;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
