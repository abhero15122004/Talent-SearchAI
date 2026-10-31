package com.talentmatch.resume.service;

import com.talentmatch.ai.dto.AiResumeExtractResponse;
import com.talentmatch.ai.service.AiServiceClient;
import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.candidate.repository.CandidateRepository;
import com.talentmatch.resume.entity.BatchStatus;
import com.talentmatch.resume.entity.ResumeBatch;
import com.talentmatch.resume.entity.ResumeFile;
import com.talentmatch.resume.entity.ResumeFileStatus;
import com.talentmatch.resume.repository.ResumeBatchRepository;
import com.talentmatch.resume.repository.ResumeFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Optional;

@Service
public class ResumeFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(ResumeFileProcessor.class);

    private final ResumeFileRepository resumeFileRepository;
    private final ResumeBatchRepository resumeBatchRepository;
    private final CandidateRepository candidateRepository;
    private final StorageService storageService;
    private final TikaParserService tikaParserService;
    private final AiServiceClient aiServiceClient;

    public ResumeFileProcessor(ResumeFileRepository resumeFileRepository,
                               ResumeBatchRepository resumeBatchRepository,
                               CandidateRepository candidateRepository,
                               StorageService storageService,
                               TikaParserService tikaParserService,
                               AiServiceClient aiServiceClient) {
        this.resumeFileRepository = resumeFileRepository;
        this.resumeBatchRepository = resumeBatchRepository;
        this.candidateRepository = candidateRepository;
        this.storageService = storageService;
        this.tikaParserService = tikaParserService;
        this.aiServiceClient = aiServiceClient;
    }

    /**
     * Processes an individual resume file inside its own isolated transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processFile(Long fileId) {
        Optional<ResumeFile> fileOpt = resumeFileRepository.findById(fileId);
        if (fileOpt.isEmpty()) {
            log.error("Resume file with ID {} not found for processing", fileId);
            return;
        }

        ResumeFile file = fileOpt.get();
        if (file.getStatus() == ResumeFileStatus.SUCCEEDED || file.getStatus() == ResumeFileStatus.SKIPPED_DUPLICATE) {
            log.info("File {} already in final state {}", fileId, file.getStatus());
            return;
        }

        file.setStatus(ResumeFileStatus.PROCESSING);
        resumeFileRepository.saveAndFlush(file);

        ResumeBatch batch = file.getBatch();
        Long orgId = file.getOrganization().getId();

        try {
            // Check for duplicate content hash within this organization
            Optional<ResumeFile> existingDuplicate = resumeFileRepository
                    .findFirstByOrganizationIdAndContentHash(orgId, file.getContentHash());

            if (existingDuplicate.isPresent() &&
                    !existingDuplicate.get().getId().equals(file.getId()) &&
                    existingDuplicate.get().getStatus() == ResumeFileStatus.SUCCEEDED) {

                log.info("File {} is a duplicate of existing file {} (hash: {})",
                        file.getId(), existingDuplicate.get().getId(), file.getContentHash());
                file.setStatus(ResumeFileStatus.SKIPPED_DUPLICATE);
                file.setCandidate(existingDuplicate.get().getCandidate());
                file.setErrorMessage("Duplicate content matching previously ingested resume");
                resumeFileRepository.saveAndFlush(file);

                updateBatchProgress(batch.getId(), 0, 0, 1);
                return;
            }

            // Stream from local storage and parse
            try (InputStream is = storageService.getFileInputStream(file.getStorageKey())) {
                TikaParserService.ParsedDocument parsedDoc =
                        tikaParserService.parseDocument(is, file.getOriginalFilename());

                file.setContentType(parsedDoc.contentType());

                TikaParserService.ParsedCandidateProfile profile = parsedDoc.profile();
                
                // AI-driven structured enrichment
                AiResumeExtractResponse aiExtract = aiServiceClient.extractResume(parsedDoc.rawText(), String.valueOf(file.getId()));
                String finalName = (aiExtract != null && aiExtract.fullName() != null && !aiExtract.fullName().isBlank())
                        ? aiExtract.fullName() : profile.fullName();
                String finalEmail = (aiExtract != null && aiExtract.email() != null && !aiExtract.email().isBlank())
                        ? aiExtract.email() : profile.email();
                String finalPhone = (aiExtract != null && aiExtract.phone() != null && !aiExtract.phone().isBlank())
                        ? aiExtract.phone() : profile.phone();
                String finalSummary = (aiExtract != null && aiExtract.summary() != null && !aiExtract.summary().isBlank())
                        ? aiExtract.summary() : profile.summary();

                String finalSkills = profile.primarySkills();
                if (aiExtract != null && aiExtract.skills() != null && !aiExtract.skills().isEmpty()) {
                    finalSkills = String.join(", ", aiExtract.skills().stream()
                            .map(AiResumeExtractResponse.AiSkillExtraction::canonicalName)
                            .distinct()
                            .toList());
                }

                Candidate candidate = null;

                // Check if candidate with matching email already exists in this organization
                if (finalEmail != null && !finalEmail.trim().isEmpty()) {
                    Optional<Candidate> existingCandidate =
                            candidateRepository.findFirstByOrganizationIdAndEmail(orgId, finalEmail.trim());
                    if (existingCandidate.isPresent()) {
                        candidate = existingCandidate.get();
                        updateCandidateIfMissing(candidate, profile, parsedDoc.rawText(), file.getOriginalFilename(), finalName, finalEmail, finalPhone, finalSummary, finalSkills);
                        candidateRepository.save(candidate);
                    }
                }

                if (candidate == null) {
                    candidate = new Candidate(
                            file.getOrganization(),
                            finalName,
                            finalEmail,
                            finalPhone,
                            parsedDoc.rawText(),
                            finalSummary,
                            finalSkills,
                            profile.educationHistory(),
                            profile.experienceHistory(),
                            profile.projectHistory(),
                            file.getOriginalFilename()
                    );
                    candidate = candidateRepository.save(candidate);
                }

                file.setCandidate(candidate);
                file.setStatus(ResumeFileStatus.SUCCEEDED);
                file.setErrorMessage(null);
                resumeFileRepository.saveAndFlush(file);

                updateBatchProgress(batch.getId(), 1, 0, 0);
            }

        } catch (Exception e) {
            log.error("Failed to parse and ingest resume file {}: {}", file.getId(), e.getMessage(), e);
            file.setStatus(ResumeFileStatus.FAILED);
            String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            file.setErrorMessage(err.length() > 500 ? err.substring(0, 500) : err);
            resumeFileRepository.saveAndFlush(file);

            updateBatchProgress(batch.getId(), 0, 1, 0);
        }
    }

    private void updateCandidateIfMissing(Candidate candidate,
                                          TikaParserService.ParsedCandidateProfile profile,
                                          String rawText,
                                          String sourceFilename,
                                          String finalName,
                                          String finalEmail,
                                          String finalPhone,
                                          String finalSummary,
                                          String finalSkills) {
        if (candidate.getFullName() == null && finalName != null) {
            candidate.setFullName(finalName);
        }
        if (candidate.getPhone() == null && finalPhone != null) {
            candidate.setPhone(finalPhone);
        }
        if (candidate.getSummary() == null && finalSummary != null) {
            candidate.setSummary(finalSummary);
        }
        if (candidate.getPrimarySkills() == null && finalSkills != null) {
            candidate.setPrimarySkills(finalSkills);
        }
        if (candidate.getEducationHistory() == null && profile.educationHistory() != null) {
            candidate.setEducationHistory(profile.educationHistory());
        }
        if (candidate.getExperienceHistory() == null && profile.experienceHistory() != null) {
            candidate.setExperienceHistory(profile.experienceHistory());
        }
        if (candidate.getProjectHistory() == null && profile.projectHistory() != null) {
            candidate.setProjectHistory(profile.projectHistory());
        }
    }

    private void updateBatchProgress(Long batchId, int successDelta, int failDelta, int dupDelta) {
        Optional<ResumeBatch> batchOpt = resumeBatchRepository.findById(batchId);
        if (batchOpt.isEmpty()) return;

        ResumeBatch batch = batchOpt.get();
        batch.setProcessedFiles(batch.getProcessedFiles() + 1);
        batch.setSuccessfulFiles(batch.getSuccessfulFiles() + successDelta);
        batch.setFailedFiles(batch.getFailedFiles() + failDelta);
        batch.setDuplicateFiles(batch.getDuplicateFiles() + dupDelta);

        if (batch.getProcessedFiles() >= batch.getTotalFiles()) {
            if (batch.getSuccessfulFiles() == 0 && batch.getDuplicateFiles() == 0) {
                batch.setStatus(BatchStatus.FAILED);
                batch.setErrorMessage("All files in batch failed extraction");
            } else if (batch.getFailedFiles() > 0) {
                batch.setStatus(BatchStatus.PARTIAL_SUCCESS);
            } else {
                batch.setStatus(BatchStatus.SUCCEEDED);
            }
        } else {
            batch.setStatus(BatchStatus.PROCESSING);
        }

        resumeBatchRepository.saveAndFlush(batch);
    }
}
