package com.talentmatch.ai.service;

import com.talentmatch.ai.dto.AiMatchRequest;
import com.talentmatch.ai.dto.AiMatchResponse;
import com.talentmatch.ai.dto.AiResumeExtractRequest;
import com.talentmatch.ai.dto.AiResumeExtractResponse;
import com.talentmatch.config.AiServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Service
public class AiServiceClientImpl implements AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClientImpl.class);

    private final RestClient restClient;
    private final AiServiceProperties properties;

    public AiServiceClientImpl(AiServiceProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Internal-Service-Key", properties.getApiKey())
                .build();
    }

    @Override
    public AiResumeExtractResponse extractResume(String rawText, String candidateId) {
        if (!properties.isEnabled()) {
            log.warn("AI service is disabled by configuration; skipping structured NLP extraction");
            return createFallbackExtraction(candidateId);
        }

        try {
            log.info("Dispatching resume text (length: {}) to AI service for candidate {}",
                    rawText != null ? rawText.length() : 0, candidateId);

            AiResumeExtractRequest request = new AiResumeExtractRequest(rawText, candidateId);

            return restClient.post()
                    .uri("/internal/v1/resumes/extract")
                    .body(request)
                    .retrieve()
                    .body(AiResumeExtractResponse.class);

        } catch (Exception e) {
            log.warn("Failed to communicate with AI extraction service for candidate {}: {}. Proceeding with fallback.",
                    candidateId, e.getMessage());
            return createFallbackExtraction(candidateId);
        }
    }

    @Override
    public AiMatchResponse retrieveMatches(AiMatchRequest matchRequest) {
        if (!properties.isEnabled()) {
            log.warn("AI service is disabled by configuration; executing deterministic heuristic matching fallback");
            return executeFallbackMatching(matchRequest);
        }

        try {
            log.info("Requesting candidate semantic retrieval for job {} against {} candidates",
                    matchRequest.job().id(), matchRequest.candidates().size());

            return restClient.post()
                    .uri("/internal/v1/matches/retrieve")
                    .body(matchRequest)
                    .retrieve()
                    .body(AiMatchResponse.class);

        } catch (Exception e) {
            log.warn("AI matching service communication failed for job {}: {}. Executing deterministic heuristic matching fallback.",
                    matchRequest.job().id(), e.getMessage());
            return executeFallbackMatching(matchRequest);
        }
    }

    @Override
    public boolean isHealthy() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/internal/v1/health")
                    .retrieve()
                    .body(Map.class);
            return response != null && "UP".equalsIgnoreCase(String.valueOf(response.get("status")));
        } catch (Exception e) {
            log.debug("AI service health probe failed: {}", e.getMessage());
            return false;
        }
    }

    private AiResumeExtractResponse createFallbackExtraction(String candidateId) {
        return new AiResumeExtractResponse(
                candidateId,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                new AiResumeExtractResponse.AiProvenanceMetadata("fallback", "none", "none", null),
                Collections.singletonList("Extraction fell back to basic heuristics; AI service was unavailable or disabled")
        );
    }

    private AiMatchResponse createFallbackExtractionResponse(String jobId) {
        return createEmptyMatchResponse(jobId);
    }

    private AiMatchResponse executeFallbackMatching(AiMatchRequest matchRequest) {
        AiMatchRequest.AiJobSpec job = matchRequest.job();
        List<AiMatchRequest.AiCandidateSpec> candidates = matchRequest.candidates();

        List<String> mandatory = job.mandatorySkills() != null ? job.mandatorySkills() : Collections.emptyList();
        List<String> preferred = job.preferredSkills() != null ? job.preferredSkills() : Collections.emptyList();

        double wSkill = job.weightSkillCoverage() != null ? job.weightSkillCoverage() : 0.40;
        double wExp = job.weightExperience() != null ? job.weightExperience() : 0.25;
        double wProj = job.weightProjectRelevance() != null ? job.weightProjectRelevance() : 0.15;
        double wSem = job.weightSemanticSimilarity() != null ? job.weightSemanticSimilarity() : 0.20;

        List<AiMatchResponse.AiCandidateMatchResult> matchResults = new ArrayList<>();
        int eligibleCount = 0;

        for (AiMatchRequest.AiCandidateSpec cand : candidates) {
            Set<String> candidateSkills = new HashSet<>();
            if (cand.skills() != null) {
                for (String s : cand.skills()) {
                    candidateSkills.add(s.toLowerCase().trim());
                }
            }

            String fullText = ((cand.rawText() != null ? cand.rawText() : "") + " " +
                    (cand.experienceText() != null ? cand.experienceText() : "") + " " +
                    (cand.summary() != null ? cand.summary() : "")).toLowerCase();

            List<String> matchedSkills = new ArrayList<>();
            List<String> missingMandatory = new ArrayList<>();
            List<String> matchedPreferred = new ArrayList<>();
            Map<String, String> evidence = new HashMap<>();

            for (String m : mandatory) {
                String mLower = m.toLowerCase().trim();
                if (candidateSkills.contains(mLower) || fullText.contains(mLower)) {
                    matchedSkills.add(m);
                    evidence.put(m, "Identified from candidate skills or experience profile");
                } else {
                    missingMandatory.add(m);
                }
            }

            for (String p : preferred) {
                String pLower = p.toLowerCase().trim();
                if (candidateSkills.contains(pLower) || fullText.contains(pLower)) {
                    matchedSkills.add(p);
                    matchedPreferred.add(p);
                    evidence.put(p, "Identified from candidate skills or experience profile");
                }
            }

            boolean isEligible = missingMandatory.isEmpty();
            if (isEligible) {
                eligibleCount++;
            }

            // Skill coverage calculation
            double totalRequirements = (mandatory.size() * 1.5) + (preferred.size() * 1.0);
            double satisfiedRequirements = ((matchedSkills.size() - matchedPreferred.size()) * 1.5) + (matchedPreferred.size() * 1.0);
            double skillCoverage = totalRequirements > 0
                    ? Math.min(100.0, (satisfiedRequirements / totalRequirements) * 100.0)
                    : 100.0;

            // Experience relevance calculation
            double expScore = 70.0;
            if (cand.experienceText() != null && !cand.experienceText().isBlank()) {
                expScore = isEligible ? 85.0 : 50.0;
            }

            // Project relevance calculation
            double projScore = (cand.projectsText() != null && !cand.projectsText().isBlank()) ? 80.0 : 50.0;

            // Semantic similarity fallback baseline
            double semScore = 60.0;

            // Final score with configurable weights
            double finalScore = (skillCoverage * wSkill) + (expScore * wExp) + (projScore * wProj) + (semScore * wSem);
            if (!isEligible) {
                finalScore = finalScore * 0.60; // Gating penalty for ineligible candidates
            }
            finalScore = Math.round(finalScore * 100.0) / 100.0;

            AiMatchResponse.AiScoreBreakdown breakdown = new AiMatchResponse.AiScoreBreakdown(
                    semScore, skillCoverage, expScore, projScore, finalScore
            );

            List<String> warnings = new ArrayList<>();
            warnings.add("Evaluated via deterministic heuristic fallback; Python AI service (port 8000) was offline");
            if (!isEligible) {
                warnings.add("Missing mandatory requirement(s): " + String.join(", ", missingMandatory));
            }

            matchResults.add(new AiMatchResponse.AiCandidateMatchResult(
                    cand.id(),
                    cand.name(),
                    finalScore,
                    breakdown,
                    isEligible,
                    matchedSkills,
                    missingMandatory,
                    matchedPreferred,
                    evidence,
                    warnings
            ));
        }

        // Sort descending by final score
        matchResults.sort((a, b) -> Double.compare(b.score(), a.score()));

        int shortlistTarget = job.shortlistCount() != null ? job.shortlistCount() : 10;
        int shortlistedCount = Math.min(shortlistTarget, (int) matchResults.stream().filter(AiMatchResponse.AiCandidateMatchResult::isEligible).count());

        return new AiMatchResponse(
                job.id(),
                candidates.size(),
                eligibleCount,
                shortlistedCount,
                matchResults,
                Map.of("model", "heuristic-fallback-v1", "warning", "Python AI service offline; heuristic fallback applied")
        );
    }

    private AiMatchResponse createEmptyMatchResponse(String jobId) {
        return new AiMatchResponse(
                jobId,
                0,
                0,
                0,
                Collections.emptyList(),
                Map.of("status", "service_disabled")
        );
    }
}
