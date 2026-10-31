package com.talentmatch.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmatch.ai.dto.AiMatchRequest;
import com.talentmatch.ai.dto.AiMatchResponse;
import com.talentmatch.ai.service.AiServiceClient;
import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.candidate.repository.CandidateRepository;
import com.talentmatch.job.entity.EmploymentType;
import com.talentmatch.job.entity.Job;
import com.talentmatch.job.entity.JobSkill;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.matching.dto.CompareCandidatesRequest;
import com.talentmatch.matching.repository.JobCandidateMatchRepository;
import com.talentmatch.matching.repository.JobMatchRunRepository;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.security.JwtTokenProvider;
import com.talentmatch.security.UserPrincipal;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import com.talentmatch.workflow.dto.AddNoteRequest;
import com.talentmatch.workflow.dto.UpdateStageRequest;
import com.talentmatch.workflow.entity.ApplicationStage;
import com.talentmatch.workflow.entity.JobApplication;
import com.talentmatch.workflow.repository.JobApplicationNoteRepository;
import com.talentmatch.workflow.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecruitmentWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobMatchRunRepository matchRunRepository;

    @Autowired
    private JobCandidateMatchRepository candidateMatchRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private JobApplicationNoteRepository noteRepository;

    @Autowired
    private com.talentmatch.resume.repository.ResumeFileRepository resumeFileRepository;

    @Autowired
    private com.talentmatch.resume.repository.ResumeBatchRepository resumeBatchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AiServiceClient aiServiceClient;

    private Organization orgA;
    private Organization orgB;
    private User recruiterA;
    private User recruiterB;
    private String tokenA;
    private String tokenB;
    private Job jobA;
    private Candidate candA1;
    private Candidate candA2;

    @BeforeEach
    void setUp() {
        resumeFileRepository.deleteAll();
        resumeBatchRepository.deleteAll();
        noteRepository.deleteAll();
        applicationRepository.deleteAll();
        candidateMatchRepository.deleteAll();
        matchRunRepository.deleteAll();
        jobRepository.deleteAll();
        candidateRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Setup Org A
        orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp", "alpha.com"));
        recruiterA = userRepository.save(new User("recruiterA@alpha.com", passwordEncoder.encode("Pass123!"), "Alice Recruiter", Role.RECRUITER, orgA));
        tokenA = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(recruiterA));

        // 2. Setup Org B
        orgB = organizationRepository.save(new Organization("Beta Corp", "beta-corp", "beta.com"));
        recruiterB = userRepository.save(new User("recruiterB@beta.com", passwordEncoder.encode("Pass123!"), "Bob Recruiter", Role.RECRUITER, orgB));
        tokenB = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(recruiterB));

        // 3. Create Job in Org A
        jobA = new Job();
        jobA.setOrganization(orgA);
        jobA.setCreatedByUser(recruiterA);
        jobA.setTitle("Senior Full-Stack Engineer");
        jobA.setDepartment("Engineering");
        jobA.setLocation("New York, NY");
        jobA.setEmploymentType(EmploymentType.FULL_TIME);
        jobA.setDescription("Build enterprise services with Spring Boot and React");
        jobA.setMinYearsExperience(4);
        jobA.setShortlistCount(2);
        jobA.setMinScoreThreshold(new BigDecimal("70.00"));
        jobA.setWeightSkillCoverage(new BigDecimal("40.00"));
        jobA.setWeightExperienceRelevance(new BigDecimal("25.00"));
        jobA.setWeightProjectRelevance(new BigDecimal("15.00"));
        jobA.setWeightSemanticSimilarity(new BigDecimal("20.00"));
        jobA.addSkill(new JobSkill("Java", true, 4));
        jobA.addSkill(new JobSkill("React", false, 2));
        jobA = jobRepository.save(jobA);

        // 4. Create Candidates in Org A
        candA1 = candidateRepository.save(new Candidate(
                orgA, "Candidate Alpha", "alpha@test.com", "555-0001",
                "Java and React developer with 5 years experience", "Summary", "Java, React, Spring Boot",
                "BS CS", "5 years experience", "Project One", "resume1.pdf"
        ));

        candA2 = candidateRepository.save(new Candidate(
                orgA, "Candidate Beta", "beta@test.com", "555-0002",
                "Junior developer with Python experience", "Summary", "Python",
                "BS CS", "1 year experience", "Project Two", "resume2.pdf"
        ));

        // Mock AI service response
        AiMatchResponse.AiScoreBreakdown b1 = new AiMatchResponse.AiScoreBreakdown(85.0, 95.0, 100.0, 80.0, 90.0);
        AiMatchResponse.AiCandidateMatchResult r1 = new AiMatchResponse.AiCandidateMatchResult(
                String.valueOf(candA1.getId()), "Candidate Alpha", 90.0, b1, true,
                List.of("Java", "React"), Collections.emptyList(), Collections.emptyList(),
                Map.of("Java", "5 years experience with Spring Boot"), Collections.emptyList()
        );

        AiMatchResponse.AiScoreBreakdown b2 = new AiMatchResponse.AiScoreBreakdown(60.0, 30.0, 25.0, 40.0, 45.0);
        AiMatchResponse.AiCandidateMatchResult r2 = new AiMatchResponse.AiCandidateMatchResult(
                String.valueOf(candA2.getId()), "Candidate Beta", 45.0, b2, false,
                Collections.emptyList(), List.of("Java"), Collections.emptyList(),
                Collections.emptyMap(), List.of("Missing mandatory requirement: Java")
        );

        AiMatchResponse mockAiResp = new AiMatchResponse(
                String.valueOf(jobA.getId()), 2, 1, 1, List.of(r1, r2), Map.of("model", "all-MiniLM-L6-v2")
        );
        when(aiServiceClient.retrieveMatches(any(AiMatchRequest.class))).thenReturn(mockAiResp);
    }

    @Test
    @DisplayName("Recruiter can execute match run, view explainable ranking, and inspect score breakdown")
    void testExecuteMatchRunAndInspectBreakdown() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobA.getId() + "/matches/run")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runNumber").value(1))
                .andExpect(jsonPath("$.data.totalCandidatesEvaluated").value(2))
                .andExpect(jsonPath("$.data.eligibleCandidatesCount").value(1))
                .andExpect(jsonPath("$.data.shortlistedCount").value(1))
                .andExpect(jsonPath("$.data.matches", hasSize(2)))
                .andExpect(jsonPath("$.data.matches[0].candidateName").value("Candidate Alpha"))
                .andExpect(jsonPath("$.data.matches[0].finalScore").value(90.0))
                .andExpect(jsonPath("$.data.matches[0].eligible").value(true))
                .andExpect(jsonPath("$.data.matches[0].shortlisted").value(true))
                .andExpect(jsonPath("$.data.matches[0].evidenceSnippets.Java").exists())
                .andExpect(jsonPath("$.data.matches[1].candidateName").value("Candidate Beta"))
                .andExpect(jsonPath("$.data.matches[1].eligible").value(false))
                .andExpect(jsonPath("$.data.matches[1].shortlisted").value(false));

        // Verify match run was persisted in DB
        assertThat(matchRunRepository.countByJobIdAndOrganizationId(jobA.getId(), orgA.getId())).isEqualTo(1);
        assertThat(candidateMatchRepository.findAll()).hasSize(2);
        assertThat(applicationRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Recruiter can transition workflow stages with valid state policy and view timeline")
    void testRecruitmentWorkflowStageTransitions() throws Exception {
        // Create initial application in NEW stage
        JobApplication app = applicationRepository.save(new JobApplication(jobA, candA1, orgA, ApplicationStage.NEW));

        // Transition NEW -> REVIEWING
        UpdateStageRequest req1 = new UpdateStageRequest(ApplicationStage.REVIEWING, "INITIAL_SCREEN", "Moving to review");
        mockMvc.perform(patch("/api/jobs/" + jobA.getId() + "/applications/" + app.getId() + "/stage")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("REVIEWING"))
                .andExpect(jsonPath("$.data.decisionReason").value("INITIAL_SCREEN"));

        // Transition REVIEWING -> INTERVIEW
        UpdateStageRequest req2 = new UpdateStageRequest(ApplicationStage.INTERVIEW, "TECH_SCREEN_PASSED", "Passed tech screen");
        mockMvc.perform(patch("/api/jobs/" + jobA.getId() + "/applications/" + app.getId() + "/stage")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("INTERVIEW"));

        // Add recruiter note
        AddNoteRequest noteReq = new AddNoteRequest("Candidate demonstrated exceptional Spring Boot knowledge in system design interview", "STRONG_SKILLS");
        mockMvc.perform(post("/api/jobs/" + jobA.getId() + "/applications/" + app.getId() + "/notes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.noteText").value(containsString("exceptional Spring Boot knowledge")))
                .andExpect(jsonPath("$.data.authorName").value("Alice Recruiter"));

        // Fetch notes timeline
        mockMvc.perform(get("/api/jobs/" + jobA.getId() + "/applications/" + app.getId() + "/notes")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3))); // 2 from stage updates, 1 explicit note
    }

    @Test
    @DisplayName("Invalid workflow stage transitions are rejected with 400 Bad Request")
    void testInvalidWorkflowTransitionRejected() throws Exception {
        JobApplication app = applicationRepository.save(new JobApplication(jobA, candA1, orgA, ApplicationStage.NEW));

        // Invalid: Direct transition from NEW to HIRED is not allowed
        UpdateStageRequest req = new UpdateStageRequest(ApplicationStage.HIRED, "DIRECT_OFFER", null);
        mockMvc.perform(patch("/api/jobs/" + jobA.getId() + "/applications/" + app.getId() + "/stage")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid workflow state transition")));
    }

    @Test
    @DisplayName("Recruiter can toggle candidate shortlist status explicitly")
    void testToggleShortlistStatus() throws Exception {
        JobApplication app = applicationRepository.save(new JobApplication(jobA, candA1, orgA, ApplicationStage.NEW));
        assertThat(app.isShortlisted()).isFalse();

        // Toggle 1: Add to shortlist
        mockMvc.perform(patch("/api/jobs/" + jobA.getId() + "/applications/" + app.getId() + "/shortlist")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortlisted").value(true))
                .andExpect(jsonPath("$.data.stage").value("SHORTLISTED"));

        // Toggle 2: Remove from shortlist
        mockMvc.perform(patch("/api/jobs/" + jobA.getId() + "/applications/" + app.getId() + "/shortlist")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortlisted").value(false))
                .andExpect(jsonPath("$.data.stage").value("REVIEWING"));
    }

    @Test
    @DisplayName("Candidate comparison endpoint returns side-by-side attributes")
    void testCandidateComparison() throws Exception {
        CompareCandidatesRequest req = new CompareCandidatesRequest(List.of(candA1.getId(), candA2.getId()));

        mockMvc.perform(post("/api/jobs/" + jobA.getId() + "/compare")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobTitle").value("Senior Full-Stack Engineer"))
                .andExpect(jsonPath("$.data.candidates", hasSize(2)))
                .andExpect(jsonPath("$.data.candidates[0].candidateName").value("Candidate Alpha"))
                .andExpect(jsonPath("$.data.candidates[1].candidateName").value("Candidate Beta"));
    }

    @Test
    @DisplayName("Tenant isolation rejects recruiter from accessing jobs and applications in another organization")
    void testTenantIsolationCrossOrgAccessForbidden() throws Exception {
        // Recruiter B from Org B tries to access Job A from Org A
        mockMvc.perform(get("/api/jobs/" + jobA.getId() + "/applications")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/jobs/" + jobA.getId() + "/matches/run")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }
}
