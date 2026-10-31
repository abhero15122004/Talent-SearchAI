package com.talentmatch.workflow;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.common.exception.BadRequestException;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.job.entity.Job;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.workflow.dto.AddNoteRequest;
import com.talentmatch.workflow.dto.JobApplicationDto;
import com.talentmatch.workflow.dto.JobApplicationNoteDto;
import com.talentmatch.workflow.dto.UpdateStageRequest;
import com.talentmatch.workflow.entity.ApplicationStage;
import com.talentmatch.workflow.entity.JobApplication;
import com.talentmatch.workflow.entity.JobApplicationNote;
import com.talentmatch.workflow.repository.JobApplicationNoteRepository;
import com.talentmatch.workflow.repository.JobApplicationRepository;
import com.talentmatch.workflow.service.RecruitmentWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruitmentWorkflowServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private JobApplicationNoteRepository noteRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RecruitmentWorkflowService workflowService;

    private Organization org1;
    private Organization org2;
    private User recruiter1;
    private Job job1;
    private Candidate candidate1;
    private JobApplication app1;

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
        job1.setTitle("Backend Engineer");

        candidate1 = new Candidate(org1, "John Candidate", "john@example.com", "555-1234", "Resume", "Summary", "Java", "BS", "Exp", "Proj", "res.pdf");
        setEntityId(candidate1, 501L);

        app1 = new JobApplication(job1, candidate1, org1, ApplicationStage.NEW);
        setEntityId(app1, 2001L);
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
    void testTenantIsolationRejectsCrossOrganizationAccess() {
        // Job belongs to org2
        job1.setOrganization(org2);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job1));

        assertThrows(AccessDeniedException.class, () ->
                workflowService.getJobApplications(100L, null, null, null, recruiter1));

        verifyNoInteractions(applicationRepository);
    }

    @Test
    void testValidStageTransition() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job1));
        when(applicationRepository.findByIdAndOrganizationId(2001L, 1L)).thenReturn(Optional.of(app1));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateStageRequest request = new UpdateStageRequest(ApplicationStage.REVIEWING, "INITIAL_SCREEN", "Candidate looks promising");
        JobApplicationDto result = workflowService.updateApplicationStage(100L, 2001L, request, recruiter1);

        assertNotNull(result);
        assertEquals(ApplicationStage.REVIEWING, result.stage());
        assertEquals("INITIAL_SCREEN", result.decisionReason());

        verify(auditLogService).recordEvent(
                eq(1L), eq(10L), eq("recruiter@acme.com"), eq("APPLICATION_STAGE_CHANGED"),
                eq("JobApplication"), eq("2001"), contains("NEW to REVIEWING"), anyString()
        );
    }

    @Test
    void testInvalidStageTransitionRejection() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job1));
        when(applicationRepository.findByIdAndOrganizationId(2001L, 1L)).thenReturn(Optional.of(app1));

        // Invalid: Direct transition from NEW to HIRED is not allowed by state policy
        UpdateStageRequest request = new UpdateStageRequest(ApplicationStage.HIRED, "DIRECT_OFFER", null);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                workflowService.updateApplicationStage(100L, 2001L, request, recruiter1));

        assertTrue(ex.getMessage().contains("Invalid workflow state transition"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void testToggleShortlist() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job1));
        when(applicationRepository.findByIdAndOrganizationId(2001L, 1L)).thenReturn(Optional.of(app1));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        // Initial state is not shortlisted, toggle should shortlist and promote from NEW to SHORTLISTED
        assertFalse(app1.isShortlisted());
        JobApplicationDto result1 = workflowService.toggleShortlist(100L, 2001L, recruiter1);
        assertTrue(result1.shortlisted());
        assertEquals(ApplicationStage.SHORTLISTED, result1.stage());

        // Second toggle removes from shortlist and moves to REVIEWING
        JobApplicationDto result2 = workflowService.toggleShortlist(100L, 2001L, recruiter1);
        assertFalse(result2.shortlisted());
        assertEquals(ApplicationStage.REVIEWING, result2.stage());
    }

    @Test
    void testAddAndGetNotes() {
        when(jobRepository.findById(100L)).thenReturn(Optional.of(job1));
        when(applicationRepository.findByIdAndOrganizationId(2001L, 1L)).thenReturn(Optional.of(app1));

        JobApplicationNote note = new JobApplicationNote(app1, org1, recruiter1, "Solid Java background", "NEW", "SKILL_CHECK");
        setEntityId(note, 3001L);

        when(noteRepository.save(any(JobApplicationNote.class))).thenReturn(note);
        when(noteRepository.findByApplicationIdAndOrganizationIdOrderByCreatedAtDesc(2001L, 1L))
                .thenReturn(List.of(note));

        JobApplicationNoteDto noteDto = workflowService.addApplicationNote(100L, 2001L,
                new AddNoteRequest("Solid Java background", "SKILL_CHECK"), recruiter1);

        assertNotNull(noteDto);
        assertEquals("Solid Java background", noteDto.noteText());
        assertEquals("Jane Recruiter", noteDto.authorName());

        List<JobApplicationNoteDto> notes = workflowService.getApplicationNotes(100L, 2001L, recruiter1);
        assertEquals(1, notes.size());
        assertEquals(3001L, notes.get(0).id());
    }
}
