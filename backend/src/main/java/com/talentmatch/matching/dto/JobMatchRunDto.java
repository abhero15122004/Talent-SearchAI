package com.talentmatch.matching.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobMatchRunDto(
        Long id,
        Long jobId,
        String jobTitle,
        int runNumber,
        String algorithmVersion,
        BigDecimal weightSkillCoverage,
        BigDecimal weightExperienceRelevance,
        BigDecimal weightProjectRelevance,
        BigDecimal weightSemanticSimilarity,
        BigDecimal minScoreThreshold,
        int shortlistTarget,
        int totalCandidatesEvaluated,
        int eligibleCandidatesCount,
        int shortlistedCount,
        Instant createdAt,
        List<JobCandidateMatchDto> matches
) {}
