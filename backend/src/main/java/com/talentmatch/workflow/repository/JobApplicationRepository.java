package com.talentmatch.workflow.repository;

import com.talentmatch.workflow.entity.ApplicationStage;
import com.talentmatch.workflow.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByJobIdAndCandidateIdAndOrganizationId(Long jobId, Long candidateId, Long organizationId);

    Optional<JobApplication> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<JobApplication> findByJobIdAndOrganizationId(Long jobId, Long organizationId, Pageable pageable);

    Page<JobApplication> findByJobIdAndOrganizationIdAndStage(Long jobId, Long organizationId, ApplicationStage stage, Pageable pageable);

    List<JobApplication> findByJobIdAndOrganizationId(Long jobId, Long organizationId);

    @Query("SELECT a FROM JobApplication a WHERE a.job.id = :jobId AND a.organization.id = :orgId AND " +
            "(:stage IS NULL OR a.stage = :stage) AND " +
            "(:search IS NULL OR LOWER(a.candidate.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.candidate.primarySkills) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<JobApplication> searchApplications(
            @Param("jobId") Long jobId,
            @Param("orgId") Long orgId,
            @Param("stage") ApplicationStage stage,
            @Param("search") String search,
            Pageable pageable
    );

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStage(Long organizationId, ApplicationStage stage);

    long countByOrganizationIdAndShortlisted(Long organizationId, boolean shortlisted);

    int countByJobIdAndOrganizationId(Long jobId, Long organizationId);

    int countByJobIdAndOrganizationIdAndShortlisted(Long jobId, Long organizationId, boolean shortlisted);
}
