package com.talentmatch.resume.repository;

import com.talentmatch.resume.entity.ResumeFile;
import com.talentmatch.resume.entity.ResumeFileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeFileRepository extends JpaRepository<ResumeFile, Long> {

    List<ResumeFile> findByBatchId(Long batchId);

    Optional<ResumeFile> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<ResumeFile> findFirstByOrganizationIdAndContentHash(Long organizationId, String contentHash);

    List<ResumeFile> findByStatusIn(Collection<ResumeFileStatus> statuses);

    long countByBatchIdAndStatus(Long batchId, ResumeFileStatus status);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, ResumeFileStatus status);
}
