package com.talentmatch.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiMatchRequest(
        @JsonProperty("job") AiJobSpec job,
        @JsonProperty("candidates") List<AiCandidateSpec> candidates
) {
    public record AiJobSpec(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("mandatory_skills") List<String> mandatorySkills,
            @JsonProperty("preferred_skills") List<String> preferredSkills,
            @JsonProperty("min_experience_years") Double minExperienceYears,
            @JsonProperty("weight_skill_coverage") Double weightSkillCoverage,
            @JsonProperty("weight_experience") Double weightExperience,
            @JsonProperty("weight_project_relevance") Double weightProjectRelevance,
            @JsonProperty("weight_semantic_similarity") Double weightSemanticSimilarity,
            @JsonProperty("shortlist_count") Integer shortlistCount,
            @JsonProperty("min_score_threshold") Double minScoreThreshold
    ) {}

    public record AiCandidateSpec(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("summary") String summary,
            @JsonProperty("skills") List<String> skills,
            @JsonProperty("total_experience_years") Double totalExperienceYears,
            @JsonProperty("experience_text") String experienceText,
            @JsonProperty("projects_text") String projectsText,
            @JsonProperty("raw_text") String rawText
    ) {}
}
