package com.talentmatch.ai;

import com.talentmatch.ai.dto.AiResumeExtractResponse;
import com.talentmatch.ai.service.AiServiceClientImpl;
import com.talentmatch.config.AiServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceClientTest {

    private AiServiceProperties properties;
    private AiServiceClientImpl client;

    @BeforeEach
    void setUp() {
        properties = new AiServiceProperties();
        properties.setUrl("http://127.0.0.1:19999"); // unreachable mock port
        properties.setTimeoutMs(500);
        properties.setEnabled(true);
        client = new AiServiceClientImpl(properties);
    }

    @Test
    void testExtractResumeFallbackWhenDisabled() {
        properties.setEnabled(false);
        AiResumeExtractResponse response = client.extractResume("Sample resume text", "cand-123");

        assertNotNull(response);
        assertEquals("cand-123", response.candidateId());
        assertTrue(response.skills().isEmpty());
        assertTrue(response.uncertaintyIndicators().stream().anyMatch(u -> u.contains("disabled") || u.contains("fallback")));
    }

    @Test
    void testExtractResumeFallbackWhenServiceUnavailable() {
        // Since http://127.0.0.1:19999 is down, it should catch the connection exception gracefully
        AiResumeExtractResponse response = client.extractResume("Sample resume text", "cand-456");

        assertNotNull(response);
        assertEquals("cand-456", response.candidateId());
        assertTrue(response.skills().isEmpty());
        assertTrue(response.uncertaintyIndicators().stream().anyMatch(u -> u.contains("fallback") || u.contains("unavailable")));
    }

    @Test
    void testRetrieveMatchesFallbackWhenServiceUnavailable() {
        com.talentmatch.ai.dto.AiMatchRequest.AiJobSpec jobSpec = new com.talentmatch.ai.dto.AiMatchRequest.AiJobSpec(
                "job-1", "Java Developer", "Description",
                java.util.List.of("Java"), java.util.List.of("Docker"),
                3.0, 0.4, 0.25, 0.15, 0.20, 5, 60.0
        );
        com.talentmatch.ai.dto.AiMatchRequest.AiCandidateSpec candSpec = new com.talentmatch.ai.dto.AiMatchRequest.AiCandidateSpec(
                "cand-1", "Alice Dev", "Summary",
                java.util.List.of("Java", "Docker"), null, "Experience", "Projects", "Raw resume text with Java and Docker"
        );
        com.talentmatch.ai.dto.AiMatchRequest req = new com.talentmatch.ai.dto.AiMatchRequest(jobSpec, java.util.List.of(candSpec));

        com.talentmatch.ai.dto.AiMatchResponse resp = client.retrieveMatches(req);
        assertNotNull(resp);
        assertEquals(1, resp.totalCandidatesEvaluated());
        assertEquals(1, resp.eligibleCandidatesCount());
        assertFalse(resp.matches().isEmpty());
        assertTrue(resp.matches().get(0).isEligible());
        assertTrue(resp.matches().get(0).uncertaintyIndicators().stream().anyMatch(u -> u.contains("fallback")));
    }

    @Test
    void testIsHealthyReturnsFalseWhenDown() {
        assertFalse(client.isHealthy());
    }

    @Test
    void testIsHealthyReturnsFalseWhenDisabled() {
        properties.setEnabled(false);
        assertFalse(client.isHealthy());
    }
}
