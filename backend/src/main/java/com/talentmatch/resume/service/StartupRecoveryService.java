package com.talentmatch.resume.service;

import com.talentmatch.resume.entity.BatchStatus;
import com.talentmatch.resume.entity.ResumeBatch;
import com.talentmatch.resume.entity.ResumeFile;
import com.talentmatch.resume.entity.ResumeFileStatus;
import com.talentmatch.resume.repository.ResumeBatchRepository;
import com.talentmatch.resume.repository.ResumeFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StartupRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(StartupRecoveryService.class);

    private final ResumeBatchRepository resumeBatchRepository;
    private final ResumeFileRepository resumeFileRepository;

    public StartupRecoveryService(ResumeBatchRepository resumeBatchRepository,
                                  ResumeFileRepository resumeFileRepository) {
        this.resumeBatchRepository = resumeBatchRepository;
        this.resumeFileRepository = resumeFileRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverInterruptedBatches() {
        log.info("Checking for interrupted resume ingestion tasks from previous server instance...");

        // 1. Recover interrupted files
        List<ResumeFile> processingFiles = resumeFileRepository.findByStatusIn(List.of(
                ResumeFileStatus.PROCESSING,
                ResumeFileStatus.QUEUED
        ));

        int recoveredFilesCount = 0;
        for (ResumeFile file : processingFiles) {
            file.setStatus(ResumeFileStatus.FAILED);
            file.setErrorMessage("Interrupted by system restart - eligible for retry");
            resumeFileRepository.save(file);
            recoveredFilesCount++;
        }

        // 2. Recover interrupted batches
        List<ResumeBatch> activeBatches = resumeBatchRepository.findByStatusIn(List.of(
                BatchStatus.PROCESSING,
                BatchStatus.QUEUED
        ));

        for (ResumeBatch batch : activeBatches) {
            int success = (int) resumeFileRepository.countByBatchIdAndStatus(batch.getId(), ResumeFileStatus.SUCCEEDED);
            int failed = (int) resumeFileRepository.countByBatchIdAndStatus(batch.getId(), ResumeFileStatus.FAILED);
            int duplicate = (int) resumeFileRepository.countByBatchIdAndStatus(batch.getId(), ResumeFileStatus.SKIPPED_DUPLICATE);

            batch.setSuccessfulFiles(success);
            batch.setFailedFiles(failed);
            batch.setDuplicateFiles(duplicate);
            batch.setProcessedFiles(success + failed + duplicate);

            if (success > 0) {
                batch.setStatus(BatchStatus.PARTIAL_SUCCESS);
            } else {
                batch.setStatus(BatchStatus.FAILED);
            }
            batch.setErrorMessage("Interrupted by system restart - failed files can be retried");
            resumeBatchRepository.save(batch);
        }

        if (recoveredFilesCount > 0 || !activeBatches.isEmpty()) {
            log.info("Startup recovery completed: marked {} interrupted files and {} batches as recoverable/failed",
                    recoveredFilesCount, activeBatches.size());
        } else {
            log.info("Startup recovery completed: no orphaned resume tasks found.");
        }
    }
}
