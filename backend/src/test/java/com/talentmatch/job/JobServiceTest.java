package com.talentmatch.job;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.common.exception.ApiException;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.job.dto.*;
import com.talentmatch.job.entity.*;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.job.service.JobServiceImpl;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private JobServiceImpl jobService;

    private Organization testOrg;
    private User testUser;
    private CreateJobRequest validRequest;

    @BeforeEach
    void setUp() {
        testOrg = new Organization("Acme Corp", "acme-corp", "acme.com");
        testOrg.setId(1L);

        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("recruiter@acme.com");
        testUser.setFullName("Jane Recruiter");
        testUser.setRole(Role.RECRUITER);
        testUser.setOrganization(testOrg);

        validRequest = new CreateJobRequest();
        validRequest.setTitle("Senior Full-Stack Engineer");
        validRequest.setDepartment("Engineering");
        validRequest.setLocation("Remote, US");
        validRequest.setEmploymentType(EmploymentType.FULL_TIME);
        validRequest.setDescription("Looking for a skilled engineer with Java and React expertise.");
        validRequest.setMinYearsExperience(3);
        validRequest.setMaxYearsExperience(7);
        validRequest.setShortlistCount(15);
        validRequest.setMinScoreThreshold(new BigDecimal("70.00"));
        validRequest.setWeightSkillCoverage(new BigDecimal("40.00"));
        validRequest.setWeightExperienceRelevance(new BigDecimal("25.00"));
        validRequest.setWeightProjectRelevance(new BigDecimal("15.00"));
        validRequest.setWeightSemanticSimilarity(new BigDecimal("20.00"));

        JobSkillDto skill1 = new JobSkillDto(null, "Java", true, 3);
        JobSkillDto skill2 = new JobSkillDto(null, "React", false, 2);
        validRequest.setSkills(List.of(skill1, skill2));

        JobQualificationDto qual = new JobQualificationDto(null, QualificationType.EDUCATION, "B.S. in Computer Science", true);
        validRequest.setQualifications(List.of(qual));
    }

    @Test
    void testCreateJob_Success() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(100L);
            return j;
        });

        JobResponse response = jobService.createJob(validRequest, 1L, 10L);

        assertNotNull(response);
        assertEquals("Senior Full-Stack Engineer", response.getTitle());
        assertEquals("Engineering", response.getDepartment());
        assertEquals(2, response.getSkills().size());
        assertEquals(1, response.getQualifications().size());
        assertEquals(JobStatus.DRAFT, response.getStatus());

        verify(jobRepository, times(1)).save(any(Job.class));
        verify(auditLogService, times(1)).recordEvent(
                eq(1L), eq(10L), eq("recruiter@acme.com"), eq("JOB_CREATED"), eq("Job"), eq("100"), anyString(), isNull()
        );
    }

    @Test
    void testCreateJob_InvalidWeightsSum_ThrowsApiException() {
        // Sum = 40 + 25 + 15 + 10 = 90 (invalid, must be 100)
        validRequest.setWeightSemanticSimilarity(new BigDecimal("10.00"));

        ApiException exception = assertThrows(ApiException.class, () ->
                jobService.createJob(validRequest, 1L, 10L)
        );

        assertTrue(exception.getMessage().contains("must sum exactly to 100.00%"));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void testCreateJob_InvalidExperienceRange_ThrowsApiException() {
        validRequest.setMinYearsExperience(5);
        validRequest.setMaxYearsExperience(2); // max < min

        ApiException exception = assertThrows(ApiException.class, () ->
                jobService.createJob(validRequest, 1L, 10L)
        );

        assertTrue(exception.getMessage().contains("Maximum experience years cannot be less than minimum"));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void testUpdateJobStatus_Success() {
        Job existingJob = new Job();
        existingJob.setId(100L);
        existingJob.setOrganization(testOrg);
        existingJob.setCreatedByUser(testUser);
        existingJob.setStatus(JobStatus.DRAFT);
        existingJob.setTitle("Backend Lead");

        when(jobRepository.findByIdAndOrganizationId(100L, 1L)).thenReturn(Optional.of(existingJob));
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(jobRepository.save(any(Job.class))).thenReturn(existingJob);

        JobStatusUpdateRequest request = new JobStatusUpdateRequest(JobStatus.OPEN);
        JobResponse response = jobService.updateJobStatus(100L, request, 1L, 10L);

        assertEquals(JobStatus.OPEN, response.getStatus());
        verify(auditLogService, times(1)).recordEvent(
                eq(1L), eq(10L), eq("recruiter@acme.com"), eq("JOB_STATUS_CHANGED"), eq("Job"), eq("100"), anyString(), isNull()
        );
    }

    @Test
    void testArchiveJob_Success() {
        Job existingJob = new Job();
        existingJob.setId(100L);
        existingJob.setOrganization(testOrg);
        existingJob.setCreatedByUser(testUser);
        existingJob.setStatus(JobStatus.OPEN);
        existingJob.setTitle("Backend Lead");

        when(jobRepository.findByIdAndOrganizationId(100L, 1L)).thenReturn(Optional.of(existingJob));
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(jobRepository.save(any(Job.class))).thenReturn(existingJob);

        jobService.archiveJob(100L, 1L, 10L);

        assertEquals(JobStatus.ARCHIVED, existingJob.getStatus());
        verify(auditLogService, times(1)).recordEvent(
                eq(1L), eq(10L), eq("recruiter@acme.com"), eq("JOB_ARCHIVED"), eq("Job"), eq("100"), anyString(), isNull()
        );
    }

    @Test
    void testGetJobById_NotFound_ThrowsResourceNotFoundException() {
        when(jobRepository.findByIdAndOrganizationId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                jobService.getJobById(999L, 1L)
        );
    }
}
