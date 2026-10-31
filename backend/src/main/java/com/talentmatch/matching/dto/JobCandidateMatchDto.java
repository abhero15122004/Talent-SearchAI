package com.talentmatch.matching.dto;

import com.talentmatch.matching.entity.MandatoryStatus;
import com.talentmatch.workflow.entity.ApplicationStage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record JobCandidateMatchDto(
        Long id,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        int rankPosition,
        BigDecimal finalScore,
        BigDecimal semanticSimilarityScore,
        BigDecimal skillCoverageScore,
        BigDecimal experienceRelevanceScore,
        BigDecimal projectRelevanceScore,
        boolean eligible,
        MandatoryStatus mandatoryStatus,
        boolean shortlisted,
        List<String> matchedSkills,
        List<String> missingMandatorySkills,
        List<String> matchedPreferredSkills,
        Map<String, String> evidenceSnippets,
        List<String> warnings,
        ApplicationStage currentStage,
        Long applicationId,
        Instant createdAt
) {}
