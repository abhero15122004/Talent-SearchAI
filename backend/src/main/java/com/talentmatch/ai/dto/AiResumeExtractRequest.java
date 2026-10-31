package com.talentmatch.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiResumeExtractRequest(
        @JsonProperty("raw_text") String rawText,
        @JsonProperty("candidate_id") String candidateId
) {}
