package com.talentmatch.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record AiMatchResponse(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("total_candidates_evaluated") Integer totalCandidatesEvaluated,
        @JsonProperty("eligible_candidates_count") Integer eligibleCandidatesCount,
        @JsonProperty("shortlisted_count") Integer shortlistedCount,
        @JsonProperty("matches") List<AiCandidateMatchResult> matches,
        @JsonProperty("model_metadata") Map<String, String> modelMetadata
) {
    public record AiCandidateMatchResult(
            @JsonProperty("candidate_id") String candidateId,
            @JsonProperty("candidate_name") String candidateName,
            @JsonProperty("score") Double score,
            @JsonProperty("score_breakdown") AiScoreBreakdown scoreBreakdown,
            @JsonProperty("is_eligible") Boolean isEligible,
            @JsonProperty("matched_skills") List<String> matchedSkills,
            @JsonProperty("missing_mandatory_skills") List<String> missingMandatorySkills,
            @JsonProperty("matched_preferred_skills") List<String> matchedPreferredSkills,
            @JsonProperty("evidence_snippets") Map<String, String> evidenceSnippets,
            @JsonProperty("uncertainty_indicators") List<String> uncertaintyIndicators
    ) {}

    public record AiScoreBreakdown(
            @JsonProperty("semantic_similarity") Double semanticSimilarity,
            @JsonProperty("skill_coverage") Double skillCoverage,
            @JsonProperty("experience_relevance") Double experienceRelevance,
            @JsonProperty("project_relevance") Double projectRelevance,
            @JsonProperty("final_score") Double finalScore
    ) {}
}
