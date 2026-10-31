package com.talentmatch.workflow.repository;

import com.talentmatch.workflow.entity.JobApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationNoteRepository extends JpaRepository<JobApplicationNote, Long> {

    List<JobApplicationNote> findByApplicationIdAndOrganizationIdOrderByCreatedAtDesc(Long applicationId, Long organizationId);
}
