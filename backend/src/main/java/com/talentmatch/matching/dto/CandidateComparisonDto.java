package com.talentmatch.matching.dto;

import com.talentmatch.matching.entity.MandatoryStatus;
import com.talentmatch.workflow.entity.ApplicationStage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CandidateComparisonDto(
        Long jobId,
        String jobTitle,
        List<CandidateComparisonItemDto> candidates
) {
    public record CandidateComparisonItemDto(
            Long candidateId,
            String candidateName,
            String candidateEmail,
            ApplicationStage stage,
            BigDecimal finalScore,
            BigDecimal semanticSimilarityScore,
            BigDecimal skillCoverageScore,
            BigDecimal experienceRelevanceScore,
            BigDecimal projectRelevanceScore,
            boolean eligible,
            MandatoryStatus mandatoryStatus,
            List<String> matchedSkills,
            List<String> missingMandatorySkills,
            List<String> matchedPreferredSkills,
            Map<String, String> evidenceSnippets,
            List<String> warnings
    ) {}
}
