package com.talentmatch.matching.repository;

import com.talentmatch.matching.entity.JobMatchRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobMatchRunRepository extends JpaRepository<JobMatchRun, Long> {

    Optional<JobMatchRun> findFirstByJobIdAndOrganizationIdOrderByRunNumberDesc(Long jobId, Long organizationId);

    Optional<JobMatchRun> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<JobMatchRun> findByJobIdAndOrganizationIdOrderByRunNumberDesc(Long jobId, Long organizationId, Pageable pageable);

    int countByJobIdAndOrganizationId(Long jobId, Long organizationId);

    long countByOrganizationId(Long organizationId);
}
