package com.talentmatch.ai.service;

import com.talentmatch.ai.dto.AiMatchRequest;
import com.talentmatch.ai.dto.AiMatchResponse;
import com.talentmatch.ai.dto.AiResumeExtractResponse;

public interface AiServiceClient {

    /**
     * Calls the AI service to extract structured candidate details, skills,
     * work experience, and provenance metadata from raw resume text.
     */
    AiResumeExtractResponse extractResume(String rawText, String candidateId);

    /**
     * Calls the AI service to perform multi-factor semantic retrieval,
     * mandatory requirement filtering, and candidate ranking.
     */
    AiMatchResponse retrieveMatches(AiMatchRequest matchRequest);

    /**
     * Checks if the AI service is reachable and reports UP status.
     */
    boolean isHealthy();
}
