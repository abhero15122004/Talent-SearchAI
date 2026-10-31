package com.talentmatch.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiResumeExtractResponse(
        @JsonProperty("candidate_id") String candidateId,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("email") String email,
        @JsonProperty("phone") String phone,
        @JsonProperty("summary") String summary,
        @JsonProperty("skills") List<AiSkillExtraction> skills,
        @JsonProperty("work_experience") List<AiExperienceItem> workExperience,
        @JsonProperty("education") List<AiEducationItem> education,
        @JsonProperty("projects") List<AiProjectItem> projects,
        @JsonProperty("total_years_experience") Double totalYearsExperience,
        @JsonProperty("provenance") AiProvenanceMetadata provenance,
        @JsonProperty("uncertainty_indicators") List<String> uncertaintyIndicators
) {
    public record AiSkillExtraction(
            @JsonProperty("canonical_id") String canonicalId,
            @JsonProperty("canonical_name") String canonicalName,
            @JsonProperty("category") String category,
            @JsonProperty("raw_text") String rawText,
            @JsonProperty("confidence") Double confidence,
            @JsonProperty("evidence_snippet") String evidenceSnippet
    ) {}

    public record AiExperienceItem(
            @JsonProperty("title") String title,
            @JsonProperty("company") String company,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate,
            @JsonProperty("years") Double years,
            @JsonProperty("description") String description,
            @JsonProperty("skills_mentioned") List<String> skillsMentioned
    ) {}

    public record AiEducationItem(
            @JsonProperty("degree") String degree,
            @JsonProperty("institution") String institution,
            @JsonProperty("field_of_study") String fieldOfStudy,
            @JsonProperty("graduation_year") Integer graduationYear
    ) {}

    public record AiProjectItem(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("technologies") List<String> technologies
    ) {}

    public record AiProvenanceMetadata(
            @JsonProperty("extractor_version") String extractorVersion,
            @JsonProperty("taxonomy_version") String taxonomyVersion,
            @JsonProperty("spacy_model") String spacyModel,
            @JsonProperty("processed_at") String processedAt
    ) {}
}
