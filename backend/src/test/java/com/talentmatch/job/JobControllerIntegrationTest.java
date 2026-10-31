package com.talentmatch.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmatch.job.dto.CreateJobRequest;
import com.talentmatch.job.dto.JobQualificationDto;
import com.talentmatch.job.dto.JobSkillDto;
import com.talentmatch.job.dto.JobStatusUpdateRequest;
import com.talentmatch.job.entity.EmploymentType;
import com.talentmatch.job.entity.Job;
import com.talentmatch.job.entity.JobStatus;
import com.talentmatch.job.entity.QualificationType;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.security.JwtTokenProvider;
import com.talentmatch.security.UserPrincipal;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Organization orgA;
    private Organization orgB;
    private User recruiterA;
    private User recruiterB;
    private String recruiterAToken;
    private String recruiterBToken;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Org A & Recruiter A
        orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp", "alpha.com"));
        recruiterA = userRepository.save(new User(
                "recruiterA@alpha.com",
                passwordEncoder.encode("Pass123!"),
                "Alice Recruiter",
                Role.RECRUITER,
                orgA
        ));
        recruiterAToken = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(recruiterA));

        // 2. Create Org B & Recruiter B
        orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc", "beta.com"));
        recruiterB = userRepository.save(new User(
                "recruiterB@beta.com",
                passwordEncoder.encode("Pass123!"),
                "Bob Recruiter",
                Role.RECRUITER,
                orgB
        ));
        recruiterBToken = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(recruiterB));
    }

    private CreateJobRequest buildValidCreateJobRequest() {
        CreateJobRequest request = new CreateJobRequest();
        request.setTitle("Senior Cloud Architect");
        request.setDepartment("Cloud Platform");
        request.setLocation("San Francisco, CA");
        request.setEmploymentType(EmploymentType.FULL_TIME);
        request.setDescription("Design and implement high-availability cloud architecture.");
        request.setMinYearsExperience(5);
        request.setMaxYearsExperience(10);
        request.setStatus(JobStatus.DRAFT);
        request.setShortlistCount(10);
        request.setMinScoreThreshold(new BigDecimal("75.00"));
        request.setWeightSkillCoverage(new BigDecimal("40.00"));
        request.setWeightExperienceRelevance(new BigDecimal("25.00"));
        request.setWeightProjectRelevance(new BigDecimal("15.00"));
        request.setWeightSemanticSimilarity(new BigDecimal("20.00"));

        request.setSkills(List.of(
                new JobSkillDto(null, "Kubernetes", true, 3),
                new JobSkillDto(null, "AWS", true, 4),
                new JobSkillDto(null, "Terraform", false, 2)
        ));

        request.setQualifications(List.of(
                new JobQualificationDto(null, QualificationType.CERTIFICATION, "AWS Certified Solutions Architect", true)
        ));

        return request;
    }

    @Test
    @DisplayName("POST /api/jobs - Recruiter creates job requisition successfully")
    void testCreateJob_Success() throws Exception {
        CreateJobRequest request = buildValidCreateJobRequest();

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + recruiterAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Senior Cloud Architect")))
                .andExpect(jsonPath("$.data.department", is("Cloud Platform")))
                .andExpect(jsonPath("$.data.skills", hasSize(3)))
                .andExpect(jsonPath("$.data.qualifications", hasSize(1)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.organizationId", is(orgA.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/jobs - Reject when matching weights do not sum to 100.00%")
    void testCreateJob_InvalidWeightsSum_Returns400() throws Exception {
        CreateJobRequest request = buildValidCreateJobRequest();
        // Sum = 40 + 25 + 15 + 10 = 90% (invalid)
        request.setWeightSemanticSimilarity(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + recruiterAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.validationErrors.weightsSumValid", containsString("weights")));
    }

    @Test
    @DisplayName("POST /api/jobs - Reject when title is blank")
    void testCreateJob_BlankTitle_Returns400() throws Exception {
        CreateJobRequest request = buildValidCreateJobRequest();
        request.setTitle("   ");

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + recruiterAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/jobs - Recruiter lists jobs with pagination and filters")
    void testListJobs_PaginationAndFiltering() throws Exception {
        // Create 2 jobs for Org A
        CreateJobRequest req1 = buildValidCreateJobRequest();
        req1.setTitle("Backend Engineer");
        req1.setStatus(JobStatus.OPEN);

        CreateJobRequest req2 = buildValidCreateJobRequest();
        req2.setTitle("Frontend Architect");
        req2.setStatus(JobStatus.DRAFT);

        mockMvc.perform(post("/api/jobs")
                .header("Authorization", "Bearer " + recruiterAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)));

        mockMvc.perform(post("/api/jobs")
                .header("Authorization", "Bearer " + recruiterAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)));

        // Filter by OPEN status
        mockMvc.perform(get("/api/jobs?status=OPEN")
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("Backend Engineer")));

        // Search by keyword "Architect"
        mockMvc.perform(get("/api/jobs?search=Architect")
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("Frontend Architect")));
    }

    @Test
    @DisplayName("PATCH /api/jobs/{id}/status - Update job status to OPEN")
    void testUpdateJobStatus_Success() throws Exception {
        CreateJobRequest req = buildValidCreateJobRequest();
        String createResponse = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + recruiterAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer jobId = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        JobStatusUpdateRequest statusReq = new JobStatusUpdateRequest(JobStatus.OPEN);

        mockMvc.perform(patch("/api/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + recruiterAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("OPEN")));
    }

    @Test
    @DisplayName("Multi-tenant isolation: Recruiter B cannot access Recruiter A's job")
    void testTenantIsolation_RecruiterBlockedFromOtherTenantJob() throws Exception {
        // Create job for Org A
        CreateJobRequest req = buildValidCreateJobRequest();
        String createResponse = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + recruiterAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer jobId = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        // Recruiter B from Org B tries to access Org A's job
        mockMvc.perform(get("/api/jobs/" + jobId)
                        .header("Authorization", "Bearer " + recruiterBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
