package com.talentmatch.job.repository;

import com.talentmatch.job.entity.Job;
import com.talentmatch.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<Job> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<Job> findByOrganizationIdAndStatus(Long organizationId, JobStatus status, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.organization.id = :orgId " +
           "AND (:status IS NULL OR j.status = :status) " +
           "AND (:search IS NULL OR TRIM(:search) = '' OR " +
           "LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.location) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Job> searchJobs(
            @Param("orgId") Long orgId,
            @Param("status") JobStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, JobStatus status);
}
