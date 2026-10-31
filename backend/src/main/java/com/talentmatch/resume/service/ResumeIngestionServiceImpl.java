package com.talentmatch.resume.service;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.common.exception.BadRequestException;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.config.StorageProperties;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.resume.dto.BatchResponse;
import com.talentmatch.resume.dto.BatchSummaryResponse;
import com.talentmatch.resume.dto.BatchUploadResponse;
import com.talentmatch.resume.dto.ResumeFileResponse;
import com.talentmatch.resume.entity.BatchStatus;
import com.talentmatch.resume.entity.ResumeBatch;
import com.talentmatch.resume.entity.ResumeFile;
import com.talentmatch.resume.entity.ResumeFileStatus;
import com.talentmatch.resume.repository.ResumeBatchRepository;
import com.talentmatch.resume.repository.ResumeFileRepository;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Service
@Transactional
public class ResumeIngestionServiceImpl implements ResumeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ResumeIngestionServiceImpl.class);

    private final ResumeBatchRepository resumeBatchRepository;
    private final ResumeFileRepository resumeFileRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ResumeFileProcessor resumeFileProcessor;
    private final Executor resumeTaskExecutor;
    private final StorageProperties storageProperties;
    private final AuditLogService auditLogService;

    public ResumeIngestionServiceImpl(
            ResumeBatchRepository resumeBatchRepository,
            ResumeFileRepository resumeFileRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            StorageService storageService,
            ResumeFileProcessor resumeFileProcessor,
            @Qualifier("resumeTaskExecutor") Executor resumeTaskExecutor,
            StorageProperties storageProperties,
            AuditLogService auditLogService) {
        this.resumeBatchRepository = resumeBatchRepository;
        this.resumeFileRepository = resumeFileRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.resumeFileProcessor = resumeFileProcessor;
        this.resumeTaskExecutor = resumeTaskExecutor;
        this.storageProperties = storageProperties;
        this.auditLogService = auditLogService;
    }

    @Override
    public BatchUploadResponse uploadZipArchive(MultipartFile zipFile, Long organizationId, Long userId) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new BadRequestException("Uploaded ZIP file cannot be empty");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String batchName = zipFile.getOriginalFilename() != null ? zipFile.getOriginalFilename() : "ZIP Batch";

        log.info("Processing ZIP resume upload '{}' for organization {}", batchName, organizationId);

        List<StorageService.StoredFile> extractedFiles;
        try {
            extractedFiles = storageService.extractZipArchive(zipFile.getInputStream(), zipFile.getSize());
        } catch (IOException e) {
            log.error("Failed to read ZIP archive: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to read uploaded ZIP archive: " + e.getMessage());
        }

        ResumeBatch batch = new ResumeBatch(organization, user, batchName, extractedFiles.size());
        batch.setStatus(BatchStatus.QUEUED);
        batch = resumeBatchRepository.save(batch);

        List<Long> fileIdsToProcess = new ArrayList<>();
        for (StorageService.StoredFile stored : extractedFiles) {
            ResumeFile rf = new ResumeFile(
                    batch,
                    organization,
                    stored.originalFilename(),
                    stored.storageKey(),
                    stored.fileSizeBytes(),
                    "application/octet-stream",
                    stored.contentHash()
            );
            batch.addFile(rf);
        }

        batch = resumeBatchRepository.saveAndFlush(batch);

        for (ResumeFile rf : batch.getFiles()) {
            fileIdsToProcess.add(rf.getId());
        }

        auditLogService.recordEvent(organizationId, userId, user.getEmail(), "RESUME_BATCH_UPLOAD",
                "ResumeBatch", batch.getId().toString(), "Uploaded ZIP archive with " + extractedFiles.size() + " files", null);

        // Dispatch async processing to bounded executor
        dispatchAsyncProcessing(fileIdsToProcess);

        return new BatchUploadResponse(
                batch.getId(),
                batch.getName(),
                batch.getStatus(),
                batch.getTotalFiles(),
                String.format("Batch queued with %d files for asynchronous extraction", batch.getTotalFiles())
        );
    }

    @Override
    public BatchUploadResponse uploadFiles(List<MultipartFile> files, String batchName, Long organizationId, Long userId) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one resume file must be uploaded");
        }

        if (files.size() > storageProperties.getMaxFilesPerBatch()) {
            throw new BadRequestException(String.format("File count (%d) exceeds maximum limit of %d files per batch",
                    files.size(), storageProperties.getMaxFilesPerBatch()));
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String finalName = (batchName != null && !batchName.trim().isEmpty())
                ? batchName.trim()
                : "Batch upload (" + files.size() + " files)";

        log.info("Processing multi-file upload '{}' ({} files) for organization {}", finalName, files.size(), organizationId);

        List<StorageService.StoredFile> storedFiles = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
                StorageService.StoredFile stored = storageService.storeFile(file.getInputStream(), filename);
                storedFiles.add(stored);
            }
        } catch (Exception e) {
            storageService.cleanupFiles(storedFiles);
            log.error("Failed to store uploaded files: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to store uploaded files: " + e.getMessage());
        }

        if (storedFiles.isEmpty()) {
            throw new BadRequestException("No valid non-empty files were provided");
        }

        ResumeBatch batch = new ResumeBatch(organization, user, finalName, storedFiles.size());
        batch.setStatus(BatchStatus.QUEUED);
        batch = resumeBatchRepository.save(batch);

        List<Long> fileIdsToProcess = new ArrayList<>();
        for (StorageService.StoredFile stored : storedFiles) {
            ResumeFile rf = new ResumeFile(
                    batch,
                    organization,
                    stored.originalFilename(),
                    stored.storageKey(),
                    stored.fileSizeBytes(),
                    "application/octet-stream",
                    stored.contentHash()
            );
            batch.addFile(rf);
        }

        batch = resumeBatchRepository.saveAndFlush(batch);

        for (ResumeFile rf : batch.getFiles()) {
            fileIdsToProcess.add(rf.getId());
        }

        auditLogService.recordEvent(organizationId, userId, user.getEmail(), "RESUME_BATCH_UPLOAD",
                "ResumeBatch", batch.getId().toString(), "Uploaded " + storedFiles.size() + " individual files", null);

        // Dispatch async processing to bounded executor
        dispatchAsyncProcessing(fileIdsToProcess);

        return new BatchUploadResponse(
                batch.getId(),
                batch.getName(),
                batch.getStatus(),
                batch.getTotalFiles(),
                String.format("Batch queued with %d files for asynchronous extraction", batch.getTotalFiles())
        );
    }

    private void dispatchAsyncProcessing(List<Long> fileIds) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Long fileId : fileIds) {
                        resumeTaskExecutor.execute(() -> {
                            try {
                                resumeFileProcessor.processFile(fileId);
                            } catch (Exception e) {
                                log.error("Unhandled error during async processing of file {}: {}", fileId, e.getMessage(), e);
                            }
                        });
                    }
                }
            });
        } else {
            for (Long fileId : fileIds) {
                resumeTaskExecutor.execute(() -> {
                    try {
                        resumeFileProcessor.processFile(fileId);
                    } catch (Exception e) {
                        log.error("Unhandled error during async processing of file {}: {}", fileId, e.getMessage(), e);
                    }
                });
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BatchResponse getBatchById(Long batchId, Long organizationId) {
        ResumeBatch batch = resumeBatchRepository.findByIdAndOrganizationIdWithFiles(batchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ResumeBatch", "id", batchId));
        return BatchResponse.fromEntity(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchSummaryResponse> listBatches(Long organizationId, Pageable pageable) {
        return resumeBatchRepository.findByOrganizationId(organizationId, pageable)
                .map(BatchSummaryResponse::fromEntity);
    }

    @Override
    public ResumeFileResponse retryFile(Long fileId, Long organizationId) {
        ResumeFile file = resumeFileRepository.findByIdAndOrganizationId(fileId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("ResumeFile", "id", fileId));

        if (file.getStatus() != ResumeFileStatus.FAILED) {
            throw new BadRequestException("Only failed files can be retried. Current status is " + file.getStatus());
        }

        ResumeBatch batch = file.getBatch();
        if (batch.getFailedFiles() > 0) {
            batch.setFailedFiles(batch.getFailedFiles() - 1);
        }
        if (batch.getProcessedFiles() > 0) {
            batch.setProcessedFiles(batch.getProcessedFiles() - 1);
        }
        batch.setStatus(BatchStatus.PROCESSING);
        resumeBatchRepository.saveAndFlush(batch);

        file.setStatus(ResumeFileStatus.QUEUED);
        file.setErrorMessage(null);
        file.setRetryCount(file.getRetryCount() + 1);
        file = resumeFileRepository.saveAndFlush(file);

        // Submit for processing after transaction commits
        final Long retryId = file.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    resumeTaskExecutor.execute(() -> {
                        try {
                            resumeFileProcessor.processFile(retryId);
                        } catch (Exception e) {
                            log.error("Unhandled error retrying file {}: {}", retryId, e.getMessage(), e);
                        }
                    });
                }
            });
        } else {
            resumeTaskExecutor.execute(() -> {
                try {
                    resumeFileProcessor.processFile(retryId);
                } catch (Exception e) {
                    log.error("Unhandled error retrying file {}: {}", retryId, e.getMessage(), e);
                }
            });
        }

        return ResumeFileResponse.fromEntity(file);
    }
}
