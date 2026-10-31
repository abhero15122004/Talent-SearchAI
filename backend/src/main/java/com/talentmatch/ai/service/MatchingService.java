package com.talentmatch.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmatch.ai.dto.AiMatchRequest;
import com.talentmatch.ai.dto.AiMatchResponse;
import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.candidate.repository.CandidateRepository;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.BadRequestException;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.job.entity.Job;
import com.talentmatch.job.entity.JobSkill;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.matching.dto.CandidateComparisonDto;
import com.talentmatch.matching.dto.JobCandidateMatchDto;
import com.talentmatch.matching.dto.JobMatchRunDto;
import com.talentmatch.matching.entity.JobCandidateMatch;
import com.talentmatch.matching.entity.JobMatchRun;
import com.talentmatch.matching.entity.MandatoryStatus;
import com.talentmatch.matching.repository.JobCandidateMatchRepository;
import com.talentmatch.matching.repository.JobMatchRunRepository;
import com.talentmatch.user.entity.User;
import com.talentmatch.workflow.entity.ApplicationStage;
import com.talentmatch.workflow.entity.JobApplication;
import com.talentmatch.workflow.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);
    private static final String ALGORITHM_VERSION = "1.0.0-hybrid";

    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final JobMatchRunRepository matchRunRepository;
    private final JobCandidateMatchRepository candidateMatchRepository;
    private final JobApplicationRepository applicationRepository;
    private final AiServiceClient aiServiceClient;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public MatchingService(JobRepository jobRepository,
                           CandidateRepository candidateRepository,
                           JobMatchRunRepository matchRunRepository,
                           JobCandidateMatchRepository candidateMatchRepository,
                           JobApplicationRepository applicationRepository,
                           AiServiceClient aiServiceClient,
                           AuditLogService auditLogService,
                           ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.matchRunRepository = matchRunRepository;
        this.candidateMatchRepository = candidateMatchRepository;
        this.applicationRepository = applicationRepository;
        this.aiServiceClient = aiServiceClient;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes an explainable, deterministic match run for a job requisition,
     * persists all scores and evidence, and synchronizes candidate workflow applications.
     */
    @Transactional
    public JobMatchRunDto executeMatchRun(Long jobId, User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        int currentRunCount = matchRunRepository.countByJobIdAndOrganizationId(job.getId(), currentUser.getOrganization().getId());
        int nextRunNumber = currentRunCount + 1;

        // Extract mandatory and preferred skills from job
        List<String> mandatorySkills = new ArrayList<>();
        List<String> preferredSkills = new ArrayList<>();
        if (job.getSkills() != null) {
            for (JobSkill skill : job.getSkills()) {
                if (skill.isMandatory()) {
                    mandatorySkills.add(skill.getSkillName());
                } else {
                    preferredSkills.add(skill.getSkillName());
                }
            }
        }

        // Build Job Spec for AI microservice
        AiMatchRequest.AiJobSpec jobSpec = new AiMatchRequest.AiJobSpec(
                job.getId().toString(),
                job.getTitle(),
                job.getDescription(),
                mandatorySkills,
                preferredSkills,
                (double) job.getMinYearsExperience(),
                job.getWeightSkillCoverage() != null ? job.getWeightSkillCoverage().doubleValue() / 100.0 : 0.40,
                job.getWeightExperienceRelevance() != null ? job.getWeightExperienceRelevance().doubleValue() / 100.0 : 0.25,
                job.getWeightProjectRelevance() != null ? job.getWeightProjectRelevance().doubleValue() / 100.0 : 0.15,
                job.getWeightSemanticSimilarity() != null ? job.getWeightSemanticSimilarity().doubleValue() / 100.0 : 0.20,
                job.getShortlistCount(),
                job.getMinScoreThreshold() != null ? job.getMinScoreThreshold().doubleValue() : null
        );

        // Fetch all candidates belonging strictly to this tenant
        List<Candidate> candidates = candidateRepository.findAllByOrganizationId(currentUser.getOrganization().getId());
        Map<Long, Candidate> candidateMap = new HashMap<>();
        List<AiMatchRequest.AiCandidateSpec> candidateSpecs = new ArrayList<>();

        for (Candidate cand : candidates) {
            candidateMap.put(cand.getId(), cand);
            List<String> skills = Collections.emptyList();
            if (cand.getPrimarySkills() != null && !cand.getPrimarySkills().isBlank()) {
                skills = Arrays.stream(cand.getPrimarySkills().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }

            candidateSpecs.add(new AiMatchRequest.AiCandidateSpec(
                    cand.getId().toString(),
                    cand.getFullName(),
                    cand.getSummary(),
                    skills,
                    null,
                    cand.getExperienceHistory(),
                    cand.getProjectHistory(),
                    cand.getRawText()
            ));
        }

        // Request AI semantic retrieval and entity scoring
        AiMatchRequest matchRequest = new AiMatchRequest(jobSpec, candidateSpecs);
        AiMatchResponse aiResponse = aiServiceClient.retrieveMatches(matchRequest);

        // Create persistent JobMatchRun
        JobMatchRun matchRun = new JobMatchRun(
                job,
                currentUser.getOrganization(),
                currentUser,
                nextRunNumber,
                ALGORITHM_VERSION,
                job.getWeightSkillCoverage(),
                job.getWeightExperienceRelevance(),
                job.getWeightProjectRelevance(),
                job.getWeightSemanticSimilarity(),
                job.getMinScoreThreshold(),
                job.getShortlistCount()
        );
        matchRun.setTotalCandidatesEvaluated(candidates.size());

        // Process AI matches and sort deterministically
        List<AiMatchResponse.AiCandidateMatchResult> rawMatches = new ArrayList<>(
                aiResponse.matches() != null ? aiResponse.matches() : Collections.emptyList()
        );

        // Deterministic sorting and tie-breaking:
        // 1. isEligible DESC
        // 2. finalScore DESC
        // 3. skillCoverageScore DESC
        // 4. experienceRelevanceScore DESC
        // 5. candidateId ASC (stable)
        rawMatches.sort((a, b) -> {
            int elig = Boolean.compare(b.isEligible(), a.isEligible());
            if (elig != 0) return elig;

            int score = Double.compare(b.score(), a.score());
            if (score != 0) return score;

            int skill = Double.compare(
                    b.scoreBreakdown() != null ? b.scoreBreakdown().skillCoverage() : 0.0,
                    a.scoreBreakdown() != null ? a.scoreBreakdown().skillCoverage() : 0.0
            );
            if (skill != 0) return skill;

            int exp = Double.compare(
                    b.scoreBreakdown() != null ? b.scoreBreakdown().experienceRelevance() : 0.0,
                    a.scoreBreakdown() != null ? a.scoreBreakdown().experienceRelevance() : 0.0
            );
            if (exp != 0) return exp;

            return a.candidateId().compareTo(b.candidateId());
        });

        int eligibleCount = 0;
        int shortlistedCount = 0;
        int rank = 1;

        for (AiMatchResponse.AiCandidateMatchResult result : rawMatches) {
            Long cId = parseCandidateId(result.candidateId());
            Candidate candidate = candidateMap.get(cId);
            if (candidate == null) continue;

            boolean isEligible = Boolean.TRUE.equals(result.isEligible());
            if (isEligible) {
                eligibleCount++;
            }

            BigDecimal finalScore = toBigDecimal(result.score());
            BigDecimal semanticScore = toBigDecimal(result.scoreBreakdown() != null ? result.scoreBreakdown().semanticSimilarity() : 0.0);
            BigDecimal skillScore = toBigDecimal(result.scoreBreakdown() != null ? result.scoreBreakdown().skillCoverage() : 0.0);
            BigDecimal expScore = toBigDecimal(result.scoreBreakdown() != null ? result.scoreBreakdown().experienceRelevance() : 0.0);
            BigDecimal projScore = toBigDecimal(result.scoreBreakdown() != null ? result.scoreBreakdown().projectRelevance() : 0.0);

            // Mandatory requirement status
            MandatoryStatus mandStatus;
            if (candidate.getRawText() == null || candidate.getRawText().isBlank()) {
                mandStatus = MandatoryStatus.NOT_VERIFIABLE;
            } else if (!isEligible) {
                mandStatus = MandatoryStatus.FAILED;
            } else {
                mandStatus = MandatoryStatus.PASSED;
            }

            // Determine if candidate qualifies for shortlist:
            // Must be within shortlistTarget count AND meet minScoreThreshold (if configured)
            boolean isShortlisted = (rank <= job.getShortlistCount()) &&
                    (job.getMinScoreThreshold() == null || finalScore.compareTo(job.getMinScoreThreshold()) >= 0);

            if (isShortlisted) {
                shortlistedCount++;
            }

            JobCandidateMatch candidateMatch = new JobCandidateMatch(
                    matchRun,
                    candidate,
                    currentUser.getOrganization(),
                    rank,
                    finalScore,
                    semanticScore,
                    skillScore,
                    expScore,
                    projScore,
                    isEligible,
                    mandStatus,
                    isShortlisted,
                    toJson(result.matchedSkills()),
                    toJson(result.missingMandatorySkills()),
                    toJson(result.matchedPreferredSkills()),
                    toJson(result.evidenceSnippets()),
                    toJson(result.uncertaintyIndicators())
            );

            matchRun.addMatch(candidateMatch);
            rank++;
        }

        matchRun.setEligibleCandidatesCount(eligibleCount);
        matchRun.setShortlistedCount(shortlistedCount);

        JobMatchRun savedRun = matchRunRepository.save(matchRun);

        // Synchronize Candidate Workflow Applications with persisted matches
        for (JobCandidateMatch candidateMatch : savedRun.getMatches()) {
            syncJobApplication(job, candidateMatch.getCandidate(), currentUser.getOrganization(), candidateMatch.isShortlisted(), candidateMatch);
        }

        // Audit Trail
        auditLogService.recordEvent(
                currentUser.getOrganization().getId(),
                currentUser.getId(),
                currentUser.getEmail(),
                "JOB_MATCH_RUN_EXECUTED",
                "JobMatchRun",
                String.valueOf(savedRun.getId()),
                String.format("Executed match run #%d for job '%s' (ID %d): %d evaluated, %d eligible, %d shortlisted",
                        savedRun.getRunNumber(), job.getTitle(), job.getId(),
                        savedRun.getTotalCandidatesEvaluated(), savedRun.getEligibleCandidatesCount(),
                        savedRun.getShortlistedCount()),
                "127.0.0.1"
        );

        return mapRunToDto(savedRun, currentUser.getOrganization().getId());
    }

    @Transactional(readOnly = true)
    public JobMatchRunDto getLatestMatchRun(Long jobId, User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        return matchRunRepository.findFirstByJobIdAndOrganizationIdOrderByRunNumberDesc(job.getId(), currentUser.getOrganization().getId())
                .map(run -> mapRunToDto(run, currentUser.getOrganization().getId()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public JobMatchRunDto getMatchRunById(Long jobId, Long runId, User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        JobMatchRun run = matchRunRepository.findByIdAndOrganizationId(runId, currentUser.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Match run " + runId + " not found"));

        if (!run.getJob().getId().equals(job.getId())) {
            throw new BadRequestException("Match run " + runId + " does not belong to job " + jobId);
        }

        return mapRunToDto(run, currentUser.getOrganization().getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobMatchRunDto> getMatchRunHistory(Long jobId, Pageable pageable, User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        Page<JobMatchRun> page = matchRunRepository.findByJobIdAndOrganizationIdOrderByRunNumberDesc(
                job.getId(), currentUser.getOrganization().getId(), pageable
        );

        List<JobMatchRunDto> dtos = page.getContent().stream()
                .map(run -> mapRunToDto(run, currentUser.getOrganization().getId()))
                .toList();

        return new PagedResponse<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CandidateComparisonDto compareCandidates(Long jobId, List<Long> candidateIds, User currentUser) {
        Job job = getJobAndValidateAccess(jobId, currentUser);

        if (candidateIds == null || candidateIds.size() < 2 || candidateIds.size() > 5) {
            throw new BadRequestException("Candidate comparison requires between 2 and 5 candidates");
        }

        // Fetch latest match run
        Optional<JobMatchRun> latestRunOpt = matchRunRepository
                .findFirstByJobIdAndOrganizationIdOrderByRunNumberDesc(job.getId(), currentUser.getOrganization().getId());

        List<CandidateComparisonDto.CandidateComparisonItemDto> comparisonItems = new ArrayList<>();

        for (Long candidateId : candidateIds) {
            Candidate candidate = candidateRepository.findByIdAndOrganizationId(candidateId, currentUser.getOrganization().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate " + candidateId + " not found"));

            Optional<JobApplication> appOpt = applicationRepository.findByJobIdAndCandidateIdAndOrganizationId(
                    job.getId(), candidate.getId(), currentUser.getOrganization().getId()
            );

            JobCandidateMatch match = null;
            if (latestRunOpt.isPresent()) {
                match = candidateMatchRepository.findByMatchRunIdAndCandidateIdAndOrganizationId(
                        latestRunOpt.get().getId(), candidate.getId(), currentUser.getOrganization().getId()
                ).orElse(null);
            }

            comparisonItems.add(new CandidateComparisonDto.CandidateComparisonItemDto(
                    candidate.getId(),
                    candidate.getFullName(),
                    candidate.getEmail(),
                    appOpt.map(JobApplication::getStage).orElse(ApplicationStage.NEW),
                    match != null ? match.getFinalScore() : BigDecimal.ZERO,
                    match != null ? match.getSemanticSimilarityScore() : BigDecimal.ZERO,
                    match != null ? match.getSkillCoverageScore() : BigDecimal.ZERO,
                    match != null ? match.getExperienceRelevanceScore() : BigDecimal.ZERO,
                    match != null ? match.getProjectRelevanceScore() : BigDecimal.ZERO,
                    match != null && match.isEligible(),
                    match != null ? match.getMandatoryStatus() : MandatoryStatus.NOT_VERIFIABLE,
                    match != null ? fromJsonList(match.getMatchedSkills()) : Collections.emptyList(),
                    match != null ? fromJsonList(match.getMissingMandatorySkills()) : Collections.emptyList(),
                    match != null ? fromJsonList(match.getMatchedPreferredSkills()) : Collections.emptyList(),
                    match != null ? fromJsonMap(match.getEvidenceSnippets()) : Collections.emptyMap(),
                    match != null ? fromJsonList(match.getWarnings()) : Collections.emptyList()
            ));
        }

        return new CandidateComparisonDto(job.getId(), job.getTitle(), comparisonItems);
    }

    private void syncJobApplication(Job job,
                                    Candidate candidate,
                                    com.talentmatch.organization.entity.Organization org,
                                    boolean isShortlisted,
                                    JobCandidateMatch candidateMatch) {
        Optional<JobApplication> existingApp = applicationRepository
                .findByJobIdAndCandidateIdAndOrganizationId(job.getId(), candidate.getId(), org.getId());

        JobApplication application;
        if (existingApp.isPresent()) {
            application = existingApp.get();
            application.setLatestMatch(candidateMatch);
            // If candidate was in stage NEW and now got shortlisted, update stage
            if (isShortlisted && application.getStage() == ApplicationStage.NEW) {
                application.setStage(ApplicationStage.SHORTLISTED);
                application.setShortlisted(true);
            }
        } else {
            ApplicationStage initialStage = isShortlisted ? ApplicationStage.SHORTLISTED : ApplicationStage.NEW;
            application = new JobApplication(job, candidate, org, initialStage);
            application.setLatestMatch(candidateMatch);
            application.setShortlisted(isShortlisted);
        }

        applicationRepository.save(application);
    }

    private Job getJobAndValidateAccess(Long jobId, User currentUser) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job requisition " + jobId + " not found"));

        if (!job.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            log.warn("Access denied for user {} to job {} in other tenant", currentUser.getEmail(), jobId);
            throw new AccessDeniedException("Access denied: You do not have permission to access this job requisition");
        }
        return job;
    }

    private JobMatchRunDto mapRunToDto(JobMatchRun run, Long orgId) {
        List<JobCandidateMatch> matches = candidateMatchRepository
                .findByMatchRunIdAndOrganizationIdOrderByRankPositionAsc(run.getId(), orgId);

        List<JobCandidateMatchDto> matchDtos = new ArrayList<>();
        for (JobCandidateMatch m : matches) {
            Optional<JobApplication> appOpt = applicationRepository.findByJobIdAndCandidateIdAndOrganizationId(
                    run.getJob().getId(), m.getCandidate().getId(), orgId
            );

            matchDtos.add(new JobCandidateMatchDto(
                    m.getId(),
                    m.getCandidate().getId(),
                    m.getCandidate().getFullName(),
                    m.getCandidate().getEmail(),
                    m.getRankPosition(),
                    m.getFinalScore(),
                    m.getSemanticSimilarityScore(),
                    m.getSkillCoverageScore(),
                    m.getExperienceRelevanceScore(),
                    m.getProjectRelevanceScore(),
                    m.isEligible(),
                    m.getMandatoryStatus(),
                    m.isShortlisted(),
                    fromJsonList(m.getMatchedSkills()),
                    fromJsonList(m.getMissingMandatorySkills()),
                    fromJsonList(m.getMatchedPreferredSkills()),
                    fromJsonMap(m.getEvidenceSnippets()),
                    fromJsonList(m.getWarnings()),
                    appOpt.map(JobApplication::getStage).orElse(ApplicationStage.NEW),
                    appOpt.map(JobApplication::getId).orElse(null),
                    m.getCreatedAt()
            ));
        }

        return new JobMatchRunDto(
                run.getId(),
                run.getJob().getId(),
                run.getJob().getTitle(),
                run.getRunNumber(),
                run.getAlgorithmVersion(),
                run.getWeightSkillCoverage(),
                run.getWeightExperienceRelevance(),
                run.getWeightProjectRelevance(),
                run.getWeightSemanticSimilarity(),
                run.getMinScoreThreshold(),
                run.getShortlistTarget(),
                run.getTotalCandidatesEvaluated(),
                run.getEligibleCandidatesCount(),
                run.getShortlistedCount(),
                run.getCreatedAt(),
                matchDtos
        );
    }

    private Long parseCandidateId(String idStr) {
        try {
            return Long.parseLong(idStr);
        } catch (Exception e) {
            return 0L;
        }
    }

    private BigDecimal toBigDecimal(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, String> fromJsonMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
