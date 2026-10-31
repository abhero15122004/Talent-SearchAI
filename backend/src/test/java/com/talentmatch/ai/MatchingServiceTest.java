package com.talentmatch.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmatch.ai.dto.AiMatchRequest;
import com.talentmatch.ai.dto.AiMatchResponse;
import com.talentmatch.ai.service.AiServiceClient;
import com.talentmatch.ai.service.MatchingService;
import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.candidate.repository.CandidateRepository;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.job.entity.Job;
import com.talentmatch.job.entity.JobSkill;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.matching.dto.CandidateComparisonDto;
import com.talentmatch.matching.dto.JobMatchRunDto;
import com.talentmatch.matching.entity.JobCandidateMatch;
import com.talentmatch.matching.entity.JobMatchRun;
import com.talentmatch.matching.entity.MandatoryStatus;
import com.talentmatch.matching.repository.JobCandidateMatchRepository;
import com.talentmatch.matching.repository.JobMatchRunRepository;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.workflow.entity.ApplicationStage;
import com.talentmatch.workflow.entity.JobApplication;
import com.talentmatch.workflow.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private JobMatchRunRepository matchRunRepository;

    @Mock
    private JobCandidateMatchRepository candidateMatchRepository;

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MatchingService matchingService;

    private Organization org1;
    private Organization org2;
    private User recruiter1;
    private Job job1;

    @BeforeEach
    void setUp() {
        org1 = new Organization("Acme Corp", "acme-corp", "acme.com");
        setEntityId(org1, 1L);

        org2 = new Organization("Other Corp", "other-corp", "other.com");
        setEntityId(org2, 2L);

        recruiter1 = new User("recruiter@acme.com", "hash", "Jane Recruiter", Role.RECRUITER, org1);
        setEntityId(recruiter1, 10L);

        job1 = new Job();
        setEntityId(job1, 100L);
        job1.setOrganization(org1);
        job1.setTitle("Senior Java Backend Engineer");
        job1.setDescription("Build microservices with Spring Boot");
        job1.setMinYearsExperience(5);
        job1.setShortlistCount(2); // Shortlist target of 2
        job1.setMinScoreThreshold(new BigDecimal("70.00"));
        job1.setWeightSkillCoverage(new BigDecimal("40.00"));
        job1.setWeightExperienceRelevance(new BigDecimal("25.00"));
        job1.setWeightProjectRelevance(new BigDecimal("15.00"));
        job1.setWeightSemanticSimilarity(new BigDecimal("20.00"));

        JobSkill skill = new JobSkill("Java", true, 5);
        job1.setSkills(List.of(skill));
    }

    private void setEntityId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testTenantIsolationRejectsCrossOrganizationMatching() {
        job1.setOrganization(org2);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job1));

        assertThrows(AccessDeniedException.class, () ->
                matchingService.executeMatchRun(100L, recruiter1));

        verifyNoInteractions(candidateRepository);
        verifyNoInteractions(aiServiceClient);
    }

    @Test
    void testJobNotFoundThrowsResourceNotFoundException() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                matchingService.executeMatchRun(999L, recruiter1));
    }

    @Test
    void testExecuteMatchRunDeterministicRankingAndPersistence() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job1));
        when(matchRunRepository.countByJobIdAndOrganizationId(100L, 1L)).thenReturn(0);

        Candidate c1 = new Candidate(org1, "Alice Senior", "alice@example.com", "555-1111",
                "Java Spring Boot developer with 6 years experience", "Summary", "Java, Spring Boot", "BS CS", "6 years", "Proj", "alice.pdf");
        setEntityId(c1, 501L);

        Candidate c2 = new Candidate(org1, "Bob Junior", "bob@example.com", "555-2222",
                "Java developer with 2 years experience", "Summary", "Java", "BS CS", "2 years", "Proj", "bob.pdf");
        setEntityId(c2, 502L);

        when(candidateRepository.findAllByOrganizationId(1L)).thenReturn(List.of(c1, c2));

        // Mock AI service responses
        AiMatchResponse.AiScoreBreakdown b1 = new AiMatchResponse.AiScoreBreakdown(85.0, 95.0, 100.0, 80.0, 90.0);
        AiMatchResponse.AiCandidateMatchResult r1 = new AiMatchResponse.AiCandidateMatchResult(
                "501", "Alice Senior", 90.0, b1, true, List.of("Java", "Spring Boot"), Collections.emptyList(),
                Collections.emptyList(), Map.of("Java", "6 years building Spring Boot"), Collections.emptyList()
        );

        AiMatchResponse.AiScoreBreakdown b2 = new AiMatchResponse.AiScoreBreakdown(70.0, 75.0, 40.0, 60.0, 65.0);
        AiMatchResponse.AiCandidateMatchResult r2 = new AiMatchResponse.AiCandidateMatchResult(
                "502", "Bob Junior", 65.0, b2, true, List.of("Java"), Collections.emptyList(),
                Collections.emptyList(), Map.of("Java", "2 years building REST"), Collections.emptyList()
        );

        AiMatchResponse aiResponse = new AiMatchResponse("100", 2, 2, 2, List.of(r1, r2), Map.of("model", "all-MiniLM-L6-v2"));
        when(aiServiceClient.retrieveMatches(any(AiMatchRequest.class))).thenReturn(aiResponse);

        // Mock save on run
        when(matchRunRepository.save(any(JobMatchRun.class))).thenAnswer(inv -> {
            JobMatchRun run = inv.getArgument(0);
            setEntityId(run, 1L);
            return run;
        });

        // Mock match repository findByMatchRunId
        when(candidateMatchRepository.findByMatchRunIdAndOrganizationIdOrderByRankPositionAsc(eq(1L), eq(1L)))
                .thenAnswer(inv -> {
                    JobCandidateMatch m1 = new JobCandidateMatch();
                    setEntityId(m1, 1001L);
                    m1.setCandidate(c1);
                    m1.setRankPosition(1);
                    m1.setFinalScore(new BigDecimal("90.00"));
                    m1.setSemanticSimilarityScore(new BigDecimal("85.00"));
                    m1.setSkillCoverageScore(new BigDecimal("95.00"));
                    m1.setExperienceRelevanceScore(new BigDecimal("100.00"));
                    m1.setProjectRelevanceScore(new BigDecimal("80.00"));
                    m1.setEligible(true);
                    m1.setMandatoryStatus(MandatoryStatus.PASSED);
                    m1.setShortlisted(true);

                    JobCandidateMatch m2 = new JobCandidateMatch();
                    setEntityId(m2, 1002L);
                    m2.setCandidate(c2);
                    m2.setRankPosition(2);
                    m2.setFinalScore(new BigDecimal("65.00"));
                    m2.setSemanticSimilarityScore(new BigDecimal("70.00"));
                    m2.setSkillCoverageScore(new BigDecimal("75.00"));
                    m2.setExperienceRelevanceScore(new BigDecimal("40.00"));
                    m2.setProjectRelevanceScore(new BigDecimal("60.00"));
                    m2.setEligible(true);
                    m2.setMandatoryStatus(MandatoryStatus.PASSED);
                    m2.setShortlisted(false); // below threshold of 70.00

                    return List.of(m1, m2);
                });

        JobMatchRunDto result = matchingService.executeMatchRun(100L, recruiter1);

        assertNotNull(result);
        assertEquals(1, result.runNumber());
        assertEquals(2, result.totalCandidatesEvaluated());
        assertEquals(2, result.eligibleCandidatesCount());
        assertEquals(1, result.shortlistedCount()); // Only Alice meets >= 70.00 threshold

        // Verify matches deterministic order
        assertEquals(2, result.matches().size());
        assertEquals(501L, result.matches().get(0).candidateId());
        assertTrue(result.matches().get(0).shortlisted());
        assertEquals(502L, result.matches().get(1).candidateId());
        assertFalse(result.matches().get(1).shortlisted());

        verify(applicationRepository, times(2)).save(any(JobApplication.class));
        verify(auditLogService).recordEvent(eq(1L), eq(10L), eq("recruiter@acme.com"), eq("JOB_MATCH_RUN_EXECUTED"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testCompareCandidates() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job1));

        Candidate c1 = new Candidate(org1, "Alice Senior", "alice@example.com", "555-1111", "Text", "Summary", "Java", "BS", "6 yrs", "Proj", "a.pdf");
        setEntityId(c1, 501L);
        Candidate c2 = new Candidate(org1, "Bob Junior", "bob@example.com", "555-2222", "Text", "Summary", "Java", "BS", "2 yrs", "Proj", "b.pdf");
        setEntityId(c2, 502L);

        when(candidateRepository.findByIdAndOrganizationId(501L, 1L)).thenReturn(Optional.of(c1));
        when(candidateRepository.findByIdAndOrganizationId(502L, 1L)).thenReturn(Optional.of(c2));

        JobMatchRun latestRun = new JobMatchRun();
        setEntityId(latestRun, 99L);
        when(matchRunRepository.findFirstByJobIdAndOrganizationIdOrderByRunNumberDesc(100L, 1L))
                .thenReturn(Optional.of(latestRun));

        JobCandidateMatch m1 = new JobCandidateMatch();
        m1.setFinalScore(new BigDecimal("92.00"));
        m1.setSkillCoverageScore(new BigDecimal("95.00"));
        m1.setExperienceRelevanceScore(new BigDecimal("100.00"));
        m1.setSemanticSimilarityScore(new BigDecimal("88.00"));
        m1.setProjectRelevanceScore(new BigDecimal("85.00"));
        m1.setEligible(true);
        m1.setMandatoryStatus(MandatoryStatus.PASSED);

        when(candidateMatchRepository.findByMatchRunIdAndCandidateIdAndOrganizationId(99L, 501L, 1L))
                .thenReturn(Optional.of(m1));

        CandidateComparisonDto comparison = matchingService.compareCandidates(100L, List.of(501L, 502L), recruiter1);

        assertNotNull(comparison);
        assertEquals(100L, comparison.jobId());
        assertEquals(2, comparison.candidates().size());
        assertEquals("Alice Senior", comparison.candidates().get(0).candidateName());
        assertEquals(new BigDecimal("92.00"), comparison.candidates().get(0).finalScore());
    }
}
