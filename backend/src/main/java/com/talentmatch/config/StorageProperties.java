package com.talentmatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.ingestion")
public class StorageProperties {

    private String storageDir = "./storage/resumes";
    private long maxFileSize = 15 * 1024 * 1024; // 15MB
    private long maxArchiveSize = 100 * 1024 * 1024; // 100MB
    private long maxDecompressedSize = 200 * 1024 * 1024; // 200MB
    private int maxFilesPerBatch = 500;

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxArchiveSize() {
        return maxArchiveSize;
    }

    public void setMaxArchiveSize(long maxArchiveSize) {
        this.maxArchiveSize = maxArchiveSize;
    }

    public long getMaxDecompressedSize() {
        return maxDecompressedSize;
    }

    public void setMaxDecompressedSize(long maxDecompressedSize) {
        this.maxDecompressedSize = maxDecompressedSize;
    }

    public int getMaxFilesPerBatch() {
        return maxFilesPerBatch;
    }

    public void setMaxFilesPerBatch(int maxFilesPerBatch) {
        this.maxFilesPerBatch = maxFilesPerBatch;
    }
}
